"""
Keymap Translation Engine for US QWERTY and German QWERTZ (DIN 2137-1).
Authoritative translation tables according to USB HID Usage Page 0x07 & DIN 2137-1.
"""
from dataclasses import dataclass, field
from enum import Enum
import unicodedata
from typing import List, Optional, Tuple, Dict

from .hid_constants import (
    MOD_NONE, MOD_L_CTRL, MOD_L_SHIFT, MOD_L_ALT, MOD_L_GUI,
    MOD_R_CTRL, MOD_R_SHIFT, MOD_R_ALT, MOD_SHIFT_ALTGR, MOD_CTRL_ALT, MOD_CTRL_SHIFT,
    KEY_NONE, KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I,
    KEY_J, KEY_K, KEY_L, KEY_M, KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S,
    KEY_T, KEY_U, KEY_V, KEY_W, KEY_X, KEY_Y, KEY_Z,
    KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_0,
    KEY_ENTER, KEY_ESCAPE, KEY_BACKSPACE, KEY_TAB, KEY_SPACE,
    KEY_MINUS, KEY_EQUAL, KEY_LEFT_BRACE, KEY_RIGHT_BRACE, KEY_BACKSLASH,
    KEY_NON_US_HASH, KEY_SEMICOLON, KEY_APOSTROPHE, KEY_GRAVE,
    KEY_COMMA, KEY_DOT, KEY_SLASH, KEY_NON_US_BACKSLASH,
    KEY_F1, KEY_F2, KEY_F3, KEY_F4, KEY_F5, KEY_F6, KEY_F7, KEY_F8, KEY_F9, KEY_F10, KEY_F11, KEY_F12,
    KEY_F13, KEY_F14, KEY_F15, KEY_F16, KEY_F17, KEY_F18, KEY_F19, KEY_F20, KEY_F21, KEY_F22, KEY_F23, KEY_F24,
    KEY_PRINT_SCREEN, KEY_SCROLL_LOCK, KEY_PAUSE, KEY_INSERT, KEY_HOME, KEY_PAGE_UP, KEY_DELETE, KEY_END, KEY_PAGE_DOWN,
    KEY_RIGHT_ARROW, KEY_LEFT_ARROW, KEY_DOWN_ARROW, KEY_UP_ARROW,
    BRACKETED_PASTE_START, BRACKETED_PASTE_END
)


class KeyLayout(Enum):
    US_QWERTY = "US_QWERTY"
    GERMAN_QWERTZ = "GERMAN_QWERTZ"


@dataclass(frozen=True)
class HidKeyStroke:
    modifier_mask: int
    usage_id: int
    is_dead_key: bool = False

    def __post_init__(self):
        if not (0 <= self.modifier_mask <= 0xFF):
            raise ValueError(f"Invalid modifier mask: {self.modifier_mask}")
        if not (0 <= self.usage_id <= 0xFF):
            raise ValueError(f"Invalid usage id: {self.usage_id}")


@dataclass
class HidReport:
    modifier: int = 0
    reserved: int = 0
    key_codes: List[int] = field(default_factory=lambda: [0, 0, 0, 0, 0, 0])

    def to_bytes(self) -> bytes:
        if len(self.key_codes) != 6:
            raise ValueError("Key codes list must contain exactly 6 elements")
        return bytes([self.modifier & 0xFF, self.reserved & 0xFF] + [k & 0xFF for k in self.key_codes])

    @classmethod
    def from_bytes(cls, data: bytes) -> "HidReport":
        if len(data) != 8:
            raise ValueError(f"HID report must be exactly 8 bytes, got {len(data)}")
        return cls(
            modifier=data[0],
            reserved=data[1],
            key_codes=list(data[2:8])
        )

    @classmethod
    def press(cls, modifier: int, usage_id: int) -> "HidReport":
        return cls(modifier=modifier, reserved=0, key_codes=[usage_id, 0, 0, 0, 0, 0])

    @classmethod
    def release(cls) -> "HidReport":
        return cls(modifier=0, reserved=0, key_codes=[0, 0, 0, 0, 0, 0])


