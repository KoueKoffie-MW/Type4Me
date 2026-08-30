"""
Boundary Tests: Feature 14 - Google GenAI SDK Rewriter.
Covers HTTP 429 rate limits, HTTP 500 server errors, invalid/empty API keys,
excessive input length, and empty prompt inputs.
"""
import unittest
from tests.e2e.harness.ai_pipeline_simulator import (
    GeminiRemoteRewriter, AiEngineType, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN
)


class TestBoundary14GenAi(unittest.TestCase):
    """Tier 2: Boundary 14 - Google GenAI SDK Rewriter"""

    def test_tc01_http_429_rate_limit_handling(self):
        """TC01: Simulates HTTP 429 Rate Limit and returns actionable error message."""
        rewriter = GeminiRemoteRewriter(
            api_key_provider=lambda: "valid_key",
            simulated_status_code=429
        )
        ok, res, err = rewriter.rewrite("Test speech", BUILTIN_CLEAN_POLISH)
        self.assertFalse(ok)
        self.assertIn("Rate limit exceeded", err)

    def test_tc02_http_500_server_error_handling(self):
        """TC02: Simulates HTTP 500 Server Error and returns failure with error details."""
        rewriter = GeminiRemoteRewriter(
            api_key_provider=lambda: "valid_key",
            simulated_status_code=500
        )
        ok, res, err = rewriter.rewrite("Test speech", BUILTIN_CLEAN_POLISH)
        self.assertFalse(ok)
        self.assertIn("Internal Server Error", err)

    def test_tc03_whitespace_and_newlines_only(self):
        """TC03: Input consisting only of spaces, tabs, and newlines returns empty string."""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")
        ok, res, err = rewriter.rewrite("  \n\t  \n  ", BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertEqual(res, "")
        self.assertIsNone(err)

    def test_tc04_large_transcript_input(self):
        """TC04: Large transcript (5,000 words) processed successfully."""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")
        large_speech = "um this is a continuous dictation stream uh testing scale. " * 200
        ok, res, err = rewriter.rewrite(large_speech, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertGreater(len(res), 500)
        self.assertNotIn("um", res.lower())

    def test_tc05_dynamic_api_key_provider(self):
        """TC05: Dynamic API key provider reflects runtime key configuration changes."""
        current_key = ""
        rewriter = GeminiRemoteRewriter(api_key_provider=lambda: current_key)
        self.assertFalse(rewriter.is_available())

        current_key = "AIzaSyValidKeyNow"
        self.assertTrue(rewriter.is_available())


if __name__ == "__main__":
    unittest.main()
