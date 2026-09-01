"""
Feature Test F27 (R4): Dual-Tier Zero-Install Desktop Context Companion.
Verifies:
- Standalone Python / PowerShell companion HTTP service on port 8765
- GET /context returning active window title, selected text, process name, timestamp
- Android CompanionClient socket communication
- Dual-tier Gemini prompt engineering with desktop context ingestion
- 100% Air-gap fallback when companion script is offline
"""
import unittest
import json
from tests.e2e.harness.companion_simulator import (
    DesktopCompanionServerSimulator, CompanionClientSimulator, DesktopContext
)
from tests.e2e.harness.ai_pipeline_simulator import PromptPreset


class TestFeature27DesktopCompanionAndAiIngestion(unittest.TestCase):
    """Tier 1: Feature 27 (R4) - Desktop Companion & AI Ingestion"""

    def setUp(self):
        self.server = DesktopCompanionServerSimulator(port=8765)
        self.client = CompanionClientSimulator(server=self.server)

    def test_tc01_companion_server_context_payload_structure(self):
        """TC01: Companion server outputs valid JSON with window_title, selected_text, process_name."""
        self.server.window_title = "VS Code - main.py"
        self.server.selected_text = "def compute_crc(): pass"
        self.server.process_name = "Code.exe"

        status, body = self.server.get_context_response()
        self.assertEqual(status, 200)
        data = json.loads(body)
        self.assertEqual(data["window_title"], "VS Code - main.py")
        self.assertEqual(data["selected_text"], "def compute_crc(): pass")
        self.assertEqual(data["process_name"], "Code.exe")
        self.assertIn("timestamp", data)

    def test_tc02_companion_client_fetch_and_deserialization(self):
        """TC02: CompanionClient successfully fetches and parses DesktopContext."""
        ok, context, err = self.client.fetch_active_context(host_ip="127.0.0.1", port=8765)
        self.assertTrue(ok)
        self.assertIsNotNone(context)
        self.assertIsNone(err)
        self.assertEqual(context.process_name, "Code.exe")

    def test_tc03_prompt_enrichment_with_desktop_context(self):
        """TC03: Ingests desktop context into Gemini system prompt prefix."""
        ok, context, _ = self.client.fetch_active_context()
        self.assertTrue(ok)

        system_prompt = "You are an AI coding assistant."
        user_input = "Write unit tests for this function."

        enriched = self.client.enrich_prompt(user_input, system_prompt, context)
        self.assertIn("--- HOST WORKSTATION CONTEXT ---", enriched)
        self.assertIn("Active Application: Code.exe (VS Code - main.py)", enriched)
        self.assertIn("def calculate_hash(data: bytes) -> str:", enriched)
        self.assertIn("User Input: Write unit tests for this function.", enriched)

    def test_tc04_air_gap_graceful_fallback_when_server_offline(self):
        """TC04: When companion script is offline, prompt gracefully falls back without failure."""
        self.server.is_running = False
        ok, context, err = self.client.fetch_active_context()
        self.assertFalse(ok)
        self.assertIsNone(context)
        self.assertIn("Connection refused", err)

        # Fallback prompt generation (100% air-gap safe)
        system_prompt = "You are an AI coding assistant."
        user_input = "Write unit tests."
        fallback_prompt = self.client.enrich_prompt(user_input, system_prompt, context=None)

        self.assertNotIn("--- HOST WORKSTATION CONTEXT ---", fallback_prompt)
        self.assertEqual(fallback_prompt, "You are an AI coding assistant.\n\nUser Input: Write unit tests.")

    def test_tc05_companion_server_http_error_handling(self):
        """TC05: Handles HTTP 500 or timeout from companion server gracefully."""
        self.server.force_http_status = 500
        ok, context, err = self.client.fetch_active_context()
        self.assertFalse(ok)
        self.assertIsNone(context)
        self.assertIn("HTTP error 500", err)


if __name__ == "__main__":
    unittest.main()
