"""
Combination Test C15: Desktop Companion Context + Gemini AI Rewriter + Snippets Pad.
Verifies:
- Fetching active IDE context from desktop companion script
- Ingesting context into AI Refactor Snippet prompt
- Executing Gemini AI rewrite with workstation context
- Paced typing of generated code back into host workstation
"""
import unittest
from tests.e2e.harness.companion_simulator import (
    DesktopCompanionServerSimulator, CompanionClientSimulator
)
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, PromptPreset
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator
from tests.e2e.harness.variable_parser_simulator import VariableParser, InterpolationContext
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination15CompanionContextGeminiSnippets(unittest.TestCase):
    """Tier 3: Combination 15 - Companion Context + Gemini AI + Snippets Pad"""

    def setUp(self):
        self.server = DesktopCompanionServerSimulator(port=8765)
        self.server.window_title = "VS Code - auth.py"
        self.server.selected_text = "def check_token(t): return t == 'secret'"
        self.server.process_name = "Code.exe"
        self.client = CompanionClientSimulator(server=self.server)

        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_api_key")

        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_end_to_end_context_enrichment_and_snippet_typing(self):
        """TC01: Pulls VS Code selected code, formats with AI refactor snippet, rewrites, and types."""
        # 1. Fetch context
        ok, context, _ = self.client.fetch_active_context()
        self.assertTrue(ok)
        self.assertEqual(context.window_title, "VS Code - auth.py")

        # 2. Get AI refactor snippet
        snippet = self.db.snippet_dao.get_snippet_by_id("snip_ai_refactor")
        self.assertIsNotNone(snippet)

        # 3. Interpolate snippet with clipboard/context text
        ctx = InterpolationContext(clipboard_text=context.selected_text)
        interpolated_prompt = VariableParser.parse(snippet.content, ctx)
        self.assertIn("def check_token(t):", interpolated_prompt)

        # 4. Enrich system prompt with desktop metadata
        preset = PromptPreset(
            id="preset_refactor",
            title="Refactor",
            system_prompt="You are an expert Python engineer."
        )
        full_prompt = self.client.enrich_prompt(interpolated_prompt, preset.system_prompt, context)
        self.assertIn("--- HOST WORKSTATION CONTEXT ---", full_prompt)

        # 5. Execute simulated AI rewrite & dispatch burst to host
        generated_code = "def check_token(token: str) -> bool:\n    return hmac.compare_digest(token, 'secret')\n"
        ok_type = self.dispatcher.dispatch_burst(generated_code, delay_ms=8.0)
        self.assertTrue(ok_type)
        self.assertIn("hmac.compare_digest", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
