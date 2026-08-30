"""
Feature 1: Standard 8-Byte HID Report Generator.
Verifies modifier byte (Byte 0), reserved byte (Byte 1), 6KRO usage IDs (Bytes 2-7),
and binary serialization conforming to USB HID Spec 1.11.
"""
import unittest
from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_CTRL, MOD_L_SHIFT, MOD_L_ALT, MOD_L_GUI,
    MOD_R_CTRL, MOD_R_SHIFT, MOD_R_ALT, MOD_R_GUI,
    KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_ENTER, KEY_SPACE, KEY_BACKSPACE
)
from tests.e2e.harness.keymap_engine import HidReport


class TestFeature01HidReportGenerator(unittest.TestCase):
    """Tier 1: Feature 1 - Standard 8-Byte HID Report Generator"""

    def test_tc01_empty_release_report_is_8_zero_bytes(self):
        """TC01: Release report generates exactly 8 zero bytes."""
        report = HidReport.release()
        payload = report.to_bytes()
        self.assertEqual(len(payload), 8)
        self.assertEqual(payload, bytes([0] * 8))

    def test_tc02_single_key_press_report_structure(self):
        """TC02: Single key press report places modifier in Byte 0 and usage in Byte 2."""
        report = HidReport.press(modifier=MOD_L_SHIFT, usage_id=KEY_A)
        payload = report.to_bytes()
        self.assertEqual(len(payload), 8)
        self.assertEqual(payload[0], MOD_L_SHIFT)  # Byte 0: Modifier
        self.assertEqual(payload[1], 0x00)         # Byte 1: Reserved
        self.assertEqual(payload[2], KEY_A)        # Byte 2: Key 1
        self.assertEqual(payload[3:8], bytes([0] * 5))  # Bytes 3-7: Unused slots

    def test_tc03_all_8_individual_modifier_bits(self):
        """TC03: Verifies all 8 individual modifier bit flags in Byte 0."""
        modifiers = [
            (MOD_L_CTRL, 0x01),
            (MOD_L_SHIFT, 0x02),
            (MOD_L_ALT, 0x04),
            (MOD_L_GUI, 0x08),
            (MOD_R_CTRL, 0x10),
            (MOD_R_SHIFT, 0x20),
            (MOD_R_ALT, 0x40),
            (MOD_R_GUI, 0x80),
        ]
        for mod_const, expected_hex in modifiers:
            report = HidReport(modifier=mod_const, key_codes=[KEY_A, 0, 0, 0, 0, 0])
            payload = report.to_bytes()
            self.assertEqual(payload[0], expected_hex, f"Modifier {hex(mod_const)} failed")
            self.assertEqual(payload[1], 0x00)

    def test_tc04_6kro_six_simultaneous_keys(self):
        """TC04: 6-Key Rollover array correctly encodes up to 6 simultaneous keys."""
        six_keys = [KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F]
        report = HidReport(modifier=MOD_NONE, key_codes=six_keys)
        payload = report.to_bytes()
        self.assertEqual(len(payload), 8)
        self.assertEqual(payload[0], 0x00)
        self.assertEqual(payload[1], 0x00)
        self.assertEqual(list(payload[2:8]), six_keys)

    def test_tc05_report_from_bytes_roundtrip(self):
        """TC05: Deserializes 8-byte array back into HidReport object."""
        raw_bytes = bytes([0x42, 0x00, 0x04, 0x05, 0x06, 0x00, 0x00, 0x00])
        report = HidReport.from_bytes(raw_bytes)
        self.assertEqual(report.modifier, 0x42)
        self.assertEqual(report.reserved, 0x00)
        self.assertEqual(report.key_codes, [0x04, 0x05, 0x06, 0x00, 0x00, 0x00])
        self.assertEqual(report.to_bytes(), raw_bytes)


if __name__ == "__main__":
    unittest.main()
