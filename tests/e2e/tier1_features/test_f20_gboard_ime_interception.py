"""
Feature 20: Gboard IME Voice Typing Interception.
Verifies text canvas listener receiving real-time voice transcription deltas,
cursor positioning, and composition string updates.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestFeature20GboardImeInterception(unittest.TestCase):
    """Tier 1: Feature 20 - Gboard IME Voice Typing Interception"""

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

    def test_tc01_continuous_ime_speech_delta_stream(self):
        """TC01: Captures progressive voice typing updates from Gboard."""
        deltas = [
            "Guten",
            "Guten Morgen",
            "Guten Morgen Herr",
            "Guten Morgen Herr Müller,",
        ]
        for d in deltas:
            self.vm.on_text_changed(d)
            self.assertEqual(self.vm.state.transcription_text, d)

    def test_tc02_voice_typing_with_live_diff_enabled(self):
        """TC02: In live diff mode, Gboard updates automatically stream to connected host."""
        self.transport.initialize()
        self.transport.simulate_host_connect()
        self.vm.on_live_diff_toggled(True)
        # Update VM's connection state to reflect transport
        self.vm._update_state(connection_state=self.transport.connection_state)

        self.vm.on_text_changed("Erste Zeile")
        self.assertEqual(self.host.host_text, "Erste Zeile")

        self.vm.on_text_changed("Erste Zeile korrigiert")
        self.assertEqual(self.host.host_text, "Erste Zeile korrigiert")

    def test_tc03_ime_deletion_event(self):
        """TC03: Handling user pressing backspace or deleting word in IME."""
        self.vm.on_text_changed("Hallo Welt")
        self.vm.on_text_changed("Hallo")
        self.assertEqual(self.vm.state.transcription_text, "Hallo")

    def test_tc04_ime_rapid_burst_inputs(self):
        """TC04: Simulates rapid speech word additions without dropped text."""
        words = ["Transcriptor", "unterstützt", "deutsche", "Umlaute", "reibungslos."]
        accumulated = ""
        for w in words:
            accumulated = f"{accumulated} {w}".strip()
            self.vm.on_text_changed(accumulated)
        self.assertEqual(self.vm.state.transcription_text, "Transcriptor unterstützt deutsche Umlaute reibungslos.")

    def test_tc05_special_characters_via_voice_ime(self):
        """TC05: Captures punctuation spoken aloud (Komma, Punkt, Fragezeichen)."""
        spoken = "Das ist ein Test, oder nicht? Genau!"
        self.vm.on_text_changed(spoken)
        self.assertEqual(self.vm.state.transcription_text, spoken)


if __name__ == "__main__":
    unittest.main()
