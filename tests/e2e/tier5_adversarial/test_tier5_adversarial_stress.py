"""
Tier 5 Empirical Adversarial Stress-Testing Suite.
Validates:
1. High-throughput bursts (1,000+ words with mixed umlauts, AltGr symbols, math notations, newlines, tabs)
2. Rapid streaming delta-diff revisions (erratic ASR hesitation, mid-sentence rewrite, prefix truncation, emoji surrogate pair insertions, empty string flips, 1,000-step randomized fuzzing)
3. Dead key stress testing (^, ´, `, ~) followed by vowels vs consonants, clusters, code blocks, and math powers
4. Timing fuzzing (0ms to 100ms delays, pacing verification, burst queue saturation >50k chars)
"""
import os
import sys
import random
import unittest

# Ensure project root is in sys.path
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from tests.e2e.harness.hid_constants import (
    MOD_NONE, MOD_L_SHIFT, MOD_R_ALT, MOD_SHIFT_ALTGR,
    KEY_NONE, KEY_A, KEY_Z, KEY_1, KEY_0, KEY_ENTER, KEY_BACKSPACE, KEY_TAB, KEY_SPACE
)
from tests.e2e.harness.keymap_engine import (
    KeyLayout, HidKeyStroke, HidReport, KeymapTranslator,
    UsQwertyKeymap, GermanQwertzKeymap
)
from tests.e2e.harness.delta_diff_engine import DeltaDiffEngine, DiffResult
from tests.e2e.harness.hid_host_simulator import HidHostSimulator
from tests.e2e.harness.dispatcher_simulator import KeystrokeDispatcher


