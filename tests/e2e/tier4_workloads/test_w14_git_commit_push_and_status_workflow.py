"""
Workload Test W14: Production Git Commit, Push & Status Workflow.
Verifies:
- Executing git status verification
- Staging changed files (git add -A)
- Creating formatted commit message with timestamp and issue ID
- Pushing to remote upstream
"""
import unittest
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator
from tests.e2e.harness.variable_parser_simulator import VariableParser, InterpolationContext
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload14GitWorkflow(unittest.TestCase):
    """Tier 4: Workload 14 - Production Git Commit & Push Workflow"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_complete_git_development_lifecycle(self):
        """TC01: Runs git status, stages files, commits with prompt message, and pushes."""
        # Step 1: Check status
        snip_status = self.db.snippet_dao.get_snippet_by_id("snip_git_status")
        self.dispatcher.dispatch_burst(snip_status.content + "\n")

        # Step 2: Stage all
        self.dispatcher.dispatch_burst("git add -A\n")

        # Step 3: Commit with prompt message and date
        template = 'git commit -m "[#{{prompt:issue_id}}] {{prompt:message}} ({{date}})"\n'
        ctx = InterpolationContext(
            prompt_values={"issue_id": "108", "message": "implement bracketed paste mode"},
            fixed_iso_date="2026-09-02"
        )
        commit_cmd = VariableParser.parse(template, ctx)
        self.dispatcher.dispatch_burst(commit_cmd)

        # Step 4: Push to branch
        snip_push = self.db.snippet_dao.get_snippet_by_id("snip_git_push")
        self.dispatcher.dispatch_burst(snip_push.content + "\n")

        # Verify host terminal received all commands in sequence
        self.assertIn("git status -sb\n", self.host.host_text)
        self.assertIn("git add -A\n", self.host.host_text)
        self.assertIn('git commit -m "[#108] implement bracketed paste mode (2026-09-02)"\n', self.host.host_text)
        self.assertIn("git push origin HEAD\n", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
