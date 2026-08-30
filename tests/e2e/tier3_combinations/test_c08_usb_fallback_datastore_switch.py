"""
Combination Test C08: USB Fallback Switch + DataStore Settings Persistence.
Verifies switching from Bluetooth HID to USB AOA 2.0 / Gadget transport,
persisting updated transmission configurations, and transmitting reports.
"""
import unittest
from tests.e2e.harness.persistence_simulator import SettingsRepositorySimulator
from tests.e2e.harness.service_simulator import (
    BluetoothHidTransport, UsbAoaTransport, UsbGadgetTransport
)
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination08UsbFallbackSettings(unittest.TestCase):
    """Tier 3: Combination 8 - USB Fallback Switch + DataStore Settings Persistence"""

    def setUp(self):
        self.settings = SettingsRepositorySimulator()
        self.ble_transport = BluetoothHidTransport(permissions_granted=True)
        self.usb_aoa = UsbAoaTransport(usb_connected=True)
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)

    def test_tc01_switch_from_ble_to_usb_aoa(self):
        """TC01: Switches active transport from BLE to USB AOA 2.0 when USB is plugged in."""
        # Start BLE
        self.ble_transport.initialize()
        self.ble_transport.simulate_host_connect()
        report = bytes([0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(self.ble_transport.send_report(report))

        # Switch to USB AOA
        self.ble_transport.disconnect()
        self.assertTrue(self.usb_aoa.initialize())
        self.assertTrue(self.usb_aoa.send_report(report))
        self.assertEqual(len(self.usb_aoa.transmitted_reports), 1)

    def test_tc02_settings_persistence_across_transport_switch(self):
        """TC02: Typing delay and keymap settings persist seamlessly across transport changes."""
        self.settings.set_key_layout(KeyLayout.GERMAN_QWERTZ)
        self.settings.set_typing_delay_ms(5)
        self.assertEqual(self.settings.get_key_layout(), KeyLayout.GERMAN_QWERTZ)
        self.assertEqual(self.settings.get_typing_delay_ms(), 5)

        # Switch transport
        self.usb_aoa.initialize()
        # Settings remain unaltered
        self.assertEqual(self.settings.get_key_layout(), KeyLayout.GERMAN_QWERTZ)
        self.assertEqual(self.settings.get_typing_delay_ms(), 5)


if __name__ == "__main__":
    unittest.main()
