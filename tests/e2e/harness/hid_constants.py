"""
HID Constants, Usage Codes (Page 0x07), Modifiers, and Report Descriptors.
Authoritative source: USB HID Usage Tables 1.5, USB HID Spec 1.11, DIN 2137-1.
"""

# Modifier Byte (Byte 0) Bitmasks
MOD_NONE: int = 0x00
MOD_L_CTRL: int = 0x01
MOD_L_SHIFT: int = 0x02
MOD_L_ALT: int = 0x04
MOD_L_GUI: int = 0x08
MOD_R_CTRL: int = 0x10
MOD_R_SHIFT: int = 0x20
MOD_R_ALT: int = 0x40  # AltGr on international/German keyboards
MOD_R_GUI: int = 0x80

# Common Combined Modifiers
MOD_SHIFT_ALTGR: int = MOD_L_SHIFT | MOD_R_ALT  # 0x42 (Capital Eszett ẞ DIN 2137)
MOD_CTRL_ALT: int = MOD_L_CTRL | MOD_L_ALT      # 0x05 (IDE shortcut prefix)
MOD_CTRL_SHIFT: int = MOD_L_CTRL | MOD_L_SHIFT  # 0x03 (Terminal hotkey prefix)

# HID Usage IDs (Page 0x07 - Keyboard/Keypad)
KEY_NONE: int = 0x00
KEY_ERROR_ROLLOVER: int = 0x01
KEY_POST_FAIL: int = 0x02
KEY_ERROR_UNDEFINED: int = 0x03

# Letters A-Z (0x04 - 0x1D)
KEY_A: int = 0x04
KEY_B: int = 0x05
KEY_C: int = 0x06
KEY_D: int = 0x07
KEY_E: int = 0x08
KEY_F: int = 0x09
KEY_G: int = 0x0A
KEY_H: int = 0x0B
KEY_I: int = 0x0C
KEY_J: int = 0x0D
KEY_K: int = 0x0E
KEY_L: int = 0x0F
KEY_M: int = 0x10
KEY_N: int = 0x11
KEY_O: int = 0x12
KEY_P: int = 0x13
KEY_Q: int = 0x14
KEY_R: int = 0x15
KEY_S: int = 0x16
KEY_T: int = 0x17
KEY_U: int = 0x18
KEY_V: int = 0x19
KEY_W: int = 0x1A
KEY_X: int = 0x1B
KEY_Y: int = 0x1C  # Top row D06 (Z on German layout)
KEY_Z: int = 0x1D  # Bottom row B01 (Y on German layout)

# Numbers 1-0 (0x1E - 0x27)
KEY_1: int = 0x1E
KEY_2: int = 0x1F
KEY_3: int = 0x20
KEY_4: int = 0x21
KEY_5: int = 0x22
KEY_6: int = 0x23
KEY_7: int = 0x24
KEY_8: int = 0x25
KEY_9: int = 0x26
KEY_0: int = 0x27

# Controls and Functional Keys (0x28 - 0x2C)
KEY_ENTER: int = 0x28
KEY_ESCAPE: int = 0x29
KEY_BACKSPACE: int = 0x2A
KEY_TAB: int = 0x2B
KEY_SPACE: int = 0x2C

# Punctuation & Layout Symbols (0x2D - 0x38, 0x64)
KEY_MINUS: int = 0x2D          # US: - _ | DE: ß ? \
KEY_EQUAL: int = 0x2E          # US: = + | DE: ´ `
KEY_LEFT_BRACE: int = 0x2F     # US: [ { | DE: Ü ü
KEY_RIGHT_BRACE: int = 0x30    # US: ] } | DE: + * ~
KEY_BACKSLASH: int = 0x31      # US: \ | | DE: (none/OEM)
KEY_NON_US_HASH: int = 0x32    # DE: # ' (next to Return, DIN 2137 C12)
KEY_SEMICOLON: int = 0x33      # US: ; : | DE: Ö ö
KEY_APOSTROPHE: int = 0x34     # US: ' " | DE: Ä ä
KEY_GRAVE: int = 0x35          # US: ` ~ | DE: ^ ° (Dead key)
KEY_COMMA: int = 0x36          # US: , < | DE: , ;
KEY_DOT: int = 0x37            # US: . > | DE: . :
KEY_SLASH: int = 0x38          # US: / ? | DE: - _
KEY_CAPS_LOCK: int = 0x39
KEY_NON_US_BACKSLASH: int = 0x64  # DE: < > | (ISO 105th key between L-Shift and Y)

