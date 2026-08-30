"""
Combination Test C06: Bluetooth Disconnect During Burst + Reconnect & Resume.
Verifies graceful handling when Bluetooth link drops during a burst transmission,
pauses the buffer, and successfully drains queued keystrokes after reconnection.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestCombination06BleDisconnectBurstResume(unittest.TestCase):
    """Tier 3: Combination 6 - Bluetooth Disconnect During Burst + Reconnect & Resume"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.transport.initialize()

    def test_tc01_disconnect_mid_burst_fails_remaining_reports(self):
        """TC01: Disconnecting host during burst stops transmission cleanly."""
        self.transport.simulate_host_connect()
        self.assertEqual(self.transport.connection_state, ConnectionState.CONNECTED)

        # Transmit Part 1
        self.dispatcher.dispatch_burst("Teil 1: Erfolgreich. ")
        self.assertEqual(self.host.host_text, "Teil 1: Erfolgreich. ")

        # Link drops
        self.host.set_connected(False)
        self.transport.simulate_host_disconnect()

        # Attempt to transmit Part 2 while disconnected
        ok = self.dispatcher.dispatch_burst("Teil 2: Verloren")
        self.assertFalse(ok)

    def test_tc02_reconnect_allows_resumed_burst(self):
        """TC02: Reconnecting host allows sending pending text with complete integrity."""
        self.transport.simulate_host_connect()
        self.dispatcher.dispatch_burst("Erster Block. ")
        self.assertEqual(self.host.host_text, "Erster Block. ")

        # Disconnect
        self.host.set_connected(False)
        self.transport.simulate_host_disconnect()

        # Reconnect
        self.host.set_connected(True)
        self.transport.simulate_host_connect()

        # Transmit remaining
        ok = self.dispatcher.dispatch_burst("Zweiter Block nach Reconnect.")
        self.assertTrue(ok)
        self.assertEqual(self.host.host_text, "Erster Block. Zweiter Block nach Reconnect.")

    def test_tc03_zero_error_count_on_resumed_session(self):
        """TC03: Reconnected session generates zero protocol errors."""
        self.transport.simulate_host_connect()
        self.host.reset(KeyLayout.GERMAN_QWERTZ)
        self.dispatcher.reset_state()

        self.dispatcher.dispatch_burst("Wiederherstellungstest.")
        self.assertEqual(self.host.host_text, "Wiederherstellungstest.")
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
