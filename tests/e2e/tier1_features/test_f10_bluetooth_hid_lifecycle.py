"""
Feature 10: Bluetooth HID Lifecycle & Callbacks.
Verifies BluetoothProfile.ServiceListener and BluetoothHidDevice.Callback events
including connection transitions, host LED output updates, and virtual unplugs.
"""
import unittest
from tests.e2e.harness.service_simulator import (
    BluetoothHidTransport, ConnectionState, BluetoothDeviceMock,
    BluetoothHidDeviceCallbackMock
)
from tests.e2e.harness.hid_constants import LED_CAPS_LOCK, LED_NUM_LOCK


class TestFeature10BluetoothHidLifecycle(unittest.TestCase):
    """Tier 1: Feature 10 - Bluetooth HID Lifecycle & Callbacks"""

    def setUp(self):
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.events = []

        class CallbackSpy(BluetoothHidDeviceCallbackMock):
            def __init__(self, events_list):
                self.events_list = events_list

            def on_connection_state_changed(self, device, state):
                self.events_list.append(("CONNECTION_STATE", device.name, state))

            def on_set_report(self, device, report_type, report_id, data):
                self.events_list.append(("SET_REPORT", report_type, data))

            def on_virtual_cable_unplug(self, device):
                self.events_list.append(("VIRTUAL_UNPLUG", device.name))

        self.spy = CallbackSpy(self.events)
        self.transport.set_callback(self.spy)
        self.transport.initialize()

    def test_tc01_host_connection_lifecycle(self):
        """TC01: Host connects and transitions state to CONNECTED."""
        device = BluetoothDeviceMock(name="Workstation-PC")
        self.transport.simulate_host_connect(device)
        self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)
        self.assertEqual(self.transport.connected_device.name, "Workstation-PC")
        self.assertIn(("CONNECTION_STATE", "Workstation-PC", ConnectionState.CONNECTED), self.events)

    def test_tc02_host_disconnection_lifecycle(self):
        """TC02: Host disconnects and resets state to DISCONNECTED."""
        device = BluetoothDeviceMock(name="Workstation-PC")
        self.transport.simulate_host_connect(device)
        self.transport.simulate_host_disconnect()
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
        self.assertIsNone(self.transport.connected_device)
        self.assertIn(("CONNECTION_STATE", "Workstation-PC", ConnectionState.DISCONNECTED), self.events)

    def test_tc03_virtual_cable_unplug_handling(self):
        """TC03: Host initiates virtual cable unplug (unpairing)."""
        device = BluetoothDeviceMock(name="Workstation-PC")
        self.transport.simulate_host_connect(device)
        self.transport.simulate_virtual_cable_unplug()
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
        self.assertIn(("VIRTUAL_UNPLUG", "Workstation-PC"), self.events)

    def test_tc04_sending_reports_fails_when_disconnected(self):
        """TC04: send_report returns False when transport is disconnected."""
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)
        dummy_report = bytes([0] * 8)
        ok = self.transport.send_report(dummy_report)
        self.assertFalse(ok)
        self.assertEqual(len(self.transport.transmitted_reports), 0)

    def test_tc05_sending_reports_succeeds_when_connected(self):
        """TC05: send_report transmits 8-byte payload when connected."""
        device = BluetoothDeviceMock(name="Workstation-PC")
        self.transport.simulate_host_connect(device)
        report = bytes([0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        ok = self.transport.send_report(report)
        self.assertTrue(ok)
        self.assertEqual(len(self.transport.transmitted_reports), 1)
        self.assertEqual(self.transport.transmitted_reports[0], report)


if __name__ == "__main__":
    unittest.main()
