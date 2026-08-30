"""
Feature 9: Bluetooth HID Device Registration.
Verifies SDP record settings, Subclass 0x40 (Keyboard), 63-byte universal descriptor,
and BluetoothHidDevice registration with the Android Bluetooth stack.
"""
import unittest
from tests.e2e.harness.hid_constants import (
    HID_KEYBOARD_REPORT_DESCRIPTOR, SDP_SUBCLASS_KEYBOARD
)
from tests.e2e.harness.service_simulator import (
    BluetoothHidTransport, ConnectionState, BluetoothDeviceMock
)


class TestFeature09BluetoothHidRegistration(unittest.TestCase):
    """Tier 1: Feature 9 - Bluetooth HID Device Registration"""

    def setUp(self):
        self.transport = BluetoothHidTransport(permissions_granted=True)

    def test_tc01_sdp_subclass_is_keyboard_0x40(self):
        """TC01: Validates SDP Subclass constant is 0x40 (Keyboard)."""
        self.assertEqual(SDP_SUBCLASS_KEYBOARD, 0x40)

    def test_tc02_universal_report_descriptor_length(self):
        """TC02: Validates universal 63-byte HID report descriptor without report ID."""
        self.assertEqual(len(HID_KEYBOARD_REPORT_DESCRIPTOR), 63)
        # Check Usage Page (Generic Desktop: 0x05 0x01) and Usage (Keyboard: 0x09 0x06)
        self.assertEqual(HID_KEYBOARD_REPORT_DESCRIPTOR[0:2], bytes([0x05, 0x01]))
        self.assertEqual(HID_KEYBOARD_REPORT_DESCRIPTOR[2:4], bytes([0x09, 0x06]))

    def test_tc03_registration_success_with_permissions(self):
        """TC03: Registration succeeds when BLUETOOTH_CONNECT permission is granted."""
        ok = self.transport.initialize()
        self.assertTrue(ok)
        self.assertTrue(self.transport.is_registered)

    def test_tc04_registration_fails_without_permissions(self):
        """TC04: Registration fails when Bluetooth permissions are denied."""
        unprivileged_transport = BluetoothHidTransport(permissions_granted=False)
        ok = unprivileged_transport.initialize()
        self.assertFalse(ok)
        self.assertFalse(unprivileged_transport.is_registered)

    def test_tc05_initial_connection_state_is_disconnected(self):
        """TC05: Connection state is DISCONNECTED immediately following registration."""
        self.transport.initialize()
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
        self.assertIsNone(self.transport.connected_device)


if __name__ == "__main__":
    unittest.main()
