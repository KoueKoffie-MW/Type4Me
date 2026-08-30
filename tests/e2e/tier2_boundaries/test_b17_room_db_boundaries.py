"""
Boundary Tests: Feature 17 - Room Database Persistence.
Covers duplicate ID conflict replacement, bulk inserts (500 presets),
special characters / SQL characters in preset names, and deletion constraints.
"""
import unittest
from tests.e2e.harness.persistence_simulator import (
    PresetDaoSimulator, PresetRepositorySimulator, PresetEntity
)
from tests.e2e.harness.ai_pipeline_simulator import PromptPreset


class TestBoundary17RoomDb(unittest.TestCase):
    """Tier 2: Boundary 17 - Room Database Persistence"""

    def setUp(self):
        self.dao = PresetDaoSimulator()
        self.repo = PresetRepositorySimulator(self.dao)

    def test_tc01_duplicate_id_replaces_on_conflict(self):
        """TC01: Inserting preset with existing ID replaces old entry (OnConflictStrategy.REPLACE)."""
        p1 = PromptPreset(id="dup_1", title="Original Title", system_prompt="Prompt 1", is_builtin=False)
        self.repo.save_preset(p1)
        self.assertEqual(self.repo.get_preset_by_id("dup_1").title, "Original Title")

        # Overwrite with same ID
        p2 = PromptPreset(id="dup_1", title="Replaced Title", system_prompt="Prompt 2", is_builtin=False)
        self.repo.save_preset(p2)
        self.assertEqual(self.repo.get_preset_by_id("dup_1").title, "Replaced Title")

    def test_tc02_bulk_insert_five_hundred_presets(self):
        """TC02: Bulk inserting 500 custom presets maintains database consistency and sorting."""
        for i in range(500):
            p = PromptPreset(id=f"bulk_{i:03d}", title=f"Preset {i}", system_prompt=f"Prompt {i}", is_builtin=False)
            self.repo.save_preset(p)

        all_presets = self.repo.get_all_presets()
        # 3 builtins + 500 custom = 503 total
        self.assertEqual(len(all_presets), 503)
        # Built-ins must always appear first
        self.assertTrue(all_presets[0].is_builtin)
        self.assertTrue(all_presets[1].is_builtin)
        self.assertTrue(all_presets[2].is_builtin)

    def test_tc03_special_and_sql_syntax_in_preset_fields(self):
        """TC03: Handles quotes, semicolons, and SQL keywords (' OR 1=1; DROP TABLE) safely."""
        tricky_title = "'; DROP TABLE ai_presets; -- <xml> & \"quotes\""
        p = PromptPreset(id="sql_inj_01", title=tricky_title, system_prompt="Safe prompt", is_builtin=False)
        self.repo.save_preset(p)
        retrieved = self.repo.get_preset_by_id("sql_inj_01")
        self.assertEqual(retrieved.title, tricky_title)

    def test_tc04_delete_non_existent_preset(self):
        """TC04: Deleting a non-existent preset returns False without error."""
        non_existent = PromptPreset(id="does_not_exist", title="Ghost", system_prompt="", is_builtin=False)
        ok = self.repo.delete_preset(non_existent)
        self.assertFalse(ok)

    def test_tc05_large_system_prompt_storage(self):
        """TC05: Stores very large system instructions (50,000 characters)."""
        huge_prompt = "You are a specialized legal assistant. " * 1250  # 50,000 chars
        p = PromptPreset(id="huge_01", title="Large Prompt", system_prompt=huge_prompt, is_builtin=False)
        self.repo.save_preset(p)
        retrieved = self.repo.get_preset_by_id("huge_01")
        self.assertEqual(len(retrieved.system_prompt), len(huge_prompt))


if __name__ == "__main__":
    unittest.main()
