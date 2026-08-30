"""
Feature 16: On-Device LiteRT-LM Contract.
Verifies the on-device LiteRT-LM architecture (LiteRtOnDeviceRewriter),
model weight availability validation, offline execution, and Gemma prompt structuring.
"""
import unittest
from tests.e2e.harness.ai_pipeline_simulator import (
    LiteRtOnDeviceRewriter, AiEngineType, BUILTIN_CLEAN_POLISH
)


class TestFeature16OnDeviceLiteRtContract(unittest.TestCase):
    """Tier 1: Feature 16 - On-Device LiteRT-LM Contract"""

    def test_tc01_available_when_weights_file_exists(self):
        """TC01: LiteRT engine is available when local weights are present."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        self.assertTrue(rewriter.is_available())
        self.assertEqual(rewriter.engine_type, AiEngineType.LITERT_ON_DEVICE)

    def test_tc02_unavailable_when_weights_missing(self):
        """TC02: Reports unavailable and returns error when model weights are missing."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=False)
        self.assertFalse(rewriter.is_available())
        ok, res, err = rewriter.rewrite("Test speech", BUILTIN_CLEAN_POLISH)
        self.assertFalse(ok)
        self.assertIn("not installed", err)

    def test_tc03_offline_execution_without_network_or_api_key(self):
        """TC03: Executes rewriting without network API keys or cloud connections."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        raw = "um testing offline LiteRT speech cleanup"
        ok, res, err = rewriter.rewrite(raw, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertNotIn("um", res.lower())
        self.assertIsNone(err)

    def test_tc04_empty_input_handling(self):
        """TC04: Empty or blank strings return empty string without error."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        ok, res, err = rewriter.rewrite("   ", BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertEqual(res, "")

    def test_tc05_engine_type_identification(self):
        """TC05: Identifies as gemma-2b-it-q4 on-device model."""
        rewriter = LiteRtOnDeviceRewriter(model_file_exists=True)
        self.assertEqual(rewriter.engine_type.value, "gemma-2b-it-q4")


if __name__ == "__main__":
    unittest.main()
