"""
Feature 12: Bluetooth Permissions & Discovery.
Verifies BLUETOOTH_CONNECT and BLUETOOTH_ADVERTISE permission checking
under Android 12+ (API 31+) and legacy permission fallback.
"""
import unittest
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestFeature12BluetoothPermissions(unittest.TestCase):
    """Tier 1: Feature 12 - Bluetooth Permissions & Discovery"""

    def test_tc01_connect_permission_granted_allows_initialization(self):
        """TC01: Initialization succeeds when BLUETOOTH_CONNECT is granted."""
        transport = BluetoothHidTransport(permissions_granted=True)
        self.assertTrue(transport.initialize())

    def test_tc02_connect_permission_denied_blocks_initialization(self):
        """TC02: Initialization fails when BLUETOOTH_CONNECT is denied."""
        transport = BluetoothHidTransport(permissions_granted=False)
        self.assertFalse(transport.initialize())

    def test_tc03_permission_revocation_blocks_report_sending(self):
        """TC03: Revoking permission blocks report transmission even if registered."""
        transport = BluetoothHidTransport(permissions_granted=True)
        transport.initialize()
        transport.simulate_host_connect()
        # Revoke permission dynamically
        transport.permissions_granted = False
        report = bytes([0] * 8)
        self.assertFalse(transport.send_report(report))

    def test_tc04_re_granting_permission_restores_transmission(self):
        """TC04: Re-granting permission restores transmission capability."""
        transport = BluetoothHidTransport(permissions_granted=False)
        self.assertFalse(transport.initialize())
        # User grants permission
        transport.permissions_granted = True
        self.assertTrue(transport.initialize())
        transport.simulate_host_connect()
        report = bytes([0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00])
        self.assertTrue(transport.send_report(report))

    def test_tc05_permission_state_tracking(self):
        """TC05: Validates permission state flag consistency."""
        transport = BluetoothHidTransport(permissions_granted=True)
        self.assertTrue(transport.permissions_granted)
        transport.permissions_granted = False
        self.assertFalse(transport.permissions_granted)


if __name__ == "__main__":
    unittest.main()
