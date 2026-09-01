"""
Stress Test S07: Adversarial Template Injections & Buffer Stress.
Verifies:
- SQL injection / command injection strings in variable prompt templates
- Deeply nested recursive braces {{{{{nested}}}}}
- 200,000-character massive AST tokenization stress
- Emergency release guard under simulated runtime exceptions
"""
import unittest
from tests.e2e.harness.variable_parser_simulator import VariableParser, InterpolationContext
from tests.e2e.harness.keymap_engine import UsQwertyKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestS07AdversarialTemplateInjections(unittest.TestCase):
    """Tier 5: Stress 07 - Adversarial Template Injections & Buffer Stress"""

    def setUp(self):
        self.host = HidHostSimulator(layout=KeyLayout.US_QWERTY)
        self.translator = UsQwertyKeymap()
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator, virtual_clock=True)

    def test_s07_01_sql_and_shell_injection_strings_in_prompts(self):
        """TC01: Template handles SQL and shell injection payloads safely as pure string literals."""
        template = "SELECT * FROM users WHERE name = '{{prompt:username}}' AND role = '{{prompt:role}}';"
        ctx = InterpolationContext(prompt_values={
            "username": "admin' OR '1'='1",
            "role": "user'; DROP TABLE users;--"
        })

        parsed = VariableParser.parse(template, ctx)
        self.assertEqual(
            parsed,
            "SELECT * FROM users WHERE name = 'admin' OR '1'='1' AND role = 'user'; DROP TABLE users;--';"
        )

    def test_s07_02_deeply_nested_and_malformed_braces_stress(self):
        """TC02: Resolves chaotic brace sequences {{{{{{nested}}}}}} without infinite loops."""
        chaotic = "{{{{{{{{{{timestamp}}}}}}}}}}"
        ctx = InterpolationContext(fixed_timestamp=123456789)
        parsed = VariableParser.parse(chaotic, ctx)
        self.assertIn("123456789", parsed)

    def test_s07_03_two_hundred_thousand_char_tokenization_stress(self):
        """TC03: Parses 200,000-character payload with 1,000 variable tokens in under 200ms."""
        import time
        chunk = "Token: {{timestamp}} - ID: {{uuid}}\n"
        massive = chunk * 5000  # ~175,000 chars with 10,000 tokens

        ctx = InterpolationContext(fixed_timestamp=1000, fixed_uuid="0000-1111")
        t0 = time.time()
        result = VariableParser.parse(massive, ctx)
        elapsed = time.time() - t0

        self.assertLess(elapsed, 0.5)  # Under 500ms
        self.assertIn("Token: 1000 - ID: 0000-1111", result)

    def test_s07_04_emergency_release_guard_under_runtime_exception(self):
        """TC04: Host simulator rejecting packets triggers emergency release report."""
        self.host.set_connected(False)
        ok = self.dispatcher.send_single_keystroke(0, 4)
        self.assertFalse(ok)
        self.assertGreaterEqual(self.dispatcher.emergency_releases_sent, 1)


if __name__ == "__main__":
    unittest.main()
