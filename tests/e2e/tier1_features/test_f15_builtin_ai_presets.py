"""
Feature 15: Built-in AI Presets.
Verifies the 3 built-in prompt presets ("Clean & Polish", "Business German", "Technical Code Comments")
and their respective system instructions and prompt formatting.
"""
import unittest
from tests.e2e.harness.ai_pipeline_simulator import (
    BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS,
    GeminiRemoteRewriter
)


class TestFeature15BuiltinAiPresets(unittest.TestCase):
    """Tier 1: Feature 15 - Built-in AI Presets"""

    def setUp(self):
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "dummy_key")

    def test_tc01_clean_and_polish_preset_properties(self):
        """TC01: Validates Clean & Polish preset attributes and system prompt."""
        p = BUILTIN_CLEAN_POLISH
        self.assertEqual(p.id, "builtin_clean_polish")
        self.assertTrue(p.is_builtin)
        self.assertIn("Clean & Polish", p.title)
        self.assertIn("filler words", p.system_prompt)

    def test_tc02_business_german_preset_transformation(self):
        """TC02: Business German transforms informal greetings into formal German (Höflichkeitsform)."""
        raw = "hallo schick mir bitte das protokoll danke dir tschüss"
        ok, res, err = self.rewriter.rewrite(raw, BUILTIN_BUSINESS_GERMAN)
        self.assertTrue(ok)
        self.assertIn("Sehr geehrte Damen und Herren,", res)
        self.assertIn("bitte senden Sie mir", res)
        self.assertIn("Mit freundlichen Grüßen", res)

    def test_tc03_technical_code_comments_transformation(self):
        """TC03: Technical Code Comments prepends standard comment formatting (//)."""
        raw = "Initializes Bluetooth HID adapter and verifies connection state"
        ok, res, err = self.rewriter.rewrite(raw, BUILTIN_CODE_COMMENTS)
        self.assertTrue(ok)
        self.assertTrue(res.startswith("// "))
        self.assertIn("Bluetooth HID adapter", res)

    def test_tc04_user_template_formatting(self):
        """TC04: Formats user prompt with {INPUT_TEXT} interpolation."""
        p = BUILTIN_CLEAN_POLISH
        formatted = p.format_user_prompt("Mein Diktat")
        self.assertEqual(formatted, "Mein Diktat")

    def test_tc05_builtin_presets_are_marked_immutable(self):
        """TC05: All 3 default presets have is_builtin set to True."""
        for p in [BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS]:
            self.assertTrue(p.is_builtin)
            self.assertGreater(len(p.system_prompt), 20)


if __name__ == "__main__":
    unittest.main()
