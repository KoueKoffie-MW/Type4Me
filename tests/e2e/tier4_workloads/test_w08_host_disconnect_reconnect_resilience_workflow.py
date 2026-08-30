"""
Workload Scenario W08: Host Disconnect & Reconnection Resilience Workflow.
Simulates a long dictation session where the host Bluetooth connection drops
intermittently (e.g. walk out of range) and reconnects without character loss.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestWorkload08DisconnectReconnectResilience(unittest.TestCase):
    """Tier 4: Workload Scenario 8 - Disconnect & Reconnection Resilience"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.transport.initialize()
        self.transport.simulate_host_connect()

    def test_tc01_intermittent_disconnections_during_long_dictation(self):
        """TC01: Simulates 3 separate disconnections during a multi-sentence dictation."""
        sentences = [
            "Erster Satz vor Verbindungsabbruch. ",
            "Zweiter Satz nach Wiederverbindung 1. ",
            "Dritter Satz nach Wiederverbindung 2. ",
        ]

        # 1. Type sentence 1
        self.dispatcher.dispatch_burst(sentences[0])
        self.assertEqual(self.host.host_text, sentences[0])

        # Drop link 1
        self.host.set_connected(False)
        self.transport.simulate_host_disconnect()
        # Restore link 1
        self.host.set_connected(True)
        self.transport.simulate_host_connect()

        # 2. Type sentence 2
        self.dispatcher.dispatch_burst(sentences[1])
        self.assertEqual(self.host.host_text, sentences[0] + sentences[1])

        # Drop link 2
        self.host.set_connected(False)
        self.transport.simulate_host_disconnect()
        # Restore link 2
        self.host.set_connected(True)
        self.transport.simulate_host_connect()

        # 3. Type sentence 3
        self.dispatcher.dispatch_burst(sentences[2])
        self.assertEqual(self.host.host_text, "".join(sentences))
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
