"""
Workload Scenario W02: Technical Code Comment & Docstring Dictation.
Simulates a software developer dictating architecture docstrings containing code syntax,
brackets, backslashes, pipes, and comments on both US QWERTY and German QWERTZ layouts.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_CODE_COMMENTS


class TestWorkload02TechnicalCodeComment(unittest.TestCase):
    """Tier 4: Workload Scenario 2 - Technical Code Comment & Docstring Dictation"""

    def setUp(self):
        self.de_translator = GermanQwertzKeymap()
        self.us_translator = UsQwertyKeymap()
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "key")

    def test_tc01_python_docstring_on_german_layout(self):
        """TC01: Dictates technical docstring with brackets and slashes on German QWERTZ."""
        host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        dispatcher = KeystrokeDispatcher(host=host, translator=self.de_translator)

        spoken = (
            "Calculates Longest Common Prefix (LCP) between old and new state.\n"
            "Returns Tuple[int, str] containing (backspaces, append_suffix)."
        )
        ok, commented, err = self.rewriter.rewrite(spoken, BUILTIN_CODE_COMMENTS)
        self.assertTrue(ok)

        dispatcher.dispatch_burst(commented)
        self.assertEqual(host.host_text, commented)
        self.assertIn("// Calculates Longest Common Prefix", host.host_text)
        self.assertIn("Tuple[int, str]", host.host_text)

    def test_tc02_c_style_comment_on_us_layout(self):
        """TC02: Dictates C-style comments with syntax operators on US QWERTY."""
        host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        dispatcher = KeystrokeDispatcher(host=host, translator=self.us_translator)

        code_doc = "// Ensure report[0] contains valid modifier bitmask (0x00 to 0xFF)"
        dispatcher.dispatch_burst(code_doc)
        self.assertEqual(host.host_text, code_doc)


if __name__ == "__main__":
    unittest.main()
