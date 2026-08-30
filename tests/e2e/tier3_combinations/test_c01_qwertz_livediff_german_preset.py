"""
Combination Test C01: German QWERTZ + Live Diff + Business German Preset.
Verifies real-time speech transformation using Business German preset, streaming
differential backspaces, and DIN 2137-1 scancode generation to host workstation.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_BUSINESS_GERMAN


class TestCombination01QwertzLiveDiffBusinessGerman(unittest.TestCase):
    """Tier 3: Combination 1 - QWERTZ + Live Diff + Business German Preset"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")

    def test_tc01_live_business_greeting_evolution(self):
        """TC01: Live speech evolves from informal 'hi' to formal business greeting with umlauts."""
        speech_steps = [
            "hi",
            "hi Herr Müller",
            "hi Herr Müller bitte schick mir das Protokoll",
        ]
        # Simulate user applying Business German preset at each completed clause
        for step in speech_steps:
            ok, rewritten, err = self.rewriter.rewrite(step, BUILTIN_BUSINESS_GERMAN)
            self.assertTrue(ok)
            self.dispatcher.dispatch_live_diff(rewritten)
            self.assertEqual(self.host.host_text, rewritten)

        self.assertIn("Guten Tag,", self.host.host_text)
        self.assertIn("Herr Müller", self.host.host_text)
        self.assertIn("bitte senden Sie mir", self.host.host_text)

    def test_tc02_closing_salutation_live_diff(self):
        """TC02: Live revision of informal 'danke dir tschüss' into formal closing."""
        initial_informal = "danke dir tschüss"
        ok, formal, err = self.rewriter.rewrite(initial_informal, BUILTIN_BUSINESS_GERMAN)
        self.assertTrue(ok)
        self.dispatcher.dispatch_live_diff(formal)
        self.assertEqual(self.host.host_text, formal)
        self.assertIn("Mit freundlichen Grüßen", self.host.host_text)

    def test_tc03_full_business_inquiry_with_backspaces(self):
        """TC03: Progressive revision with live corrections of formal German inquiry."""
        h1 = "Sehr geehrte Damen und Herren, wir möchten ein Angebot"
        h2 = "Sehr geehrte Damen und Herren, wir möchten ein unverbindliches Angebot anfordern."
        self.dispatcher.dispatch_live_diff(h1)
        self.assertEqual(self.host.host_text, h1)
        self.dispatcher.dispatch_live_diff(h2)
        self.assertEqual(self.host.host_text, h2)


if __name__ == "__main__":
    unittest.main()
