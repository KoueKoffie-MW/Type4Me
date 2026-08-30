package com.transcriptor.hid.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GermanQwertzKeymapTest {

    private lateinit var keymap: GermanQwertzKeymap

    @Before
    fun setUp() {
        keymap = GermanQwertzKeymap()
    }

    @Test
    fun testLayoutIdentifier() {
        assertEquals(KeyLayout.GERMAN_QWERTZ, keymap.layout)
    }

    @Test
    fun testYZPhysicalSwap() {
        // 'y' is at physical position B01 (KEY_Z = 0x1D) on DE layout
        val lowerY = keymap.translateChar('y')
        assertEquals(1, lowerY.size)
        assertEquals(HidConstants.MOD_NONE, lowerY[0].modifierMask)
        assertEquals(HidConstants.KEY_Z, lowerY[0].usageId)

        val upperY = keymap.translateChar('Y')
        assertEquals(1, upperY.size)
        assertEquals(HidConstants.MOD_LSHIFT, upperY[0].modifierMask)
        assertEquals(HidConstants.KEY_Z, upperY[0].usageId)

        // 'z' is at physical position D06 (KEY_Y = 0x1C) on DE layout
        val lowerZ = keymap.translateChar('z')
        assertEquals(1, lowerZ.size)
        assertEquals(HidConstants.MOD_NONE, lowerZ[0].modifierMask)
        assertEquals(HidConstants.KEY_Y, lowerZ[0].usageId)

        val upperZ = keymap.translateChar('Z')
        assertEquals(1, upperZ.size)
        assertEquals(HidConstants.MOD_LSHIFT, upperZ[0].modifierMask)
        assertEquals(HidConstants.KEY_Y, upperZ[0].usageId)
    }

    @Test
    fun testGermanUmlautsAndEszett() {
        // ä / Ä
        val lowerAe = keymap.translateChar('ä')
        assertEquals(1, lowerAe.size)
        assertEquals(HidConstants.MOD_NONE, lowerAe[0].modifierMask)
        assertEquals(HidConstants.KEY_APOSTROPHE, lowerAe[0].usageId)

        val upperAe = keymap.translateChar('Ä')
        assertEquals(1, upperAe.size)
        assertEquals(HidConstants.MOD_LSHIFT, upperAe[0].modifierMask)
        assertEquals(HidConstants.KEY_APOSTROPHE, upperAe[0].usageId)

        // ö / Ö
        val lowerOe = keymap.translateChar('ö')
        assertEquals(1, lowerOe.size)
        assertEquals(HidConstants.MOD_NONE, lowerOe[0].modifierMask)
        assertEquals(HidConstants.KEY_SEMICOLON, lowerOe[0].usageId)

        val upperOe = keymap.translateChar('Ö')
        assertEquals(1, upperOe.size)
        assertEquals(HidConstants.MOD_LSHIFT, upperOe[0].modifierMask)
        assertEquals(HidConstants.KEY_SEMICOLON, upperOe[0].usageId)

        // ü / Ü
        val lowerUe = keymap.translateChar('ü')
        assertEquals(1, lowerUe.size)
        assertEquals(HidConstants.MOD_NONE, lowerUe[0].modifierMask)
        assertEquals(HidConstants.KEY_LEFTBRACE, lowerUe[0].usageId)

        val upperUe = keymap.translateChar('Ü')
        assertEquals(1, upperUe.size)
        assertEquals(HidConstants.MOD_LSHIFT, upperUe[0].modifierMask)
        assertEquals(HidConstants.KEY_LEFTBRACE, upperUe[0].usageId)

        // ß
        val sz = keymap.translateChar('ß')
        assertEquals(1, sz.size)
        assertEquals(HidConstants.MOD_NONE, sz[0].modifierMask)
        assertEquals(HidConstants.KEY_MINUS, sz[0].usageId)

        // Capital sharp S (ẞ U+1E9E)
        val capSz = keymap.translateChar('ẞ')
        assertEquals(1, capSz.size)
        assertEquals(HidConstants.MOD_SHIFT_ALT_GR, capSz[0].modifierMask)
        assertEquals(HidConstants.KEY_MINUS, capSz[0].usageId)
    }

    @Test
    fun testAltGrThirdLevelSymbols() {
        val cases = mapOf(
            '@' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_Q),
            '€' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_E),
            '[' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_8),
            '\\' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_MINUS),
            ']' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_9),
            '{' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_7),
            '|' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_NON_US_BACKSLASH),
            '}' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_0),
            'µ' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_M),
            '²' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_2),
            '³' to Pair(HidConstants.MOD_ALT_GR, HidConstants.KEY_3)
        )

        for ((char, expected) in cases) {
            val strokes = keymap.translateChar(char)
            assertEquals("Failed for char: $char", 1, strokes.size)
            assertEquals("Modifier mismatch for $char", expected.first, strokes[0].modifierMask)
            assertEquals("Usage ID mismatch for $char", expected.second, strokes[0].usageId)
        }
    }

    @Test
    fun testDeadKeysEmitTwoKeystrokesWithSpace() {
        // Circumflex ^ (Edge Case 7)
        val circumflex = keymap.translateChar('^')
        assertEquals(2, circumflex.size)
        assertEquals(HidConstants.MOD_NONE, circumflex[0].modifierMask)
        assertEquals(HidConstants.KEY_GRAVE, circumflex[0].usageId)
        assertEquals(HidConstants.MOD_NONE, circumflex[1].modifierMask)
        assertEquals(HidConstants.KEY_SPACE, circumflex[1].usageId)

        // Acute Accent ´
        val acute = keymap.translateChar('´')
        assertEquals(2, acute.size)
        assertEquals(HidConstants.MOD_NONE, acute[0].modifierMask)
        assertEquals(HidConstants.KEY_EQUAL, acute[0].usageId)
        assertEquals(HidConstants.MOD_NONE, acute[1].modifierMask)
        assertEquals(HidConstants.KEY_SPACE, acute[1].usageId)

        // Grave Accent `
        val grave = keymap.translateChar('`')
        assertEquals(2, grave.size)
        assertEquals(HidConstants.MOD_LSHIFT, grave[0].modifierMask)
        assertEquals(HidConstants.KEY_EQUAL, grave[0].usageId)
        assertEquals(HidConstants.MOD_NONE, grave[1].modifierMask)
        assertEquals(HidConstants.KEY_SPACE, grave[1].usageId)

        // Tilde ~ (Edge Case 8)
        val tilde = keymap.translateChar('~')
        assertEquals(2, tilde.size)
        assertEquals(HidConstants.MOD_ALT_GR, tilde[0].modifierMask)
        assertEquals(HidConstants.KEY_RIGHTBRACE, tilde[0].usageId)
        assertEquals(HidConstants.MOD_NONE, tilde[1].modifierMask)
        assertEquals(HidConstants.KEY_SPACE, tilde[1].usageId)
    }

    @Test
    fun testIsoSpecificKeys() {
        // Non-US < > | key (Usage 0x64)
        val lessThan = keymap.translateChar('<')
        assertEquals(1, lessThan.size)
        assertEquals(HidConstants.MOD_NONE, lessThan[0].modifierMask)
        assertEquals(HidConstants.KEY_NON_US_BACKSLASH, lessThan[0].usageId)

        val greaterThan = keymap.translateChar('>')
        assertEquals(1, greaterThan.size)
        assertEquals(HidConstants.MOD_LSHIFT, greaterThan[0].modifierMask)
        assertEquals(HidConstants.KEY_NON_US_BACKSLASH, greaterThan[0].usageId)

        // Non-US # ' key (Usage 0x32)
        val hash = keymap.translateChar('#')
        assertEquals(1, hash.size)
        assertEquals(HidConstants.MOD_NONE, hash[0].modifierMask)
        assertEquals(HidConstants.KEY_NON_US_HASH, hash[0].usageId)

        val singleQuote = keymap.translateChar('\'')
        assertEquals(1, singleQuote.size)
        assertEquals(HidConstants.MOD_LSHIFT, singleQuote[0].modifierMask)
        assertEquals(HidConstants.KEY_NON_US_HASH, singleQuote[0].usageId)
    }

    @Test
    fun testDigitsAndBasicPunctuation() {
        // Digits
        for (d in '0'..'9') {
            val s = keymap.translateChar(d)
            assertEquals(1, s.size)
            assertEquals(HidConstants.MOD_NONE, s[0].modifierMask)
        }

        // Section sign §
        val section = keymap.translateChar('§')
        assertEquals(1, section.size)
        assertEquals(HidConstants.MOD_LSHIFT, section[0].modifierMask)
        assertEquals(HidConstants.KEY_3, section[0].usageId)

        // Degree sign °
        val degree = keymap.translateChar('°')
        assertEquals(1, degree.size)
        assertEquals(HidConstants.MOD_LSHIFT, degree[0].modifierMask)
        assertEquals(HidConstants.KEY_GRAVE, degree[0].usageId)
    }

    @Test
    fun testGermanSentenceTranslation() {
        val sentence = "Übergrößen-Träger zahlen 100€ für die Maßnahme!"
        val strokes = keymap.translateString(sentence)
        assertTrue(strokes.isNotEmpty())
    }
}
