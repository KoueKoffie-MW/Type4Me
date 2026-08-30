"""
Boundary Tests: Feature 2 - US QWERTY Keymap.
Covers boundary ASCII codes (0x20 Space, 0x7E Tilde), unmapped Unicode fallback,
empty inputs, control character boundaries, and whitespace-only streams.
"""
import unittest
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary02UsQwerty(unittest.TestCase):
    """Tier 2: Boundary 2 - US QWERTY Keymap"""

    def setUp(self):
        self.translator = UsQwertyKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_ascii_extremes_space_and_tilde(self):
        """TC01: Translates bottom ASCII printable (0x20 ' ') and top ASCII printable (0x7E '~')."""
        s_space = self.translator.translate_char(" ")
        self.assertEqual(len(s_space), 1)
        self.assertEqual(s_space[0].usage_id, 0x2C)

        s_tilde = self.translator.translate_char("~")
        self.assertEqual(len(s_tilde), 1)
        self.assertEqual(s_tilde[0].usage_id, 0x35)
        self.assertEqual(s_tilde[0].modifier_mask, 0x02)

    def test_tc02_smart_quotes_and_dashes_transliteration(self):
        """TC02: Transliterates curly smart quotes (“ ”) and em-dashes (—) to standard ASCII."""
        smart_text = "“Hello” — ‘World’…"
        strokes = self.translator.translate_string(smart_text)
        self.assertGreater(len(strokes), 0)
        self.dispatcher.dispatch_burst(smart_text)
        self.assertEqual(self.host.host_text, '"Hello" - \'World\'...')

    def test_tc03_empty_string_input(self):
        """TC03: Translating empty string returns empty keystroke list."""
        strokes = self.translator.translate_string("")
        self.assertEqual(strokes, [])
        self.dispatcher.dispatch_burst("")
        self.assertEqual(self.host.host_text, "")

    def test_tc04_whitespace_only_stream(self):
        """TC04: Translating tabs, newlines, and multiple spaces."""
        text = "  \t\t \n\n  "
        self.dispatcher.dispatch_burst(text)
        self.assertEqual(self.host.host_text, text)

    def test_tc05_unmapped_foreign_glyph_fallback(self):
        """TC05: Unmapped non-ASCII glyphs (e.g. Cyrillic/CJK) return empty strokes without crash."""
        strokes = self.translator.translate_string("Привет 漢字")
        # Unmapped characters produce no corrupted scancodes
        self.assertEqual(strokes, [self.translator.translate_char(" ")[0]])


if __name__ == "__main__":
    unittest.main()
