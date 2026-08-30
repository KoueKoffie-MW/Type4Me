"""
Feature 2: US QWERTY Keymap Translation.
Verifies exact 1:1 mapping of all 95 ASCII printable characters (32-126) and
controls (\\n, \\t, \\b) to US HID usage codes and Shift modifiers.
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_SHIFT, KEY_A, KEY_Z,
    KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_0,
    KEY_SPACE, KEY_ENTER, KEY_TAB, KEY_BACKSPACE, KEY_GRAVE, KEY_MINUS, KEY_EQUAL,
    KEY_LEFT_BRACE, KEY_RIGHT_BRACE, KEY_BACKSLASH, KEY_SEMICOLON,
    KEY_APOSTROPHE, KEY_COMMA, KEY_DOT, KEY_SLASH
)
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature02UsQwertyKeymap(unittest.TestCase):
    """Tier 1: Feature 2 - US QWERTY Keymap Translation"""

    def setUp(self):
        self.translator = UsQwertyKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_lowercase_and_uppercase_letters(self):
        """TC01: Translates all lowercase a-z (unmodified) and uppercase A-Z (Shift)."""
        for i in range(26):
            lower_ch = chr(ord('a') + i)
            upper_ch = chr(ord('A') + i)
            expected_usage = KEY_A + i

            # Test lowercase
            strokes_lower = self.translator.translate_char(lower_ch)
            self.assertEqual(len(strokes_lower), 1)
            self.assertEqual(strokes_lower[0].modifier_mask, MOD_NONE)
            self.assertEqual(strokes_lower[0].usage_id, expected_usage)

            # Test uppercase
            strokes_upper = self.translator.translate_char(upper_ch)
            self.assertEqual(len(strokes_upper), 1)
            self.assertEqual(strokes_upper[0].modifier_mask, MOD_L_SHIFT)
            self.assertEqual(strokes_upper[0].usage_id, expected_usage)

    def test_tc02_numbers_and_shifted_symbols(self):
        """TC02: Translates number row 0-9 and shifted symbols (!@#$%^&*())."""
        pairs = [
            ("1", "!", KEY_1), ("2", "@", KEY_2), ("3", "#", KEY_3),
            ("4", "$", KEY_4), ("5", "%", KEY_5), ("6", "^", KEY_6),
            ("7", "&", KEY_7), ("8", "*", KEY_8), ("9", "(", KEY_9),
            ("0", ")", KEY_0),
        ]
        for unshifted, shifted, expected_usage in pairs:
            # Unshifted digit
            s1 = self.translator.translate_char(unshifted)
            self.assertEqual(s1[0].modifier_mask, MOD_NONE)
            self.assertEqual(s1[0].usage_id, expected_usage)

            # Shifted symbol
            s2 = self.translator.translate_char(shifted)
            self.assertEqual(s2[0].modifier_mask, MOD_L_SHIFT)
            self.assertEqual(s2[0].usage_id, expected_usage)

    def test_tc03_punctuation_and_brackets(self):
        """TC03: Translates punctuation, quotes, brackets, slash, and backslash."""
        punct_cases = [
            ("`", "~", KEY_GRAVE),
            ("-", "_", KEY_MINUS),
            ("=", "+", KEY_EQUAL),
            ("[", "{", KEY_LEFT_BRACE),
            ("]", "}", KEY_RIGHT_BRACE),
            ("\\", "|", KEY_BACKSLASH),
            (";", ":", KEY_SEMICOLON),
            ("'", '"', KEY_APOSTROPHE),
            (",", "<", KEY_COMMA),
            (".", ">", KEY_DOT),
            ("/", "?", KEY_SLASH),
        ]
        for unshifted, shifted, usage in punct_cases:
            s_un = self.translator.translate_char(unshifted)
            self.assertEqual(s_un[0].modifier_mask, MOD_NONE)
            self.assertEqual(s_un[0].usage_id, usage)

            s_sh = self.translator.translate_char(shifted)
            self.assertEqual(s_sh[0].modifier_mask, MOD_L_SHIFT)
            self.assertEqual(s_sh[0].usage_id, usage)

    def test_tc04_control_characters(self):
        """TC04: Translates control characters (\\n, \\t, \\b, Space)."""
        controls = [
            ("\n", MOD_L_SHIFT, KEY_ENTER), # Soft Enter (Shift+Enter)
            ("\t", MOD_NONE, KEY_TAB),
            ("\b", MOD_NONE, KEY_BACKSPACE),
            (" ",  MOD_NONE, KEY_SPACE),
        ]
        for ch, expected_mod, expected_usage in controls:
            strokes = self.translator.translate_char(ch)
            self.assertEqual(len(strokes), 1)
            self.assertEqual(strokes[0].modifier_mask, expected_mod)
            self.assertEqual(strokes[0].usage_id, expected_usage)

    def test_tc05_end_to_end_qwerty_sentence_reconstruction(self):
        """TC05: End-to-end typing of complex ASCII text and host reconstruction."""
        test_text = "The quick brown fox jumps over 13 lazy dogs! { [ ( $100 + 20% = $120 ) ] }."
        self.dispatcher.dispatch_burst(test_text)
        self.assertEqual(self.host.host_text, test_text)


if __name__ == "__main__":
    unittest.main()
