package com.transcriptor.hid.engine

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keystroke Dispatcher & Transmission Engine interface.
 */
interface KeystrokeDispatcher {
    /**
     * Observable state of the text currently rendered on the host editor.
     */
    val currentHostText: StateFlow<String>

    /**
     * Types an entire text string in fast buffered burst mode with deterministic pacing.
     *
     * @param text The text to type.
     * @param delayMs Total inter-character cycle duration in milliseconds (default 8ms).
     */
    suspend fun dispatchBurst(text: String, delayMs: Long = 8L)

    /**
     * Updates the host editor to match [newHypothesis] by computing the minimal LCP diff,
     * emitting backspaces, and typing the remaining suffix.
     *
     * @param newHypothesis The updated speech hypothesis text.
     * @param delayMs Total inter-character cycle duration in milliseconds.
     */
    suspend fun dispatchLiveDiff(newHypothesis: String, delayMs: Long = 8L)

    /**
     * Transmits a list of raw HID keystrokes (e.g. terminal hotkeys or special commands)
     * with deterministic key-down / key-up duty cycle pacing.
     */
    suspend fun sendRawKeyStrokes(keyStrokes: List<HidKeyStroke>, delayMs: Long = 8L)

    /**
     * Streams clipboard text to the host PC. Optionally wraps the stream in ANSI
     * bracketed paste mode sequences (`\x1b[200~` and `\x1b[201~`) to prevent Vim/Zsh staircase indentation.
     */
    suspend fun streamClipboardToHost(
        clipText: String,
        bracketedPaste: Boolean = false,
        delayMs: Long = 8L
    )

    /**
     * Resets the internal host text tracking state to empty string (e.g. on clear or editor wipe).
     */
    fun resetState()

    companion object {
        fun create(
            translator: KeymapTranslator,
            deltaDiffEngine: DeltaDiffEngine = DeltaDiffEngine.create(),
            reportSender: suspend (ByteArray) -> Boolean = { true },
            newlineDelayMs: Long = 30L
        ): KeystrokeDispatcher =
            DefaultKeystrokeDispatcher(
                translator = translator,
                deltaDiffEngine = deltaDiffEngine,
                reportSender = reportSender,
                newlineDelayMs = newlineDelayMs
            )
    }
}

/**
 * Default coroutine-driven implementation of [KeystrokeDispatcher].
 *
 * Enforces serialized transmission, deterministic key-down ($t_{down}$) and key-up ($t_{up}$)
 * pacing (8ms duty cycle by default), inter-line delay for Enter keys, and NonCancellable emergency
 * zero release report guard to prevent stuck keys on host OS input queues.
 */
class DefaultKeystrokeDispatcher(
    @Volatile var translator: KeymapTranslator,
    val deltaDiffEngine: DeltaDiffEngine = DefaultDeltaDiffEngine(),
    private val reportSender: suspend (ByteArray) -> Boolean = { true },
    @Volatile var newlineDelayMs: Long = 30L
) : KeystrokeDispatcher {

    private val mutex = Mutex()
    private val _currentHostText = MutableStateFlow("")
    override val currentHostText: StateFlow<String> = _currentHostText.asStateFlow()

    override suspend fun dispatchBurst(text: String, delayMs: Long) {
        if (text.isEmpty()) return
        mutex.withLock {
            val strokes = translator.translateString(text)
            transmitStrokesInternal(strokes, delayMs)
            _currentHostText.value += text
        }
    }

    override suspend fun dispatchLiveDiff(newHypothesis: String, delayMs: Long) {
        mutex.withLock {
            val diff = deltaDiffEngine.computeDiff(_currentHostText.value, newHypothesis)

            // Emit required backspaces
            if (diff.backspacesNeeded > 0) {
                val backspaceStrokes = translator.translateChar('\b')
                val stroke = backspaceStrokes.firstOrNull()
                    ?: HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
                val bsList = List(diff.backspacesNeeded) { stroke }
                transmitStrokesInternal(bsList, delayMs)
            }

            // Emit new suffix to append
            if (diff.textToAppend.isNotEmpty()) {
                val appendStrokes = translator.translateString(diff.textToAppend)
                transmitStrokesInternal(appendStrokes, delayMs)
            }

            _currentHostText.value = newHypothesis
        }
    }

    override suspend fun sendRawKeyStrokes(keyStrokes: List<HidKeyStroke>, delayMs: Long) {
        if (keyStrokes.isEmpty()) return
        mutex.withLock {
            transmitStrokesInternal(keyStrokes, delayMs)
        }
    }

    override suspend fun streamClipboardToHost(
        clipText: String,
        bracketedPaste: Boolean,
        delayMs: Long
    ) {
        if (clipText.isEmpty()) return
        mutex.withLock {
            val strokes = mutableListOf<HidKeyStroke>()

            if (bracketedPaste) {
                // Bracketed paste start: \x1b[200~
                strokes.add(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_ESCAPE))
                strokes.addAll(translator.translateString("[200~"))
            }

            // Payload
            strokes.addAll(translator.translateString(clipText))

            if (bracketedPaste) {
                // Bracketed paste end: \x1b[201~
                strokes.add(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_ESCAPE))
                strokes.addAll(translator.translateString("[201~"))
            }

            transmitStrokesInternal(strokes, delayMs)
            _currentHostText.value += clipText
        }
    }

    override fun resetState() {
        if (mutex.tryLock()) {
            try {
                _currentHostText.value = ""
            } finally {
                mutex.unlock()
            }
        } else {
            // Mutex is held by a suspended typing operation.
            // Do NOT block calling thread with runBlocking to prevent deadlocks on single-threaded dispatchers.
            // MutableStateFlow.value update is thread-safe and atomic.
            _currentHostText.value = ""
        }
    }

    /**
     * Transmits a list of keystrokes with key-down and key-up reports and deterministic pacing.
     * Enforces NonCancellable emergency zero-release report guard.
     */
    private suspend fun transmitStrokesInternal(strokes: List<HidKeyStroke>, delayMs: Long) {
        val tDown = if (delayMs > 0) maxOf(1L, delayMs / 2) else 0L
        val tUp = if (delayMs > 0) maxOf(1L, delayMs - tDown) else 0L

        var completedNormally = false
        try {
            for (stroke in strokes) {
                // 1. Key-Down Report
                val downReport = stroke.toKeyDownReport().toByteArray()
                reportSender(downReport)
                if (tDown > 0) {
                    delay(tDown)
                }

                // 2. Key-Up (Release) Report
                val upReport = HidKeyStroke.RELEASE_REPORT.toByteArray()
                reportSender(upReport)
                if (tUp > 0) {
                    delay(tUp)
                }

                // 3. Inter-line delay for Enter key to accommodate shell syntax highlighters
                if (delayMs > 0 && (stroke.usageId == HidConstants.KEY_ENTER || stroke.usageId == HidConstants.KEYPAD_ENTER)) {
                    if (newlineDelayMs > 0) {
                        delay(newlineDelayMs)
                    }
                }
            }
            completedNormally = true
        } finally {
            if (!completedNormally) {
                withContext(NonCancellable) {
                    val emergencyRelease = HidKeyStroke.RELEASE_REPORT.toByteArray()
                    reportSender(emergencyRelease)
                }
            }
        }
    }
}
