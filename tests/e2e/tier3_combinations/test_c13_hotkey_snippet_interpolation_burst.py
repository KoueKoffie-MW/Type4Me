"""
Combination Test C13: Hotkey Bar + Snippet Interpolation + Burst Dispatch.
Verifies:
- Snippet selection with variable prompting
- Variable interpolation with user parameter answers
- Paced keystroke typing to host
- Subsequent terminal hotkey injection (Enter, Tab, Alt+Tab)
"""
import unittest
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator, SnippetEntity
from tests.e2e.harness.variable_parser_simulator import VariableParser, InterpolationContext
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout, HOTKEY_MAP
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher
from tests.e2e.harness.ui_state_simulator import MainViewModelSimulator
from tests.e2e.harness.service_simulator import BluetoothHidTransport
from tests.e2e.harness.ai_pipeline_simulator import GeminiRemoteRewriter


class TestCombination13HotkeySnippetInterpolationBurst(unittest.TestCase):
    """Tier 3: Combination 13 - Hotkey Bar + Snippet Interpolation + Burst Dispatch"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)
        self.transport = BluetoothHidTransport(permissions_granted=True)
        self.transport.initialize()
        self.transport.simulate_host_connect()

        self.vm = MainViewModelSimulator(
            preset_repo=None,
            settings_repo=None,
            rewriter=GeminiRemoteRewriter(lambda: "key"),
            dispatcher=self.dispatcher,
            transport=self.transport,
            db=self.db
        )

    def test_tc01_snippet_prompt_modal_flow_and_hotkey_followup(self):
        """TC01: Selects git commit snippet, fills prompt parameter, dispatches, then taps hotkey."""
        commit_snip = self.db.snippet_dao.get_snippet_by_id("snip_git_commit")
        self.assertIsNotNone(commit_snip)

        # 1. Click snippet -> opens VariablePromptBottomSheet
        self.vm.on_snippet_clicked(commit_snip)
        self.assertTrue(self.vm.state.is_prompt_sheet_visible)
        self.assertEqual(len(self.vm.state.prompt_descriptors), 1)

        # 2. Enter commit message in prompt modal
        self.vm.on_prompt_answer_changed("commit_message", "feat: implement hotkey bar")

        # 3. Confirm prompt and dispatch to host
        ok = self.vm.on_confirm_prompt_and_dispatch()
        self.assertTrue(ok)
        self.assertIn('git commit -m "feat: implement hotkey bar"', self.host.host_text)

        # 4. Tap Enter hotkey from HotkeyDockBar
        ok_enter = self.vm.on_hotkey_pressed("ENTER")
        self.assertTrue(ok_enter)
        self.assertTrue(self.host.host_text.endswith("\n"))

        # 5. Tap Alt+Tab hotkey from HotkeyDockBar
        ok_alt_tab = self.vm.on_hotkey_pressed("ALT_TAB")
        self.assertTrue(ok_alt_tab)


if __name__ == "__main__":
    unittest.main()
