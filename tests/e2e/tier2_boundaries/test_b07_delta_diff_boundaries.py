"""
Boundary Tests: Feature 7 - Live Delta-Diff Engine.
Covers complete erasure to empty string, oscillating speech hypotheses,
single-character mutations in long texts, and rapid prefix switches.
"""
import unittest
from tests.e2e.harness.delta_diff_engine import DeltaDiffEngine
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary07DeltaDiff(unittest.TestCase):
    """Tier 2: Boundary 7 - Live Delta-Diff Engine"""

    def setUp(self):
        self.diff_engine = DeltaDiffEngine()
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_complete_erasure_to_empty(self):
        """TC01: Transition from 100-character text to empty string emits 100 backspaces."""
        initial = "A" * 100
        self.dispatcher.dispatch_live_diff(initial)
        self.assertEqual(self.host.host_text, initial)

        # Clear to empty
        diff = self.dispatcher.dispatch_live_diff("")
        self.assertEqual(diff.backspaces_needed, 100)
        self.assertEqual(diff.text_to_append, "")
        self.assertEqual(self.host.host_text, "")

    def test_tc02_oscillating_hypotheses(self):
        """TC02: Rapid back-and-forth oscillation between similar phrases."""
        phrase_a = "Ich denke an das Haus"
        phrase_b = "Ich denke an das Auto"

        for _ in range(5):
            self.dispatcher.dispatch_live_diff(phrase_a)
            self.assertEqual(self.host.host_text, phrase_a)
            self.dispatcher.dispatch_live_diff(phrase_b)
            self.assertEqual(self.host.host_text, phrase_b)

    def test_tc03_single_character_typo_correction_in_long_string(self):
        """TC03: Changing the last character of a 200-char string requires only 1 backspace."""
        base = "X" * 199 + "1"
        target = "X" * 199 + "2"
        self.dispatcher.dispatch_live_diff(base)
        diff = self.dispatcher.dispatch_live_diff(target)
        self.assertEqual(diff.common_prefix_length, 199)
        self.assertEqual(diff.backspaces_needed, 1)
        self.assertEqual(diff.text_to_append, "2")
        self.assertEqual(self.host.host_text, target)

    def test_tc04_first_character_replacement(self):
        """TC04: Changing only the first character requires full backspace and retyping."""
        self.dispatcher.dispatch_live_diff("Haus")
        diff = self.dispatcher.dispatch_live_diff("Maus")
        self.assertEqual(diff.common_prefix_length, 0)
        self.assertEqual(diff.backspaces_needed, 4)
        self.assertEqual(diff.text_to_append, "Maus")
        self.assertEqual(self.host.host_text, "Maus")

    def test_tc05_multi_paragraph_live_edits(self):
        """TC05: Live differential updates spanning across newline boundaries."""
        state1 = "Zeile 1\nZeile 2\nZeile 3"
        state2 = "Zeile 1\nZeile 2\nZeile 3 - Ergänzung"
        self.dispatcher.dispatch_live_diff(state1)
        self.dispatcher.dispatch_live_diff(state2)
        self.assertEqual(self.host.host_text, state2)


if __name__ == "__main__":
    unittest.main()
