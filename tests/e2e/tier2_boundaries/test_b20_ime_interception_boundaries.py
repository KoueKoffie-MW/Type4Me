"""
Boundary Tests: Feature 20 - Gboard IME Interception.
Covers rapid 50ms speech delta updates, pasting huge text from clipboard,
rapid word replacements, and multiline IME insertions.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestBoundary20ImeInterception(unittest.TestCase):
    """Tier 2: Boundary 20 - Gboard IME Interception"""

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

    def test_tc01_ultra_rapid_speech_delta_emission_fifty_updates(self):
        """TC01: 50 progressive speech delta updates processed without dropping any intermediate state."""
        for i in range(1, 51):
            text = "Word " * i
            self.vm.on_text_changed(text.strip())
            self.assertEqual(self.vm.state.transcription_text, text.strip())

    def test_tc02_pasting_large_clipboard_content(self):
        """TC02: Simulates user pasting 10,000 characters from clipboard into canvas."""
        pasted = "Pasted text line from documentation.\n" * 270  # ~10,000 chars
        self.vm.on_text_changed(pasted)
        self.assertEqual(len(self.vm.state.transcription_text), len(pasted))

    def test_tc03_ime_partial_word_backspacing(self):
        """TC03: Gboard auto-corrects half-typed word ('Transkrip' -> 'Transkription')."""
        self.vm.on_text_changed("Transkrip")
        self.vm.on_text_changed("Transkription")
        self.assertEqual(self.vm.state.transcription_text, "Transkription")

    def test_tc04_ime_multiline_speech_formatting(self):
        """TC04: Spoken 'neue Zeile' commands creating multiline paragraphs."""
        spoken = "Absatz 1\n\nAbsatz 2\n\nAbsatz 3"
        self.vm.on_text_changed(spoken)
        self.assertEqual(self.vm.state.transcription_text, spoken)

    def test_tc05_ime_replacement_with_live_diff_active(self):
        """TC05: Real-time speech revision with Live Diff active accurately reflects on host."""
        self.transport.initialize()
        self.transport.simulate_host_connect()
        self.vm.on_live_diff_toggled(True)
        self.vm._update_state(connection_state=self.transport.connection_state)

        self.vm.on_text_changed("Hallo Herr Schmidt")
        self.assertEqual(self.host.host_text, "Hallo Herr Schmidt")
        # Gboard revises to "Hallo Herr Schneider"
        self.vm.on_text_changed("Hallo Herr Schneider")
        self.assertEqual(self.host.host_text, "Hallo Herr Schneider")


if __name__ == "__main__":
    unittest.main()
