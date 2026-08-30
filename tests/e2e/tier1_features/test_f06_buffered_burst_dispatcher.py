"""
Feature 6: Buffered Burst Keystroke Dispatcher.
Verifies queueing of text, 2-phase press/release report pairs, and deterministic
5-10ms inter-character pacing without dropping keystrokes.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature06BufferedBurstDispatcher(unittest.TestCase):
    """Tier 1: Feature 6 - Buffered Burst Keystroke Dispatcher"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator, virtual_clock=True)

    def test_tc01_each_keystroke_emits_press_then_release_pair(self):
        """TC01: Each keystroke generates exactly 1 press report and 1 zero release report."""
        self.dispatcher.dispatch_burst("A")
        self.assertEqual(len(self.host.received_reports), 2)
        # First report: Press (Shift + Key A)
        press = self.host.received_reports[0]
        self.assertEqual(press[0], 0x02)  # L-Shift
        self.assertEqual(press[2], 0x04)  # Key A
        # Second report: Release (All zeros)
        release = self.host.received_reports[1]
        self.assertEqual(release, bytes([0] * 8))

    def test_tc02_deterministic_pacing_intervals(self):
        """TC02: Validates that inter-character timing advances virtual clock by delay_ms."""
        delay = 10.0
        self.dispatcher.dispatch_burst("ABC", delay_ms=delay)
        # 3 characters = 6 reports (press + release each)
        self.assertEqual(len(self.host.received_reports), 6)
        # Total virtual time should be 3 * 10ms = 30ms (0.030s)
        self.assertAlmostEqual(self.dispatcher.virtual_time, 0.030, places=3)

    def test_tc03_burst_dispatch_clears_queue_and_updates_acknowledged_text(self):
        """TC03: Acknowledged text matches sent string and host text buffer."""
        text = "Guten Tag, wie geht es Ihnen?"
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.dispatcher.acknowledged_host_text, text)
        self.assertEqual(self.host.host_text, text)

    def test_tc04_multiple_consecutive_bursts(self):
        """TC04: Appending multiple bursts sequentially maintains continuous host state."""
        self.dispatcher.dispatch_burst("Absatz 1.\n")
        self.dispatcher.dispatch_burst("Absatz 2.\n")
        self.dispatcher.dispatch_burst("Absatz 3.")
        expected = "Absatz 1.\nAbsatz 2.\nAbsatz 3."
        self.assertEqual(self.host.host_text, expected)

    def test_tc05_large_burst_fidelity(self):
        """TC05: Dispatches 500-character burst without dropped packets or corruption."""
        long_text = "Standardtest " * 38 + "Ende!"  # 501 chars
        self.dispatcher.dispatch_burst(long_text)
        self.assertEqual(self.host.host_text, long_text)
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
