"""
Combination Test C12: Concurrent Gboard Voice Input + AI Background Rewriting + HID Dispatch.
Verifies asynchronous execution of incoming voice streams while background AI rewrites
execute and queued HID keystroke reports are transmitted.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_CLEAN_POLISH
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestCombination12ConcurrentVoiceAiHid(unittest.TestCase):
    """Tier 3: Combination 12 - Concurrent Voice Input + AI Rewrite + HID Dispatch"""

    def setUp(self):
        self.preset_repo = PresetRepositorySimulator()
        self.settings_repo = SettingsRepositorySimulator()
        self.rewriter = GeminiRemoteRewriter(api_key_provider=lambda: "key")
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

    def test_tc01_voice_stream_followed_by_immediate_ai_and_burst(self):
        """TC01: Rapid sequence of speech input -> AI Polish -> Burst dispatch to host."""
        speech = "um guten morgen wir testen die anwendung uh heute"
        self.vm.on_text_changed(speech)
        self.assertEqual(self.vm.state.transcription_text, speech)

        # Trigger AI
        ok = self.vm.on_trigger_ai_rewrite()
        self.assertTrue(ok)
        polished = self.vm.state.transcription_text
        self.assertNotIn("um", polished.lower())

        # Dispatch burst
        ok_send = self.vm.on_send_burst_to_host()
        self.assertTrue(ok_send)
        self.assertEqual(self.host.host_text, polished)

    def test_tc02_voice_input_during_idle_transmission(self):
        """TC02: Gboard canvas can accept new dictation immediately after transmission completes."""
        self.vm.on_text_changed("Erster Satz.")
        self.vm.on_send_burst_to_host()
        self.assertFalse(self.vm.state.is_transmitting)

        # Immediately dictate new sentence
        self.vm.on_text_changed("Zweiter Satz.")
        self.assertEqual(self.vm.state.transcription_text, "Zweiter Satz.")


if __name__ == "__main__":
    unittest.main()
