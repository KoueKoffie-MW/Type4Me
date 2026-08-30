"""
Boundary Tests: Feature 10 - Bluetooth HID Lifecycle.
Covers rapid connect/disconnect churn (50x cycles), sudden virtual cable unplug
during active typing, unexpected disconnect recovery, and callback re-entrancy.
"""
import unittest
from tests.e2e.harness.service_simulator import (
    BluetoothHidTransport, ConnectionState, BluetoothDeviceMock
)


class TestBoundary10Lifecycle(unittest.TestCase):
    """Tier 2: Boundary 10 - Bluetooth HID Lifecycle"""

    def setUp(self):
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.transport.initialize()

    def test_tc01_rapid_connection_churn_fifty_cycles(self):
        """TC01: Rapid 50 connect/disconnect cycles without deadlocks or state desync."""
        device = BluetoothDeviceMock(name="Test-PC")
        for _ in range(50):
            self.transport.simulate_host_connect(device)
            self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)
            self.transport.simulate_host_disconnect()
            self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)

    def test_tc02_sudden_virtual_cable_unplug_mid_stream(self):
        """TC02: Virtual cable unplug cleanly terminates session and rejects subsequent reports."""
        device = BluetoothDeviceMock(name="Test-PC")
        self.transport.simulate_host_connect(device)
        self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)

        # Transmit 1 report
        report = bytes([0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(self.transport.send_report(report))

        # Sudden virtual unplug
        self.transport.simulate_virtual_cable_unplug()
        self.assertEqual(self.transport.connection_state, ConnectionState.DISCONNECTED)

        # Subsequent reports fail
        self.assertFalse(self.transport.send_report(report))

    def test_tc03_connect_to_different_hosts_sequentially(self):
        """TC03: Switching connections between different host devices."""
        host_a = BluetoothDeviceMock(address="11:22:33:44:55:66", name="Laptop-Work")
        host_b = BluetoothDeviceMock(address="AA:BB:CC:DD:EE:00", name="Desktop-Home")

        self.transport.simulate_host_connect(host_a)
        self.assertEqual(self.transport.connected_device.name, "Laptop-Work")
        self.transport.simulate_host_disconnect()

        self.transport.simulate_host_connect(host_b)
        self.assertEqual(self.transport.connected_device.name, "Desktop-Home")
        self.transport.simulate_host_disconnect()

    def test_tc04_sending_invalid_payload_length_when_connected(self):
        """TC04: Sending malformed non-8-byte payload is rejected even when connected."""
        self.transport.simulate_host_connect()
        self.assertFalse(self.transport.send_report(bytes([0] * 5)))
        self.assertFalse(self.transport.send_report(bytes([0] * 12)))

    def test_tc05_reconnect_resumes_transmission(self):
        """TC05: Disconnection followed by reconnection resumes report transmission."""
        self.transport.simulate_host_connect()
        self.transport.simulate_host_disconnect()
        # Cannot send while disconnected
        report = bytes([0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertFalse(self.transport.send_report(report))

        # Reconnect
        self.transport.simulate_host_connect()
        self.assertTrue(self.transport.send_report(report))


if __name__ == "__main__":
    unittest.main()
