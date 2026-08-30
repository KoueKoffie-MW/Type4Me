"""
Boundary Tests: Feature 19 - Jetpack Compose Single-Screen UI.
Covers rapid button spamming, massive canvas text size (50,000 chars),
undo stack limit boundaries, and error message dismissals.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestBoundary19ComposeUi(unittest.TestCase):
    """Tier 2: Boundary 19 - Jetpack Compose Single-Screen UI"""

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

    def test_tc01_massive_canvas_text_length(self):
        """TC01: Canvas UI state holds 50,000 characters without memory issues."""
        large_text = "Transcriptor voice dictation canvas buffer test. " * 1000
        self.vm.on_text_changed(large_text)
        self.assertEqual(len(self.vm.state.transcription_text), len(large_text))

    def test_tc02_repeated_clear_and_undo_cycles(self):
        """TC02: Rapidly alternating Clear and Undo 50 times."""
        self.vm.on_text_changed("Persistent Content")
        for _ in range(50):
            self.vm.on_clear_text()
            self.assertEqual(self.vm.state.transcription_text, "")
            self.vm.on_undo()
            self.assertEqual(self.vm.state.transcription_text, "Persistent Content")

    def test_tc03_rapid_send_button_clicks_when_empty(self):
        """TC03: Clicking Send FAB when text canvas is empty does not trigger transmission."""
        self.vm.on_clear_text()
        ok = self.vm.on_send_burst_to_host()
        self.assertFalse(ok)
        self.assertFalse(self.vm.state.is_transmitting)

    def test_tc04_undo_on_empty_history(self):
        """TC04: Calling undo when history is empty returns False gracefully."""
        self.vm.on_clear_text()
        self.vm._update_state(undo_history=[])
        ok = self.vm.on_undo()
        self.assertFalse(ok)

    def test_tc05_error_message_clears_on_next_input(self):
        """TC05: Existing error message is cleared as soon as user types new text."""
        # Trigger an error
        self.vm.on_text_changed("Text")
        self.vm.on_send_burst_to_host()  # Fails because host is disconnected
        self.assertIsNotNone(self.vm.state.error_message)

        # User edits text -> error message must be cleared
        self.vm.on_text_changed("New Text")
        self.assertIsNone(self.vm.state.error_message)


if __name__ == "__main__":
    unittest.main()
