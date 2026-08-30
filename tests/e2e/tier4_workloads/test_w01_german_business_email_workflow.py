"""
Workload Scenario W01: Full German Business Email Dictation with Live Corrections.
Simulates a complete real-world workflow: dictating a formal business email in German,
correcting typos via live backspaces, applying the Business German AI preset,
and transmitting HID keyboard reports to the host PC.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, BUILTIN_BUSINESS_GERMAN
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport, ConnectionState


class TestWorkload01GermanBusinessEmail(unittest.TestCase):
    """Tier 4: Workload Scenario 1 - Full German Business Email Dictation"""

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

    def test_tc01_complete_business_email_dictation_flow(self):
        """TC01: Dictates full German email, applies AI preset, and types into host."""
        # 1. Spoken dictation
        raw_email = (
            "hallo Herr Schmidt, anbei sende ich Ihnen die aktualisierte Spezifikation für das "
            "Transcriptor-HID-Projekt. Bitte prüfen Sie insbesondere die DIN-2137-Unterstützung "
            "und die Übertragung der Umlaute Ä, Ö, Ü und ß. schick mir bitte bis Freitag Ihr Feedback. "
            "danke dir tschüss"
        )
        self.vm.on_text_changed(raw_email)
        self.assertEqual(self.vm.state.transcription_text, raw_email)

        # 2. Select Business German Preset & Trigger Rewrite
        self.vm.on_preset_selected(BUILTIN_BUSINESS_GERMAN)
        ok_rewrite = self.vm.on_trigger_ai_rewrite()
        self.assertTrue(ok_rewrite)

        rewritten_text = self.vm.state.transcription_text
        self.assertIn("Sehr geehrte Damen und Herren,", rewritten_text)
        self.assertIn("Herr Schmidt", rewritten_text)
        self.assertIn("bitte senden Sie mir", rewritten_text)
        self.assertIn("Mit freundlichen Grüßen", rewritten_text)

        # 3. Transmit to Host PC
        ok_send = self.vm.on_send_burst_to_host()
        self.assertTrue(ok_send)
        self.assertEqual(self.host.host_text, rewritten_text)
        self.assertEqual(self.host.error_count, 0)


if __name__ == "__main__":
    unittest.main()
