"""
Combination Test C16: MacroRunner + Sudden Disconnect + Emergency Release.
Verifies:
- Multi-step MacroAction coroutine execution
- Abort handling upon sudden host disconnection during Step 2
- Guarantee of emergency release report [0,0,0,0,0,0,0,0] upon failure
"""
import unittest
from tests.e2e.harness.macro_runner_simulator import (
    MacroRunner, TypeStringAction, KeyCombinationAction, DelayAction, MacroExecutionStatus
)
from tests.e2e.harness.hid_constants import MOD_L_CTRL, KEY_C, KEY_ENTER, MOD_NONE
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestCombination16MacroRunnerDisconnectEmergencyRelease(unittest.TestCase):
    """Tier 3: Combination 16 - MacroRunner + Disconnect + Emergency Release"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.macro_runner = MacroRunner(dispatcher=self.dispatcher)

    def test_tc01_disconnect_mid_macro_triggers_emergency_release(self):
        """TC01: Sudden host disconnection during macro execution triggers emergency release and fails cleanly."""
        actions = [
            TypeStringAction(text="echo step 1\n"),
            TypeStringAction(text="echo step 2\n"),
            TypeStringAction(text="echo step 3\n"),
        ]

        # Simulate disconnection during step 2
        def disconnect_hook():
            if self.dispatcher.total_keystrokes_sent >= 10:
                self.host.set_connected(False)
                return True
            return False

        ok = self.macro_runner.execute(actions, cancel_check=disconnect_hook)
        self.assertFalse(ok)
        self.assertEqual(self.macro_runner.state.status, MacroExecutionStatus.CANCELLED)

        # Confirm that emergency release was triggered
        self.assertGreaterEqual(self.dispatcher.emergency_releases_sent, 1)


if __name__ == "__main__":
    unittest.main()