# Function Keys F1-F24 (0x3A - 0x45, 0x68 - 0x73)
KEY_F1: int = 0x3A
KEY_F2: int = 0x3B
KEY_F3: int = 0x3C
KEY_F4: int = 0x3D
KEY_F5: int = 0x3E
KEY_F6: int = 0x3F
KEY_F7: int = 0x40
KEY_F8: int = 0x41
KEY_F9: int = 0x42
KEY_F10: int = 0x43
KEY_F11: int = 0x44
KEY_F12: int = 0x45
KEY_F13: int = 0x68
KEY_F14: int = 0x69
KEY_F15: int = 0x6A
KEY_F16: int = 0x6B
KEY_F17: int = 0x6C
KEY_F18: int = 0x6D
KEY_F19: int = 0x6E
KEY_F20: int = 0x6F
KEY_F21: int = 0x70
KEY_F22: int = 0x71
KEY_F23: int = 0x72
KEY_F24: int = 0x73

# Navigation & Extended Editing (0x46 - 0x52)
KEY_PRINT_SCREEN: int = 0x46
KEY_SCROLL_LOCK: int = 0x47
KEY_PAUSE: int = 0x48
KEY_INSERT: int = 0x49
KEY_HOME: int = 0x4A
KEY_PAGE_UP: int = 0x4B
KEY_DELETE: int = 0x4C        # Forward Delete
KEY_END: int = 0x4D
KEY_PAGE_DOWN: int = 0x4E
KEY_RIGHT_ARROW: int = 0x4F
KEY_LEFT_ARROW: int = 0x50
KEY_DOWN_ARROW: int = 0x51
KEY_UP_ARROW: int = 0x52

# Keypad (0x53 - 0x63, 0x67)
KEY_NUM_LOCK: int = 0x53
KEYPAD_SLASH: int = 0x54
KEYPAD_ASTERISK: int = 0x55
KEYPAD_MINUS: int = 0x56
KEYPAD_PLUS: int = 0x57
KEYPAD_ENTER: int = 0x58
KEYPAD_1: int = 0x59
KEYPAD_2: int = 0x5A
KEYPAD_3: int = 0x5B
KEYPAD_4: int = 0x5C
KEYPAD_5: int = 0x5D
KEYPAD_6: int = 0x5E
KEYPAD_7: int = 0x5F
KEYPAD_8: int = 0x60
KEYPAD_9: int = 0x61
KEYPAD_0: int = 0x62
KEYPAD_DOT: int = 0x63
KEYPAD_EQUAL: int = 0x67

# System & Application Controls (0x65 - 0x66)
KEY_APPLICATION: int = 0x65   # Context Menu key
KEY_POWER: int = 0x66

# Consumer Control Usages (Page 0x0C)
CONSUMER_MUTE: int = 0x00E2
CONSUMER_VOLUME_UP: int = 0x00E9
CONSUMER_VOLUME_DOWN: int = 0x00EA
CONSUMER_PLAY_PAUSE: int = 0x00CD
CONSUMER_SCAN_NEXT: int = 0x00B5
CONSUMER_SCAN_PREV: int = 0x00B6
CONSUMER_STOP: int = 0x00B7
CONSUMER_BRIGHTNESS_UP: int = 0x006F
CONSUMER_BRIGHTNESS_DOWN: int = 0x0070
CONSUMER_CALCULATOR: int = 0x0192
CONSUMER_BROWSER: int = 0x0194

# System Control Usages (Page 0x01)
SYSTEM_POWER_DOWN: int = 0x0081
SYSTEM_SLEEP: int = 0x0082
SYSTEM_WAKE_UP: int = 0x0083

# Host LED Bitmasks (Output Report Byte 0)
LED_NUM_LOCK: int = 0x01
LED_CAPS_LOCK: int = 0x02
LED_SCROLL_LOCK: int = 0x04
LED_COMPOSE: int = 0x08
LED_KANA: int = 0x10

# Terminal Bracketed Paste Mode Markers
BRACKETED_PASTE_START: str = "\x1b[200~"
BRACKETED_PASTE_END: str = "\x1b[201~"

