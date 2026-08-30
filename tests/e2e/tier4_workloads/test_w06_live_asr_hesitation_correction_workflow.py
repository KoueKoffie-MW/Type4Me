"""
Workload Scenario W06: Live ASR Hesitation & Retraction Correction Workflow.
Simulates a real-time speech recognition stream where the speaker stumbles,
hesitates, and self-corrects dates and times in German ("nein am Freitag... eigentlich 15:30 Uhr").
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload06LiveAsrHesitationCorrection(unittest.TestCase):
    """Tier 4: Workload Scenario 6 - Live ASR Hesitation & Retraction Correction"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_progressive_hesitation_and_correction_stream(self):
        """TC01: Simulates ASR streaming hypotheses correcting meeting day and time."""
        hypotheses = [
            "Wir treffen uns am",
            "Wir treffen uns am Donnerstag",
            "Wir treffen uns am Donnerstag um 14 Uhr",
            "Wir treffen uns am Freitag um 14 Uhr",             # speaker corrected day
            "Wir treffen uns am Freitag um 15:30 Uhr im Büro."  # speaker corrected time
        ]
        for h in hypotheses:
            self.dispatcher.dispatch_live_diff(h)
            self.assertEqual(self.host.host_text, h)

        final_expected = "Wir treffen uns am Freitag um 15:30 Uhr im Büro."
        self.assertEqual(self.host.host_text, final_expected)
        self.assertGreater(self.dispatcher.total_backspaces_sent, 0)


if __name__ == "__main__":
    unittest.main()
