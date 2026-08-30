"""
Boundary Tests: Feature 22 - Preset Dialog UI Boundaries.
Covers creating presets with whitespace/special characters, deleting while selected,
large preset collections in UI, and preserving selected state.
"""
import unittest
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.persistence_simulator import PresetRepositorySimulator, SettingsRepositorySimulator
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter, PromptPreset
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.service_simulator import BluetoothHidTransport


class TestBoundary22PresetDialog(unittest.TestCase):
    """Tier 2: Boundary 22 - Preset Dialog UI Boundaries"""

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

    def test_tc01_save_preset_with_special_characters_in_title(self):
        """TC01: Preset title containing emojis, German umlauts, and symbols."""
        title = "✨ Übersetzungs-Assistent (DE -> EN) / [v2.0] & 🚀"
        p = self.vm.on_save_custom_preset(title, "Translate to English")
        self.assertEqual(p.title, title)
        self.assertEqual(self.vm.state.selected_preset.title, title)

    def test_tc02_delete_currently_selected_custom_preset(self):
        """TC02: Deleting the currently active custom preset safely reverts selection to default."""
        custom = self.vm.on_save_custom_preset("Active Preset", "Prompt")
        self.assertEqual(self.vm.state.selected_preset.id, custom.id)

        # Delete it
        ok = self.vm.on_delete_custom_preset(custom)
        self.assertTrue(ok)
        self.assertTrue(self.vm.state.selected_preset.is_builtin)

    def test_tc03_create_twenty_custom_presets(self):
        """TC03: Adding 20 custom presets expands the preset list seamlessly."""
        for i in range(20):
            self.vm.on_save_custom_preset(f"Custom #{i}", f"Prompt #{i}")
        # 3 builtins + 20 custom = 23 total
        self.assertEqual(len(self.vm.state.presets), 23)

    def test_tc04_preset_selection_persists_across_text_changes(self):
        """TC04: Modifying canvas text does not reset or alter selected preset."""
        target = self.vm.state.presets[1]
        self.vm.on_preset_selected(target)
        self.vm.on_text_changed("Text 1")
        self.assertEqual(self.vm.state.selected_preset.id, target.id)
        self.vm.on_text_changed("Text 2")
        self.assertEqual(self.vm.state.selected_preset.id, target.id)

    def test_tc05_duplicate_preset_title_creation(self):
        """TC05: Multiple custom presets can have identical titles with unique IDs."""
        p1 = self.vm.on_save_custom_preset("Duplicate Title", "Prompt 1")
        p2 = self.vm.on_save_custom_preset("Duplicate Title", "Prompt 2")
        self.assertNotEqual(p1.id, p2.id)
        self.assertEqual(p1.title, p2.title)


if __name__ == "__main__":
    unittest.main()
