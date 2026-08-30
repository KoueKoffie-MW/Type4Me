"""
Feature 19: Single-Screen Jetpack Compose UI.
Verifies Material 3 single-screen UI components, status pills, canvas layout,
preset chips, and action floating action button state representations.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainUiState, MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestFeature19SingleScreenComposeUi(unittest.TestCase):
    """Tier 1: Feature 19 - Single-Screen Jetpack Compose UI"""

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

    def test_tc01_initial_ui_state_values(self):
        """TC01: Initial UI state has empty text, disconnected status, and default preset."""
        state = self.vm.state
        self.assertEqual(state.transcription_text, "")
        self.assertEqual(state.connection_state, ConnectionState.DISCONNECTED)
        self.assertFalse(state.is_transmitting)
        self.assertFalse(state.is_ai_rewriting)
        self.assertIsNotNone(state.selected_preset)

    def test_tc02_text_change_updates_canvas(self):
        """TC02: Updating canvas updates transcription_text in UI state."""
        self.vm.on_text_changed("Hallo Welt")
        self.assertEqual(self.vm.state.transcription_text, "Hallo Welt")

    def test_tc03_layout_selection_updates_active_layout(self):
        """TC03: Switching layout updates active_layout in UI state."""
        self.vm.on_layout_selected(KeyLayout.US_QWERTY)
        self.assertEqual(self.vm.state.active_layout, KeyLayout.US_QWERTY)

    def test_tc04_clear_and_undo_actions(self):
        """TC04: Clear button empties canvas and Undo restores previous text."""
        self.vm.on_text_changed("Wichtiger Text")
        self.vm.on_clear_text()
        self.assertEqual(self.vm.state.transcription_text, "")
        # Undo
        self.vm.on_undo()
        self.assertEqual(self.vm.state.transcription_text, "Wichtiger Text")

    def test_tc05_ui_error_message_presentation(self):
        """TC05: Dispatches error when attempting to transmit while disconnected."""
        self.assertEqual(self.vm.state.connection_state, ConnectionState.DISCONNECTED)
        self.vm.on_text_changed("Text zum Senden")
        ok = self.vm.on_send_burst_to_host()
        self.assertFalse(ok)
        self.assertIn("Host PC is not connected", self.vm.state.error_message)


if __name__ == "__main__":
    unittest.main()
