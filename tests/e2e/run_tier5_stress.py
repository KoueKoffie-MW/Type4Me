#!/usr/bin/env python3
"""
Transcriptor HID — Tier 5 Adversarial Stress Test Suite Runner.
Executes empirical adversarial stress testing across:
- S01: Bluetooth Sudden Disconnects During 100-Char Burst & Emergency Key Release
- S02: Host Caps Lock Inverted States & Dynamic Modifier Bitmask Shifts
- S03: AI Rewriter Adversarial Inputs, Injections, Extreme Lengths & Error Recovery
- S04: Concurrent Room DB CRUD Operations & Built-in Preset Protection
- S05: Rapid UI Intent Flooding (1,000 Intents/Sec) Against MainViewModel

Usage:
    python tests/e2e/run_tier5_stress.py [-v]
"""
import sys
import os
import time
import unittest

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)


def main():
    tier5_dir = os.path.join(PROJECT_ROOT, "tests", "e2e", "tier5_stress")
    loader = unittest.TestLoader()
    suite = loader.discover(start_dir=tier5_dir, pattern="test_s*.py")

    print("================================================================================")
    print("      TRANSCRIPTOR HID — TIER 5 ADVERSARIAL STRESS TEST SUITE RUNNER            ")
    print("================================================================================")
    print(f"Working Directory : {PROJECT_ROOT}")
    print(f"Stress Suite Path : {tier5_dir}")
    print(f"Discovered Tests  : {suite.countTestCases()} test cases")
    print("================================================================================\n")

    start_time = time.time()
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    elapsed = time.time() - start_time

    print("\n================================================================================")
    print("                      TIER 5 STRESS TEST SUMMARY REPORT                         ")
    print("================================================================================")
    print(f"Total Test Cases  : {result.testsRun}")
    print(f"Passed            : {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failed            : {len(result.failures)}")
    print(f"Errors            : {len(result.errors)}")
    print(f"Execution Time    : {elapsed:.3f} seconds")
    print(f"Overall Status    : {'SUCCESS (100% PASS)' if result.wasSuccessful() else 'FAILED'}")
    print("================================================================================\n")

    if not result.wasSuccessful():
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
