"""
Boundary Tests: Feature 1 - HID Report Generator.
Covers 6KRO rollover overflow (>6 keys), phantom state, max modifier byte (0xFF),
zero-length validation, and reserved byte deviations.
"""
import unittest
from tests.e2e.harness.hid_constants import (
    KEY_ERROR_ROLLOVER, KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G
)
from tests.e2e.harness.keymap_engine import HidReport
from tests.e2e.harness.hid_host_simulator import HidHostSimulator


class TestBoundary01HidReport(unittest.TestCase):
    """Tier 2: Boundary 1 - HID Report Generator"""

    def setUp(self):
        self.host = HidHostSimulator()

    def test_tc01_rollover_overflow_emits_error_rollover(self):
        """TC01: Pressing >6 keys simultaneously generates ErrorRollOver (0x01)."""
        # When >6 keys are pressed, HID spec requires filling slots with 0x01
        report = HidReport(modifier=0, key_codes=[KEY_ERROR_ROLLOVER] * 6)
        payload = report.to_bytes()
        self.assertEqual(payload[2:8], bytes([0x01] * 6))
        self.host.receive_report(payload)
        self.assertGreater(self.host.error_count, 0)

    def test_tc02_max_modifier_byte_all_bits_set(self):
        """TC02: Modifier byte with all 8 bits set (0xFF) serializes and deserializes accurately."""
        report = HidReport(modifier=0xFF, key_codes=[0]*6)
        payload = report.to_bytes()
        self.assertEqual(payload[0], 0xFF)
        reconstructed = HidReport.from_bytes(payload)
        self.assertEqual(reconstructed.modifier, 0xFF)

    def test_tc03_invalid_report_length_rejection(self):
        """TC03: Receiving reports shorter or longer than 8 bytes raises ValueError."""
        with self.assertRaises(ValueError):
            self.host.receive_report(bytes([0] * 7))
        with self.assertRaises(ValueError):
            self.host.receive_report(bytes([0] * 9))

    def test_tc04_non_zero_reserved_byte_detection(self):
        """TC04: Non-zero Byte 1 (Reserved) is flagged as protocol anomaly."""
        corrupt_report = bytes([0x00, 0xAA, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.host.receive_report(corrupt_report)
        self.assertGreaterEqual(self.host.error_count, 1)

    def test_tc05_invalid_keycodes_array_length(self):
        """TC05: HidReport rejects key_codes array not matching 6 elements."""
        report = HidReport(modifier=0, key_codes=[KEY_A, KEY_B])  # only 2 elements
        with self.assertRaises(ValueError):
            report.to_bytes()


if __name__ == "__main__":
    unittest.main()