class TestTier5AdversarialBurstWorkloads(unittest.TestCase):
    """Tier 5 Suite 1: High-Throughput Burst Stress Testing (>1,000 words / >10,000 chars)"""

    def setUp(self):
        self.host_de = HidHostSimulator(KeyLayout.GERMAN_QWERTZ)
        self.trans_de = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.disp_de = KeystrokeDispatcher(self.host_de, self.trans_de, virtual_clock=True)

        self.host_us = HidHostSimulator(KeyLayout.US_QWERTY)
        self.trans_us = UsQwertyKeymap()
        self.disp_us = KeystrokeDispatcher(self.host_us, self.trans_us, virtual_clock=True)

    def test_tc01_german_massive_1000_word_burst(self):
        """TC01: Transmits 1,200+ German words with heavy umlauts, compound nouns, and punctuation."""
        sample_paragraphs = [
            "Sehr geehrte Damen und Herren,\n\n"
            "hiermit möchten wir Ihnen den ausführlichen Bericht über die Einführung der "
            "neuen Hintergrundübertragungsgeschwindigkeit und der Fahrerassistenzsysteme vorlegen. "
            "In den vergangenen Monaten wurden zahlreiche Qualitätsprüfungen an verschiedenen "
            "Fußgängerüberwegen und Straßenkreuzungen in München, Nürnberg, Düsseldorf und Köln durchgeführt.\n",

            "Dabei zeigte sich, dass die Größenbeschränkung der Datenpakete auf höchstem Niveau eingehalten wurde. "
            "Die Übertragungsrate über die Bluetooth-Schnittstelle betrug durchschnittlich 8 Millisekunden pro Tastendruck. "
            "Sämtliche Umlaute (ä, ö, ü, Ä, Ö, Ü) sowie das Eszett (ß) und das große Eszett (ẞ) wurden fehlerfrei "
            "an das Host-Betriebssystem übermittelt, ohne dass ein einziger Buchstabe verloren ging.\n",

            "Im technischen Bereich wurden folgende Schnittstellen definiert:\n"
            "1. Bluetooth Low Energy HID Profile (SDP Subclass 0x40)\n"
            "2. Universeller 63-Byte HID-Report-Deskriptor mit 6-Key-Rollover (6KRO)\n"
            "3. DIN 2137-1 Tastenbelegung für das deutsche QWERTZ-Layout mit automatischer Leerzeichen-Injektion.\n\n",

            "Wir bitten Sie, die beigefügte Dokumentation sorgfältig zu prüfen und uns Rückmeldung zu geben.\n"
            "Mit freundlichen Grüßen,\n"
            "Jan van der Merwe\n"
            "Leitender Systemingenieur für Echtzeitsimulation und Fahrerarbeitsplätze\n"
        ]

        full_text = "\n".join(sample_paragraphs * 8)
        word_count = len(full_text.split())
        char_count = len(full_text)

        self.assertGreaterEqual(word_count, 1000, f"Expected >= 1000 words, got {word_count}")
        self.assertGreaterEqual(char_count, 7000, f"Expected >= 7000 chars, got {char_count}")

        success = self.disp_de.dispatch_burst(full_text, delay_ms=5.0)
        self.assertTrue(success)

        self.assertEqual(self.host_de.error_count, 0)
        self.assertEqual(self.host_de.host_text, full_text)
        self.assertEqual(len(self.host_de.host_text), len(full_text))

    def test_tc02_dense_altgr_and_programming_syntax_burst(self):
        """TC02: Dense AltGr symbols (@, €, \\, {, }, [, ], |, µ, ², ³) and programming constructs."""
        lines = [
            "// TypeScript / Kotlin Hardware Descriptor\n",
            "@Entity(tableName = \"hid_presets_v2\")\n",
            "data class PresetEntity(\n",
            "    @PrimaryKey(autoGenerate = true) val id: Long = 0L,\n",
            "    @ColumnInfo(name = \"title\") val title: String,\n",
            "    @ColumnInfo(name = \"price\") val priceStr: String = \"125.50 € / Unit\",\n",
            "    @ColumnInfo(name = \"surface_area\") val areaStr: String = \"45 m² + 12 m³\",\n",
            "    @ColumnInfo(name = \"latency_budget\") val latency: String = \"500 µs <= budget <= 1000 µs\"\n",
            ") {\n",
            "    fun generateReportPath(baseDir: String): String = \"C:\\\\Users\\\\Transcriptor\\\\AppData\\\\Local\\\\Temp\\\\report.log\"\n",
            "    fun getFilterMask(): Int = (0x01 | 0x02 | 0x40 | 0x80) & ~0x04\n",
            "    fun evaluateArray(): Array<String> = arrayOf(\"[\", \"{\", \"}\", \"]\", \"|\", \"@\", \"\\\\\")\n",
            "}\n"
        ]
        full_code = "".join(lines * 15)
        self.assertGreaterEqual(len(full_code), 5000)

        success = self.disp_de.dispatch_burst(full_code, delay_ms=5.0)
        self.assertTrue(success)
        self.assertEqual(self.host_de.error_count, 0)
        self.assertEqual(self.host_de.host_text, full_code)

    def test_tc03_scientific_and_mathematical_notation_burst(self):
        """TC03: Mathematical formulas, superscripts, Greek transliterations, and symbols."""
        math_text = (
            "# Mathematical & Scientific Notation Benchmark\n\n"
            "Equation 1: E = mc² (Einstein mass-energy equivalence)\n"
            "Equation 2: V = (4/3) * pi * r³ (Volume of sphere with cubic power r³)\n"
            "Equation 3: f(x) = a*x² + b*x + c, where Delta = b² - 4*a*c\n"
            "Equation 4: Temperature variation Delta_T = 24.5 °C (Degree symbol check)\n"
            "Equation 5: Integral: \\int_{0}^{10} (3x² - 2x + 1) dx = [x³ - x² + x]_0^10 = 1000 - 100 + 10 = 910\n"
            "Equation 6: Logic: (A && B) || (!C && D) == {x in R | x > 0 && x <= 100}\n"
            "Equation 7: Paragraph check § 201 StGB, Abs. 1 Nr. 2 & 3.\n"
        )
        full_math = math_text * 12

        success = self.disp_de.dispatch_burst(full_math, delay_ms=6.0)
        self.assertTrue(success)
        self.assertEqual(self.host_de.error_count, 0)
        self.assertEqual(self.host_de.host_text, full_math)

    def test_tc04_us_qwerty_full_ascii_spectrum_burst(self):
        """TC04: Full 95 printable ASCII characters + control keys in US QWERTY mode."""
        ascii_chars = "".join(chr(i) for i in range(32, 127))
        document = f"Standard ASCII Printable Range (32-126):\n{ascii_chars}\n\n"
        document += "Testing every single US QWERTY shifted and unshifted pair:\n"
        document += "1234567890 -=\t!@#$%^&*() _+\n"
        document += "qwertyuiop []\\ \tQWERTYUIOP {}|\n"
        document += "asdfghjkl; ' \tASDFGHJKL: \"\n"
        document += "zxcvbnm,./ \tZXCVBNM<>?\n"

        full_doc = document * 30
        self.assertGreaterEqual(len(full_doc), 6000)

        success = self.disp_us.dispatch_burst(full_doc, delay_ms=5.0)
        self.assertTrue(success)
        self.assertEqual(self.host_us.error_count, 0)
        self.assertEqual(self.host_us.host_text, full_doc)

    def test_tc05_extreme_unspaced_continuous_tokens(self):
        """TC05: Extreme continuous unspaced alphanumeric tokens (1,000+ chars in single token)."""
        random.seed(42)
        charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_=+<>"
        long_token = "".join(random.choice(charset) for _ in range(1500))

        success = self.disp_de.dispatch_burst(long_token, delay_ms=4.0)
        self.assertTrue(success)
        self.assertEqual(self.host_de.host_text, long_token)