# Hotkey Name -> HidKeyStroke mapping dictionary
HOTKEY_MAP: Dict[str, HidKeyStroke] = {
    "ESC": HidKeyStroke(MOD_NONE, KEY_ESCAPE),
    "ESCAPE": HidKeyStroke(MOD_NONE, KEY_ESCAPE),
    "TAB": HidKeyStroke(MOD_NONE, KEY_TAB),
    "ENTER": HidKeyStroke(MOD_NONE, KEY_ENTER),
    "BACKSPACE": HidKeyStroke(MOD_NONE, KEY_BACKSPACE),
    "CTRL_C": HidKeyStroke(MOD_L_CTRL, KEY_C),
    "CTRL_Z": HidKeyStroke(MOD_L_CTRL, KEY_Z),
    "CTRL_D": HidKeyStroke(MOD_L_CTRL, KEY_D),
    "CTRL_L": HidKeyStroke(MOD_L_CTRL, KEY_L),
    "CTRL_A": HidKeyStroke(MOD_L_CTRL, KEY_A),
    "CTRL_E": HidKeyStroke(MOD_L_CTRL, KEY_E),
    "CTRL_R": HidKeyStroke(MOD_L_CTRL, KEY_R),
    "ALT_TAB": HidKeyStroke(MOD_L_ALT, KEY_TAB),
    "UP": HidKeyStroke(MOD_NONE, KEY_UP_ARROW),
    "DOWN": HidKeyStroke(MOD_NONE, KEY_DOWN_ARROW),
    "LEFT": HidKeyStroke(MOD_NONE, KEY_LEFT_ARROW),
    "RIGHT": HidKeyStroke(MOD_NONE, KEY_RIGHT_ARROW),
    "HOME": HidKeyStroke(MOD_NONE, KEY_HOME),
    "END": HidKeyStroke(MOD_NONE, KEY_END),
    "PAGE_UP": HidKeyStroke(MOD_NONE, KEY_PAGE_UP),
    "PAGE_DOWN": HidKeyStroke(MOD_NONE, KEY_PAGE_DOWN),
    "INSERT": HidKeyStroke(MOD_NONE, KEY_INSERT),
    "DELETE": HidKeyStroke(MOD_NONE, KEY_DELETE),
    "F1": HidKeyStroke(MOD_NONE, KEY_F1),
    "F2": HidKeyStroke(MOD_NONE, KEY_F2),
    "F3": HidKeyStroke(MOD_NONE, KEY_F3),
    "F4": HidKeyStroke(MOD_NONE, KEY_F4),
    "F5": HidKeyStroke(MOD_NONE, KEY_F5),
    "F6": HidKeyStroke(MOD_NONE, KEY_F6),
    "F7": HidKeyStroke(MOD_NONE, KEY_F7),
    "F8": HidKeyStroke(MOD_NONE, KEY_F8),
    "F9": HidKeyStroke(MOD_NONE, KEY_F9),
    "F10": HidKeyStroke(MOD_NONE, KEY_F10),
    "F11": HidKeyStroke(MOD_NONE, KEY_F11),
    "F12": HidKeyStroke(MOD_NONE, KEY_F12),
    "F13": HidKeyStroke(MOD_NONE, KEY_F13),
    "F14": HidKeyStroke(MOD_NONE, KEY_F14),
    "F15": HidKeyStroke(MOD_NONE, KEY_F15),
    "F16": HidKeyStroke(MOD_NONE, KEY_F16),
    "F17": HidKeyStroke(MOD_NONE, KEY_F17),
    "F18": HidKeyStroke(MOD_NONE, KEY_F18),
    "F19": HidKeyStroke(MOD_NONE, KEY_F19),
    "F20": HidKeyStroke(MOD_NONE, KEY_F20),
    "F21": HidKeyStroke(MOD_NONE, KEY_F21),
    "F22": HidKeyStroke(MOD_NONE, KEY_F22),
    "F23": HidKeyStroke(MOD_NONE, KEY_F23),
    "F24": HidKeyStroke(MOD_NONE, KEY_F24),
}


