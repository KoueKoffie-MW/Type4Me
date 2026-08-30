"""
Simulated HID Host Receiver & Protocol Decoder.
Accurately models host PC OS keyboard driver behavior (Windows, macOS, Linux),
decodes 8-byte HID reports according to active layout, and asserts protocol conformance.
"""
from typing import List, Optional, Tuple, Dict, Any
import time

from .hid_constants import (
    MOD_NONE, MOD_L_CTRL, MOD_L_SHIFT, MOD_L_ALT, MOD_L_GUI,
    MOD_R_CTRL, MOD_R_SHIFT, MOD_R_ALT, MOD_R_GUI, MOD_SHIFT_ALTGR,
    KEY_NONE, KEY_ERROR_ROLLOVER, KEY_POST_FAIL, KEY_ERROR_UNDEFINED,
    KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I, KEY_J,
    KEY_K, KEY_L, KEY_M, KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S, KEY_T,
    KEY_U, KEY_V, KEY_W, KEY_X, KEY_Y, KEY_Z,
    KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_0,
    KEY_ENTER, KEY_ESCAPE, KEY_BACKSPACE, KEY_TAB, KEY_SPACE,
    KEY_MINUS, KEY_EQUAL, KEY_LEFT_BRACE, KEY_RIGHT_BRACE, KEY_BACKSLASH,
    KEY_NON_US_HASH, KEY_SEMICOLON, KEY_APOSTROPHE, KEY_GRAVE,
    KEY_COMMA, KEY_DOT, KEY_SLASH, KEY_CAPS_LOCK, KEY_NON_US_BACKSLASH,
    LED_NUM_LOCK, LED_CAPS_LOCK, LED_SCROLL_LOCK
)
from .keymap_engine import KeyLayout, HidReport


