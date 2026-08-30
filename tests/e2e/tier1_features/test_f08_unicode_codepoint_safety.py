"""
Feature 8: Unicode Code Point Safety.
Verifies that 32-bit Unicode code points and surrogate pairs (emojis)
are handled atomically without surrogate splitting or corruption during diffing.
"""
import unittest
from tests.e2e.harness.delta_diff_engine import DeltaDiffEngine
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature08UnicodeCodePointSafety(unittest.TestCase):
    """Tier 1: Feature 8 - Unicode Code Point Safety"""

    def setUp(self):
        self.diff_engine = DeltaDiffEngine()

    def test_tc01_single_emoji_counted_as_one_codepoint(self):
        """TC01: Astral plane emoji 😀 (U+1F600) counts as 1 code point, not 2 UTF-16 units."""
        old_text = "Hallo 😀"
        new_text = "Hallo"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        # Should need exactly 2 backspaces: 1 for ' ' and 1 for '😀'
        self.assertEqual(diff.backspaces_needed, 2)
        self.assertEqual(diff.common_prefix_length, 5)

    def test_tc02_emoji_replacement_delta(self):
        """TC02: Replacing an emoji with another emoji needs 1 backspace."""
        old_text = "Status: 🚀"
        new_text = "Status: ⚡"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 8)  # "Status: "
        self.assertEqual(diff.backspaces_needed, 1)     # "🚀"
        self.assertEqual(diff.text_to_append, "⚡")

    def test_tc03_multiple_surrogate_pairs(self):
        """TC03: Handling string with multiple emojis in sequence."""
        old_text = "Icons: 🍎🍊🍋"
        new_text = "Icons: 🍎🍌🍇"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        # LCP is "Icons: 🍎" (8 codepoints)
        self.assertEqual(diff.common_prefix_length, 8)
        self.assertEqual(diff.backspaces_needed, 2)  # "🍊🍋"
        self.assertEqual(diff.text_to_append, "🍌🍇")

    def test_tc04_nfc_canonical_composition_prevents_diaeresis_split(self):
        """TC04: NFC normalization combines 'a' + U+0308 into 'ä' U+00E4."""
        import unicodedata
        nfd_text = "a\u0308"  # Decomposed 'ä'
        nfc_text = unicodedata.normalize("NFC", nfd_text)
        self.assertEqual(len(nfc_text), 1)
        self.assertEqual(nfc_text, "ä")

    def test_tc05_code_points_roundtrip_fidelity(self):
        """TC05: Reconstructing string from code points preserves all 32-bit characters."""
        original = "Transcriptor 🎙️ ✨ 🚀 German: ÄÖÜß!"
        points = DeltaDiffEngine._to_code_points(original)
        reconstructed = DeltaDiffEngine._from_code_points(points)
        self.assertEqual(reconstructed, original)


if __name__ == "__main__":
    unittest.main()
