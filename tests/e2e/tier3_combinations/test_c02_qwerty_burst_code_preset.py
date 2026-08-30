"""
Combination Test C02: US QWERTY + Burst Mode + Technical Code Preset.
Verifies spoken developer commentary transformed into formatted code comments (//)
and transmitted in high-speed burst mode to a US QWERTY host.
"""
import unittest
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_CODE_COMMENTS


class TestCombination02QwertyBurstCodePreset(unittest.TestCase):
    """Tier 3: Combination 2 - QWERTY + Burst Mode + Technical Code Preset"""

    def setUp(self):
        self.translator = UsQwertyKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")

    def test_tc01_spoken_function_docstring_burst(self):
        """TC01: Spoken function description converted to // comments and typed in burst."""
        spoken = "Parses 8-byte HID keyboard report and updates modifier state"
        ok, commented, err = self.rewriter.rewrite(spoken, BUILTIN_CODE_COMMENTS)
        self.assertTrue(ok)
        self.assertEqual(commented, "// Parses 8-byte HID keyboard report and updates modifier state")

        self.dispatcher.dispatch_burst(commented)
        self.assertEqual(self.host.host_text, commented)

    def test_tc02_multiline_code_explanation_burst(self):
        """TC02: Multiline developer comment transmitted with exact indentation and slashes."""
        spoken = "TODO: Implement USB fallback transport\nFIXME: Handle Bluetooth disconnect"
        ok, commented, err = self.rewriter.rewrite(spoken, BUILTIN_CODE_COMMENTS)
        self.assertTrue(ok)

        self.dispatcher.dispatch_burst(commented)
        self.assertEqual(self.host.host_text, commented)
        self.assertIn("// TODO:", self.host.host_text)
        self.assertIn("// FIXME:", self.host.host_text)

    def test_tc03_code_comment_with_syntax_symbols(self):
        """TC03: Spoken comment containing code symbols: { }, ( ), [ ], $, #."""
        spoken = "Returns Map<String, List<HidKeyStroke>> for layout ID = 0"
        ok, commented, err = self.rewriter.rewrite(spoken, BUILTIN_CODE_COMMENTS)
        self.assertTrue(ok)

        self.dispatcher.dispatch_burst(commented)
        self.assertEqual(self.host.host_text, commented)


if __name__ == "__main__":
    unittest.main()
