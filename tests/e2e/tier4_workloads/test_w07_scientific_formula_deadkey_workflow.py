"""
Workload Scenario W07: Scientific Formula Dictation with Dead Keys & Superscripts.
Simulates dictating complex mathematical and thermodynamic equations:
f(x) = x^2 + y³ - 100°C ± 5% on German QWERTZ with dead-key space injection.
"""
import unittest
from tests.e2e.harness.keymap_engine import GermanQwertzKeymap, KeyLayout
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestWorkload07ScientificFormula(unittest.TestCase):
    """Tier 4: Workload Scenario 7 - Scientific Formula Dictation"""

    def setUp(self):
        self.translator = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.host = HidHostSimulator(layout=KeyLayout.GERMAN_QWERTZ)
        self.dispatcher = KeystrokeDispatcher(host=self.host, translator=self.translator)

    def test_tc01_thermodynamic_formula_dictation(self):
        """TC01: Dictates thermodynamic formula with degree sign (°), superscripts (² ³), and caret (^)."""
        formula = "T_eff = 20°C + (delta_T)^2 / (V³ * 100)"
        self.dispatcher.dispatch_burst(formula)
        self.assertEqual(self.host.host_text, formula)

    def test_tc02_complex_algebraic_equation(self):
        """TC02: Algebraic equation with powers and brackets: [a^2 + b^2] = c^2."""
        equation = "Formel: [a^2 + b^2] = c^2 | Fehlergrenze: ~ 0.01 %"
        self.dispatcher.dispatch_burst(equation)
        self.assertEqual(self.host.host_text, equation)


if __name__ == "__main__":
    unittest.main()
