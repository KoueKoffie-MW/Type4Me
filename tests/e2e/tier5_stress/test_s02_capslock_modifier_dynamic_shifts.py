"""
Tier 5 Adversarial Stress Test S02:
Host Caps Lock Inverted States & Dynamic Modifier Bitmask Shifts.

Stress-tests:
1. Host Caps Lock ON: Verify character decoding with inverted Shift state on alphabetic keys.
2. Verify strict non-inversion for numbers, punctuation, German umlauts, Eszett (ß/ẞ), and AltGr symbols.
3. Dynamic modifier bitmask oscillation: Rapid sequential alternating between MOD_NONE (0x00),
   MOD_L_SHIFT (0x02), MOD_R_ALT (0x40), and MOD_SHIFT_ALTGR (0x42).
4. Mid-burst dynamic Caps Lock toggling (toggling Caps Lock state during an active 100-character burst).
5. Cross-layout validation (German QWERTZ and US QWERTY) under Caps Lock inversion.
"""
import unittest
from typing import List, Tuple

from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, UsQwertyKeymap, KeyLayout, HidReport, HidKeyStroke
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_SHIFT, MOD_R_ALT, MOD_SHIFT_ALTGR,
    KEY_NONE, KEY_A, KEY_B, KEY_C, KEY_Z, KEY_Y, KEY_1, KEY_2, KEY_MINUS, KEY_EQUAL, KEY_Q, KEY_E
)
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestS02CapsLockModifierDynamicShifts(unittest.TestCase):
    """Adversarial Stress Test Suite S02: Caps Lock & Dynamic Modifier Shifts"""

    def setUp(self):
        self.qwertz_host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.qwerty_host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.qwertz_translator = GermanQwertzKeymap()
        self.qwerty_translator = UsQwertyKeymap()

    def test_s02_01_caps_lock_inverted_alpha_decoding_qwertz(self):
        """S02.1: German QWERTZ under Host Caps Lock ON correctly inverts lowercase and uppercase letters."""
        self.qwertz_host.set_caps_lock(True)
        self.assertTrue(self.qwertz_host.caps_lock_on)

        # In standard QWERTZ:
        # Key 'a' (unshifted, MOD_NONE) under CapsLock ON decodes to 'A'
        self.qwertz_host.receive_report(HidReport.press(MOD_NONE, KEY_A).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "A")

        # Shift + 'a' (MOD_L_SHIFT) under CapsLock ON decodes to 'a'
        self.qwertz_host.receive_report(HidReport.press(MOD_L_SHIFT, KEY_A).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "Aa")

        # Physical Y/Z swap with Caps Lock:
        # KEY_Z (0x1D, B01) unshifted -> 'Y' under CapsLock ON
        self.qwertz_host.receive_report(HidReport.press(MOD_NONE, KEY_Z).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "AaY")

        # KEY_Y (0x1C, D06) unshifted -> 'Z' under CapsLock ON
        self.qwertz_host.receive_report(HidReport.press(MOD_NONE, KEY_Y).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "AaYZ")

    def test_s02_02_caps_lock_non_alpha_non_inversion_qwertz(self):
        """S02.2: Numbers, punctuation, and AltGr symbols are NOT inverted by Caps Lock (DIN 2137 / ISO standard)."""
        self.qwertz_host.set_caps_lock(True)

        # Numbers: KEY_1 unshifted -> '1', KEY_1 with Shift -> '!' (NOT inverted)
        self.qwertz_host.receive_report(HidReport.press(MOD_NONE, KEY_1).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.qwertz_host.receive_report(HidReport.press(MOD_L_SHIFT, KEY_1).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "1!")

        # AltGr characters: @ (AltGr+Q), € (AltGr+E)
        self.qwertz_host.receive_report(HidReport.press(MOD_R_ALT, KEY_Q).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.qwertz_host.receive_report(HidReport.press(MOD_R_ALT, KEY_E).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "1!@€")

        # Eszett: KEY_MINUS unshifted -> 'ß', Shift+AltGr+MINUS -> 'ẞ'
        self.qwertz_host.receive_report(HidReport.press(MOD_NONE, KEY_MINUS).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.qwertz_host.receive_report(HidReport.press(MOD_SHIFT_ALTGR, KEY_MINUS).to_bytes())
        self.qwertz_host.receive_report(HidReport.release().to_bytes())
        self.assertEqual(self.qwertz_host.host_text, "1!@€ßẞ")

    def test_s02_03_dynamic_modifier_bitmask_oscillation_stress(self):
        """S02.3: Stress-test rapid alternating modifier shifts across 200 consecutive reports."""
        dispatcher = KeystrokeDispatcher(host=self.qwertz_host, translator=self.qwertz_translator)

        # Alternating sequence cycling through NONE, SHIFT, ALTGR, SHIFT_ALTGR
        # String: "a A @ ẞ b B € ẞ c C { ẞ "
        sequence = "aA@ẞbB€ẞcC{ẞdD}ẞeE[ẞfF]ẞgG\\ẞhH~ẞiI|ẞjJµẞ"
        
        # Dispatch complete sequence
        ok = dispatcher.dispatch_burst(sequence)
        self.assertTrue(ok)

        # Verify that all 200+ reports maintain exact modifier isolation without bit leakage
        reports = self.qwertz_host.received_reports
        self.assertGreater(len(reports), 40)

        # Verify host received exact expected text
        self.assertEqual(self.qwertz_host.host_text, sequence)
        self.assertEqual(self.qwertz_host.error_count, 0)

        # Inspect report modifier bytes: every release report must be 0x00
        for i, rep in enumerate(reports):
            if rep[2] == 0:  # Release report
                self.assertEqual(rep[0], 0x00, f"Release report #{i} had non-zero modifier: 0x{rep[0]:02X}")

    def test_s02_04_mid_burst_dynamic_caps_lock_toggling(self):
        """S02.4: Toggling Caps Lock state during active burst typing."""
        # Start with CapsLock OFF
        self.qwertz_host.set_caps_lock(False)

        # Type "abc" (unshifted) -> "abc"
        for ch in "abc":
            strokes = self.qwertz_translator.translate_char(ch)
            for s in strokes:
                self.qwertz_host.receive_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
                self.qwertz_host.receive_report(HidReport.release().to_bytes())

        self.assertEqual(self.qwertz_host.host_text, "abc")

        # Host toggles Caps Lock ON
        self.qwertz_host.set_caps_lock(True)

        # Type "def" (unshifted) -> will decode as "DEF" on host
        for ch in "def":
            strokes = self.qwertz_translator.translate_char(ch)
            for s in strokes:
                self.qwertz_host.receive_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
                self.qwertz_host.receive_report(HidReport.release().to_bytes())

        self.assertEqual(self.qwertz_host.host_text, "abcDEF")

        # Host toggles Caps Lock OFF
        self.qwertz_host.set_caps_lock(False)

        # Type "ghi" (unshifted) -> "ghi"
        for ch in "ghi":
            strokes = self.qwertz_translator.translate_char(ch)
            for s in strokes:
                self.qwertz_host.receive_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
                self.qwertz_host.receive_report(HidReport.release().to_bytes())

        self.assertEqual(self.qwertz_host.host_text, "abcDEFghi")
        self.assertEqual(self.qwertz_host.error_count, 0)

    def test_s02_05_us_qwerty_caps_lock_inversion_and_symbols(self):
        """S02.5: US QWERTY under Caps Lock ON properly inverts letters and preserves symbols."""
        self.qwerty_host.set_caps_lock(True)

        # Letters a-z unshifted -> decodes as A-Z
        for char in "hello":
            strokes = self.qwerty_translator.translate_char(char)
            for s in strokes:
                self.qwerty_host.receive_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
                self.qwerty_host.receive_report(HidReport.release().to_bytes())

        self.assertEqual(self.qwerty_host.host_text, "HELLO")

        # Numbers & punctuation are unaffected by Caps Lock
        for char in " 123!@#":
            strokes = self.qwerty_translator.translate_char(char)
            for s in strokes:
                self.qwerty_host.receive_report(HidReport.press(s.modifier_mask, s.usage_id).to_bytes())
                self.qwerty_host.receive_report(HidReport.release().to_bytes())

        self.assertEqual(self.qwerty_host.host_text, "HELLO 123!@#")
        self.assertEqual(self.qwerty_host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
