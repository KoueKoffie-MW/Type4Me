package com.transcriptor.hid.engine

/**
 * USB HID Keyboard definitions per USB HID Usage Tables v1.5 (Page 0x07)
 * and USB HID 8-byte Input Report modifier masks.
 */
object HidConstants {
    // -------------------------------------------------------------
    // Modifier Byte Bitmasks (Byte 0 of standard 8-byte HID report)
    // -------------------------------------------------------------
    const val MOD_NONE: Byte = 0x00
    const val MOD_LCTRL: Byte = 0x01
    const val MOD_LSHIFT: Byte = 0x02
    const val MOD_LALT: Byte = 0x04
    const val MOD_LGUI: Byte = 0x08
    const val MOD_LMETA: Byte = 0x08
    const val MOD_RCTRL: Byte = 0x10
    const val MOD_RSHIFT: Byte = 0x20
    const val MOD_RALT: Byte = 0x40
    const val MOD_ALT_GR: Byte = 0x40
    val MOD_RGUI: Byte = 0x80.toByte()
    val MOD_RMETA: Byte = 0x80.toByte()

    // Extended Composite Modifier Masks
    val MOD_SHIFT_ALT_GR: Byte = (0x02 or 0x40).toByte() // 0x42
    val MOD_CTRL_ALT: Byte = (0x01 or 0x04).toByte()     // 0x05
    val MOD_CTRL_SHIFT: Byte = (0x01 or 0x02).toByte()   // 0x03
    const val MOD_ALT_TAB: Byte = MOD_LALT               // 0x04
    val MOD_CTRL_ALT_SHIFT: Byte = (0x01 or 0x04 or 0x02).toByte() // 0x07

    // -------------------------------------------------------------
    // USB HID Usage Table (Page 0x07 - Keyboard / Keypad)
    // -------------------------------------------------------------
    const val KEY_NONE: Byte = 0x00
    const val KEY_ERROR_ROLL_OVER: Byte = 0x01
    const val KEY_POST_FAIL: Byte = 0x02
    const val KEY_ERROR_UNDEFINED: Byte = 0x03

    // Letters A-Z (0x04 - 0x1D)
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D

    // Digits 1-9, 0 (0x1E - 0x27)
    const val KEY_1: Byte = 0x1E
    const val KEY_2: Byte = 0x1F
    const val KEY_3: Byte = 0x20
    const val KEY_4: Byte = 0x21
    const val KEY_5: Byte = 0x22
    const val KEY_6: Byte = 0x23
    const val KEY_7: Byte = 0x24
    const val KEY_8: Byte = 0x25
    const val KEY_9: Byte = 0x26
    const val KEY_0: Byte = 0x27

    // Control & Navigation Keys (0x28 - 0x2C)
    const val KEY_ENTER: Byte = 0x28
    const val KEY_RETURN: Byte = 0x28
    const val KEY_ESCAPE: Byte = 0x29
    const val KEY_ESC: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C

    // Punctuation & Layout-Specific Usage Codes (0x2D - 0x39, 0x64)
    const val KEY_MINUS: Byte = 0x2D          // US: '-/_', DE: 'ß/?/\'
    const val KEY_EQUAL: Byte = 0x2E          // US: '=/+', DE: '´/`'
    const val KEY_LEFTBRACE: Byte = 0x2F      // US: '[/{', DE: 'ü/Ü'
    const val KEY_LEFT_BRACE: Byte = 0x2F
    const val KEY_RIGHTBRACE: Byte = 0x30     // US: ']/}', DE: '+/*/~'
    const val KEY_RIGHT_BRACE: Byte = 0x30
    const val KEY_BACKSLASH: Byte = 0x31      // US: '\/|' (ANSI)
    const val KEY_NON_US_HASH: Byte = 0x32    // DE: '#/\'' (ISO next to Enter)
    const val KEY_SEMICOLON: Byte = 0x33     // US: ';/:', DE: 'ö/Ö'
    const val KEY_APOSTROPHE: Byte = 0x34    // US: '\'/\"', DE: 'ä/Ä'
    const val KEY_GRAVE: Byte = 0x35         // US: '`/~', DE: '^/°'
    const val KEY_COMMA: Byte = 0x36         // US: ',/<', DE: ',/;'
    const val KEY_DOT: Byte = 0x37           // US: './>', DE: './:'
    const val KEY_SLASH: Byte = 0x38         // US: '//?', DE: '-/_'
    const val KEY_CAPSLOCK: Byte = 0x39
    const val KEY_CAPS_LOCK: Byte = 0x39

