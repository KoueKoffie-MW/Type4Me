"""
Boundary Tests: Feature 6 - Buffered Burst Dispatcher Pacing.
Covers massive burst payloads (5,000+ characters), single-character bursts,
rapid enqueueing during transmission, and pacing interval verification.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary06BurstPacing(unittest.TestCase):
    """Tier 2: Boundary 6 - Buffered Burst Dispatcher Pacing"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator, virtual_clock=True)

    def test_tc01_single_character_burst(self):
        """TC01: Minimal 1-character burst correctly emits 1 press and 1 release report."""
        self.dispatcher.dispatch_burst("x")
        self.assertEqual(len(self.host.received_reports), 2)
        self.assertEqual(self.host.host_text, "x")

    def test_tc02_massive_five_thousand_character_burst(self):
        """TC02: Dispatches 5,000-character burst without dropped packets or buffer overrun."""
        chunk = "TranscriptorHID2026 "  # 20 chars
        payload = chunk * 250          # 5000 chars
        ok = self.dispatcher.dispatch_burst(payload)
        self.assertTrue(ok)
        self.assertEqual(len(self.host.host_text), 5000)
        self.assertEqual(self.host.host_text, payload)
        self.assertEqual(self.host.error_count, 0)

    def test_tc03_zero_delay_edge_case(self):
        """TC03: Pacing delay of 0ms is clamped internally to maintain at least 1ms hold time."""
        self.dispatcher.dispatch_burst("ABC", delay_ms=0.0)
        self.assertEqual(self.host.host_text, "ABC")
        self.assertGreater(len(self.host.received_reports), 0)

    def test_tc04_rapid_enqueueing_of_multiple_bursts(self):
        """TC04: Enqueueing 50 bursts in rapid succession sequentially appends to host text."""
        for i in range(50):
            self.dispatcher.dispatch_burst(f"[{i}]")
        expected = "".join(f"[{i}]" for i in range(50))
        self.assertEqual(self.host.host_text, expected)

    def test_tc05_inter_report_pacing_verification(self):
        """TC05: Validates that virtual timestamps reflect deterministic pacing."""
        self.dispatcher.dispatch_burst("Hello", delay_ms=8.0)
        ok, msg = self.host.verify_pacing()
        self.assertTrue(ok)
        self.assertIn("Average pacing", msg)


if __name__ == "__main__":
    unittest.main()
