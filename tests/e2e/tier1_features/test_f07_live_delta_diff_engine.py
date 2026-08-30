"""
Feature 7: Live Delta-Diff Transmission Engine.
Verifies Longest Common Prefix (LCP) computation, minimum backspace emission,
and incremental character typing as speech recognition hypotheses evolve.
"""
import unittest
from tests.e2e.harness.delta_diff_engine import DeltaDiffEngine, DiffResult
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature07LiveDeltaDiffEngine(unittest.TestCase):
    """Tier 1: Feature 7 - Live Delta-Diff Transmission Engine"""

    def setUp(self):
        self.diff_engine = DeltaDiffEngine()
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_pure_append_differential(self):
        """TC01: Appending new text requires 0 backspaces and appends only the difference."""
        old_text = "Hallo"
        new_text = "Hallo Welt"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 5)
        self.assertEqual(diff.backspaces_needed, 0)
        self.assertEqual(diff.text_to_append, " Welt")

    def test_tc02_word_correction_with_backspaces(self):
        """TC02: Revising a word emits exact backspaces and appends replacement word."""
        old_text = "Ich möchte gehen"
        new_text = "Ich möchte fahren"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 11)  # "Ich möchte " (11 chars)
        self.assertEqual(diff.backspaces_needed, 5)      # "gehen" (5 chars)
        self.assertEqual(diff.text_to_append, "fahren")

    def test_tc03_complete_replacement_zero_lcp(self):
        """TC03: Completely different hypothesis requires backspacing entire old text."""
        old_text = "Alpha"
        new_text = "Omega"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 0)
        self.assertEqual(diff.backspaces_needed, 5)
        self.assertEqual(diff.text_to_append, "Omega")

    def test_tc04_identical_hypothesis_no_op(self):
        """TC04: Identical text emits 0 backspaces and empty append."""
        text = "Keine Änderung"
        diff = self.diff_engine.compute_diff(text, text)
        self.assertEqual(diff.common_prefix_length, len(text))
        self.assertEqual(diff.backspaces_needed, 0)
        self.assertEqual(diff.text_to_append, "")

    def test_tc05_end_to_end_streaming_speech_evolution(self):
        """TC05: Simulates multi-step streaming speech recognition updates on host."""
        speech_steps = [
            "Heute ist ein",
            "Heute ist ein schöner",
            "Heute ist ein schöner Tag",
            "Heute ist ein herrlicher Tag!",
        ]
        for step in speech_steps:
            self.dispatcher.dispatch_live_diff(step)
            self.assertEqual(self.host.host_text, step)

        self.assertEqual(self.host.host_text, "Heute ist ein herrlicher Tag!")


if __name__ == "__main__":
    unittest.main()
