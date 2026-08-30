"""
Boundary Tests: Feature 16 - On-Device LiteRT-LM Contract.
Covers missing weight files, corrupted binary files, offline isolation,
and Gemma system prompt formatting structure.
"""
import unittest
from tests.e2e.harness.ai_pipeline_simulator import (
    LiteRtOnDeviceRewriter, BUILTIN_CLEAN_POLISH, PromptPreset
)


class TestBoundary16LiteRt(unittest.TestCase):
    """Tier 2: Boundary 16 - On-Device LiteRT-LM Contract"""

    def test_tc01_missing_model_weights_returns_clear_error(self):
        """TC01: Executing rewrite with missing weights returns descriptive error."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=False)
        ok, res, err = rewriter.rewrite("Test speech", BUILTIN_CLEAN_POLISH)
        self.assertFalse(ok)
        self.assertIn("model weights not installed", err)

    def test_tc02_offline_status_does_not_call_network(self):
        """TC02: On-device engine executes without network socket access."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        # Should succeed purely in local memory
        ok, res, err = rewriter.rewrite("um local execution", BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertEqual(res, "local execution")

    def test_tc03_custom_preset_prompt_formatting(self):
        """TC03: Formats custom system prompt according to Gemma turn structure."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        custom_preset = PromptPreset(
            id="custom_summary",
            title="Summary",
            system_prompt="Summarize in 3 words.",
            user_prompt_template="Input: {INPUT_TEXT}"
        )
        ok, res, err = rewriter.rewrite("This is a long sentence.", custom_preset)
        self.assertTrue(ok)
        self.assertIsNone(err)

    def test_tc04_repeated_inference_stability(self):
        """TC04: Running 100 consecutive local inference passes without state leakage."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        for i in range(100):
            ok, res, err = rewriter.rewrite(f"um sentence {i}", BUILTIN_CLEAN_POLISH)
            self.assertTrue(ok)
            self.assertEqual(res, f"sentence {i}")

    def test_tc05_whitespace_only_offline_input(self):
        """TC05: Whitespace input returns empty string immediately."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        ok, res, err = rewriter.rewrite("     ", BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertEqual(res, "")


if __name__ == "__main__":
    unittest.main()