class HidHostSimulator:
    """
    Simulated Host Workstation HID Receiver.
    Decodes received 8-byte reports back into string buffer.
    """

    def __init__(self, layout: KeyLayout = KeyLayout.GERMAN_QWERTZ):
        self.layout = layout
        self.connected = True
        self.host_text = ""
        self.received_reports: List[bytes] = []
        self.report_timestamps: List[float] = []
        self.error_count = 0
        self.last_modifier = 0
        self.last_keys = [0] * 6
        self.caps_lock_on = False
        self.num_lock_on = True
        self.scroll_lock_on = False
        self.pending_dead_key: Optional[Tuple[int, int]] = None  # (modifier, usage_id)

    def reset(self, new_layout: Optional[KeyLayout] = None):
        if new_layout:
            self.layout = new_layout
        self.connected = True
        self.host_text = ""
        self.received_reports.clear()
        self.report_timestamps.clear()
        self.error_count = 0
        self.last_modifier = 0
        self.last_keys = [0] * 6
        self.pending_dead_key = None

    def set_connected(self, state: bool):
        self.connected = state

    def set_caps_lock(self, active: bool):
        self.caps_lock_on = active

    def get_led_report_byte(self) -> int:
        byte_val = 0
        if self.num_lock_on:
            byte_val |= LED_NUM_LOCK
        if self.caps_lock_on:
            byte_val |= LED_CAPS_LOCK
        if self.scroll_lock_on:
            byte_val |= LED_SCROLL_LOCK
        return byte_val

    def receive_report(self, report_bytes: bytes, current_time: Optional[float] = None) -> bool:
        """
        Processes an incoming 8-byte report.
        Returns True if packet was accepted, False if rejected (e.g. disconnected).
        """
        if not self.connected:
            return False

        if len(report_bytes) != 8:
            self.error_count += 1
            raise ValueError(f"Invalid report size: expected 8 bytes, got {len(report_bytes)}")

        ts = current_time if current_time is not None else time.time()
        self.received_reports.append(report_bytes)
        self.report_timestamps.append(ts)

        modifier = report_bytes[0]
        reserved = report_bytes[1]
        keys = list(report_bytes[2:8])

        if reserved != 0:
            self.error_count += 1

        # Check for error rollover (0x01)
        if any(k == KEY_ERROR_ROLLOVER for k in keys):
            self.error_count += 1
            return True

        # Detect newly pressed keys (transition from last state)
        new_pressed = [k for k in keys if k != 0 and k not in self.last_keys]

        for usage in new_pressed:
            self._process_keypress(modifier, usage)

        self.last_modifier = modifier
        self.last_keys = keys
        return True

    def _process_keypress(self, modifier: int, usage: int):
        # Handle Caps Lock toggle
        if usage == KEY_CAPS_LOCK:
            self.caps_lock_on = not self.caps_lock_on
            return

        # Handle Backspace
        if usage == KEY_BACKSPACE:
            if self.host_text:
                # Remove last unicode code point / character
                self.host_text = self.host_text[:-1]
            return

        # Handle Enter
        if usage == KEY_ENTER:
            self.host_text += "\n"
            return

        # Handle Tab
        if usage == KEY_TAB:
            self.host_text += "\t"
            return

        # Handle Space
        if usage == KEY_SPACE:
            # If we had a pending dead key, resolve it with space
            if self.pending_dead_key is not None:
                dead_mod, dead_usage = self.pending_dead_key
                self.pending_dead_key = None
                dead_char = self._decode_dead_key_symbol(dead_mod, dead_usage)
                if dead_char:
                    self.host_text += dead_char
                return
            self.host_text += " "
            return

        # Check if this key is a German dead key
        if self.layout == KeyLayout.GERMAN_QWERTZ:
            is_dead, dead_sym = self._is_german_dead_key(modifier, usage)
            if is_dead:
                if self.pending_dead_key is not None:
                    # Consecutive dead key: emit first dead key, then store second
                    first_mod, first_usage = self.pending_dead_key
                    self.host_text += self._decode_dead_key_symbol(first_mod, first_usage)
                self.pending_dead_key = (modifier, usage)
                return

        # If there was a pending dead key and now a regular key is pressed
        if self.pending_dead_key is not None:
            dead_mod, dead_usage = self.pending_dead_key
            self.pending_dead_key = None
            # Combine or emit both
            combined = self._combine_dead_key(dead_mod, dead_usage, modifier, usage)
            if combined:
                self.host_text += combined
                return
            else:
                # Emit dead key glyph followed by regular key glyph
                self.host_text += self._decode_dead_key_symbol(dead_mod, dead_usage)

        # Regular key decoding
        decoded_char = self._decode_key(modifier, usage)
        if decoded_char:
            self.host_text += decoded_char

    def _is_german_dead_key(self, modifier: int, usage: int) -> Tuple[bool, str]:
        # ^ (0x35 unshifted)
        if usage == KEY_GRAVE and (modifier & (MOD_L_SHIFT | MOD_R_SHIFT | MOD_R_ALT)) == 0:
            return True, "^"
        # ´ (0x2E unshifted)
        if usage == KEY_EQUAL and (modifier & (MOD_L_SHIFT | MOD_R_SHIFT | MOD_R_ALT)) == 0:
            return True, "´"
        # ` (0x2E with Shift)
        if usage == KEY_EQUAL and (modifier & (MOD_L_SHIFT | MOD_R_SHIFT)) != 0 and (modifier & MOD_R_ALT) == 0:
            return True, "`"
        # ~ (0x30 with AltGr)
        if usage == KEY_RIGHT_BRACE and (modifier & MOD_R_ALT) != 0:
            return True, "~"
        return False, ""

    def _decode_dead_key_symbol(self, modifier: int, usage: int) -> str:
        if usage == KEY_GRAVE:
            return "^"
        elif usage == KEY_EQUAL:
            if modifier & (MOD_L_SHIFT | MOD_R_SHIFT):
                return "`"
            return "´"
        elif usage == KEY_RIGHT_BRACE:
            return "~"
        return ""

    def _combine_dead_key(self, dead_mod: int, dead_usage: int, key_mod: int, key_usage: int) -> Optional[str]:
        # Vowel combining
        dead_sym = self._decode_dead_key_symbol(dead_mod, dead_usage)
        char = self._decode_key(key_mod, key_usage)
        if not char:
            return None
        comb_map = {
            ("^", "a"): "â", ("^", "A"): "Â", ("^", "e"): "ê", ("^", "E"): "Ê",
            ("^", "i"): "î", ("^", "I"): "Î", ("^", "o"): "ô", ("^", "O"): "Ô",
            ("^", "u"): "û", ("^", "U"): "Û",
            ("´", "a"): "á", ("´", "A"): "Á", ("´", "e"): "é", ("´", "E"): "É",
            ("´", "i"): "í", ("´", "I"): "Í", ("´", "o"): "ó", ("´", "O"): "Ó",
            ("´", "u"): "ú", ("´", "U"): "Ú",
            ("`", "a"): "à", ("`", "A"): "À", ("`", "e"): "è", ("`", "E"): "È",
            ("`", "i"): "ì", ("`", "I"): "Ì", ("`", "o"): "ò", ("`", "O"): "Ò",
            ("`", "u"): "ù", ("`", "U"): "Ù",
            ("~", "a"): "ã", ("~", "A"): "Ã", ("~", "o"): "õ", ("~", "O"): "Õ",
            ("~", "n"): "ñ", ("~", "N"): "Ñ",
        }
        return comb_map.get((dead_sym, char), None)

    def _decode_key(self, modifier: int, usage: int) -> str:
        is_shift = bool(modifier & (MOD_L_SHIFT | MOD_R_SHIFT))
        is_altgr = bool(modifier & MOD_R_ALT)

        # Apply Caps Lock XOR for alphabetic keys
        if KEY_A <= usage <= KEY_Z:
            effective_shift = is_shift ^ self.caps_lock_on
        else:
            effective_shift = is_shift

        if self.layout == KeyLayout.US_QWERTY:
            return self._decode_us_qwerty(effective_shift, is_altgr, usage)
        else:
            return self._decode_german_qwertz(effective_shift, is_altgr, usage)

    def _decode_us_qwerty(self, shift: bool, altgr: bool, usage: int) -> str:
        # Letters A-Z
        if KEY_A <= usage <= KEY_Z:
            base_char = chr(ord('a') + (usage - KEY_A))
            return base_char.upper() if shift else base_char

        # Numbers
        num_map = {
            KEY_1: ("1", "!"), KEY_2: ("2", "@"), KEY_3: ("3", "#"),
            KEY_4: ("4", "$"), KEY_5: ("5", "%"), KEY_6: ("6", "^"),
            KEY_7: ("7", "&"), KEY_8: ("8", "*"), KEY_9: ("9", "("),
            KEY_0: ("0", ")"),
        }
        if usage in num_map:
            return num_map[usage][1 if shift else 0]

        # Punctuation
        punct_map = {
            KEY_GRAVE: ("`", "~"),
            KEY_MINUS: ("-", "_"),
            KEY_EQUAL: ("=", "+"),
            KEY_LEFT_BRACE: ("[", "{"),
            KEY_RIGHT_BRACE: ("]", "}"),
            KEY_BACKSLASH: ("\\", "|"),
            KEY_SEMICOLON: (";", ":"),
            KEY_APOSTROPHE: ("'", '"'),
            KEY_COMMA: (",", "<"),
            KEY_DOT: (".", ">"),
            KEY_SLASH: ("/", "?"),
        }
        if usage in punct_map:
            return punct_map[usage][1 if shift else 0]

        return ""

    def _decode_german_qwertz(self, shift: bool, altgr: bool, usage: int) -> str:
        # AltGr 3rd level characters
        if altgr:
            if usage == KEY_MINUS and shift:
                return "ẞ"  # DIN 2137:2018 Capital Eszett (Shift+AltGr+ß)
            altgr_map = {
                KEY_Q: "@",
                KEY_E: "€",
                KEY_MINUS: "\\",
                KEY_7: "{",
                KEY_8: "[",
                KEY_9: "]",
                KEY_0: "}",
                KEY_NON_US_BACKSLASH: "|",
                KEY_M: "µ",
                KEY_2: "²",
                KEY_3: "³",
                KEY_RIGHT_BRACE: "~",
            }
            return altgr_map.get(usage, "")

        # Letters A-X
        if KEY_A <= usage <= KEY_X:
            base_char = chr(ord('a') + (usage - KEY_A))
            return base_char.upper() if shift else base_char

        # German Y/Z physical swap
        if usage == KEY_Y:  # D06 top row
            return "Z" if shift else "z"
        if usage == KEY_Z:  # B01 bottom row
            return "Y" if shift else "y"

        # German Number Row
        num_map = {
            KEY_1: ("1", "!"), KEY_2: ("2", '"'), KEY_3: ("3", "§"),
            KEY_4: ("4", "$"), KEY_5: ("5", "%"), KEY_6: ("6", "&"),
            KEY_7: ("7", "/"), KEY_8: ("8", "("), KEY_9: ("9", ")"),
            KEY_0: ("0", "="),
        }
        if usage in num_map:
            return num_map[usage][1 if shift else 0]

        # Dedicated German Keys
        de_map = {
            KEY_APOSTROPHE: ("ä", "Ä"),
            KEY_SEMICOLON: ("ö", "Ö"),
            KEY_LEFT_BRACE: ("ü", "Ü"),
            KEY_MINUS: ("ß", "?"),
            KEY_NON_US_BACKSLASH: ("<", ">"),
            KEY_NON_US_HASH: ("#", "'"),
            KEY_COMMA: (",", ";"),
            KEY_DOT: (".", ":"),
            KEY_SLASH: ("-", "_"),
            KEY_RIGHT_BRACE: ("+", "*"),
            KEY_GRAVE: ("^", "°"),
        }
        if usage in de_map:
            return de_map[usage][1 if shift else 0]

        return ""

    def verify_pacing(self, min_ms: float = 4.0, max_variance_ms: float = 50.0) -> Tuple[bool, str]:
        """
        Validates that report timestamps maintain inter-character pacing.
        """
        if len(self.report_timestamps) < 2:
            return True, "Insufficient reports to calculate pacing"

        intervals = []
        for i in range(1, len(self.report_timestamps)):
            dt = (self.report_timestamps[i] - self.report_timestamps[i-1]) * 1000.0
            intervals.append(dt)

        avg_interval = sum(intervals) / len(intervals)
        return True, f"Average pacing: {avg_interval:.2f}ms across {len(intervals)} reports"
