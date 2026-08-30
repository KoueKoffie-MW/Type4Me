"""
Feature 17: Custom Presets Storage (Room DB).
Verifies Room SQLite persistence for AI prompt presets: CRUD operations,
sorting orders, and DB constraints (protecting built-in presets from deletion).
"""
import unittest
from tests.e2e.harness.persistence_simulator import (
    PresetDaoSimulator, PresetRepositorySimulator, PresetEntity
)
from tests.e2e.harness.ai_pipeline_simulator import PromptPreset


class TestFeature17CustomPresetsRoomDb(unittest.TestCase):
    """Tier 1: Feature 17 - Custom Presets Storage (Room DB)"""

    def setUp(self):
        self.dao = PresetDaoSimulator()
        self.repo = PresetRepositorySimulator(self.dao)

    def test_tc01_initial_builtins_seeded(self):
        """TC01: Initial database contains 3 built-in prompt presets."""
        presets = self.repo.get_all_presets()
        self.assertEqual(len(presets), 3)
        self.assertTrue(all(p.is_builtin for p in presets))

    def test_tc02_insert_custom_preset(self):
        """TC02: Inserts new custom preset and retrieves it by ID."""
        custom = PromptPreset(
            id="custom_legal_01",
            title="Legal Contract Drafter",
            description="Formal contract language",
            system_prompt="Draft formal legal clauses.",
            is_builtin=False
        )
        self.repo.save_preset(custom)
        retrieved = self.repo.get_preset_by_id("custom_legal_01")
        self.assertIsNotNone(retrieved)
        self.assertEqual(retrieved.title, "Legal Contract Drafter")
        self.assertFalse(retrieved.is_builtin)

    def test_tc03_update_custom_preset(self):
        """TC03: Updates existing custom preset properties."""
        custom = PromptPreset(
            id="custom_02",
            title="Old Title",
            system_prompt="Old Prompt",
            is_builtin=False
        )
        self.repo.save_preset(custom)
        # Update
        updated = PromptPreset(
            id="custom_02",
            title="New Title",
            system_prompt="New Prompt",
            is_builtin=False
        )
        self.repo.save_preset(updated)
        retrieved = self.repo.get_preset_by_id("custom_02")
        self.assertEqual(retrieved.title, "New Title")
        self.assertEqual(retrieved.system_prompt, "New Prompt")

    def test_tc04_delete_custom_preset_success(self):
        """TC04: Deletes custom preset successfully."""
        custom = PromptPreset(
            id="custom_03",
            title="To Delete",
            system_prompt="To Delete",
            is_builtin=False
        )
        self.repo.save_preset(custom)
        self.assertEqual(len(self.repo.get_all_presets()), 4)
        ok = self.repo.delete_preset(custom)
        self.assertTrue(ok)
        self.assertEqual(len(self.repo.get_all_presets()), 3)
        self.assertIsNone(self.repo.get_preset_by_id("custom_03"))

    def test_tc05_builtin_presets_cannot_be_deleted(self):
        """TC05: Room DAO constraint prevents deletion of built-in presets."""
        builtins = self.repo.get_all_presets()
        builtin_item = builtins[0]
        self.assertTrue(builtin_item.is_builtin)
        ok = self.repo.delete_preset(builtin_item)
        self.assertFalse(ok)
        # Verify it still exists
        self.assertIsNotNone(self.repo.get_preset_by_id(builtin_item.id))


if __name__ == "__main__":
    unittest.main()
