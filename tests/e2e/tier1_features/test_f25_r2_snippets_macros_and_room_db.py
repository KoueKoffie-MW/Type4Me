"""
Feature Test F25 (R2): Quick Snippets & Action Macros Pad (Room DB 2.6).
Verifies:
- Room 2.6 Schema (CategoryEntity, SnippetEntity, MacroEntity, PairedHostEntity)
- Pre-loaded Developer Tool Pack with 20+ categorized production snippets
- Database Migration MIGRATION_1_2 DDL upgrade from V1 to V2
- Single-pass mustache AST VariableParser with escaping and prompt extraction
- Polymorphic MacroAction hierarchy and MacroRunner coroutines execution
- Category filtering and full-text search in Snippets Pad
"""
import unittest
import json
from tests.e2e.harness.persistence_simulator import (
    AppDatabaseSimulator, CategoryEntity, SnippetEntity, MacroEntity, DefaultToolPackProvider
)
from tests.e2e.harness.variable_parser_simulator import VariableParser, InterpolationContext, PromptDescriptor
from tests.e2e.harness.macro_runner_simulator import (
    MacroRunner, TypeStringAction, KeyCombinationAction, DelayAction,
    PromptVariableAction, ClipboardPasteAction, MacroExecutionStatus
)
from tests.e2e.harness.hid_constants import MOD_L_CTRL, KEY_C, MOD_NONE, KEY_ENTER
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestFeature25SnippetsMacrosAndRoomDb(unittest.TestCase):
    """Tier 1: Feature 25 (R2) - Quick Snippets, Macros & Room DB 2.6"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.macro_runner = MacroRunner(dispatcher=self.dispatcher)

    def test_tc01_room_entities_crud_and_foreign_key_cascade(self):
        """TC01: Validates Room 2.6 entities CRUD and category cascade delete to snippets."""
        # Insert new category
        custom_cat = CategoryEntity("cat_custom", "Custom DevOps", "ic_custom", 10)
        self.db.category_dao.insert_category(custom_cat)
        self.assertEqual(len(self.db.category_dao.get_all_categories()), 6)

        # Insert snippet attached to custom category
        snip = SnippetEntity("snip_c1", "cat_custom", "Custom Build", "cargo build --release", "RUST")
        self.db.snippet_dao.insert_snippet(snip)
        self.assertIsNotNone(self.db.snippet_dao.get_snippet_by_id("snip_c1"))

        # Delete category -> cascading delete removes attached snippet
        self.db.category_dao.delete_category("cat_custom")
        self.assertIsNone(self.db.category_dao.get_category_by_id("cat_custom"))
        self.assertIsNone(self.db.snippet_dao.get_snippet_by_id("snip_c1"))

    def test_tc02_default_tool_pack_seeding_twenty_plus_snippets(self):
        """TC02: Preloads 20+ production developer snippets across 5 categories."""
        categories = self.db.category_dao.get_all_categories()
        snippets = self.db.snippet_dao.get_all_snippets()

        self.assertEqual(len(categories), 5)
        self.assertGreaterEqual(len(snippets), 20)

        cat_names = [c.name for c in categories]
        self.assertIn("Git Version Control", cat_names)
        self.assertIn("Docker & Containers", cat_names)
        self.assertIn("Kubernetes", cat_names)
        self.assertIn("Python & Pytest", cat_names)
        self.assertIn("Terminal & AI Prompts", cat_names)

    def test_tc03_migration_1_2_schema_upgrade(self):
        """TC03: Upgrades database from V1 (presets only) to V2 (snippets/macros/hosts) cleanly."""
        v1_db = AppDatabaseSimulator(version=1, seed_defaults=False)
        self.assertEqual(v1_db.version, 1)
        self.assertEqual(len(v1_db.category_dao.get_all_categories()), 0)

        # Perform migration
        upgraded = v1_db.apply_migration_1_2()
        self.assertTrue(upgraded)
        self.assertEqual(v1_db.version, 2)
        self.assertEqual(len(v1_db.category_dao.get_all_categories()), 5)
        self.assertGreaterEqual(len(v1_db.snippet_dao.get_all_snippets()), 20)

    def test_tc04_variable_parser_ast_interpolation_and_escaping(self):
        """TC04: Single-pass parser handles date/uuid/clipboard/prompt tokens and \\{\\{ escaping."""
        template = r"Commit: {{prompt:Message}} at {{iso_date}} with token {{clipboard}} and escaped \{\{literal\}\}"
        prompts = VariableParser.extract_prompts(template)
        self.assertEqual(len(prompts), 1)
        self.assertEqual(prompts[0].key, "message")

        ctx = InterpolationContext(
            clipboard_text="tok_abc123",
            prompt_values={"message": "fix issue #42"},
            fixed_iso_date="2026-09-02T00:00:00Z"
        )
        result = VariableParser.parse(template, ctx)
        expected = "Commit: fix issue #42 at 2026-09-02T00:00:00Z with token tok_abc123 and escaped {{literal}}"
        self.assertEqual(result, expected)

    def test_tc05_polymorphic_macro_action_execution(self):
        """TC05: Polymorphic MacroRunner executes TypeString, KeyCombination, and Delay actions."""
        actions = [
            TypeStringAction(text="pytest -k {{prompt:test_name}}"),
            KeyCombinationAction(modifier=MOD_NONE, usage_id=KEY_ENTER),
            DelayAction(duration_ms=20),
            TypeStringAction(text="echo done\n")
        ]

        ctx = InterpolationContext(prompt_values={"test_name": "test_e2e"})
        ok = self.macro_runner.execute(actions, context=ctx)
        self.assertTrue(ok)
        self.assertEqual(self.macro_runner.state.status, MacroExecutionStatus.COMPLETED)
        self.assertIn("pytest -k test_e2e\necho done\n", self.host.host_text)

    def test_tc06_snippets_search_and_category_filtering(self):
        """TC06: Searches snippets by keyword and filters by category chip."""
        git_snippets = self.db.snippet_dao.get_snippets_by_category("cat_git")
        self.assertGreaterEqual(len(git_snippets), 5)
        for s in git_snippets:
            self.assertEqual(s.category_id, "cat_git")

        # Full-text search
        search_res = self.db.snippet_dao.search_snippets("commit")
        self.assertTrue(any("commit" in s.title.lower() or "commit" in s.content.lower() for s in search_res))


if __name__ == "__main__":
    unittest.main()
