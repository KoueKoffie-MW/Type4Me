"""
Feature 22: Preset Management UI.
Verifies dialog/sheet UI interactions for creating, editing, selecting,
and deleting custom AI prompt presets in the MainViewModel.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, PromptPreset, BUILTIN_CLEAN_POLISH
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestFeature22PresetManagementUi(unittest.TestCase):
    """Tier 1: Feature 22 - Preset Management UI"""

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

    def test_tc01_create_custom_preset_via_ui(self):
        """TC01: Creating custom preset saves it and auto-selects it in UI state."""
        preset = self.vm.on_save_custom_preset(
            title="Markdown Formatter",
            system_prompt="Format the spoken text as structured Markdown."
        )
        self.assertIsNotNone(preset)
        self.assertEqual(preset.title, "Markdown Formatter")
        self.assertEqual(self.vm.state.selected_preset.id, preset.id)
        self.assertEqual(len(self.vm.state.presets), 4)

    def test_tc02_select_preset_from_chips(self):
        """TC02: Selecting preset chip updates selected_preset in UI state."""
        presets = self.vm.state.presets
        self.assertGreaterEqual(len(presets), 2)
        target = presets[1]
        self.vm.on_preset_selected(target)
        self.assertEqual(self.vm.state.selected_preset.id, target.id)

    def test_tc03_delete_custom_preset_via_ui(self):
        """TC03: Deleting custom preset removes it from list and reverts selection."""
        preset = self.vm.on_save_custom_preset(
            title="Temporary Preset",
            system_prompt="Temp"
        )
        self.assertEqual(len(self.vm.state.presets), 4)
        ok = self.vm.on_delete_custom_preset(preset)
        self.assertTrue(ok)
        self.assertEqual(len(self.vm.state.presets), 3)
        self.assertNotEqual(self.vm.state.selected_preset.id, preset.id)

    def test_tc04_cannot_delete_builtin_preset_via_ui(self):
        """TC04: Deleting built-in preset fails and maintains list integrity."""
        builtin = self.vm.state.presets[0]
        self.assertTrue(builtin.is_builtin)
        ok = self.vm.on_delete_custom_preset(builtin)
        self.assertFalse(ok)
        self.assertEqual(len(self.vm.state.presets), 3)

    def test_tc05_multiple_custom_presets_ordering(self):
        """TC05: Multiple custom presets are stored and listed with proper indexing."""
        p1 = self.vm.on_save_custom_preset("Custom A", "Prompt A")
        p2 = self.vm.on_save_custom_preset("Custom B", "Prompt B")
        self.assertEqual(len(self.vm.state.presets), 5)
        titles = [p.title for p in self.vm.state.presets]
        self.assertIn("Custom A", titles)
        self.assertIn("Custom B", titles)


if __name__ == "__main__":
    unittest.main()
