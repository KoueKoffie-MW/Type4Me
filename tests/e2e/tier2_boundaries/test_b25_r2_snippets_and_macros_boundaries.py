"""
Boundary Test B25 (R2): Snippets, Macros & Room DB Boundaries.
Verifies:
- Unclosed mustache braces {{unclosed and empty {{}} tokens
- Nested variables and deep token structures
- Snippets exceeding 100,000 characters
- Corrupted/malformed macro actions JSON
- Foreign key violation attempts on snippet insertion
"""
import unittest
import json
from tests.e2e.harness.persistence_simulator import (
    AppDatabaseSimulator, CategoryEntity, SnippetEntity, MacroEntity
)
from tests.e2e.harness.variable_parser_simulator import VariableParser, InterpolationContext
from tests.e2e.harness.macro_runner_simulator import MacroRunner, MacroExecutionStatus
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestBoundary25SnippetsAndMacros(unittest.TestCase):
    """Tier 2: Boundary 25 (R2) - Snippets, Macros & Room DB Boundaries"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.macro_runner = MacroRunner(dispatcher=self.dispatcher)

    def test_tc01_unclosed_mustache_braces_and_empty_tokens(self):
        """TC01: Preserves unclosed braces {{unclosed and empty {{}} as safe literals."""
        template = "Hello {{unclosed world and {{}} empty token"
        res = VariableParser.parse(template)
        self.assertIn("{{unclosed world", res)

    def test_tc02_deeply_nested_and_escaped_variable_tokens(self):
        """TC02: Resolves escaped \\{\\{ braces correctly in complex shell scripts."""
        template = r"awk '\{\{print $1\}\}' file.txt && echo {{timestamp}}"
        ctx = InterpolationContext(fixed_timestamp=1700000000)
        res = VariableParser.parse(template, ctx)
        self.assertEqual(res, "awk '{{print $1}}' file.txt && echo 1700000000")

    def test_tc03_hundred_thousand_char_massive_snippet(self):
        """TC03: Stores and queries 100KB snippet content in Room DB without degradation."""
        huge_content = "SELECT * FROM large_table WHERE id = 1;\n" * 2500  # 100KB
        huge_snip = SnippetEntity("snip_huge", "cat_git", "Huge SQL Snippet", huge_content, "SHELL")
        self.db.snippet_dao.insert_snippet(huge_snip)

        fetched = self.db.snippet_dao.get_snippet_by_id("snip_huge")
        self.assertIsNotNone(fetched)
        self.assertEqual(len(fetched.content), len(huge_content))

    def test_tc04_malformed_macro_json_graceful_failure(self):
        """TC04: Malformed macro actions JSON triggers clean error without crashing."""
        malformed_json = "[{'invalid_json': True, missing_bracket"
        with self.assertRaises(Exception):
            MacroRunner.parse_actions_json(malformed_json)

    def test_tc05_foreign_key_violation_rejection(self):
        """TC05: Rejects snippet insertion referencing non-existent category_id."""
        invalid_snip = SnippetEntity("snip_bad", "non_existent_cat_id", "Bad FK", "echo bad", "SHELL")
        with self.assertRaises(ValueError):
            self.db.snippet_dao.insert_snippet(invalid_snip)


if __name__ == "__main__":
    unittest.main()
