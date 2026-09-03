package com.transcriptor.hid.engine

/**
 * Configuration for Enter / Newline submission behavior.
 */
enum class NewlineSubmissionMode {
    /** Standard Enter (KEY_ENTER: 0x28, MOD_NONE) - Mandatory for Bash, Zsh, PowerShell, SSH, Vim, Nano */
    TERMINAL_ENTER,
    /** Soft-Enter (MOD_LSHIFT | KEY_ENTER: 0x28) - Designed for Slack, Teams, Discord, Claude Web UI */
    CHAT_SOFT_ENTER
}

/**
 * Interface for translating Unicode characters and strings into HID keystrokes
 * based on a specific keyboard layout.
 */
interface KeymapTranslator {
    val layout: KeyLayout

    var newlineMode: NewlineSubmissionMode
        get() = NewlineSubmissionMode.TERMINAL_ENTER
        set(_) {}

    /**
     * Translates a single character into one or more HID keystrokes.
     * Dead keys will return two keystrokes (dead key + space).
     * Unmapped characters return an empty list.
     */
    fun translateChar(char: Char): List<HidKeyStroke>

    /**
     * Translates a full string into a sequence of HID keystrokes.
     * Performs NFC normalization and character transliteration before translation.
     */
    fun translateString(text: String): List<HidKeyStroke>

    companion object {
        fun create(
            layout: KeyLayout,
            newlineMode: NewlineSubmissionMode = NewlineSubmissionMode.TERMINAL_ENTER
        ): KeymapTranslator {
            return when (layout) {
                KeyLayout.US_QWERTY -> UsQwertyKeymap(newlineMode = newlineMode)
                KeyLayout.GERMAN_QWERTZ -> GermanQwertzKeymap(newlineMode = newlineMode)
            }
        }
    }
}
