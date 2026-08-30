"""
Feature 21: UI State & Intent Management (MVI).
Verifies MainViewModel handling of immutable MainUiState emissions and
unidirectional data flow via MainUiIntent events.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator, MainUiState
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_BUSINESS_GERMAN
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestFeature21MviUiStateManagement(unittest.TestCase):
    """Tier 1: Feature 21 - UI State & Intent Management (MVI)"""

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

    def test_tc01_state_immutability(self):
        """TC01: Validates that state is an immutable frozen dataclass."""
        state = self.vm.state
        with self.assertRaises(Exception):
            state.transcription_text = "Direct mutation should fail"  # type: ignore

    def test_tc02_state_listener_receives_updates(self):
        """TC02: State listener observes successive state transitions."""
        observed_states = []
        self.vm.add_state_listener(lambda s: observed_states.append(s.transcription_text))

        self.vm.on_text_changed("Alpha")
        self.vm.on_text_changed("Beta")
        self.vm.on_text_changed("Gamma")

        self.assertIn("Alpha", observed_states)
        self.assertIn("Beta", observed_states)
        self.assertIn("Gamma", observed_states)

    def test_tc03_ai_rewrite_flow_updates_state(self):
        """TC03: Triggering AI rewrite manages is_ai_rewriting lifecycle flag."""
        self.vm.on_text_changed("hallo schick mir den bericht")
        self.vm.on_preset_selected(BUILTIN_BUSINESS_GERMAN)
        ok = self.vm.on_trigger_ai_rewrite()
        self.assertTrue(ok)
        self.assertFalse(self.vm.state.is_ai_rewriting)
        self.assertIn("Sehr geehrte Damen und Herren,", self.vm.state.transcription_text)

    def test_tc04_burst_transmission_updates_state(self):
        """TC04: Triggering burst updates is_transmitting state and clears error message."""
        self.transport.initialize()
        self.transport.simulate_host_connect()
        self.vm._update_state(connection_state=ConnectionState.CONNECTED)

        self.vm.on_text_changed("Text zum Übertragen")
        ok = self.vm.on_send_burst_to_host()
        self.assertTrue(ok)
        self.assertFalse(self.vm.state.is_transmitting)
        self.assertEqual(self.host.host_text, "Text zum Übertragen")

    def test_tc05_undo_history_stack(self):
        """TC05: Multiple edits build up an undo history stack."""
        self.vm.on_text_changed("State 1")
        self.vm.on_text_changed("State 2")
        self.vm.on_text_changed("State 3")

        self.vm.on_undo()
        self.assertEqual(self.vm.state.transcription_text, "State 2")
        self.vm.on_undo()
        self.assertEqual(self.vm.state.transcription_text, "State 1")


if __name__ == "__main__":
    unittest.main()
