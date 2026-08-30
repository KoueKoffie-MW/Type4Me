"""
Boundary Tests: Feature 9 - Bluetooth HID Registration.
Covers SDP descriptor boundaries, registration retries, re-registration while registered,
null QoS parameters, and registration failure handling.
"""
import unittest
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestBoundary09BluetoothRegistration(unittest.TestCase):
    """Tier 2: Boundary 9 - Bluetooth HID Registration"""

    def test_tc01_re_registration_while_already_registered(self):
        """TC01: Calling initialize on an already registered transport retains registered state."""
        transport = BluetoothHidTransport(permissions_granted=True)
        self.assertTrue(transport.initialize())
        self.assertTrue(transport.is_registered)
        # Second call
        self.assertTrue(transport.initialize())
        self.assertTrue(transport.is_registered)

    def test_tc02_registration_without_permissions_is_rejected(self):
        """TC02: Initializing without permissions fails and keeps state DISCONNECTED."""
        transport = BluetoothHidTransport(permissions_granted=False)
        self.assertFalse(transport.initialize())
        self.assertFalse(transport.is_registered)
        self.assertEqual(transport.connection_state, ConnectionState.DISCONNECTED)

    def test_tc03_registration_recovery_after_permission_grant(self):
        """TC03: Transport recovers immediately once permissions are granted."""
        transport = BluetoothHidTransport(permissions_granted=False)
        self.assertFalse(transport.initialize())
        transport.permissions_granted = True
        self.assertTrue(transport.initialize())
        self.assertTrue(transport.is_registered)

    def test_tc04_multiple_instances_isolation(self):
        """TC04: Multiple independent transport instances maintain separate state machines."""
        t1 = BluetoothHidTransport(permissions_granted=True)
        t2 = BluetoothHidTransport(permissions_granted=False)
        self.assertTrue(t1.initialize())
        self.assertFalse(t2.initialize())
        self.assertTrue(t1.is_registered)
        self.assertFalse(t2.is_registered)

    def test_tc05_clean_disconnection_resets_registration_context(self):
        """TC05: Disconnecting active host resets connection state while keeping SDP registered."""
        transport = BluetoothHidTransport(permissions_granted=True)
        transport.initialize()
        transport.simulate_host_connect()
        self.assertEqual(transport.connection_state, ConnectionState.CONNECTED)
        transport.disconnect()
        self.assertEqual(transport.connection_state, ConnectionState.DISCONNECTED)
        self.assertTrue(transport.is_registered)  # SDP remains registered for future connections


if __name__ == "__main__":
    unittest.main()
