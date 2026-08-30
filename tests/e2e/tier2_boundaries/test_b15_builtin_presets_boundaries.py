"""
Boundary Tests: Feature 15 - Built-in AI Presets Boundaries.
Covers temperature boundary values (0.0 to 1.0), empty user templates,
foreign language inputs, and code-mixed speech dictation.
"""
import unittest
from tests.e2e.harness.ai_pipeline_simulator import (
    BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS,
    PromptPreset, GeminiRemoteRewriter
)


class TestBoundary15BuiltinPresets(unittest.TestCase):
    """Tier 2: Boundary 15 - Built-in AI Presets Boundaries"""

    def setUp(self):
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "key")

    def test_tc01_temperature_boundary_range(self):
        """TC01: Temperature configuration allows values between 0.0 (deterministic) and 1.0."""
        p_low = PromptPreset(id="low", title="Low Temp", system_prompt="Sys", temperature=0.0)
        p_high = PromptPreset(id="high", title="High Temp", system_prompt="Sys", temperature=1.0)
        self.assertEqual(p_low.temperature, 0.0)
        self.assertEqual(p_high.temperature, 1.0)

    def test_tc02_custom_template_without_input_placeholder(self):
        """TC02: Template without {INPUT_TEXT} appends raw text at the bottom."""
        p = PromptPreset(
            id="test",
            title="Append Only",
            system_prompt="Sys",
            user_prompt_template="Instructions:"
        )
        formatted = p.format_user_prompt("Mein Text")
        self.assertIn("Instructions:", formatted)
        self.assertIn("Mein Text", formatted)

    def test_tc03_multilingual_code_switching_clean_and_polish(self):
        """TC03: Polish preset cleans speech containing both German and English words."""
        raw = "um bitte checken Sie die parameters in der config like immediately"
        ok, res, err = self.rewriter.rewrite(raw, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertNotIn("um", res.lower())
        self.assertNotIn("like", res.lower())
        self.assertIn("parameters", res)

    def test_tc04_all_caps_shouting_input(self):
        """TC04: Dictation in all uppercase letters is normalized cleanly."""
        raw = "UM BITTE ANTWORTEN SIE SOFORT"
        ok, res, err = self.rewriter.rewrite(raw, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertNotIn("UM", res)

    def test_tc05_multi_line_code_comment_conversion(self):
        """TC05: Multi-line spoken comment is converted line-by-line into // comments."""
        raw = "Step 1: Initialize HID\nStep 2: Connect host\nStep 3: Transmit packets"
        ok, res, err = self.rewriter.rewrite(raw, BUILTIN_CODE_COMMENTS)
        self.assertTrue(ok)
        lines = res.splitlines()
        self.assertEqual(len(lines), 3)
        self.assertTrue(all(l.startswith("// ") for l in lines))


if __name__ == "__main__":
    unittest.main()
