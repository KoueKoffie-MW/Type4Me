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
MOD_SHIFT_ALTGR: int = MOD_L_SHIFT | MOD_R_ALT  # 0x42 (Used for Capital Eszett ẞ DIN 2137)

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

# Controls and Functional Keys
KEY_ENTER: int = 0x28
KEY_ESCAPE: int = 0x29
KEY_BACKSPACE: int = 0x2A
KEY_TAB: int = 0x2B
KEY_SPACE: int = 0x2C
KEY_MINUS: int = 0x2D       # US: - _ | DE: ß ? \
KEY_EQUAL: int = 0x2E       # US: = + | DE: ´ `
KEY_LEFT_BRACE: int = 0x2F  # US: [ { | DE: Ü ü
KEY_RIGHT_BRACE: int = 0x30 # US: ] } | DE: + * ~
KEY_BACKSLASH: int = 0x31   # US: \ | | DE: (none/OEM)
KEY_NON_US_HASH: int = 0x32 # DE: # ' (next to Return, DIN 2137 C12)
KEY_SEMICOLON: int = 0x33   # US: ; : | DE: Ö ö
KEY_APOSTROPHE: int = 0x34  # US: ' " | DE: Ä ä
KEY_GRAVE: int = 0x35       # US: ` ~ | DE: ^ ° (Dead key)
KEY_COMMA: int = 0x36       # US: , < | DE: , ;
KEY_DOT: int = 0x37         # US: . > | DE: . :
KEY_SLASH: int = 0x38       # US: / ? | DE: - _
KEY_CAPS_LOCK: int = 0x39

# ISO Extra Key
KEY_NON_US_BACKSLASH: int = 0x64  # DE: < > | (ISO 105th key between L-Shift and Y)

# Host LED Bitmasks (Output Report Byte 0)
LED_NUM_LOCK: int = 0x01
LED_CAPS_LOCK: int = 0x02
LED_SCROLL_LOCK: int = 0x03
LED_COMPOSE: int = 0x08
LED_KANA: int = 0x10

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

# SDP Subclass for HID Keyboard
SDP_SUBCLASS_KEYBOARD: int = 0x40
