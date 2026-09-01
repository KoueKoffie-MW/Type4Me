"""
Workload Test W13: VS Code Save, Terminal Focus & Test Execution Macro Workflow.
Verifies:
- Saving active editor buffer (Ctrl+S)
- Focusing terminal panel
- Typing test runner command with prompt parameter interpolation
- Executing test run
"""
import unittest
from tests.e2e.harness.macro_runner_simulator import (
    MacroRunner, TypeStringAction, KeyCombinationAction, DelayAction, MacroExecutionStatus
)
from tests.e2e.harness.variable_parser_simulator import InterpolationContext
from tests.e2e.harness.hid_constants import MOD_L_CTRL, KEY_S, KEY_ENTER, MOD_NONE
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload13VsCodeSaveAndTestMacro(unittest.TestCase):
    """Tier 4: Workload 13 - VS Code Save & Run Test Macro"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.macro_runner = MacroRunner(dispatcher=self.dispatcher)

    def test_tc01_vscode_save_and_run_pytest_workflow(self):
        """TC01: Emits Ctrl+S, short delay, then types pytest command and Enter."""
        actions = [
            # 1. Save file: Ctrl+S
            KeyCombinationAction(modifier=MOD_L_CTRL, usage_id=KEY_S),
            DelayAction(duration_ms=50),
            # 2. Type pytest command with prompt interpolation
            TypeStringAction(text="pytest -v tests/{{prompt:test_path}}\n")
        ]

        ctx = InterpolationContext(prompt_values={"test_path": "unit/test_auth.py"})
        ok = self.macro_runner.execute(actions, context=ctx)

        self.assertTrue(ok)
        self.assertEqual(self.macro_runner.state.status, MacroExecutionStatus.COMPLETED)
        self.assertIn("pytest -v tests/unit/test_auth.py\n", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
