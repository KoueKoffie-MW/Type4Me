package com.transcriptor.hid.engine

/**
 * Interface for translating Unicode characters and strings into HID keystrokes
 * based on a specific keyboard layout.
 */
interface KeymapTranslator {
    val layout: KeyLayout

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
        fun create(layout: KeyLayout): KeymapTranslator {
            return when (layout) {
                KeyLayout.US_QWERTY -> UsQwertyKeymap()
                KeyLayout.GERMAN_QWERTZ -> GermanQwertzKeymap()
            }
        }
    }
}
