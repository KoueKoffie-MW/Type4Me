"""
Feature 3: German QWERTZ Keymap Translation (DIN 2137-1).
Verifies full German layout mapping including Y/Z swap, Umlauts (ä, ö, ü, Ä, Ö, Ü, ß),
punctuation keys, ISO extra key (< >), and ISO hash key (# ').
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_SHIFT, KEY_Y, KEY_Z, KEY_APOSTROPHE, KEY_SEMICOLON,
    KEY_LEFT_BRACE, KEY_MINUS, KEY_NON_US_BACKSLASH, KEY_NON_US_HASH,
    KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_0,
    KEY_COMMA, KEY_DOT, KEY_SLASH, KEY_RIGHT_BRACE
)
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature03GermanQwertzKeymap(unittest.TestCase):
    """Tier 1: Feature 3 - German QWERTZ Keymap Translation (DIN 2137-1)"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_yz_physical_swap(self):
        """TC01: Letter 'y' maps to physical key B01 (KEY_Z), 'z' maps to D06 (KEY_Y)."""
        # Lowercase 'y'
        sy = self.translator.translate_char("y")
        self.assertEqual(sy[0].usage_id, KEY_Z)
        self.assertEqual(sy[0].modifier_mask, MOD_NONE)

        # Uppercase 'Y'
        sY = self.translator.translate_char("Y")
        self.assertEqual(sY[0].usage_id, KEY_Z)
        self.assertEqual(sY[0].modifier_mask, MOD_L_SHIFT)

        # Lowercase 'z'
        sz = self.translator.translate_char("z")
        self.assertEqual(sz[0].usage_id, KEY_Y)
        self.assertEqual(sz[0].modifier_mask, MOD_NONE)

        # Uppercase 'Z'
        sZ = self.translator.translate_char("Z")
        self.assertEqual(sZ[0].usage_id, KEY_Y)
        self.assertEqual(sZ[0].modifier_mask, MOD_L_SHIFT)

    def test_tc02_dedicated_umlauts_and_eszett(self):
        """TC02: Dedicated keys for ä, ö, ü, Ä, Ö, Ü, ß."""
        umlauts = [
            ("ä", KEY_APOSTROPHE, MOD_NONE),
            ("Ä", KEY_APOSTROPHE, MOD_L_SHIFT),
            ("ö", KEY_SEMICOLON, MOD_NONE),
            ("Ö", KEY_SEMICOLON, MOD_L_SHIFT),
            ("ü", KEY_LEFT_BRACE, MOD_NONE),
            ("Ü", KEY_LEFT_BRACE, MOD_L_SHIFT),
            ("ß", KEY_MINUS, MOD_NONE),
            ("?", KEY_MINUS, MOD_L_SHIFT),
        ]
        for ch, expected_usage, expected_mod in umlauts:
            strokes = self.translator.translate_char(ch)
            self.assertEqual(len(strokes), 1, f"Failed for {ch}")
            self.assertEqual(strokes[0].usage_id, expected_usage)
            self.assertEqual(strokes[0].modifier_mask, expected_mod)

    def test_tc03_german_number_row_shifted_characters(self):
        """TC03: Shifted number row matches German DIN 2137 (! " § $ % & / ( ) =)."""
        de_numbers = [
            ("1", "!", KEY_1), ("2", '"', KEY_2), ("3", "§", KEY_3),
            ("4", "$", KEY_4), ("5", "%", KEY_5), ("6", "&", KEY_6),
            ("7", "/", KEY_7), ("8", "(", KEY_8), ("9", ")", KEY_9),
            ("0", "=", KEY_0),
        ]
        for unshifted, shifted, usage in de_numbers:
            s_un = self.translator.translate_char(unshifted)
            self.assertEqual(s_un[0].usage_id, usage)
            self.assertEqual(s_un[0].modifier_mask, MOD_NONE)

            s_sh = self.translator.translate_char(shifted)
            self.assertEqual(s_sh[0].usage_id, usage)
            self.assertEqual(s_sh[0].modifier_mask, MOD_L_SHIFT)

    def test_tc04_iso_extra_and_hash_keys(self):
        """TC04: ISO keys: Non-US Backslash (0x64: < >) and Non-US Hash (0x32: # ')."""
        # < and > (Key 0x64)
        s_less = self.translator.translate_char("<")
        self.assertEqual(s_less[0].usage_id, KEY_NON_US_BACKSLASH)
        self.assertEqual(s_less[0].modifier_mask, MOD_NONE)

        s_great = self.translator.translate_char(">")
        self.assertEqual(s_great[0].usage_id, KEY_NON_US_BACKSLASH)
        self.assertEqual(s_great[0].modifier_mask, MOD_L_SHIFT)

        # # and ' (Key 0x32)
        s_hash = self.translator.translate_char("#")
        self.assertEqual(s_hash[0].usage_id, KEY_NON_US_HASH)
        self.assertEqual(s_hash[0].modifier_mask, MOD_NONE)

        s_apostrophe = self.translator.translate_char("'")
        self.assertEqual(s_apostrophe[0].usage_id, KEY_NON_US_HASH)
        self.assertEqual(s_apostrophe[0].modifier_mask, MOD_L_SHIFT)

    def test_tc05_end_to_end_german_qwertz_reconstruction(self):
        """TC05: Full German sentence typing and exact host reconstruction."""
        sentence = "Überraschung! Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich."
        self.dispatcher.dispatch_burst(sentence)
        self.assertEqual(self.host.host_text, sentence)


if __name__ == "__main__":
    unittest.main()
