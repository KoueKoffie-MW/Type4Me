"""
Feature 5: Dead Key Auto-Space Injection.
Verifies that solitary dead keys (^, ´, `, ~) automatically emit a follow-up
Space keystroke (0x2C) to prevent host IME buffer merging on German layouts.
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_SHIFT, MOD_R_ALT, KEY_GRAVE, KEY_EQUAL, KEY_RIGHT_BRACE, KEY_SPACE
)
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature05DeadKeySpaceInjection(unittest.TestCase):
    """Tier 1: Feature 5 - Dead Key Auto-Space Injection"""

    def setUp(self):
        self.translator = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_circumflex_emits_dead_key_and_space(self):
        """TC01: Circumflex ^ generates [0x35 (Mod 0x00), 0x2C Space (Mod 0x00)]."""
        strokes = self.translator.translate_char("^")
        self.assertEqual(len(strokes), 2)
        self.assertEqual(strokes[0].usage_id, KEY_GRAVE)
        self.assertEqual(strokes[0].modifier_mask, MOD_NONE)
        self.assertTrue(strokes[0].is_dead_key)
        self.assertEqual(strokes[1].usage_id, KEY_SPACE)
        self.assertEqual(strokes[1].modifier_mask, MOD_NONE)

    def test_tc02_acute_accent_emits_dead_key_and_space(self):
        """TC02: Acute accent ´ generates [0x2E (Mod 0x00), 0x2C Space (Mod 0x00)]."""
        strokes = self.translator.translate_char("´")
        self.assertEqual(len(strokes), 2)
        self.assertEqual(strokes[0].usage_id, KEY_EQUAL)
        self.assertEqual(strokes[0].modifier_mask, MOD_NONE)
        self.assertEqual(strokes[1].usage_id, KEY_SPACE)

    def test_tc03_grave_accent_emits_dead_key_and_space(self):
        """TC03: Grave accent ` generates [0x2E (Mod 0x02 Shift), 0x2C Space (Mod 0x00)]."""
        strokes = self.translator.translate_char("`")
        self.assertEqual(len(strokes), 2)
        self.assertEqual(strokes[0].usage_id, KEY_EQUAL)
        self.assertEqual(strokes[0].modifier_mask, MOD_L_SHIFT)
        self.assertEqual(strokes[1].usage_id, KEY_SPACE)

    def test_tc04_tilde_emits_dead_key_and_space(self):
        """TC04: Tilde ~ generates [0x30 (Mod 0x40 AltGr), 0x2C Space (Mod 0x00)]."""
        strokes = self.translator.translate_char("~")
        self.assertEqual(len(strokes), 2)
        self.assertEqual(strokes[0].usage_id, KEY_RIGHT_BRACE)
        self.assertEqual(strokes[0].modifier_mask, MOD_R_ALT)
        self.assertEqual(strokes[1].usage_id, KEY_SPACE)

    def test_tc05_end_to_end_dead_keys_prevent_vowel_accents(self):
        """TC05: Typing '^a', '´e', '`o', '~n' renders literal characters instead of â, é, è, ñ."""
        test_strings = [
            ("^a", "^a"),
            ("´e", "´e"),
            ("`o", "`o"),
            ("~n", "~n"),
            ("x^2 + y^3 ~ 100", "x^2 + y^3 ~ 100"),
        ]
        for input_text, expected in test_strings:
            self.host.reset(KeyLayout.GERMAN_QWERTZ)
            self.dispatcher.reset_state()
            self.dispatcher.dispatch_burst(input_text)
            self.assertEqual(self.host.host_text, expected)


if __name__ == "__main__":
    unittest.main()
