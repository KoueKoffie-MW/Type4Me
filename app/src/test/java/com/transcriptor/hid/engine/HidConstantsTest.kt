package com.transcriptor.hid.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HidConstantsTest {

    @Test
    fun testModifierBitmasks() {
        assertEquals(0x00.toByte(), HidConstants.MOD_NONE)
        assertEquals(0x01.toByte(), HidConstants.MOD_LCTRL)
        assertEquals(0x02.toByte(), HidConstants.MOD_LSHIFT)
        assertEquals(0x04.toByte(), HidConstants.MOD_LALT)
        assertEquals(0x08.toByte(), HidConstants.MOD_LGUI)
        assertEquals(0x08.toByte(), HidConstants.MOD_LMETA)
        assertEquals(0x10.toByte(), HidConstants.MOD_RCTRL)
        assertEquals(0x20.toByte(), HidConstants.MOD_RSHIFT)
        assertEquals(0x40.toByte(), HidConstants.MOD_RALT)
        assertEquals(0x40.toByte(), HidConstants.MOD_ALT_GR)
        assertEquals(0x80.toByte(), HidConstants.MOD_RGUI)
        assertEquals(0x80.toByte(), HidConstants.MOD_RMETA)
    }

    @Test
    fun testCompositeModifierBitmasks() {
        assertEquals(0x42.toByte(), HidConstants.MOD_SHIFT_ALT_GR)
        assertEquals(0x05.toByte(), HidConstants.MOD_CTRL_ALT)
        assertEquals(0x03.toByte(), HidConstants.MOD_CTRL_SHIFT)
        assertEquals(0x04.toByte(), HidConstants.MOD_ALT_TAB)
        assertEquals(0x07.toByte(), HidConstants.MOD_CTRL_ALT_SHIFT)
    }

    @Test
    fun testNavigationAndEditingUsageCodes() {
        assertEquals(0x50.toByte(), HidConstants.KEY_LEFT)
        assertEquals(0x50.toByte(), HidConstants.KEY_LEFT_ARROW)
        assertEquals(0x4F.toByte(), HidConstants.KEY_RIGHT)
        assertEquals(0x4F.toByte(), HidConstants.KEY_RIGHT_ARROW)
        assertEquals(0x52.toByte(), HidConstants.KEY_UP)
        assertEquals(0x52.toByte(), HidConstants.KEY_UP_ARROW)
        assertEquals(0x51.toByte(), HidConstants.KEY_DOWN)
        assertEquals(0x51.toByte(), HidConstants.KEY_DOWN_ARROW)
        assertEquals(0x4A.toByte(), HidConstants.KEY_HOME)
        assertEquals(0x4D.toByte(), HidConstants.KEY_END)
        assertEquals(0x4B.toByte(), HidConstants.KEY_PAGE_UP)
        assertEquals(0x4E.toByte(), HidConstants.KEY_PAGE_DOWN)
        assertEquals(0x49.toByte(), HidConstants.KEY_INSERT)
        assertEquals(0x4C.toByte(), HidConstants.KEY_DELETE)
    }

    @Test
    fun testControlKeysUsageCodes() {
        assertEquals(0x28.toByte(), HidConstants.KEY_ENTER)
        assertEquals(0x29.toByte(), HidConstants.KEY_ESCAPE)
        assertEquals(0x29.toByte(), HidConstants.KEY_ESC)
        assertEquals(0x2A.toByte(), HidConstants.KEY_BACKSPACE)
        assertEquals(0x2B.toByte(), HidConstants.KEY_TAB)
        assertEquals(0x2C.toByte(), HidConstants.KEY_SPACE)
    }

    @Test
    fun testFunctionKeysF1ThroughF24() {
        val expectedF1ToF12 = listOf(
            0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
            0x40, 0x41, 0x42, 0x43, 0x44, 0x45
        )
        val actualF1ToF12 = listOf(
            HidConstants.KEY_F1, HidConstants.KEY_F2, HidConstants.KEY_F3,
            HidConstants.KEY_F4, HidConstants.KEY_F5, HidConstants.KEY_F6,
            HidConstants.KEY_F7, HidConstants.KEY_F8, HidConstants.KEY_F9,
            HidConstants.KEY_F10, HidConstants.KEY_F11, HidConstants.KEY_F12
        )
        for (i in expectedF1ToF12.indices) {
            assertEquals("F${i + 1} mismatch", expectedF1ToF12[i].toByte(), actualF1ToF12[i])
        }

        val expectedF13ToF24 = listOf(
            0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D,
            0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73
        )
        val actualF13ToF24 = listOf(
            HidConstants.KEY_F13, HidConstants.KEY_F14, HidConstants.KEY_F15,
            HidConstants.KEY_F16, HidConstants.KEY_F17, HidConstants.KEY_F18,
            HidConstants.KEY_F19, HidConstants.KEY_F20, HidConstants.KEY_F21,
            HidConstants.KEY_F22, HidConstants.KEY_F23, HidConstants.KEY_F24
        )
        for (i in expectedF13ToF24.indices) {
            assertEquals("F${i + 13} mismatch", expectedF13ToF24[i].toByte(), actualF13ToF24[i])
        }
    }

    @Test
    fun testKeypadAndSystemUsages() {
        assertEquals(0x53.toByte(), HidConstants.KEY_NUM_LOCK)
        assertEquals(0x54.toByte(), HidConstants.KEYPAD_SLASH)
        assertEquals(0x55.toByte(), HidConstants.KEYPAD_ASTERISK)
        assertEquals(0x56.toByte(), HidConstants.KEYPAD_MINUS)
        assertEquals(0x57.toByte(), HidConstants.KEYPAD_PLUS)
        assertEquals(0x58.toByte(), HidConstants.KEYPAD_ENTER)
        assertEquals(0x64.toByte(), HidConstants.KEY_NON_US_BACKSLASH)
        assertEquals(0x65.toByte(), HidConstants.KEY_APPLICATION)
        assertEquals(0x66.toByte(), HidConstants.KEY_POWER)
    }
}
