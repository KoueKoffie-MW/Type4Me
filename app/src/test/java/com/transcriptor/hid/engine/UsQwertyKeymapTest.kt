package com.transcriptor.hid.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsQwertyKeymapTest {

    private lateinit var keymap: UsQwertyKeymap

    @Before
    fun setUp() {
        keymap = UsQwertyKeymap()
    }

    @Test
    fun testLayoutIdentifier() {
        assertEquals(KeyLayout.US_QWERTY, keymap.layout)
    }

    @Test
    fun testControlKeys() {
        // Backspace
        val bs = keymap.translateChar('\b')
        assertEquals(1, bs.size)
        assertEquals(HidConstants.MOD_NONE, bs[0].modifierMask)
        assertEquals(HidConstants.KEY_BACKSPACE, bs[0].usageId)

        // Tab
        val tab = keymap.translateChar('\t')
        assertEquals(1, tab.size)
        assertEquals(HidConstants.MOD_NONE, tab[0].modifierMask)
        assertEquals(HidConstants.KEY_TAB, tab[0].usageId)

        // Enter / Newline (Terminal Enter vs Chat Soft Enter)
        val nlTerminal = keymap.translateChar('\n')
        assertEquals(1, nlTerminal.size)
        assertEquals(HidConstants.MOD_NONE, nlTerminal[0].modifierMask)
        assertEquals(HidConstants.KEY_ENTER, nlTerminal[0].usageId)

        keymap.newlineMode = NewlineSubmissionMode.CHAT_SOFT_ENTER
        val nlSoft = keymap.translateChar('\n')
        assertEquals(1, nlSoft.size)
        assertEquals(HidConstants.MOD_LSHIFT, nlSoft[0].modifierMask)
        assertEquals(HidConstants.KEY_ENTER, nlSoft[0].usageId)
        keymap.newlineMode = NewlineSubmissionMode.TERMINAL_ENTER

        // Escape (\u001b)
        val esc = keymap.translateChar('\u001b')
        assertEquals(1, esc.size)
        assertEquals(HidConstants.MOD_NONE, esc[0].modifierMask)
        assertEquals(HidConstants.KEY_ESCAPE, esc[0].usageId)

        // Space
        val sp = keymap.translateChar(' ')
        assertEquals(1, sp.size)
        assertEquals(HidConstants.MOD_NONE, sp[0].modifierMask)
        assertEquals(HidConstants.KEY_SPACE, sp[0].usageId)
    }

    @Test
    fun testLowercaseLetters() {
        for (c in 'a'..'z') {
            val strokes = keymap.translateChar(c)
            assertEquals("Failed for char: $c", 1, strokes.size)
            assertEquals("Modifier mismatch for $c", HidConstants.MOD_NONE, strokes[0].modifierMask)
            val expectedUsage = (HidConstants.KEY_A + (c - 'a')).toByte()
            assertEquals("Usage ID mismatch for $c", expectedUsage, strokes[0].usageId)
        }
    }

    @Test
    fun testUppercaseLetters() {
        for (c in 'A'..'Z') {
            val strokes = keymap.translateChar(c)
            assertEquals("Failed for char: $c", 1, strokes.size)
            assertEquals("Modifier mismatch for $c", HidConstants.MOD_LSHIFT, strokes[0].modifierMask)
            val expectedUsage = (HidConstants.KEY_A + (c - 'A')).toByte()
            assertEquals("Usage ID mismatch for $c", expectedUsage, strokes[0].usageId)
        }
    }

    @Test
    fun testDigits() {
        for (c in '1'..'9') {
            val strokes = keymap.translateChar(c)
            assertEquals("Failed for char: $c", 1, strokes.size)
            assertEquals(HidConstants.MOD_NONE, strokes[0].modifierMask)
            val expectedUsage = (HidConstants.KEY_1 + (c - '1')).toByte()
            assertEquals(expectedUsage, strokes[0].usageId)
        }
        val zero = keymap.translateChar('0')
        assertEquals(1, zero.size)
        assertEquals(HidConstants.MOD_NONE, zero[0].modifierMask)
        assertEquals(HidConstants.KEY_0, zero[0].usageId)
    }

    @Test
    fun testShiftedPunctuation() {
        val cases = mapOf(
            '!' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_1),
            '@' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_2),
            '#' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_3),
            '$' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_4),
            '%' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_5),
            '^' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_6),
            '&' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_7),
            '*' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_8),
            '(' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_9),
            ')' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_0),
            '_' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_MINUS),
            '+' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_EQUAL),
            '{' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_LEFTBRACE),
            '}' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_RIGHTBRACE),
            '|' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_BACKSLASH),
            ':' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_SEMICOLON),
            '"' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_APOSTROPHE),
            '<' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_COMMA),
            '>' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_DOT),
            '?' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_SLASH),
            '~' to Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_GRAVE)
        )

        for ((char, expected) in cases) {
            val strokes = keymap.translateChar(char)
            assertEquals("Failed for char: $char", 1, strokes.size)
            assertEquals("Modifier mismatch for $char", expected.first, strokes[0].modifierMask)
            assertEquals("Usage ID mismatch for $char", expected.second, strokes[0].usageId)
        }
    }

    @Test
    fun testUnshiftedPunctuation() {
        val cases = mapOf(
            '-' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_MINUS),
            '=' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_EQUAL),
            '[' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_LEFTBRACE),
            ']' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_RIGHTBRACE),
            '\\' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_BACKSLASH),
            ';' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_SEMICOLON),
            '\'' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE),
            ',' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_COMMA),
            '.' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_DOT),
            '/' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_SLASH),
            '`' to Pair(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE)
        )

        for ((char, expected) in cases) {
            val strokes = keymap.translateChar(char)
            assertEquals("Failed for char: $char", 1, strokes.size)
            assertEquals("Modifier mismatch for $char", expected.first, strokes[0].modifierMask)
            assertEquals("Usage ID mismatch for $char", expected.second, strokes[0].usageId)
        }
    }

    @Test
    fun testFullAsciiPrintableRangeCoverage() {
        for (code in 32..126) {
            val ch = code.toChar()
            val strokes = keymap.translateChar(ch)
            assertTrue("Unmapped ASCII printable char: $ch (code $code)", strokes.isNotEmpty())
        }
    }

    @Test
    fun testTranslateSentence() {
        val text = "Hello, World! 123"
        val strokes = keymap.translateString(text)
        assertEquals(text.length, strokes.size)
    }
}
