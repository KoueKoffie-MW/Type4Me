package com.transcriptor.hid.engine

import java.text.Normalizer

/**
 * Keymap translator for standard ANSI US QWERTY keyboard layout.
 * Covers all 95 printable ASCII characters (0x20..0x7E), standard control keys,
 * and smart punctuation transliteration with NFC Unicode normalization.
 */
class UsQwertyKeymap : KeymapTranslator {

    override val layout: KeyLayout = KeyLayout.US_QWERTY

    override fun translateChar(char: Char): List<HidKeyStroke> {
        val stroke = when (char) {
            // Control characters
            '\b' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
            '\t' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_TAB)
            '\n', '\r' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_ENTER) // Soft-enter (Shift+Enter) prevents premature agent submission

            // Space & Punctuation
            ' ' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            '!' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_1)
            '"' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_APOSTROPHE)
            '#' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_3)
            '$' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_4)
            '%' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_5)
            '&' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_7)
            '\'' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE)
            '(' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_9)
            ')' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_0)
            '*' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_8)
            '+' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_EQUAL)
            ',' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_COMMA)
            '-' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_MINUS)
            '.' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_DOT)
            '/' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SLASH)

            // Digits 0-9
            '0' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_0)
            '1' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_1)
            '2' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_2)
            '3' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_3)
            '4' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_4)
            '5' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_5)
            '6' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_6)
            '7' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_7)
            '8' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_8)
            '9' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_9)

            // Colon & Semicolon
            ':' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_SEMICOLON)
            ';' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SEMICOLON)
            '<' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_COMMA)
            '=' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_EQUAL)
            '>' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_DOT)
            '?' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_SLASH)
            '@' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_2)

            // Uppercase letters A-Z
            'A' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_A)
            'B' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_B)
            'C' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_C)
            'D' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_D)
            'E' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_E)
            'F' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_F)
            'G' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_G)
            'H' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_H)
            'I' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_I)
            'J' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_J)
            'K' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_K)
            'L' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_L)
            'M' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_M)
            'N' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_N)
            'O' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_O)
            'P' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_P)
            'Q' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_Q)
            'R' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_R)
            'S' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_S)
            'T' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_T)
            'U' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_U)
            'V' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_V)
            'W' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_W)
            'X' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_X)
            'Y' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_Y)
            'Z' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_Z)

            // Brackets & Symbols
            '[' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFTBRACE)
            '\\' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSLASH)
            ']' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_RIGHTBRACE)
            '^' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_6)
            '_' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_MINUS)
            '`' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE)

            // Lowercase letters a-z
            'a' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_A)
            'b' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_B)
            'c' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_C)
            'd' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_D)
            'e' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_E)
            'f' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_F)
            'g' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_G)
            'h' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_H)
            'i' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_I)
            'j' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_J)
            'k' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_K)
            'l' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_L)
            'm' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_M)
            'n' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_N)
            'o' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_O)
            'p' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_P)
            'q' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_Q)
            'r' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_R)
            's' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_S)
            't' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_T)
            'u' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_U)
            'v' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_V)
            'w' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_W)
            'x' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_X)
            'y' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_Y)
            'z' -> HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_Z)

            // Braces & Symbols
            '{' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_LEFTBRACE)
            '|' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_BACKSLASH)
            '}' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_RIGHTBRACE)
            '~' -> HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_GRAVE)

            else -> null
        }

        return if (stroke != null) listOf(stroke) else emptyList()
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
                    '“', '”', '«', '»' -> result.addAll(translateChar('"'))
                    '‘', '’' -> result.addAll(translateChar('\''))
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
                // Supplementary characters not representable in standard ASCII QWERTY
            }
            i += charCount
        }
        return result
    }
}
