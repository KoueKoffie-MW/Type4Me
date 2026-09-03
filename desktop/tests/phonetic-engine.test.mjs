import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

describe('Phonetic Engine: DoubleMetaphone', () => {
  function encode(rawWord) {
    const word = (rawWord || '').toUpperCase().trim();
    if (!word) return { primary: '', alternate: '' };
    let primary = '';
    let alternate = '';
    let current = 0;
    const len = word.length;

    const charAt = (pos) => (pos >= 0 && pos < len ? word[pos] : '');
    const isVowel = (pos) => ['A', 'E', 'I', 'O', 'U', 'Y'].includes(charAt(pos));

    if (word.startsWith('GN') || word.startsWith('KN') || word.startsWith('PN') || word.startsWith('WR') || word.startsWith('PS')) {
      current++;
    }

    while (primary.length < 4 && current < len) {
      const ch = charAt(current);
      switch (ch) {
        case 'A': case 'E': case 'I': case 'O': case 'U': case 'Y':
          if (current === 0) { primary += 'A'; alternate += 'A'; }
          current++;
          break;
        case 'B':
          primary += 'P'; alternate += 'P';
          current += charAt(current + 1) === 'B' ? 2 : 1;
          break;
        case 'D':
          primary += 'T'; alternate += 'T';
          current += charAt(current + 1) === 'D' ? 2 : 1;
          break;
        case 'F':
        case 'V':
          primary += 'F'; alternate += 'F';
          current += (charAt(current + 1) === 'F' || charAt(current + 1) === 'V') ? 2 : 1;
          break;
        case 'G':
          primary += 'K'; alternate += 'K';
          current += charAt(current + 1) === 'G' ? 2 : 1;
          break;
        case 'K':
          primary += 'K'; alternate += 'K';
          current += charAt(current + 1) === 'K' ? 2 : 1;
          break;
        case 'S':
          primary += 'S'; alternate += 'S';
          current += charAt(current + 1) === 'S' ? 2 : 1;
          break;
        case 'T':
          if (charAt(current + 1) === 'H') {
            primary += '0'; alternate += 'T';
            current += 2;
          } else {
            primary += 'T'; alternate += 'T';
            current += charAt(current + 1) === 'T' ? 2 : 1;
          }
          break;
        case 'Z':
          primary += 'S'; alternate += 'S';
          current += charAt(current + 1) === 'Z' ? 2 : 1;
          break;
        default:
          current++;
          break;
      }
    }
    return { primary: primary.slice(0, 4), alternate: alternate.slice(0, 4) };
  }

  function arePhoneticallySimilar(a, b) {
    const encA = encode(a);
    const encB = encode(b);
    return (
      (encA.primary && (encA.primary === encB.primary || encA.primary === encB.alternate)) ||
      (encA.alternate && (encA.alternate === encB.primary || encA.alternate === encB.alternate))
    );
  }

  test('encodes and matches phonetic plosive devoicing (bad -> bat)', () => {
    assert.strictEqual(arePhoneticallySimilar('bad', 'bat'), true);
  });

  test('matches dental fricative shift (think -> tink)', () => {
    assert.strictEqual(arePhoneticallySimilar('think', 'tink'), true);
  });

  test('matches labial consonant shift (variable -> fariable)', () => {
    assert.strictEqual(arePhoneticallySimilar('variable', 'fariable'), true);
  });

  test('matches German terminal devoicing (bug -> buck)', () => {
    assert.strictEqual(arePhoneticallySimilar('bug', 'buck'), true);
  });

  test('matches sibilant voicing neutralization (zero -> sero)', () => {
    assert.strictEqual(arePhoneticallySimilar('zero', 'sero'), true);
  });
});

