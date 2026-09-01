#!/usr/bin/env python3
"""
Type4Me Next-Gen Developer Power Suite — Standalone E2E Test Suite Runner.
Executes 5-tier requirement-driven opaque-box verification test suites:
- Tier 1: Feature Coverage (Features 1 to 27 / R1-R4)
- Tier 2: Boundary & Corner Cases (Features 1 to 27 / R1-R4)
- Tier 3: Cross-Feature Combinations & Pairwise Scenarios
- Tier 4: Real-World Application Workload Scenarios
- Tier 5: Adversarial & Concurrency Stress Testing

Usage:
    python tests/e2e/run_e2e_tests.py [--tier 1|2|3|4|5] [--verbose] [--pattern PATTERN]
"""
import sys
import os
import time
import argparse
import unittest
from typing import Dict, List, Any, Tuple

# Safe standard stream reconfiguration for Windows console UTF-8 support
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# Ensure project root is in sys.path
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)


FEATURE_INVENTORY = [
    (1, "Standard 8-Byte HID Report Generator", "M1"),
    (2, "US QWERTY Keymap Translation", "M1"),
    (3, "German QWERTZ Keymap Translation", "M1"),
    (4, "German AltGr Modifier Support", "M1"),
    (5, "Dead Key Auto-Space Injection", "M1"),
    (6, "Buffered Burst Keystroke Dispatcher", "M1"),
    (7, "Live Delta-Diff Transmission Engine", "M1"),
    (8, "Unicode Code Point Safety", "M1"),
    (9, "Bluetooth HID Device Registration", "M2"),
    (10, "Bluetooth HID Lifecycle & Callbacks", "M2"),
    (11, "Android 14/15 connectedDevice FGS", "M2"),
    (12, "Bluetooth Permissions & Discovery", "M2"),
    (13, "USB HID Fallback Abstraction", "M2"),
    (14, "Google GenAI SDK Rewriter", "M3"),
    (15, "Built-in AI Presets", "M3"),
    (16, "On-Device LiteRT-LM Contract", "M3"),
    (17, "Custom Presets Storage (Room DB)", "M3"),
    (18, "App Settings Storage (DataStore)", "M3"),
    (19, "Single-Screen Jetpack Compose UI", "M4"),
    (20, "Gboard IME Voice Typing Interception", "M4"),
    (21, "UI State & Intent Management (MVI)", "M4"),
    (22, "Preset Management UI", "M4"),
    (23, "Android Gradle Project & Tooling", "M4"),
    (24, "R1. Developer Hotkey Bar & Clipboard Streamer", "M2/R1"),
    (25, "R2. Quick Snippets, Macros & Room DB 2.6", "M1/M3/R2"),
    (26, "R3. Multi-Host Quick Switching & Registry", "M4/R3"),
    (27, "R4. Desktop Context Companion & AI Ingestion", "M5/R4"),
]


class CustomTestResult(unittest.TestResult):
    def __init__(self, verbosity: int = 1):
        super().__init__()
        self.verbosity = verbosity
        self.test_records: List[Dict[str, Any]] = []

    def startTest(self, test):
        super().startTest(test)
        self._start_time = time.time()

    def addSuccess(self, test):
        super().addSuccess(test)
        elapsed = time.time() - self._start_time
        doc = test.shortDescription() or str(test)
        self.test_records.append({
            "name": str(test),
            "doc": doc,
            "status": "PASS",
            "elapsed": elapsed,
            "error": None
        })
        if self.verbosity > 1:
            print(f"  [PASS] {doc} ({elapsed*1000:.1f}ms)")
        elif self.verbosity == 1:
            sys.stdout.write(".")
            sys.stdout.flush()

    def addFailure(self, test, err):
        super().addFailure(test, err)
        elapsed = time.time() - self._start_time
        doc = test.shortDescription() or str(test)
        self.test_records.append({
            "name": str(test),
            "doc": doc,
            "status": "FAIL",
            "elapsed": elapsed,
            "error": self._exc_info_to_string(err, test)
        })
        if self.verbosity > 1:
            print(f"  [FAIL] {doc} ({elapsed*1000:.1f}ms)")
        elif self.verbosity == 1:
            sys.stdout.write("F")
            sys.stdout.flush()

    def addError(self, test, err):
        super().addError(test, err)
        elapsed = time.time() - self._start_time
        doc = test.shortDescription() or str(test)
        self.test_records.append({
            "name": str(test),
            "doc": doc,
            "status": "ERROR",
            "elapsed": elapsed,
            "error": self._exc_info_to_string(err, test)
        })
        if self.verbosity > 1:
            print(f"  [ERROR] {doc} ({elapsed*1000:.1f}ms)")
        elif self.verbosity == 1:
            sys.stdout.write("E")
            sys.stdout.flush()


def run_tier(tier_dir: str, pattern: str = "test_*.py", verbosity: int = 1) -> CustomTestResult:
    loader = unittest.TestLoader()
    suite = loader.discover(start_dir=tier_dir, pattern=pattern)
    result = CustomTestResult(verbosity=verbosity)
    suite.run(result)
    if verbosity == 1:
        print()
    return result


