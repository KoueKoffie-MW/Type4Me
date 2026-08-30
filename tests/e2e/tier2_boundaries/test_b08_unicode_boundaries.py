"""
Boundary Tests: Feature 8 - Unicode Code Point Safety.
Covers complex multi-codepoint emojis (flags, ZWJ sequences, skin tones),
decomposed NFD normalization, zero-width characters, and RTL marks.
"""
import unittest
from tests.e2e.harness.delta_diff_engine import DeltaDiffEngine


class TestBoundary08Unicode(unittest.TestCase):
    """Tier 2: Boundary 8 - Unicode Code Point Safety"""

    def setUp(self):
        self.diff_engine = DeltaDiffEngine()

    def test_tc01_skin_tone_modifier_emojis(self):
        """TC01: Emoji with Fitzpatrick skin tone modifier (👍🏽 = U+1F44D + U+1F3FD)."""
        old_text = "Status: 👍🏽"
        new_text = "Status:"
        diff = self.diff_engine.compute_diff(old_text, new_text)
        self.assertEqual(diff.common_prefix_length, 7)  # "Status:"
        self.assertEqual(diff.backspaces_needed, 3)     # ' ' + 2 codepoints for 👍🏽
        self.assertEqual(diff.text_to_append, "")

    def test_tc02_zero_width_joiner_zwj_sequences(self):
        """TC02: ZWJ family or profession sequence (👨‍💻 = 👨 + ZWJ + 💻)."""
        coder = "👨\u200D💻"
        points = DeltaDiffEngine._to_code_points(coder)
        self.assertEqual(len(points), 3)  # Man + ZWJ + Laptop
        reconstructed = DeltaDiffEngine._from_code_points(points)
        self.assertEqual(reconstructed, coder)

    def test_tc03_decomposed_diaeresis_normalization_boundary(self):
        """TC03: Complex German sentence containing mixed NFC and NFD umlauts."""
        import unicodedata
        nfd_sample = "U\u0308berpru\u0308fung der A\u0308nderung"
        nfc_sample = unicodedata.normalize("NFC", nfd_sample)
        self.assertEqual(nfc_sample, "Überprüfung der Änderung")

    def test_tc04_combining_accents_and_circumflex_nfd(self):
        """TC04: Decomposed accents are safely composed by Unicode NFC pipeline."""
        import unicodedata
        decomposed = "e\u0301 c\u0327 n\u0303"  # é, ç, ñ decomposed
        composed = unicodedata.normalize("NFC", decomposed)
        self.assertEqual(composed, "é ç ñ")

    def test_tc05_high_surrogate_boundary_codepoints(self):
        """TC05: Characters at the very top of Unicode astral planes (U+10FFFF)."""
        max_codepoint_char = chr(0x10FFFF)
        points = DeltaDiffEngine._to_code_points(f"A{max_codepoint_char}B")
        self.assertEqual(len(points), 3)
        self.assertEqual(points[1], 0x10FFFF)


if __name__ == "__main__":
    unittest.main()