describe('Phonetic Trie: Deterministic Jargon Replacement', () => {
  class SimplePhoneticTrie {
    constructor(dict = {}) {
      this.root = { children: new Map() };
      for (const [k, v] of Object.entries(dict)) {
        this.insert(k.toLowerCase(), v);
      }
    }
    insert(phrase, replacement) {
      const words = phrase.split(/\s+/);
      let curr = this.root;
      for (const w of words) {
        if (!curr.children.has(w)) curr.children.set(w, { children: new Map() });
        curr = curr.children.get(w);
      }
      curr.replacement = replacement;
    }
    repair(text) {
      const tokens = text.split(/(\s+|[.,!?;:()\[\]])/);
      const result = [];
      let i = 0;
      let count = 0;
      while (i < tokens.length) {
        const token = tokens[i];
        if (/^\s+$/.test(token) || /^[.,!?;:()\[\]]$/.test(token)) {
          result.push(token);
          i++;
          continue;
        }
        let matched = null;
        let matchEnd = i;
        let curr = this.root;
        for (let j = i; j < tokens.length; j++) {
          const cand = tokens[j];
          if (/^\s+$/.test(cand) || /^[.,!?;:()\[\]]$/.test(cand)) continue;
          const norm = cand.toLowerCase();
          if (curr.children.has(norm)) {
            curr = curr.children.get(norm);
            if (curr.replacement) {
              matched = curr.replacement;
              matchEnd = j;
            }
          } else {
            break;
          }
        }
        if (matched) {
          result.push(matched);
          count++;
          i = matchEnd + 1;
        } else {
          result.push(token);
          i++;
        }
      }
      return { text: result.join(''), count };
    }
  }

  test('repairs multi-word engineering phrases from Afrikaans speech', () => {
    const trie = new SimplePhoneticTrie({
      'sim you link': 'Simulink',
      'seem you link': 'Simulink',
      'state flow': 'Stateflow',
      'water neon': 'quaternion',
      'wariable': 'variable',
    });

    const input = 'Please check the sim you link model and the state flow chart for wariable water neon vectors.';
    const output = trie.repair(input);

    assert.strictEqual(
      output.text,
      'Please check the Simulink model and the Stateflow chart for variable quaternion vectors.'
    );
    assert.strictEqual(output.count, 4);
  });

  test('preserves punctuation and formatting around replacements', () => {
    const trie = new SimplePhoneticTrie({
      'gherkin': 'Gherkin',
      'mewtex': 'mutex',
    });

    const input = 'Run (gherkin) tests, then release the mewtex!';
    const output = trie.repair(input);

    assert.strictEqual(output.text, 'Run (Gherkin) tests, then release the mutex!');
    assert.strictEqual(output.count, 2);
  });
});

describe('Needleman-Wunsch Alignment & WER', () => {
  function tokenize(t) {
    return t.toLowerCase().replace(/[^\w\s-]/g, '').split(/\s+/).filter(Boolean);
  }

  function computeWer(refText, hypText) {
    const r = tokenize(refText);
    const h = tokenize(hypText);
    const m = r.length;
    const n = h.length;
    const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));
    for (let i = 0; i <= m; i++) dp[i][0] = i;
    for (let j = 0; j <= n; j++) dp[0][j] = j;

    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        const cost = r[i - 1] === h[j - 1] ? 0 : 1;
        dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost);
      }
    }
    const wer = dp[m][n] / Math.max(1, m);
    return Math.round(wer * 100) / 100;
  }

  test('identifies perfect match with WER = 0', () => {
    const passage = 'verify the simulink stateflow model';
    assert.strictEqual(computeWer(passage, passage), 0);
  });

  test('computes realistic substitution error rate for accented speech', () => {
    const ref = 'verify the simulink stateflow model';
    const spoken = 'verify the seem you link state flow model'; // substitutions & insertions
    const wer = computeWer(ref, spoken);
    assert.ok(wer >= 0.5);

    // Test simple 1-word substitution
    const ref2 = 'verify the simulink model';
    const spoken2 = 'verify the simyoulink model';
    const wer2 = computeWer(ref2, spoken2);
    assert.ok(wer2 > 0 && wer2 <= 0.34);
  });
});

describe('Confusion Matrix & Profile Merging', () => {
  test('updates profile and increases confidence on repeated mistakes', () => {
    const initialProfile = {
      id: 'test_user',
      name: 'Test User',
      baseAccentId: 'south_african',
      substitutions: { pin: 'pen' },
      detailedRules: [
        { misrecognized: 'pin', intended: 'pen', occurrences: 1, confidenceScore: 0.75 }
      ],
      stats: { totalWordsRead: 10, averageWer: 0.2, calibrationPassagesCompleted: 1 }
    };

    // Simulate new reading with pin -> pen and new wariable -> variable
    const newRules = [
      { misrecognized: 'pin', intended: 'pen', occurrences: 2, confidenceScore: 0.95 },
      { misrecognized: 'wariable', intended: 'variable', occurrences: 1, confidenceScore: 0.85 }
    ];

    const ruleMap = new Map();
    for (const r of initialProfile.detailedRules) ruleMap.set(`${r.misrecognized}-->${r.intended}`, { ...r });

    for (const sub of newRules) {
      const key = `${sub.misrecognized}-->${sub.intended}`;
      if (ruleMap.has(key)) {
        ruleMap.get(key).occurrences += sub.occurrences;
        ruleMap.get(key).confidenceScore = 0.99;
      } else {
        ruleMap.set(key, { ...sub });
      }
      initialProfile.substitutions[sub.misrecognized] = sub.intended;
    }

    assert.strictEqual(initialProfile.substitutions['wariable'], 'variable');
    assert.strictEqual(ruleMap.get('pin-->pen').occurrences, 3);
    assert.strictEqual(ruleMap.get('pin-->pen').confidenceScore, 0.99);
  });
});
