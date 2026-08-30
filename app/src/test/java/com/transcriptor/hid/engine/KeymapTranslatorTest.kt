package com.transcriptor.hid.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeymapTranslatorTest {

    @Test
    fun testFactoryCreatesCorrectTranslator() {
        val usTranslator = KeymapTranslator.create(KeyLayout.US_QWERTY)
        assertEquals(KeyLayout.US_QWERTY, usTranslator.layout)
        assertTrue(usTranslator is UsQwertyKeymap)

        val deTranslator = KeymapTranslator.create(KeyLayout.GERMAN_QWERTZ)
        assertEquals(KeyLayout.GERMAN_QWERTZ, deTranslator.layout)
        assertTrue(deTranslator is GermanQwertzKeymap)
    }

    @Test
    fun testEmptyStringTranslation() {
        val usTranslator = KeymapTranslator.create(KeyLayout.US_QWERTY)
        val deTranslator = KeymapTranslator.create(KeyLayout.GERMAN_QWERTZ)

        assertTrue(usTranslator.translateString("").isEmpty())
        assertTrue(deTranslator.translateString("").isEmpty())
    }

    @Test
    fun testHidReportFormat() {
        val report = HidReport(
            modifier = HidConstants.MOD_LSHIFT,
            reserved = 0,
            keyCodes = byteArrayOf(HidConstants.KEY_A, 0, 0, 0, 0, 0)
        )
        val bytes = report.toByteArray()

        assertEquals(8, bytes.size)
        assertEquals(HidConstants.MOD_LSHIFT, bytes[0])
        assertEquals(0.toByte(), bytes[1])
        assertEquals(HidConstants.KEY_A, bytes[2])
        for (i in 3..7) {
            assertEquals(0.toByte(), bytes[i])
        }
    }

    @Test
    fun testHidReportEqualityAndHashCode() {
        val r1 = HidReport(modifier = 0x02, reserved = 0, keyCodes = byteArrayOf(0x04, 0, 0, 0, 0, 0))
        val r2 = HidReport(modifier = 0x02, reserved = 0, keyCodes = byteArrayOf(0x04, 0, 0, 0, 0, 0))
        val r3 = HidReport(modifier = 0x00, reserved = 0, keyCodes = byteArrayOf(0x04, 0, 0, 0, 0, 0))

        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
        assertTrue(r1 != r3)
    }

    @Test
    fun testHidKeyStrokeToKeyDownReport() {
        val stroke = HidKeyStroke(HidConstants.MOD_ALT_GR, HidConstants.KEY_Q)
        val report = stroke.toKeyDownReport()

        assertEquals(HidConstants.MOD_ALT_GR, report.modifier)
        assertEquals(HidConstants.KEY_Q, report.keyCodes[0])

        val release = HidKeyStroke.RELEASE_REPORT
        assertEquals(0.toByte(), release.modifier)
        assertArrayEquals(ByteArray(6), release.keyCodes)
    }

    @Test
    fun testNfcNormalizationHandling() {
        val deTranslator = KeymapTranslator.create(KeyLayout.GERMAN_QWERTZ)

        // Decomposed NFD 'ä' is 'a' (U+0061) + combining diaeresis (U+0308)
        val nfdAWithUmlaut = "a\u0308"
        val strokes = deTranslator.translateString(nfdAWithUmlaut)

        // Should normalize to NFC '\u00E4' (ä) and emit KEY_APOSTROPHE with MOD_NONE
        assertEquals(1, strokes.size)
        assertEquals(HidConstants.MOD_NONE, strokes[0].modifierMask)
        assertEquals(HidConstants.KEY_APOSTROPHE, strokes[0].usageId)
    }

    @Test
    fun testSmartTypographyTransliteration() {
        val usTranslator = KeymapTranslator.create(KeyLayout.US_QWERTY)
        val text = "“Hello” — it’s… done"
        val strokes = usTranslator.translateString(text)

        assertNotNull(strokes)
        assertTrue(strokes.isNotEmpty())
    }
}