# Universal 63-Byte Standard HID Keyboard Report Descriptor (Report ID 0)
HID_KEYBOARD_REPORT_DESCRIPTOR: bytes = bytes([
    0x05, 0x01,        # Usage Page (Generic Desktop Ctrls)
    0x09, 0x06,        # Usage (Keyboard)
    0xA1, 0x01,        # Collection (Application)
    0x05, 0x07,        #   Usage Page (Kbrd/Keypad)
    0x19, 0xE0,        #   Usage Minimum (Keyboard LeftControl)
    0x29, 0xE7,        #   Usage Maximum (Keyboard Right GUI)
    0x15, 0x00,        #   Logical Minimum (0)
    0x25, 0x01,        #   Logical Maximum (1)
    0x75, 0x01,        #   Report Size (1)
    0x95, 0x08,        #   Report Count (8)
    0x81, 0x02,        #   Input (Data, Var, Abs) -> Modifier Byte
    0x95, 0x01,        #   Report Count (1)
    0x75, 0x08,        #   Report Size (8)
    0x81, 0x01,        #   Input (Const, Array, Abs) -> Reserved Byte
    0x95, 0x05,        #   Report Count (5)
    0x75, 0x01,        #   Report Size (1)
    0x05, 0x08,        #   Usage Page (LEDs)
    0x19, 0x01,        #   Usage Minimum (Num Lock)
    0x29, 0x05,        #   Usage Maximum (Kana)
    0x91, 0x02,        #   Output (Data, Var, Abs) -> LED Bits (5 bits)
    0x95, 0x01,        #   Report Count (1)
    0x75, 0x03,        #   Report Size (3)
    0x91, 0x01,        #   Output (Const, Array, Abs) -> LED Padding (3 bits)
    0x95, 0x06,        #   Report Count (6)
    0x75, 0x08,        #   Report Size (8)
    0x15, 0x00,        #   Logical Minimum (0)
    0x25, 0x65,        #   Logical Maximum (101 keys)
    0x05, 0x07,        #   Usage Page (Kbrd/Keypad)
    0x19, 0x00,        #   Usage Minimum (0)
    0x29, 0x65,        #   Usage Maximum (101)
    0x81, 0x00,        #   Input (Data, Array, Abs) -> 6-Key Array
    0xC0               # End Collection
])

