package com.transcriptor.hid.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed interface MacroExecutionState {
    data object Idle : MacroExecutionState
    data class Running(val stepIndex: Int, val totalSteps: Int, val description: String) : MacroExecutionState
    data class PromptRequired(val stepIndex: Int, val promptAction: MacroAction.PromptVariable) : MacroExecutionState
    data object Success : MacroExecutionState
    data class Error(val message: String, val failedStepIndex: Int) : MacroExecutionState
}

class MacroRunner(
    private val keystrokeDispatcher: KeystrokeDispatcher,
    private val reportSender: suspend (ByteArray) -> Boolean
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _executionState = MutableStateFlow<MacroExecutionState>(MacroExecutionState.Idle)
    val executionState: StateFlow<MacroExecutionState> = _executionState.asStateFlow()

    suspend fun execute(
        stepsJson: String,
        context: InterpolationContext = InterpolationContext()
    ) {
        val actions: List<MacroAction> = try {
            json.decodeFromString(stepsJson)
        } catch (e: Exception) {
            _executionState.value = MacroExecutionState.Error("Malformed macro JSON: ${e.message}", 0)
            return
        }

        if (actions.isEmpty()) {
            _executionState.value = MacroExecutionState.Success
            return
        }

        val totalSteps = actions.size
        for ((index, action) in actions.withIndex()) {
            try {
                _executionState.value = MacroExecutionState.Running(index + 1, totalSteps, action.javaClass.simpleName)

                when (action) {
                    is MacroAction.TypeString -> {
                        val (resolvedText, backtrack) = VariableParser.evaluate(action.text, context)
                        keystrokeDispatcher.dispatchBurst(resolvedText, action.delayMs)
                        if (backtrack > 0) {
                            val leftArrow = HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFT)
                            keystrokeDispatcher.sendRawKeyStrokes(List(backtrack) { leftArrow }, action.delayMs)
                        }
                    }

                    is MacroAction.KeyCombination -> {
                        repeat(action.repeatCount) {
                            val stroke = HidKeyStroke(action.modifiers, action.usageId)
                            reportSender(stroke.toKeyDownReport().toByteArray())
                            delay(action.holdMs)
                            reportSender(HidKeyStroke.RELEASE_REPORT.toByteArray())
                            delay(action.holdMs)
                        }
                    }

                    is MacroAction.Delay -> {
                        delay(action.durationMs)
                    }

                    is MacroAction.PromptVariable -> {
                        val value = context.promptAnswers[action.variableName]
                            ?: context.promptAnswers[action.variableName.lowercase().replace(" ", "_")]
                            ?: action.defaultValue
                        if (value.isNotEmpty()) {
                            keystrokeDispatcher.dispatchBurst(value)
                        }
                    }

                    is MacroAction.ClipboardPaste -> {
                        if (action.streamAsKeystrokes && !context.clipboardText.isNullOrEmpty()) {
                            keystrokeDispatcher.dispatchBurst(context.clipboardText, action.delayMs)
                        } else {
                            val mod = if (context.hostOs.equals("MACOS", ignoreCase = true)) {
                                HidConstants.MOD_LGUI
                            } else {
                                HidConstants.MOD_LCTRL
                            }
                            val vStroke = HidKeyStroke(mod, HidConstants.KEY_V)
                            reportSender(vStroke.toKeyDownReport().toByteArray())
                            delay(20L)
                            reportSender(HidKeyStroke.RELEASE_REPORT.toByteArray())
                        }
                    }
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    reportSender(HidKeyStroke.RELEASE_REPORT.toByteArray())
                }
                _executionState.value = MacroExecutionState.Idle
                throw e
            } catch (e: Exception) {
                withContext(NonCancellable) {
                    reportSender(HidKeyStroke.RELEASE_REPORT.toByteArray())
                }
                _executionState.value = MacroExecutionState.Error(e.message ?: "Execution error", index)
                return
            }
        }

        _executionState.value = MacroExecutionState.Success
    }

    fun resetState() {
        _executionState.value = MacroExecutionState.Idle
    }
}
