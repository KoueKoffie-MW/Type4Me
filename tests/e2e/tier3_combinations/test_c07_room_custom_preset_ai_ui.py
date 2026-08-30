"""
Combination Test C07: Custom Preset DB -> AI Rewriter -> MVI UI Update -> HID Output.
Verifies the complete pipeline: creating custom preset in Room DB, selecting it in UI,
running AI text transformation, updating state, and transmitting via HID reports.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestCombination07RoomCustomPresetAiUi(unittest.TestCase):
    """Tier 3: Combination 7 - Custom Preset DB -> AI Rewriter -> MVI UI -> HID Output"""

    def setUp(self):
        self.preset_repo = PresetRepositorySimulator()
        self.settings_repo = SettingsRepositorySimulator()
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "valid_key")
        self.translator = GermanQwertzKeymap()
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.transport.initialize()
        self.transport.simulate_host_connect()

        self.vm = MainViewModelSimulator(
            preset_repo=self.preset_repo,
            settings_repo=self.settings_repo,
            rewriter=self.rewriter,
            dispatcher=self.dispatcher,
            transport=self.transport
        )
        self.vm._update_state(connection_state=ConnectionState.CONNECTED)

    def test_tc01_full_custom_preset_dictation_pipeline(self):
        """TC01: Complete end-to-end custom preset creation, AI rewrite, and burst output."""
        # 1. User creates custom preset
        preset = self.vm.on_save_custom_preset(
            title="Bullet Point Converter",
            system_prompt="Convert text into bullet points.",
        )
        self.assertEqual(self.vm.state.selected_preset.id, preset.id)

        # 2. User dictates raw text
        raw_text = "Erstens HID initialisieren. Zweitens Bericht senden."
        self.vm.on_text_changed(raw_text)

        # 3. User triggers AI rewrite
        ok = self.vm.on_trigger_ai_rewrite()
        self.assertTrue(ok)

        # 4. User sends to host
        ok_send = self.vm.on_send_burst_to_host()
        self.assertTrue(ok_send)
        self.assertEqual(self.host.host_text, self.vm.state.transcription_text)

    def test_tc02_undo_restores_pre_ai_transcription(self):
        """TC02: Undo action restores original raw transcript before AI rewrite."""
        original = "Original Raw Voice Transcript"
        self.vm.on_text_changed(original)
        self.vm.on_trigger_ai_rewrite()
        self.assertNotEqual(self.vm.state.transcription_text, "")

        # Undo
        self.vm.on_undo()
        self.assertEqual(self.vm.state.transcription_text, original)


if __name__ == "__main__":
    unittest.main()
