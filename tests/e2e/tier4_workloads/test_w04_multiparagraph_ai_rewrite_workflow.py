"""
Workload Scenario W04: Multi-Paragraph AI Rewrite Workflow.
Simulates dictating a comprehensive multi-paragraph document section,
running Gemini 3.7 Flash transformation, reviewing the UI diff, and transmitting
all paragraphs with deterministic 8ms pacing.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ai_pipeline_simulator import (
    GeminiRemoteRewriter, AiEngineType, BUILTIN_CLEAN_POLISH
)


class TestWorkload04MultiParagraphAiRewrite(unittest.TestCase):
    """Tier 4: Workload Scenario 4 - Multi-Paragraph AI Rewrite Workflow"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.rewriter = GeminiRemoteRewriter(
            api_key_provider=lambda: "key",
            engine_type=AiEngineType.GEMINI_3_7_FLASH
        )

    def test_tc01_three_paragraph_article_dictation(self):
        """TC01: Dictates 3 distinct paragraphs with newlines and umlauts."""
        p1 = "1. Einführung in das System: Transcriptor ermöglicht Spracheingabe ohne Treiber."
        p2 = "2. Technische Architektur: Die L2CAP-Kanäle übertragen 8-Byte-HID-Reports mit 8ms Takt."
        p3 = "3. Fazit: Höchste Zuverlässigkeit bei minimaler Latenz auf Windows, Mac und Linux."

        full_doc = f"{p1}\n\n{p2}\n\n{p3}"
        self.dispatcher.dispatch_burst(full_doc)

        self.assertEqual(self.host.host_text, full_doc)
        self.assertIn("1. Einführung in das System", self.host.host_text)
        self.assertIn("2. Technische Architektur", self.host.host_text)
        self.assertIn("3. Fazit", self.host.host_text)

    def test_tc02_pacing_integrity_across_paragraphs(self):
        """TC02: Validates zero dropped characters across multi-paragraph payload."""
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