class KeymapTranslator:
    """Base Keymap Translator interface."""

    def __init__(self, layout: KeyLayout):
        self.layout = layout

    def translate_char(self, char: str) -> List[HidKeyStroke]:
        raise NotImplementedError

    def translate_string(self, text: str) -> List[HidKeyStroke]:
        # Step 1: Enforce Unicode NFC canonical composition
        normalized = unicodedata.normalize("NFC", text)
        strokes: List[HidKeyStroke] = []
        for ch in normalized:
            strokes.extend(self.translate_char(ch))
        return strokes

    def translate_hotkey(self, name: str) -> Optional[HidKeyStroke]:
        normalized = name.strip().upper().replace("+", "_").replace("-", "_").replace(" ", "_")
        return HOTKEY_MAP.get(normalized, None)

    def wrap_bracketed_paste(self, text: str) -> str:
        return f"{BRACKETED_PASTE_START}{text}{BRACKETED_PASTE_END}"


class UsQwertyKeymap(KeymapTranslator):
    """
    US QWERTY Keymap Translator covering all 95 ASCII printable characters
    plus control keys (\\n, \\t, \\b).
    """

    # Static mapping table: char -> (modifier_mask, usage_id)
    _MAP = {
        # Control keys
        "\b": (MOD_NONE, KEY_BACKSPACE),
        "\t": (MOD_NONE, KEY_TAB),
        "\n": (MOD_L_SHIFT, KEY_ENTER), # Soft Enter (Shift+Enter)
        "\r": (MOD_L_SHIFT, KEY_ENTER), # Soft Enter (Shift+Enter)
        " ":  (MOD_NONE, KEY_SPACE),

        # Numbers & unshifted punctuation
        "0": (MOD_NONE, KEY_0),
        "1": (MOD_NONE, KEY_1),
        "2": (MOD_NONE, KEY_2),
        "3": (MOD_NONE, KEY_3),
        "4": (MOD_NONE, KEY_4),
        "5": (MOD_NONE, KEY_5),
        "6": (MOD_NONE, KEY_6),
        "7": (MOD_NONE, KEY_7),
        "8": (MOD_NONE, KEY_8),
        "9": (MOD_NONE, KEY_9),

        "`": (MOD_NONE, KEY_GRAVE),
        "-": (MOD_NONE, KEY_MINUS),
        "=": (MOD_NONE, KEY_EQUAL),
        "[": (MOD_NONE, KEY_LEFT_BRACE),
        "]": (MOD_NONE, KEY_RIGHT_BRACE),
        "\\": (MOD_NONE, KEY_BACKSLASH),
        ";": (MOD_NONE, KEY_SEMICOLON),
        "'": (MOD_NONE, KEY_APOSTROPHE),
        ",": (MOD_NONE, KEY_COMMA),
        ".": (MOD_NONE, KEY_DOT),
        "/": (MOD_NONE, KEY_SLASH),

        # Shifted symbols
        "~": (MOD_L_SHIFT, KEY_GRAVE),
        "!": (MOD_L_SHIFT, KEY_1),
        "@": (MOD_L_SHIFT, KEY_2),
        "#": (MOD_L_SHIFT, KEY_3),
        "$": (MOD_L_SHIFT, KEY_4),
        "%": (MOD_L_SHIFT, KEY_5),
        "^": (MOD_L_SHIFT, KEY_6),
        "&": (MOD_L_SHIFT, KEY_7),
        "*": (MOD_L_SHIFT, KEY_8),
        "(": (MOD_L_SHIFT, KEY_9),
        ")": (MOD_L_SHIFT, KEY_0),
        "_": (MOD_L_SHIFT, KEY_MINUS),
        "+": (MOD_L_SHIFT, KEY_EQUAL),
        "{": (MOD_L_SHIFT, KEY_LEFT_BRACE),
        "}": (MOD_L_SHIFT, KEY_RIGHT_BRACE),
        "|": (MOD_L_SHIFT, KEY_BACKSLASH),
        ":": (MOD_L_SHIFT, KEY_SEMICOLON),
        '"': (MOD_L_SHIFT, KEY_APOSTROPHE),
        "<": (MOD_L_SHIFT, KEY_COMMA),
        ">": (MOD_L_SHIFT, KEY_DOT),
        "?": (MOD_L_SHIFT, KEY_SLASH),
    }

    # Transliteration fallbacks for common unmapped symbols
    _FALLBACKS = {
        "“": '"', "”": '"', "„": '"',
        "‘": "'", "’": "'", "‚": "'",
        "—": "-", "–": "-", "―": "-",
        "…": "...",
        "«": '"', "»": '"',
    }

    def __init__(self):
        super().__init__(KeyLayout.US_QWERTY)
        # Populate lowercase and uppercase letters a-z, A-Z
        self._table = dict(self._MAP)
        for i in range(26):
            lower_ch = chr(ord('a') + i)
            upper_ch = chr(ord('A') + i)
            usage = KEY_A + i
            self._table[lower_ch] = (MOD_NONE, usage)
            self._table[upper_ch] = (MOD_L_SHIFT, usage)

    def translate_char(self, char: str) -> List[HidKeyStroke]:
        if char in self._table:
            mod, usage = self._table[char]
            return [HidKeyStroke(modifier_mask=mod, usage_id=usage)]
        
        # Check fallbacks
        if char in self._FALLBACKS:
            sub = self._FALLBACKS[char]
            res = []
            for c in sub:
                res.extend(self.translate_char(c))
            return res

        # Unknown / unmapped unicode char -> fallback to empty list
        return []


