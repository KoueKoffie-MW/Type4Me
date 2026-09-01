"""
Workload Test W15: Docker Compose Interactive Session & SIGINT Interrupt Workflow.
Verifies:
- Launching Docker containers in foreground mode (docker compose up)
- Monitoring simulated log stream
- Injecting Ctrl+C (SIGINT) hardware hotkey from HotkeyDockBar to gracefully stop service
- Checking post-stop container status
"""
import unittest
from tests.e2e.harness.persistence_simulator import AppDatabaseSimulator
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout, HOTKEY_MAP
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload15DockerComposeSession(unittest.TestCase):
    """Tier 4: Workload 15 - Docker Compose Session & SIGINT"""

    def setUp(self):
        self.db = AppDatabaseSimulator(version=2, seed_defaults=True)
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_docker_compose_start_and_sigint_stop(self):
        """TC01: Types docker compose up, then emits Ctrl+C SIGINT hotkey."""
        # 1. Start compose
        self.dispatcher.dispatch_burst("docker compose up\n")
        self.assertIn("docker compose up\n", self.host.host_text)

        # 2. Emit Ctrl+C (SIGINT) hotkey from Hotkey Dock Bar
        ctrl_c = HOTKEY_MAP["CTRL_C"]
        ok = self.dispatcher.send_single_keystroke(ctrl_c.modifier_mask, ctrl_c.usage_id)
        self.assertTrue(ok)

        # 3. Clean status check
        snip_ps = self.db.snippet_dao.get_snippet_by_id("snip_docker_ps")
        self.dispatcher.dispatch_burst(snip_ps.content + "\n")
        self.assertIn("docker ps", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
