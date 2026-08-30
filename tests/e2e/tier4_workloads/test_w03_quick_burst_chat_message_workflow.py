"""
Workload Scenario W03: Quick Burst Chat Message with Slang, Emojis & Polish Preset.
Simulates a quick spoken message on Slack/Teams containing conversational slang,
disfluencies, emojis, and rapid 8ms burst typing into the chat field.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_CLEAN_POLISH


class TestWorkload03QuickBurstChatMessage(unittest.TestCase):
    """Tier 4: Workload Scenario 3 - Quick Burst Chat Message"""

    def setUp(self):
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "key")

    def test_tc01_casual_chat_cleanup_and_typing(self):
        """TC01: Cleans casual speech and transmits to chat box with ending punctuation."""
        spoken = "um bin gleich da wie sieht es bei dir aus"
        ok, polished, err = self.rewriter.rewrite(spoken, BUILTIN_CLEAN_POLISH)
        self.assertTrue(ok)
        self.assertNotIn("um", polished.lower())
        self.assertTrue(polished.endswith("."))

        self.dispatcher.dispatch_burst(polished)
        self.assertEqual(self.host.host_text, polished)

    def test_tc02_rapid_two_second_chat_burst(self):
        """TC02: Short confirmation message typed with 5ms pacing."""
        confirmation = "Super, danke für die schnelle Rückmeldung! Bis gleich."
        self.dispatcher.dispatch_burst(confirmation, delay_ms=5.0)
        self.assertEqual(self.host.host_text, confirmation)


if __name__ == "__main__":
    unittest.main()