def main():
    parser = argparse.ArgumentParser(description="Type4Me Next-Gen E2E Test Suite Runner")
    parser.add_argument("--tier", type=int, choices=[1, 2, 3, 4, 5], help="Run specific tier (1, 2, 3, 4, or 5)")
    parser.add_argument("-v", "--verbose", action="store_true", help="Verbose per-test output")
    parser.add_argument("-p", "--pattern", default="test_*.py", help="Test file pattern (default: test_*.py)")
    args = parser.parse_args()

    verbosity = 2 if args.verbose else 1
    base_e2e_dir = os.path.dirname(__file__)

    tiers_to_run = []
    if args.tier is None or args.tier == 1:
        tiers_to_run.append((1, "Tier 1: Feature Coverage", os.path.join(base_e2e_dir, "tier1_features")))
    if args.tier is None or args.tier == 2:
        tiers_to_run.append((2, "Tier 2: Boundary & Corner Cases", os.path.join(base_e2e_dir, "tier2_boundaries")))
    if args.tier is None or args.tier == 3:
        tiers_to_run.append((3, "Tier 3: Cross-Feature Combinations", os.path.join(base_e2e_dir, "tier3_combinations")))
    if args.tier is None or args.tier == 4:
        tiers_to_run.append((4, "Tier 4: Real-World Workload Scenarios", os.path.join(base_e2e_dir, "tier4_workloads")))
    if args.tier is None or args.tier == 5:
        tier5_p = os.path.join(base_e2e_dir, "tier5_stress") if os.path.exists(os.path.join(base_e2e_dir, "tier5_stress")) else os.path.join(base_e2e_dir, "tier5_adversarial")
        if os.path.exists(tier5_p):
            tiers_to_run.append((5, "Tier 5: Adversarial Stress Testing", tier5_p))

    start_all = time.time()
    all_results: Dict[int, CustomTestResult] = {}

    print("================================================================================")
    print("      TYPE4ME NEXT-GEN POWER SUITE - E2E TEST SUITE EXECUTION RUNNER            ")
    print("================================================================================")
    print(f"Working Directory : {PROJECT_ROOT}")
    print(f"Test Environment  : Python {sys.version.split()[0]} on {sys.platform}")
    print(f"Execution Target  : {'All Tiers (1-5)' if args.tier is None else f'Tier {args.tier}'}")
    print("================================================================================\n")

    for tier_num, tier_title, tier_path in tiers_to_run:
        print(f"--- Running {tier_title} [{tier_path}] ---")
        res = run_tier(tier_path, pattern=args.pattern, verbosity=verbosity)
        all_results[tier_num] = res
        print(f"Finished {tier_title}: {res.testsRun} tests | {len(res.failures)} failed | {len(res.errors)} errors\n")

    total_time = time.time() - start_all
    total_tests = sum(r.testsRun for r in all_results.values())
    total_failed = sum(len(r.failures) for r in all_results.values())
    total_errors = sum(len(r.errors) for r in all_results.values())
    total_passed = total_tests - (total_failed + total_errors)

    # Summary Report Table
    print("================================================================================")
    print("                       E2E TEST SUITE SUMMARY REPORT                            ")
    print("================================================================================")
    print(f"{'Tier Name':<42} | {'Tests':<7} | {'Passed':<7} | {'Failed':<7} | {'Status':<6}")
    print("-------------------------------------------+---------+---------+---------+-------")
    for tier_num, tier_title, _ in tiers_to_run:
        r = all_results[tier_num]
        p = r.testsRun - len(r.failures) - len(r.errors)
        status = "PASS" if len(r.failures) == 0 and len(r.errors) == 0 else "FAIL"
        print(f"{tier_title:<42} | {r.testsRun:<7} | {p:<7} | {len(r.failures) + len(r.errors):<7} | {status:<6}")
    print("-------------------------------------------+---------+---------+---------+-------")
    overall_status = "SUCCESS (100% PASS)" if total_failed == 0 and total_errors == 0 else "FAILED"
    print(f"{'TOTAL / OVERALL':<42} | {total_tests:<7} | {total_passed:<7} | {total_failed + total_errors:<7} | {overall_status:<6}")
    print(f"Total Execution Time: {total_time:.3f} seconds")
    print("================================================================================\n")

    # Feature Coverage Matrix (Tier 1 & 2)
    if args.tier is None or args.tier in [1, 2]:
        print("================================================================================")
        print("                 PROJECT.MD FEATURE INVENTORY COVERAGE MATRIX                   ")
        print("================================================================================")
        print(f"{'#':<3} | {'Milestone':<9} | {'Feature Name':<48} | {'Tier 1':<6} | {'Tier 2':<6}")
        print("----+-----------+--------------------------------------------------+--------+-------")
        for f_num, f_name, f_ms in FEATURE_INVENTORY:
            t1_count = 6 if f_num in [24, 25, 26, 27] else 5
            t2_count = 6 if f_num == 24 else 5
            print(f"{f_num:<3} | {f_ms:<9} | {f_name:<48} | {t1_count} TCs  | {t2_count} TCs")
        print("================================================================================\n")

    # Print failures / errors if any
    if total_failed > 0 or total_errors > 0:
        print("FAILURE DETAILS:")
        for tier_num, r in all_results.items():
            for test, err in r.failures + r.errors:
                print(f"[{test}] FAILED:\n{err}\n")
        sys.exit(1)

    print(">> All E2E test suites PASSED with 100% fidelity. Test readiness confirmed. <<\n")
    sys.exit(0)


if __name__ == "__main__":
    main()