# Full 154-Byte Composite HID Descriptor (Report ID 1: Keyboard up to F24, ID 2: Mouse, ID 3: Consumer Media)
COMPOSITE_154_BYTE_REPORT_DESCRIPTOR: bytes = bytes([
    # ========================================================================
    # REPORT ID 1: KEYBOARD (65 Bytes: Standard 8-Byte Input + 1-Byte LED Output)
    # ========================================================================
    0x05, 0x01, # USAGE_PAGE (Generic Desktop Controls: 0x01)
    0x09, 0x06, # USAGE (Keyboard: 0x06)
    0xA1, 0x01, # COLLECTION (Application: 0x01)
    0x85, 0x01, #   REPORT_ID (1)
    0x05, 0x07, #   USAGE_PAGE (Keyboard/Keypad: 0x07)
    0x19, 0xE0, #   USAGE_MINIMUM (Keyboard LeftControl: 0xE0)
    0x29, 0xE7, #   USAGE_MAXIMUM (Keyboard Right GUI: 0xE7)
    0x15, 0x00, #   LOGICAL_MINIMUM (0)
    0x25, 0x01, #   LOGICAL_MAXIMUM (1)
    0x75, 0x01, #   REPORT_SIZE (1 bit)
    0x95, 0x08, #   REPORT_COUNT (8 fields -> Byte 0: Modifier Bitmask)
    0x81, 0x02, #   INPUT (Data, Variable, Absolute)
    0x95, 0x01, #   REPORT_COUNT (1 field)
    0x75, 0x08, #   REPORT_SIZE (8 bits = 1 byte)
    0x81, 0x01, #   INPUT (Constant, Array, Absolute -> Byte 1: Reserved OEM)
    0x95, 0x05, #   REPORT_COUNT (5 fields)
    0x75, 0x01, #   REPORT_SIZE (1 bit)
    0x05, 0x08, #   USAGE_PAGE (LEDs: 0x08)
    0x19, 0x01, #   USAGE_MINIMUM (Num Lock: 0x01)
    0x29, 0x05, #   USAGE_MAXIMUM (Kana: 0x05)
    0x91, 0x02, #   OUTPUT (Data, Variable, Absolute -> LED Output)
    0x95, 0x01, #   REPORT_COUNT (1 field)
    0x75, 0x03, #   REPORT_SIZE (3 bits)
    0x91, 0x01, #   OUTPUT (Constant, Array, Absolute -> LED Padding)
    0x95, 0x06, #   REPORT_COUNT (6 fields -> 6 simultaneous key slots)
    0x75, 0x08, #   REPORT_SIZE (8 bits per key)
    0x15, 0x00, #   LOGICAL_MINIMUM (0)
    0x25, 0x73, #   LOGICAL_MAXIMUM (115 keys: covers up to F24: 0x73)
    0x05, 0x07, #   USAGE_PAGE (Keyboard/Keypad: 0x07)
    0x19, 0x00, #   USAGE_MINIMUM (0x00)
    0x29, 0x73, #   USAGE_MAXIMUM (0x73 - F24)
    0x81, 0x00, #   INPUT (Data, Array, Absolute -> Bytes 2..7: 6KRO Array)
    0xC0,       # END_COLLECTION

    # ========================================================================
    # REPORT ID 2: MOUSE (64 Bytes: 4-Byte Relative Movement & Scroll Input Report)
    # ========================================================================
    0x05, 0x01, # USAGE_PAGE (Generic Desktop Controls: 0x01)
    0x09, 0x02, # USAGE (Mouse: 0x02)
    0xA1, 0x01, # COLLECTION (Application: 0x01)
    0x85, 0x02, #   REPORT_ID (2)
    0x09, 0x01, #   USAGE (Pointer: 0x01)
    0xA1, 0x00, #   COLLECTION (Physical: 0x00)
    0x05, 0x09, #     USAGE_PAGE (Button: 0x09)
    0x19, 0x01, #     USAGE_MINIMUM (Button 1: Left: 0x01)
    0x29, 0x03, #     USAGE_MAXIMUM (Button 3: Middle: 0x03)
    0x15, 0x00, #     LOGICAL_MINIMUM (0)
    0x25, 0x01, #     LOGICAL_MAXIMUM (1)
    0x75, 0x01, #     REPORT_SIZE (1 bit)
    0x95, 0x03, #     REPORT_COUNT (3 fields -> 3 buttons)
    0x81, 0x02, #     INPUT (Data, Variable, Absolute -> Bits 0-2)
    0x75, 0x05, #     REPORT_SIZE (5 bits)
    0x95, 0x01, #     REPORT_COUNT (1 field)
    0x81, 0x01, #     INPUT (Constant, Array, Absolute -> Bits 3-7: Padding)
    0x05, 0x01, #     USAGE_PAGE (Generic Desktop Controls: 0x01)
    0x09, 0x30, #     USAGE (X: 0x30)
    0x09, 0x31, #     USAGE (Y: 0x31)
    0x15, 0x81, #     LOGICAL_MINIMUM (-127)
    0x25, 0x7F, #     LOGICAL_MAXIMUM (127)
    0x75, 0x08, #     REPORT_SIZE (8 bits)
    0x95, 0x02, #     REPORT_COUNT (2 fields -> dX, dY)
    0x81, 0x06, #     INPUT (Data, Variable, Relative)
    0x09, 0x38, #     USAGE (Wheel: 0x38)
    0x15, 0x81, #     LOGICAL_MINIMUM (-127)
    0x25, 0x7F, #     LOGICAL_MAXIMUM (127)
    0x75, 0x08, #     REPORT_SIZE (8 bits)
    0x95, 0x01, #     REPORT_COUNT (1 field -> Wheel)
    0x81, 0x06, #     INPUT (Data, Variable, Relative)
    0xC0,       #   END_COLLECTION
    0xC0,       # END_COLLECTION

    # ========================================================================
    # REPORT ID 3: CONSUMER CONTROL (25 Bytes: Media, Volume & Playback Controls)
    # ========================================================================
    0x05, 0x0C, # USAGE_PAGE (Consumer Devices: 0x0C)
    0x09, 0x01, # USAGE (Consumer Control: 0x01)
    0xA1, 0x01, # COLLECTION (Application: 0x01)
    0x85, 0x03, #   REPORT_ID (3)
    0x15, 0x00, #   LOGICAL_MINIMUM (0)
    0x26, 0xFF, 0x03, # LOGICAL_MAXIMUM (1023: 0x03FF)
    0x19, 0x00, #   USAGE_MINIMUM (0)
    0x2A, 0xFF, 0x03, # USAGE_MAXIMUM (1023)
    0x75, 0x10, #   REPORT_SIZE (16 bits = 2 bytes)
    0x95, 0x01, #   REPORT_COUNT (1 field)
    0x81, 0x00, #   INPUT (Data, Array, Absolute -> 2-byte Consumer Usage ID)
    0xC0        # END_COLLECTION
])

# Exact length check
assert len(COMPOSITE_154_BYTE_REPORT_DESCRIPTOR) == 154, f"Descriptor length is {len(COMPOSITE_154_BYTE_REPORT_DESCRIPTOR)}, expected 154"

# SDP Subclass for HID Keyboard
SDP_SUBCLASS_KEYBOARD: int = 0x40
SDP_SUBCLASS_COMBO: int = 0xC0  # Keyboard + Mouse Combo
