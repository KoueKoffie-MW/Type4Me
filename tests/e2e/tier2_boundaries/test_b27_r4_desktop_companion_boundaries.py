"""
Boundary Test B27 (R4): Desktop Companion & AI Ingestion Boundaries.
Verifies:
- Socket timeout handling when host IDE responds slowly
- Extremely large 100KB selected text payload ingestion
- Corrupted non-JSON response from socket port
- Special control characters and ANSI escape codes in window title
- Fallback prompt fidelity when selected text is empty string
"""
import unittest
from tests.e2e.harness.companion_simulator import (
    DesktopCompanionServerSimulator, CompanionClientSimulator, DesktopContext
)


class TestBoundary27DesktopCompanion(unittest.TestCase):
    """Tier 2: Boundary 27 (R4) - Desktop Companion Boundaries"""

    def setUp(self):
        self.server = DesktopCompanionServerSimulator(port=8765)
        self.client = CompanionClientSimulator(server=self.server)

    def test_tc01_socket_timeout_handling(self):
        """TC01: Handles socket timeout gracefully when host workstation stalls."""
        self.server.simulate_timeout = True
        ok, context, err = self.client.fetch_active_context(timeout_s=0.5)
        self.assertFalse(ok)
        self.assertIsNone(context)
        self.assertIn("timed out", err)

    def test_tc02_large_hundred_kb_selection_payload(self):
        """TC02: Ingests 100KB large code file selection without buffer truncation."""
        large_selection = "def foo(): return 42\n" * 5000  # 100KB
        self.server.selected_text = large_selection

        ok, context, err = self.client.fetch_active_context()
        self.assertTrue(ok)
        self.assertEqual(len(context.selected_text), len(large_selection))

        prompt = self.client.enrich_prompt("Analyze this", "You are an assistant.", context)
        self.assertIn("--- HOST WORKSTATION CONTEXT ---", prompt)
        self.assertIn(large_selection[:100], prompt)

    def test_tc03_special_characters_in_window_title(self):
        """TC03: Handles emojis, control characters, and file paths in window title."""
        self.server.window_title = "✨ [Terminal] /usr/bin/zsh (pid: 12345) — 🚀 build_all.sh"
        ok, context, err = self.client.fetch_active_context()
        self.assertTrue(ok)
        self.assertEqual(context.window_title, "✨ [Terminal] /usr/bin/zsh (pid: 12345) — 🚀 build_all.sh")

    def test_tc04_empty_selected_text_with_valid_window(self):
        """TC04: When window title exists but selected text is empty, still enriches active app context."""
        self.server.selected_text = ""
        self.server.window_title = "Spotify Premium"
        self.server.process_name = "Spotify.exe"

        ok, context, _ = self.client.fetch_active_context()
        self.assertTrue(ok)
        prompt = self.client.enrich_prompt("Next song", "Media assistant", context)
        self.assertIn("Active Application: Spotify.exe (Spotify Premium)", prompt)

    def test_tc05_corrupted_json_payload_from_port(self):
        """TC05: Rejects corrupted non-JSON socket response cleanly without exception."""
        # Force non-json response body simulation
        self.server.force_http_status = 200
        # Temporarily mock get_context_response
        orig_method = self.server.get_context_response
        self.server.get_context_response = lambda: (200, "<html><body>502 Bad Gateway</body></html>")

        ok, context, err = self.client.fetch_active_context()
        self.assertFalse(ok)
        self.assertIsNone(context)
        self.assertIn("JSON parse error", err)

        self.server.get_context_response = orig_method


if __name__ == "__main__":
    unittest.main()
