package com.transcriptor.hid.engine

import java.text.Normalizer

/**
 * Keymap translator for German QWERTZ layout (DIN 2137-1 / ISO/IEC 9995).
 *
 * Implements:
 * - Y/Z physical key swap (Y at B01 -> KEY_Z 0x1D, Z at D06 -> KEY_Y 0x1C)
 * - German Umlauts: ä, ö, ü, Ä, Ö, Ü, ß, and capital sharp S (ẞ U+1E9E)
 * - AltGr third-level symbols: @, €, \, {, }, [, ], |, µ, ², ³
 * - Dead keys with automatic space injection: ^, ´, `, ~
 * - Smart quote and typography transliteration
 * - NFC Unicode composition normalization
 */
class GermanQwertzKeymap : KeymapTranslator {

    override val layout: KeyLayout = KeyLayout.GERMAN_QWERTZ

    override fun translateChar(char: Char): List<HidKeyStroke> {
        return when (char) {
            // Control characters
            '\b' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE))
            '\t' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_TAB))
            '\n', '\r' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_ENTER)) // Soft-enter (Shift+Enter) prevents premature agent submission

            // Space
            ' ' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE))

            // Dead keys: emit dead key stroke followed by Space (0x2C)
            '^' -> listOf(
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            '´' -> listOf(
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_EQUAL),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            '`' -> listOf(
                HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_EQUAL),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            '~' -> listOf(
                HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_RIGHTBRACE),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )

            // Punctuation & Standard Symbols
            '!' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_1))
            '"' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_2))
            '#' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_NON_US_HASH))
            '$' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_4))
            '%' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_5))
            '&' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_6))
            '\'' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_NON_US_HASH))
            '(' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_8))
            ')' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_9))
            '*' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_RIGHTBRACE))
            '+' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_RIGHTBRACE))
            ',' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_COMMA))
            '-' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SLASH))
            '.' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_DOT))
            '/' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_7))

            // Digits 0-9
            '0' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_0))
            '1' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_1))
            '2' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_2))
            '3' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_3))
            '4' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_4))
            '5' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_5))
            '6' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_6))
            '7' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_7))
            '8' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_8))
            '9' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_9))

            // Colon, Semicolon, Comparison
            ':' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_DOT))
            ';' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_COMMA))
            '<' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_NON_US_BACKSLASH))
            '=' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_0))
            '>' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_NON_US_BACKSLASH))
            '?' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_MINUS))
            '_' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_SLASH))

            // Special Symbols
            '§' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_3))
            '°' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_GRAVE))

            // AltGr Third-Level Symbols (Right Alt = 0x40)
            '@' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_Q))
            '€' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_E))
            '[' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_8))
            '\\' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_MINUS))
            ']' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_9))
            '{' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_7))
            '|' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_NON_US_BACKSLASH))
            '}' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_0))
            'µ' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_M))
            '²' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_2))
            '³' -> listOf(HidKeyStroke(HidConstants.MOD_RALT, HidConstants.KEY_3))

            // German Umlauts & Eszett
            'ä' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE))
            'Ä' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_APOSTROPHE))
            'ö' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SEMICOLON))
            'Ö' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_SEMICOLON))
            'ü' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFTBRACE))
            'Ü' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_LEFTBRACE))
            'ß' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_MINUS))
            'ẞ' -> listOf(HidKeyStroke(HidConstants.MOD_SHIFT_ALT_GR, HidConstants.KEY_MINUS))

            // Letters A-X (Standard QWERTZ mapping)
            'A' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_A))
            'B' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_B))
            'C' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_C))
            'D' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_D))
            'E' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_E))
            'F' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_F))
            'G' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_G))
            'H' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_H))
            'I' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_I))
            'J' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_J))
            'K' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_K))
            'L' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_L))
            'M' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_M))
            'N' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_N))
            'O' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_O))
            'P' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_P))
            'Q' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_Q))
            'R' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_R))
            'S' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_S))
            'T' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_T))
            'U' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_U))
            'V' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_V))
            'W' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_W))
            'X' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_X))

            'a' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_A))
            'b' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_B))
            'c' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_C))
            'd' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_D))
            'e' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_E))
            'f' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_F))
            'g' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_G))
            'h' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_H))
            'i' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_I))
            'j' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_J))
            'k' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_K))
            'l' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_L))
            'm' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_M))
            'n' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_N))
            'o' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_O))
            'p' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_P))
            'q' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_Q))
            'r' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_R))
            's' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_S))
            't' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_T))
            'u' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_U))
            'v' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_V))
            'w' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_W))
            'x' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_X))

            // Y and Z Physical Key Swap
            'y' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_Z)) // Physical Z key
            'Y' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_Z))
            'z' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_Y)) // Physical Y key
            'Z' -> listOf(HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_Y))

            else -> emptyList()
        }
    }

    override fun translateString(text: String): List<HidKeyStroke> {
        if (text.isEmpty()) return emptyList()
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        val result = mutableListOf<HidKeyStroke>()
        var i = 0
        while (i < normalized.length) {
            val codePoint = normalized.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            if (charCount == 1) {
                val ch = normalized[i]
                when (ch) {
                    '…' -> repeat(3) { result.addAll(translateChar('.')) }
                    '“', '”', '„', '«', '»' -> result.addAll(translateChar('"'))
                    '‘', '’', '‚' -> result.addAll(translateChar('\''))
                    '–', '—' -> result.addAll(translateChar('-'))
                    '\u00A0' -> result.addAll(translateChar(' '))
                    '\r' -> {
                        if (i + 1 >= normalized.length || normalized[i + 1] != '\n') {
                            result.addAll(translateChar('\n'))
                        }
                    }
                    else -> result.addAll(translateChar(ch))
                }
            } else {
                // Check supplementary characters if any
                if (codePoint == 0x1E9E) {
                    result.add(HidKeyStroke(HidConstants.MOD_SHIFT_ALT_GR, HidConstants.KEY_MINUS))
                }
            }
            i += charCount
        }
        return result
    }
}
