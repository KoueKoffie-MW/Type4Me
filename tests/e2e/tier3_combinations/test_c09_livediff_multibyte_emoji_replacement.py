"""
Combination Test C09: Live Diff with Multibyte Emoji & Surrogate Replacements.
Verifies real-time speech revisions involving emojis and multibyte Unicode glyphs,
ensuring backspace counts match 32-bit code points without corrupting text.
"""
import unittest
from tests.e2e.harness.delta_diff_engine import DeltaDiffEngine
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination09LiveDiffMultibyteEmoji(unittest.TestCase):
    """Tier 3: Combination 9 - Live Diff with Multibyte Emoji & Surrogate Replacements"""

    def setUp(self):
        self.diff_engine = DeltaDiffEngine()
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_emoji_to_text_live_revision(self):
        """TC01: Revising 'Status: 🚀' to 'Status: Bereit' deletes 1 codepoint and appends text."""
        old_text = "Status: 🚀"
        new_text = "Status: Bereit"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 8)  # "Status: "
        self.assertEqual(diff.backspaces_needed, 1)     # "🚀" (1 codepoint)
        self.assertEqual(diff.text_to_append, "Bereit")

    def test_tc02_text_to_emoji_live_revision(self):
        """TC02: Revising 'Super' to 'Super 🎉' appends space and emoji."""
        old_text = "Super"
        new_text = "Super 🎉"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 5)
        self.assertEqual(diff.backspaces_needed, 0)
        self.assertEqual(diff.text_to_append, " 🎉")

    def test_tc03_multilingual_umlaut_and_emoji_stream(self):
        """TC03: Complex stream containing both German umlauts (ÄÖÜ) and emojis."""
        s1 = "Überprüfung: ⏳"
        s2 = "Überprüfung: Abgeschlossen! ✨"
        diff = self.diff_engine.compute_diff(s1, s2)
        self.assertEqual(diff.common_prefix_length, 13)  # "Überprüfung: "
        self.assertEqual(diff.backspaces_needed, 1)      # "⏳"
        self.assertEqual(diff.text_to_append, "Abgeschlossen! ✨")


if __name__ == "__main__":
    unittest.main()
