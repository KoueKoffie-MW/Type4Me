"""
Combination Test C05: Clean & Polish Preset + German QWERTZ + Burst Pacing.
Verifies raw spoken German transcript cleaned with AI Polish preset, converted to
DIN 2137-1 scancodes, and typed in buffered burst mode with deterministic pacing.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_CLEAN_POLISH


class TestCombination05AiPolishQwertzBurst(unittest.TestCase):
    """Tier 3: Combination 5 - Clean & Polish Preset + German QWERTZ + Burst Pacing"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")

    def test_tc01_spoken_german_disfluency_cleanup_and_burst(self):
        """TC01: Cleans German filler words ('ehm', 'halt', 'quasi') and types in paced burst."""
        raw_speech = "ehm wir müssen halt die Überprüfung der Geräte quasi morgen durchführen"
        ok, polished, err = self.rewriter.rewrite(raw_speech, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertNotIn("ehm", polished.lower())
        self.assertNotIn("halt", polished.lower())
        self.assertNotIn("quasi", polished.lower())

        self.dispatcher.dispatch_burst(polished, delay_ms=8.0)
        self.assertEqual(self.host.host_text, polished)
        self.assertIn("Überprüfung der Geräte", self.host.host_text)

    def test_tc02_sentence_capitalization_and_punctuation(self):
        """TC02: AI Polish capitalizes first word and adds ending period before typing."""
        raw = "um dieses system funktioniert einwandfrei"
        ok, polished, err = self.rewriter.rewrite(raw, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertTrue(polished.endswith("."))

        self.dispatcher.dispatch_burst(polished)
        self.assertEqual(self.host.host_text, polished)

    def test_tc03_burst_report_count_validation(self):
        """TC03: Verifies exact 2 reports per character (press + release) for polished text."""
        raw = "ehm test"
        ok, polished, err = self.rewriter.rewrite(raw, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.dispatcher.dispatch_burst(polished)
        # Each char in polished string should emit 2 reports
        expected_reports = len(self.translator.translate_string(polished)) * 2
        self.assertEqual(len(self.host.received_reports), expected_reports)


if __name__ == "__main__":
    unittest.main()
