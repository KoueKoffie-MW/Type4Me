"""
Workload Scenario W09: Custom User Preset Lifecycle & Dictation Workflow.
Simulates creating a custom 'Medical German' preset, persisting in Room DB,
selecting in UI, dictating a medical diagnostic summary, rewriting, and typing to host.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestWorkload09CustomPresetLifecycle(unittest.TestCase):
    """Tier 4: Workload Scenario 9 - Custom User Preset Lifecycle"""

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

    def test_tc01_medical_preset_creation_and_execution(self):
        """TC01: Creates 'Medical German' preset and performs diagnosis dictation."""
        # 1. Create preset
        preset = self.vm.on_save_custom_preset(
            title="Arztbrief / Befund",
            system_prompt="Formuliere den Text als präzisen medizinischen Befundbericht auf Deutsch.",
        )
        self.assertIsNotNone(preset)

        # 2. Dictate medical summary
        raw_diagnosis = (
            "Patient klagt über akute Schmerzen im Bereich des rechten Knöchels. "
            "Keine Schwellung sichtbar. Empfehlung: Röntgenaufnahme zur Überprüfung."
        )
        self.vm.on_text_changed(raw_diagnosis)

        # 3. Trigger AI Rewrite
        ok_rewrite = self.vm.on_trigger_ai_rewrite()
        self.assertTrue(ok_rewrite)

        # 4. Transmit to host PC
        ok_send = self.vm.on_send_burst_to_host()
        self.assertTrue(ok_send)
        self.assertEqual(self.host.host_text, self.vm.state.transcription_text)
        self.assertIn("Röntgenaufnahme zur Überprüfung", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
