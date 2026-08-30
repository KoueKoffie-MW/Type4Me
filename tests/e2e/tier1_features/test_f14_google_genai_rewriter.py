"""
Feature 14: Google GenAI SDK Rewriter.
Verifies integration with Google GenAI models (gemini-3.7-flash / gemini-3.5-flash-lite),
API key validation, prompt execution, and response handling.
"""
import unittest
from tests.e2e.harness.ai_pipeline_simulator import (
    GeminiRemoteRewriter, AiEngineType, BUILTIN_CLEAN_POLISH
)


class TestFeature14GoogleGenAiRewriter(unittest.TestCase):
    """Tier 1: Feature 14 - Google GenAI SDK Rewriter"""

    def test_tc01_availability_with_valid_api_key(self):
        """TC01: Rewriter is available when a non-blank API key is configured."""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "AIzaSyFakeKey1234567890")
        self.assertTrue(rewriter.is_available())

    def test_tc02_unavailability_with_missing_key(self):
        """TC02: Rewriter is unavailable when API key is missing or blank."""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "")
        self.assertFalse(rewriter.is_available())
        ok, res, err = rewriter.rewrite("Test speech", BUILTIN_CLEAN_POLISH)
        self.assertFalse(ok)
        self.assertIn("API key is not configured", err)

    def test_tc03_empty_input_returns_empty_result(self):
        """TC03: Empty or whitespace input returns immediately with empty string."""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")
        ok, res, err = rewriter.rewrite("   ", BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertEqual(res, "")
        self.assertIsNone(err)

    def test_tc04_successful_text_transformation(self):
        """TC04: Executes successful transformation and removes filler words."""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")
        raw_text = "um hello like we want to test this uh app"
        ok, res, err = rewriter.rewrite(raw_text, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertNotIn("um", res.lower())
        self.assertNotIn("like", res.lower())
        self.assertNotIn("uh", res.lower())
        self.assertIsNone(err)

    def test_tc05_engine_type_model_id(self):
        """TC05: Validates correct model ID assignment (gemini-3.7-flash / gemini-3.5-flash-lite)."""
        rewriter_flash = GeminiRemoteRewriter(
            api_key_provider=lambda: "key",
            engine_type=AiEngineType.GEMINI_3_7_FLASH
        )
        self.assertEqual(rewriter_flash.engine_type.value, "gemini-3.7-flash")

        rewriter_lite = GeminiRemoteRewriter(
            api_key_provider=lambda: "key",
            engine_type=AiEngineType.GEMINI_3_5_FLASH_LITE
        )
        self.assertEqual(rewriter_lite.engine_type.value, "gemini-3.5-flash-lite")


if __name__ == "__main__":
    unittest.main()
