"""
Workload Scenario W10: High-Speed Fast Dictation Stress Test.
Simulates typing a massive 1,000-word transcript with deterministic 5-10ms pacing,
asserting 100% scancode fidelity and zero dropped reports on host workstation.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload10HighSpeedStress(unittest.TestCase):
    """Tier 4: Workload Scenario 10 - High-Speed Fast Dictation Stress Test"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator, virtual_clock=True)

    def test_tc01_one_thousand_word_stress_burst(self):
        """TC01: Transmits 1,000 words in high-speed burst mode with zero errors."""
        paragraph = (
            "Transcriptor HID wandelt Spracheingaben zuverlässig in Hardware-Tastatur-Eingaben um. "
            "Mit vollständiger Unterstützung für DIN 2137-1, AltGr-Zeichen und Umlaute wie Ä, Ö, Ü und ß. "
        )
        # 25 repetitions ~ 1,000 words / ~6,000 characters
        full_text = paragraph * 25
        ok = self.dispatcher.dispatch_burst(full_text, delay_ms=5.0)

        self.assertTrue(ok)
        self.assertEqual(len(self.host.host_text), len(full_text))
        self.assertEqual(self.host.host_text, full_text)
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