class TestTier5AdversarialStreamingDeltaDiff(unittest.TestCase):
    """Tier 5 Suite 2: Rapid Streaming Delta-Diff Revisions & Erratic ASR Simulation"""

    def setUp(self):
        self.host = HidHostSimulator(KeyLayout.GERMAN_QWERTZ)
        self.trans = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.disp = KeystrokeDispatcher(self.host, self.trans, virtual_clock=True)

    def test_tc01_erratic_character_by_character_asr_growth(self):
        """TC01: Character-by-character growth simulating real-time ASR acoustic frame decoding."""
        target_sentence = "Transcriptor HID ermöglicht blitzschnelle Spracheingabe auf Windows, macOS und Linux."
        stream = []
        for i in range(1, len(target_sentence) + 1):
            stream.append(target_sentence[:i])

        for hypothesis in stream:
            diff = self.disp.dispatch_live_diff(hypothesis, delay_ms=4.0)
            self.assertEqual(self.host.host_text, hypothesis)

        self.assertEqual(self.host.host_text, target_sentence)
        self.assertEqual(self.disp.total_backspaces_sent, 0)

    def test_tc02_aggressive_mid_sentence_rewrites_and_retractions(self):
        """TC02: Realistic speech hesitation rewrites with word retractions and replacements."""
        hypotheses = [
            "Ich möchte morgen",
            "Ich möchte morgen nach",
            "Ich möchte morgen nach Berlin fahren",
            "Ich möchte morgen früh nach Berlin fahren",
            "Ich möchte morgen früh um acht Uhr nach Berlin fahren",
            "Ich muss morgen früh um acht Uhr nach Berlin reisen",
            "Wir müssen morgen früh um 08:30 Uhr nach Hamburg reisen",
            "Wir planen morgen früh um 08:30 Uhr nach Hamburg zu fliegen.",
            "Termin: Flug LH 204 nach München um 07:15 Uhr bestätigt!"
        ]

        for hyp in hypotheses:
            self.disp.dispatch_live_diff(hyp, delay_ms=4.0)
            self.assertEqual(self.host.host_text, hyp, f"Desynchronization on hyp: {hyp}")

        self.assertEqual(self.host.host_text, hypotheses[-1])
        self.assertGreater(self.disp.total_backspaces_sent, 50)

    def test_tc03_progressive_prefix_and_suffix_truncation(self):
        """TC03: Progressive slicing from head, tail, and middle to verify LCP calculation."""
        text = "Das System verarbeitet alle Eingaben deterministisch."
        self.disp.dispatch_live_diff(text, delay_ms=4.0)
        self.assertEqual(self.host.host_text, text)

        for length in range(len(text) - 1, 0, -3):
            sub = text[:length]
            self.disp.dispatch_live_diff(sub, delay_ms=4.0)
            self.assertEqual(self.host.host_text, sub)

        self.disp.dispatch_live_diff("", delay_ms=4.0)
        self.assertEqual(self.host.host_text, "")

        for start in range(0, len(text), 4):
            sub = text[start:]
            self.disp.dispatch_live_diff(sub, delay_ms=4.0)
            self.assertEqual(self.host.host_text, sub)

    def test_tc04_emoji_and_surrogate_pair_integrity(self):
        """TC04: Multi-byte emoji surrogate pair handling during delta-diff editing."""
        engine = DeltaDiffEngine()

        d1 = engine.compute_diff("System Status: 🚀", "System Status: 🚀🔥 SUCCESS")
        self.assertEqual(d1.common_prefix_length, 16)
        self.assertEqual(d1.backspaces_needed, 0)
        self.assertEqual(d1.text_to_append, "🔥 SUCCESS")

        d2 = engine.compute_diff("System Status: 🚀🔥", "System Status: ⚠️ ERROR")
        self.assertEqual(d2.common_prefix_length, 15)
        self.assertEqual(d2.backspaces_needed, 2)
        self.assertEqual(d2.text_to_append, "⚠️ ERROR")

    def test_tc05_empty_string_oscillation_stress(self):
        """TC05: Rapid toggling between full text and empty string (100 iterations)."""
        full_text = "Vollständiger Text mit Umlauten: äöü ÄÖÜ ß und Symbolen: @ € { }"
        for i in range(100):
            self.disp.dispatch_live_diff(full_text, delay_ms=2.0)
            self.assertEqual(self.host.host_text, full_text)

            self.disp.dispatch_live_diff("", delay_ms=2.0)
            self.assertEqual(self.host.host_text, "")

    def test_tc06_1000_step_randomized_delta_fuzzing(self):
        """TC06: 1,000 randomized ASR mutations: insertions, deletions, replacements, random strings."""
        random.seed(1337)
        words_pool = [
            "der", "die", "das", "und", "in", "den", "von", "zu", "dem", "mit",
            "sich", "des", "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine",
            "Größe", "Übertragungsrate", "Prüfung", "Änderung", "Schlüssel", "Straße",
            "E-Mail", "info@example.com", "100.50 €", "{key: 'value'}", "x² + y³",
            "\\path\\to\\file", "[1, 2, 3]", "Status: OK", "Abschnitt § 5"
        ]

        current_words = ["Startpunkt", "der", "Übertragung"]
        current_text = " ".join(current_words)
        self.disp.dispatch_live_diff(current_text, delay_ms=1.0)
        self.assertEqual(self.host.host_text, current_text)

        for step in range(1000):
            op = random.random()
            if op < 0.35:
                current_words.append(random.choice(words_pool))
            elif op < 0.60 and current_words:
                current_words.pop()
            elif op < 0.80 and current_words:
                idx = random.randint(0, len(current_words) - 1)
                current_words[idx] = random.choice(words_pool)
            elif op < 0.90:
                keep = random.randint(0, len(current_words))
                current_words = current_words[:keep]
            elif op < 0.95:
                current_words = []
            else:
                current_words = [random.choice(words_pool) for _ in range(random.randint(1, 6))]

            new_text = " ".join(current_words)
            self.disp.dispatch_live_diff(new_text, delay_ms=1.0)
            self.assertEqual(
                self.host.host_text,
                new_text,
                f"Desync at step {step}: expected '{new_text}', got '{self.host.host_text}'"
            )

        self.assertEqual(self.host.error_count, 0)


