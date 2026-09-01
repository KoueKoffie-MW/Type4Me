package com.transcriptor.hid.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic base sealed interface for discrete macro actions.
 */
@Serializable
sealed interface MacroAction {

    @Serializable
    @SerialName("type_string")
    data class TypeString(
        val text: String,
        val delayMs: Long = 8L
    ) : MacroAction

    @Serializable
    @SerialName("key_combo")
    data class KeyCombination(
        val modifiers: Byte = HidConstants.MOD_NONE,
        val usageId: Byte,
        val repeatCount: Int = 1,
        val holdMs: Long = 20L
    ) : MacroAction

    @Serializable
    @SerialName("delay")
    data class Delay(
        val durationMs: Long
    ) : MacroAction

    @Serializable
    @SerialName("prompt_variable")
    data class PromptVariable(
        val variableName: String = "",
        val defaultValue: String = "",
        val promptLabel: String = ""
    ) : MacroAction

    @Serializable
    @SerialName("clipboard_paste")
    data class ClipboardPaste(
        val streamAsKeystrokes: Boolean = false,
        val delayMs: Long = 8L
    ) : MacroAction
}