    // Function Keys F1-F24 (0x3A - 0x45, 0x68 - 0x73)
    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F4: Byte = 0x3D
    const val KEY_F5: Byte = 0x3E
    const val KEY_F6: Byte = 0x3F
    const val KEY_F7: Byte = 0x40
    const val KEY_F8: Byte = 0x41
    const val KEY_F9: Byte = 0x42
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44
    const val KEY_F12: Byte = 0x45
    const val KEY_F13: Byte = 0x68
    const val KEY_F14: Byte = 0x69
    const val KEY_F15: Byte = 0x6A
    const val KEY_F16: Byte = 0x6B
    const val KEY_F17: Byte = 0x6C
    const val KEY_F18: Byte = 0x6D
    const val KEY_F19: Byte = 0x6E
    const val KEY_F20: Byte = 0x6F
    const val KEY_F21: Byte = 0x70
    const val KEY_F22: Byte = 0x71
    const val KEY_F23: Byte = 0x72
    const val KEY_F24: Byte = 0x73

    // Navigation & Extended Editing (0x46 - 0x52)
    const val KEY_PRINT_SCREEN: Byte = 0x46
    const val KEY_SCROLL_LOCK: Byte = 0x47
    const val KEY_PAUSE: Byte = 0x48
    const val KEY_INSERT: Byte = 0x49
    const val KEY_HOME: Byte = 0x4A
    const val KEY_PAGE_UP: Byte = 0x4B
    const val KEY_DELETE: Byte = 0x4C        // Forward Delete
    const val KEY_END: Byte = 0x4D
    const val KEY_PAGE_DOWN: Byte = 0x4E
    const val KEY_RIGHT: Byte = 0x4F
    const val KEY_RIGHT_ARROW: Byte = 0x4F
    const val KEY_LEFT: Byte = 0x50
    const val KEY_LEFT_ARROW: Byte = 0x50
    const val KEY_DOWN: Byte = 0x51
    const val KEY_DOWN_ARROW: Byte = 0x51
    const val KEY_UP: Byte = 0x52
    const val KEY_UP_ARROW: Byte = 0x52

    // Keypad (0x53 - 0x63, 0x67)
    const val KEY_NUM_LOCK: Byte = 0x53
    const val KEYPAD_SLASH: Byte = 0x54
    const val KEYPAD_ASTERISK: Byte = 0x55
    const val KEYPAD_MINUS: Byte = 0x56
    const val KEYPAD_PLUS: Byte = 0x57
    const val KEYPAD_ENTER: Byte = 0x58
    const val KEYPAD_1: Byte = 0x59
    const val KEYPAD_2: Byte = 0x5A
    const val KEYPAD_3: Byte = 0x5B
    const val KEYPAD_4: Byte = 0x5C
    const val KEYPAD_5: Byte = 0x5D
    const val KEYPAD_6: Byte = 0x5E
    const val KEYPAD_7: Byte = 0x5F
    const val KEYPAD_8: Byte = 0x60
    const val KEYPAD_9: Byte = 0x61
    const val KEYPAD_0: Byte = 0x62
    const val KEYPAD_DOT: Byte = 0x63
    const val KEYPAD_EQUAL: Byte = 0x67

    // ISO Extra Key (B00)
    const val KEY_NON_US_BACKSLASH: Byte = 0x64 // DE: '</>/|' (ISO between LShift and Y)

    // System & Application Controls (0x65 - 0x66)
    const val KEY_APPLICATION: Byte = 0x65   // Context Menu key
    const val KEY_POWER: Byte = 0x66
}