class GermanQwertzKeymap(KeymapTranslator):
    """
    German QWERTZ Keymap Translator (DIN 2137-1).
    Handles:
    - Y / Z physical swap
    - Dedicated Umlauts (ä, ö, ü, Ä, Ö, Ü, ß, ẞ)
    - AltGr (0x40) 3rd-level characters (@, €, \\, ~, {, }, [, ], |, µ, ², ³)
    - ISO Extra Key (0x64: <, >, |) and Hash Key (0x32: #, ')
    - Dead Key Auto-Space Injection (^, ´, `, ~)
    """

    # Static German Mapping Table
    _BASE_MAP = {
        # Controls
        "\b": (MOD_NONE, KEY_BACKSPACE),
        "\t": (MOD_NONE, KEY_TAB),
        "\n": (MOD_L_SHIFT, KEY_ENTER), # Soft Enter (Shift+Enter)
        "\r": (MOD_L_SHIFT, KEY_ENTER), # Soft Enter (Shift+Enter)
        " ":  (MOD_NONE, KEY_SPACE),

        # German Number Row (unmodified)
        "1": (MOD_NONE, KEY_1),
        "2": (MOD_NONE, KEY_2),
        "3": (MOD_NONE, KEY_3),
        "4": (MOD_NONE, KEY_4),
        "5": (MOD_NONE, KEY_5),
        "6": (MOD_NONE, KEY_6),
        "7": (MOD_NONE, KEY_7),
        "8": (MOD_NONE, KEY_8),
        "9": (MOD_NONE, KEY_9),
        "0": (MOD_NONE, KEY_0),

        # German Number Row (Shifted)
        "!": (MOD_L_SHIFT, KEY_1),
        '"': (MOD_L_SHIFT, KEY_2),
        "§": (MOD_L_SHIFT, KEY_3),
        "$": (MOD_L_SHIFT, KEY_4),
        "%": (MOD_L_SHIFT, KEY_5),
        "&": (MOD_L_SHIFT, KEY_6),
        "/": (MOD_L_SHIFT, KEY_7),
        "(": (MOD_L_SHIFT, KEY_8),
        ")": (MOD_L_SHIFT, KEY_9),
        "=": (MOD_L_SHIFT, KEY_0),

        # German Punctuation & Dedicated Keys
        ",": (MOD_NONE, KEY_COMMA),
        ";": (MOD_L_SHIFT, KEY_COMMA),
        ".": (MOD_NONE, KEY_DOT),
        ":": (MOD_L_SHIFT, KEY_DOT),
        "-": (MOD_NONE, KEY_SLASH),
        "_": (MOD_L_SHIFT, KEY_SLASH),
        "+": (MOD_NONE, KEY_RIGHT_BRACE),
        "*": (MOD_L_SHIFT, KEY_RIGHT_BRACE),
        "#": (MOD_NONE, KEY_NON_US_HASH),
        "'": (MOD_L_SHIFT, KEY_NON_US_HASH),
        "<": (MOD_NONE, KEY_NON_US_BACKSLASH),
        ">": (MOD_L_SHIFT, KEY_NON_US_BACKSLASH),
        "°": (MOD_L_SHIFT, KEY_GRAVE),

        # Dedicated German Umlauts & Eszett
        "ß": (MOD_NONE, KEY_MINUS),
        "?": (MOD_L_SHIFT, KEY_MINUS),
        "ä": (MOD_NONE, KEY_APOSTROPHE),
        "Ä": (MOD_L_SHIFT, KEY_APOSTROPHE),
        "ö": (MOD_NONE, KEY_SEMICOLON),
        "Ö": (MOD_L_SHIFT, KEY_SEMICOLON),
        "ü": (MOD_NONE, KEY_LEFT_BRACE),
        "Ü": (MOD_L_SHIFT, KEY_LEFT_BRACE),
        "ẞ": (MOD_SHIFT_ALTGR, KEY_MINUS),  # Capital Sharp S (DIN 2137:2018)

        # AltGr 3rd-Level Characters
        "@": (MOD_R_ALT, KEY_Q),
        "€": (MOD_R_ALT, KEY_E),
        "\\": (MOD_R_ALT, KEY_MINUS),
        "{": (MOD_R_ALT, KEY_7),
        "[": (MOD_R_ALT, KEY_8),
        "]": (MOD_R_ALT, KEY_9),
        "}": (MOD_R_ALT, KEY_0),
        "|": (MOD_R_ALT, KEY_NON_US_BACKSLASH),
        "µ": (MOD_R_ALT, KEY_M),
        "²": (MOD_R_ALT, KEY_2),
        "³": (MOD_R_ALT, KEY_3),
    }

    # Dead Keys that require space injection when solitary
    _DEAD_KEYS = {
        "^": (MOD_NONE, KEY_GRAVE),
        "´": (MOD_NONE, KEY_EQUAL),
        "`": (MOD_L_SHIFT, KEY_EQUAL),
        "~": (MOD_R_ALT, KEY_RIGHT_BRACE),
    }

    _FALLBACKS = {
        "“": '"', "”": '"', "„": '"',
        "‘": "'", "’": "'", "‚": "'",
        "—": "-", "–": "-", "―": "-",
        "…": "...",
        "«": '"', "»": '"',
    }

    def __init__(self, auto_space_dead_keys: bool = True):
        super().__init__(KeyLayout.GERMAN_QWERTZ)
        self.auto_space_dead_keys = auto_space_dead_keys
        self._table = dict(self._BASE_MAP)

        # Populate standard letters a-x (skipping y and z for custom swap)
        for i in range(24):  # A through X
            lower_ch = chr(ord('a') + i)
            upper_ch = chr(ord('A') + i)
            usage = KEY_A + i
            self._table[lower_ch] = (MOD_NONE, usage)
            self._table[upper_ch] = (MOD_L_SHIFT, usage)

        # German Y / Z physical swap:
        # Lowercase 'y' is physical key B01 (KEY_Z = 0x1D)
        # Uppercase 'Y' is physical key B01 (KEY_Z = 0x1D with Shift)
        # Lowercase 'z' is physical key D06 (KEY_Y = 0x1C)
        # Uppercase 'Z' is physical key D06 (KEY_Y = 0x1C with Shift)
        self._table["y"] = (MOD_NONE, KEY_Z)
        self._table["Y"] = (MOD_L_SHIFT, KEY_Z)
        self._table["z"] = (MOD_NONE, KEY_Y)
        self._table["Z"] = (MOD_L_SHIFT, KEY_Y)

    def translate_char(self, char: str) -> List[HidKeyStroke]:
        # Check standard table
        if char in self._table:
            mod, usage = self._table[char]
            return [HidKeyStroke(modifier_mask=mod, usage_id=usage)]

        # Check dead keys
        if char in self._DEAD_KEYS:
            mod, usage = self._DEAD_KEYS[char]
            dead_stroke = HidKeyStroke(modifier_mask=mod, usage_id=usage, is_dead_key=True)
            if self.auto_space_dead_keys:
                # Emit dead key followed by Space (0x2C)
                space_stroke = HidKeyStroke(modifier_mask=MOD_NONE, usage_id=KEY_SPACE)
                return [dead_stroke, space_stroke]
            return [dead_stroke]

        # Check fallbacks
        if char in self._FALLBACKS:
            sub = self._FALLBACKS[char]
            res = []
            for c in sub:
                res.extend(self.translate_char(c))
            return res

        return []