class TestTier5AdversarialDeadKeyStress(unittest.TestCase):
    """Tier 5 Suite 3: Dead Key Stress Testing (^, ´, `, ~) followed by vowels vs consonants"""

    def setUp(self):
        self.host = HidHostSimulator(KeyLayout.GERMAN_QWERTZ)
        self.trans = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.disp = KeystrokeDispatcher(self.host, self.trans, virtual_clock=True)

    def test_tc01_solitary_dead_keys_with_space_injection(self):
        """TC01: Solitary dead keys (^, ´, `, ~) followed by space."""
        for dead_key in ["^", "´", "`", "~"]:
            self.host.reset()
            self.disp.reset_state()
            self.disp.dispatch_burst(dead_key, delay_ms=4.0)
            self.assertEqual(self.host.host_text, dead_key, f"Failed for solitary dead key '{dead_key}'")

    def test_tc02_dead_keys_followed_by_all_vowels(self):
        """TC02: Dead keys followed by vowels (^a, ´e, `i, ~o, etc.) maintain explicit glyphs."""
        vowels = ["a", "e", "i", "o", "u", "A", "E", "I", "O", "U"]
        dead_keys = ["^", "´", "`", "~"]

        for dk in dead_keys:
            for v in vowels:
                test_str = f"{dk}{v}"
                self.host.reset()
                self.disp.reset_state()
                self.disp.dispatch_burst(test_str, delay_ms=4.0)
                self.assertEqual(
                    self.host.host_text,
                    test_str,
                    f"Dead key vowel pairing failed for '{test_str}', got '{self.host.host_text}'"
                )

    def test_tc03_dead_keys_followed_by_consonants(self):
        """TC03: Dead keys followed by consonants (^k, ´b, `s, ~m)."""
        consonants = ["k", "t", "p", "s", "m", "n", "b", "d", "g", "z", "K", "T", "P", "S", "M"]
        dead_keys = ["^", "´", "`", "~"]

        for dk in dead_keys:
            for c in consonants:
                test_str = f"{dk}{c}"
                self.host.reset()
                self.disp.reset_state()
                self.disp.dispatch_burst(test_str, delay_ms=4.0)
                self.assertEqual(
                    self.host.host_text,
                    test_str,
                    f"Dead key consonant pairing failed for '{test_str}'"
                )

    def test_tc04_consecutive_dead_key_clusters(self):
        """TC04: Consecutive clusters of identical and alternating dead keys."""
        clusters = [
            "^^^^^",
            "´´´´´",
            "`````",
            "~~~~~",
            "^´`~^´`~^´`~",
            "^ ^ ´ ´ ` ` ~ ~",
            "^^a ^^b ´´c ``d ~~e"
        ]
        for cluster in clusters:
            self.host.reset()
            self.disp.reset_state()
            self.disp.dispatch_burst(cluster, delay_ms=4.0)
            self.assertEqual(
                self.host.host_text,
                cluster,
                f"Dead key cluster failed for '{cluster}', got '{self.host.host_text}'"
            )

    def test_tc05_markdown_and_code_backtick_blocks(self):
        """TC05: Triple backtick markdown code blocks in German QWERTZ layout."""
        code_block = (
            "```kotlin\n"
            "fun main() {\n"
            "    println(\"Hello Transcriptor HID!\")\n"
            "}\n"
            "```\n"
        )
        self.disp.dispatch_burst(code_block, delay_ms=5.0)
        self.assertEqual(self.host.host_text, code_block)

    def test_tc06_dead_keys_in_delta_diff_streaming(self):
        """TC06: Live delta-diff editing and backspacing inside dead-key containing expressions."""
        stages = [
            "f(x) = x^2",
            "f(x) = x^2 + 2x",
            "f(x) = x^3 + 3x^2 + 3x + 1",
            "Berechnung: ~500 € pro m²",
            "Berechnung: ~750 € pro m² (~15% Rabatt)",
            "Code: `val x = a ^ b`",
            "Code: `val y = a ^ b ^ c`",
            "Fertig!"
        ]
        for stage in stages:
            self.disp.dispatch_live_diff(stage, delay_ms=4.0)
            self.assertEqual(self.host.host_text, stage)


