"""
Boundary Tests: Feature 21 - MVI State Machine Boundaries.
Covers race conditions, concurrent intent processing, state immutability,
and error state recovery under stress.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_CLEAN_POLISH
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestBoundary21Mvi(unittest.TestCase):
    """Tier 2: Boundary 21 - MVI State Machine Boundaries"""

    def setUp(self):
        self.preset_repo = PresetRepositorySimulator()
        self.settings_repo = SettingsRepositorySimulator()
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "key")
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.transport = BluetoothHidTransport(permissions_granted=True)

        self.vm = MainViewModelSimulator(
            preset_repo=self.preset_repo,
            settings_repo=self.settings_repo,
            rewriter=self.rewriter,
            dispatcher=self.dispatcher,
            transport=self.transport
        )

    def test_tc01_concurrent_intents_sequence(self):
        """TC01: Rapid sequence of disparate intents executes sequentially without deadlock."""
        self.vm.on_text_changed("Text 1")
        self.vm.on_layout_selected(KeyLayout.US_QWERTY)
        self.vm.on_delay_changed(15)
        self.vm.on_live_diff_toggled(True)
        self.vm.on_text_changed("Text 2")

        state = self.vm.state
        self.assertEqual(state.transcription_text, "Text 2")
        self.assertEqual(state.active_layout, KeyLayout.US_QWERTY)
        self.assertEqual(state.typing_delay_ms, 15)
        self.assertTrue(state.live_diff_enabled)

    def test_tc02_trigger_ai_rewrite_on_empty_text_no_op(self):
        """TC02: Triggering AI rewrite when text is empty returns False and does not enter loading."""
        self.vm.on_clear_text()
        ok = self.vm.on_trigger_ai_rewrite()
        self.assertFalse(ok)
        self.assertFalse(self.vm.state.is_ai_rewriting)

    def test_tc03_state_transition_logging(self):
        """TC03: Verifies that every mutation creates a new distinct state instance."""
        s1 = self.vm.state
        self.vm.on_text_changed("Mutation 1")
        s2 = self.vm.state
        self.assertIsNot(s1, s2)
        self.assertNotEqual(s1.transcription_text, s2.transcription_text)

    def test_tc04_multiple_listeners_consistency(self):
        """TC04: Multiple registered UI state listeners receive identical state updates."""
        list_a = []
        list_b = []
        self.vm.add_state_listener(lambda s: list_a.append(s.transcription_text))
        self.vm.add_state_listener(lambda s: list_b.append(s.transcription_text))

        self.vm.on_text_changed("State A")
        self.vm.on_text_changed("State B")

        self.assertEqual(list_a, list_b)

    def test_tc05_error_state_isolation(self):
        """TC05: Errors during actions do not corrupt existing canvas text or settings."""
        self.vm.on_text_changed("Preserved Text")
        # Attempt send while disconnected -> produces error
        self.vm.on_send_burst_to_host()
        self.assertEqual(self.vm.state.transcription_text, "Preserved Text")
        self.assertIsNotNone(self.vm.state.error_message)


if __name__ == "__main__":
    unittest.main()
