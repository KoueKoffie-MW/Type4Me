package com.transcriptor.hid.ui

import com.transcriptor.hid.engine.HidConstants
import com.transcriptor.hid.engine.HidKeyStroke
import com.transcriptor.hid.ui.components.HotkeyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotkeyDockBarTest {

    @Test
    fun testPrimaryHotkeyCatalogContainsAllRequiredTerminalKeys() {
        val keys = HotkeyCatalog.PRIMARY_HOTKEYS
        val labelMap = keys.associateBy { it.label }

        // ESC
        assertNotNull(labelMap["ESC"])
        assertEquals(HidConstants.MOD_NONE, labelMap["ESC"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_ESCAPE, labelMap["ESC"]?.stroke?.usageId)

        // TAB
        assertNotNull(labelMap["TAB"])
        assertEquals(HidConstants.MOD_NONE, labelMap["TAB"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_TAB, labelMap["TAB"]?.stroke?.usageId)

        // ^C (SIGINT)
        assertNotNull(labelMap["^C"])
        assertEquals(HidConstants.MOD_LCTRL, labelMap["^C"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_C, labelMap["^C"]?.stroke?.usageId)

        // ^Z (SIGTSTP)
        assertNotNull(labelMap["^Z"])
        assertEquals(HidConstants.MOD_LCTRL, labelMap["^Z"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_Z, labelMap["^Z"]?.stroke?.usageId)

        // ^D (EOF)
        assertNotNull(labelMap["^D"])
        assertEquals(HidConstants.MOD_LCTRL, labelMap["^D"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_D, labelMap["^D"]?.stroke?.usageId)

        // ^L (Clear)
        assertNotNull(labelMap["^L"])
        assertEquals(HidConstants.MOD_LCTRL, labelMap["^L"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_L, labelMap["^L"]?.stroke?.usageId)

        // ALT+TAB
        assertNotNull(labelMap["ALT+TAB"])
        assertEquals(HidConstants.MOD_LALT, labelMap["ALT+TAB"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_TAB, labelMap["ALT+TAB"]?.stroke?.usageId)

        // Navigation arrows
        assertNotNull(labelMap["←"])
        assertEquals(HidConstants.KEY_LEFT, labelMap["←"]?.stroke?.usageId)

        assertNotNull(labelMap["↑"])
        assertEquals(HidConstants.KEY_UP, labelMap["↑"]?.stroke?.usageId)

        assertNotNull(labelMap["↓"])
        assertEquals(HidConstants.KEY_DOWN, labelMap["↓"]?.stroke?.usageId)

        assertNotNull(labelMap["→"])
        assertEquals(HidConstants.KEY_RIGHT, labelMap["→"]?.stroke?.usageId)

        // Backtick `
        assertNotNull(labelMap["`"])
        assertEquals(HidConstants.KEY_GRAVE, labelMap["`"]?.stroke?.usageId)

        // ^P
        assertNotNull(labelMap["^P"])
        assertEquals(HidConstants.MOD_LCTRL, labelMap["^P"]?.stroke?.modifierMask)
        assertEquals(HidConstants.KEY_P, labelMap["^P"]?.stroke?.usageId)
    }

    @Test
    fun testFunctionKeysCatalogF1ThroughF12() {
        val fKeys = HotkeyCatalog.FUNCTION_KEYS
        assertEquals(12, fKeys.size)

        for (i in 1..12) {
            val item = fKeys[i - 1]
            assertEquals("F$i", item.label)
            assertEquals(HidConstants.MOD_NONE, item.stroke.modifierMask)
            val expectedUsage = (HidConstants.KEY_F1 + (i - 1)).toByte()
            assertEquals("F$i usage ID mismatch", expectedUsage, item.stroke.usageId)
        }
    }

    @Test
    fun testHotkeyItemAccessibilityDescriptions() {
        for (item in HotkeyCatalog.PRIMARY_HOTKEYS) {
            assertTrue("Description must not be blank for ${item.label}", item.description.isNotBlank())
            assertTrue("Tag must not be blank for ${item.label}", item.tag.isNotBlank())
        }
        for (item in HotkeyCatalog.FUNCTION_KEYS) {
            assertTrue("Description must not be blank for ${item.label}", item.description.isNotBlank())
            assertTrue("Tag must not be blank for ${item.label}", item.tag.isNotBlank())
        }
    }

    @Test
    fun testHotkeyEmissionsExecution() {
        val emittedStrokes = mutableListOf<HidKeyStroke>()
        val onSendKeyStroke: (HidKeyStroke) -> Unit = { emittedStrokes.add(it) }

        // Simulate clicking ^C
        val ctrlC = HotkeyCatalog.PRIMARY_HOTKEYS.first { it.label == "^C" }
        onSendKeyStroke(ctrlC.stroke)

        assertEquals(1, emittedStrokes.size)
        assertEquals(HidConstants.MOD_LCTRL, emittedStrokes[0].modifierMask)
        assertEquals(HidConstants.KEY_C, emittedStrokes[0].usageId)
    }
}