class TestTier5AdversarialTimingAndSaturation(unittest.TestCase):
    """Tier 5 Suite 4: Timing Fuzzing, Queue Saturation & Protocol Limits"""

    def setUp(self):
        self.host = HidHostSimulator(KeyLayout.GERMAN_QWERTZ)
        self.trans = GermanQwertzKeymap(auto_space_dead_keys=True)
        self.disp = KeystrokeDispatcher(self.host, self.trans, virtual_clock=True)

    def test_tc01_timing_delay_fuzzing_spectrum(self):
        """TC01: Validates delayMs values across full range: 0.0ms to 100.0ms."""
        delays = [0.0, 0.5, 1.0, 2.0, 5.0, 8.0, 10.0, 15.0, 20.0, 50.0, 100.0]
        test_phrase = "Testübertragung @ 8ms"

        for d in delays:
            self.host.reset()
            self.disp.reset_state()
            ok = self.disp.dispatch_burst(test_phrase, delay_ms=d)
            self.assertTrue(ok)
            self.assertEqual(self.host.host_text, test_phrase)
            self.assertEqual(self.host.error_count, 0)

    def test_tc02_pacing_interval_verification(self):
        """TC02: Validates that timestamps maintain strict monotonic increase and expected intervals."""
        self.host.reset()
        self.disp.reset_state()
        delay_target = 10.0
        self.disp.dispatch_burst("PacingTest123", delay_ms=delay_target)

        timestamps = self.host.report_timestamps
        self.assertGreaterEqual(len(timestamps), 26)

        for i in range(1, len(timestamps)):
            self.assertGreater(timestamps[i], timestamps[i - 1])

        total_time_ms = (timestamps[-1] - timestamps[0]) * 1000.0
        self.assertGreaterEqual(total_time_ms, 120.0)

    def test_tc03_burst_queue_saturation_50000_chars(self):
        """TC03: 100 consecutive rapid bursts accumulating 50,000+ characters without state drift."""
        chunk = "Schnelle Datenübertragung mit Transcriptor HID über Bluetooth und USB! " * 8
        accumulated = ""

        for burst_idx in range(100):
            self.disp.dispatch_burst(chunk, delay_ms=1.0)
            accumulated += chunk

        self.assertEqual(len(self.host.host_text), len(accumulated))
        self.assertEqual(self.host.host_text, accumulated)
        self.assertEqual(self.host.error_count, 0)
        self.assertGreaterEqual(len(self.host.host_text), 50000)

    def test_tc04_host_disconnect_and_reconnect_resilience(self):
        """TC04: Host disconnects mid-transmission and reconnects with state recovery."""
        self.disp.dispatch_burst("Erster Abschnitt vor Verbindungsabbruch. ", delay_ms=4.0)
        self.assertEqual(self.host.host_text, "Erster Abschnitt vor Verbindungsabbruch. ")

        self.host.set_connected(False)
        ok = self.disp.dispatch_burst("Dieser Text sollte fehlschlagen.", delay_ms=4.0)
        self.assertFalse(ok, "Transmission should fail when host is disconnected")

        self.host.set_connected(True)
        ok = self.disp.dispatch_burst("Zweiter Abschnitt nach Wiederverbindung.", delay_ms=4.0)
        self.assertTrue(ok)
        self.assertIn("Zweiter Abschnitt nach Wiederverbindung.", self.host.host_text)


if __name__ == "__main__":
    unittest.main()
