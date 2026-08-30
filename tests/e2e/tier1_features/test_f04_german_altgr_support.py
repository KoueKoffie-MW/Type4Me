"""
Feature 4: German AltGr Modifier Support.
Verifies 3rd-level characters (@, €, \\, ~, {, }, [, ], |, µ, ², ³, ẞ) using
Right Alt (0x40) modifier bit on German QWERTZ layout.
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_R_ALT, MOD_SHIFT_ALTGR, KEY_Q, KEY_E, KEY_MINUS, KEY_7, KEY_8, KEY_9,
    KEY_0, KEY_NON_US_BACKSLASH, KEY_M, KEY_2, KEY_3, KEY_RIGHT_BRACE
)
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature04GermanAltGrSupport(unittest.TestCase):
    """Tier 1: Feature 4 - German AltGr Modifier Support"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_at_sign_and_euro_symbol(self):
        """TC01: Translates @ (AltGr+Q) and € (AltGr+E)."""
        # @ (Key Q = 0x14, Mod 0x40)
        s_at = self.translator.translate_char("@")
        self.assertEqual(s_at[0].usage_id, KEY_Q)
        self.assertEqual(s_at[0].modifier_mask, MOD_R_ALT)

        # € (Key E = 0x08, Mod 0x40)
        s_eur = self.translator.translate_char("€")
        self.assertEqual(s_eur[0].usage_id, KEY_E)
        self.assertEqual(s_eur[0].modifier_mask, MOD_R_ALT)

    def test_tc02_brackets_and_braces_altgr(self):
        """TC02: Translates braces { }, brackets [ ], backslash \\, and pipe |."""
        cases = [
            ("{", KEY_7, MOD_R_ALT),
            ("[", KEY_8, MOD_R_ALT),
            ("]", KEY_9, MOD_R_ALT),
            ("}", KEY_0, MOD_R_ALT),
            ("\\", KEY_MINUS, MOD_R_ALT),
            ("|", KEY_NON_US_BACKSLASH, MOD_R_ALT),
        ]
        for ch, usage, mod in cases:
            strokes = self.translator.translate_char(ch)
            self.assertEqual(strokes[0].usage_id, usage, f"Failed for {ch}")
            self.assertEqual(strokes[0].modifier_mask, mod, f"Failed for {ch}")

    def test_tc03_superscripts_and_micro_sign(self):
        """TC03: Translates superscripts ² (AltGr+2), ³ (AltGr+3), and micro µ (AltGr+M)."""
        cases = [
            ("²", KEY_2, MOD_R_ALT),
            ("³", KEY_3, MOD_R_ALT),
            ("µ", KEY_M, MOD_R_ALT),
        ]
        for ch, usage, mod in cases:
            strokes = self.translator.translate_char(ch)
            self.assertEqual(strokes[0].usage_id, usage)
            self.assertEqual(strokes[0].modifier_mask, mod)

    def test_tc04_capital_sharp_s_din2137(self):
        """TC04: Translates DIN 2137:2018 Capital Sharp S (ẞ = Shift + AltGr + ß)."""
        s_cap_sz = self.translator.translate_char("ẞ")
        self.assertEqual(s_cap_sz[0].usage_id, KEY_MINUS)
        self.assertEqual(s_cap_sz[0].modifier_mask, MOD_SHIFT_ALTGR)  # 0x42

    def test_tc05_end_to_end_altgr_sentence_reconstruction(self):
        """TC05: End-to-end host decoding of complex programming and currency text."""
        text = "Kontakt: dev@transcriptor.de | Budget: 15.000 € | Array: [a, b] -> {x: 'y'} \\ µm²."
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)


if __name__ == "__main__":
    unittest.main()
