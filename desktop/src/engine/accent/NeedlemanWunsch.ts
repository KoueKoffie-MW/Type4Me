/**
 * Type4Me Alignment Engine: Needleman-Wunsch Dynamic Programming
 * Computes global alignment between reference calibration text and
 * raw ASR transcription to identify systematic phonetic misrecognitions.
 */

import { DoubleMetaphone } from './DoubleMetaphone';

export type AlignmentType = 'MATCH' | 'SUBSTITUTION' | 'INSERTION' | 'DELETION';

export interface AlignedToken {
  type: AlignmentType;
  referenceWord: string | null;
  spokenWord: string | null;
  phoneticMatch: boolean;
  scorePenalty: number;
}

export interface AlignmentReport {
  tokens: AlignedToken[];
  totalReferenceWords: number;
  wordErrorRate: number; // WER = (S + D + I) / N
  substitutions: number;
  deletions: number;
  insertions: number;
  phoneticMatchesCount: number;
  accuracyPercentage: number;
}

export class NeedlemanWunschAligner {
  /**
   * Tokenizes text cleanly (stripping punctuation and normalizing whitespace)
   */
  public static tokenize(text: string): string[] {
    return text
      .toLowerCase()
      .replace(/[^\w\s-]/g, '')
      .split(/\s+/)
      .filter((w) => w.length > 0);
  }

  /**
   * Aligns reference tokens against spoken tokens
   */
  public static align(referenceText: string, spokenText: string): AlignmentReport {
    const ref = this.tokenize(referenceText);
    const hyp = this.tokenize(spokenText);

    const m = ref.length;
    const n = hyp.length;

    // Penalty costs
    const MATCH_SCORE = 2;
    const PHONETIC_MATCH_SCORE = 1;
    const MISMATCH_PENALTY = -2;
    const GAP_PENALTY = -1;

    // DP Matrix: (m + 1) x (n + 1)
    const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));

    for (let i = 0; i <= m; i++) dp[i][0] = i * GAP_PENALTY;
    for (let j = 0; j <= n; j++) dp[0][j] = j * GAP_PENALTY;

    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        const refWord = ref[i - 1];
        const hypWord = hyp[j - 1];

        let matchCost = MISMATCH_PENALTY;
        if (refWord === hypWord) {
          matchCost = MATCH_SCORE;
        } else if (DoubleMetaphone.arePhoneticallySimilar(refWord, hypWord)) {
          matchCost = PHONETIC_MATCH_SCORE;
        }

        const scoreDiag = dp[i - 1][j - 1] + matchCost;
        const scoreUp = dp[i - 1][j] + GAP_PENALTY; // Deletion
        const scoreLeft = dp[i][j - 1] + GAP_PENALTY; // Insertion

        dp[i][j] = Math.max(scoreDiag, scoreUp, scoreLeft);
      }
    }

    // Backtrack to find optimal alignment
    const tokens: AlignedToken[] = [];
    let i = m;
    let j = n;

    while (i > 0 || j > 0) {
      if (i > 0 && j > 0) {
        const refWord = ref[i - 1];
        const hypWord = hyp[j - 1];
        const isExact = refWord === hypWord;
        const isPhonetic = DoubleMetaphone.arePhoneticallySimilar(refWord, hypWord);

        let matchCost = isExact ? MATCH_SCORE : (isPhonetic ? PHONETIC_MATCH_SCORE : MISMATCH_PENALTY);

        if (dp[i][j] === dp[i - 1][j - 1] + matchCost) {
          tokens.unshift({
            type: isExact ? 'MATCH' : 'SUBSTITUTION',
            referenceWord: refWord,
            spokenWord: hypWord,
            phoneticMatch: isPhonetic,
            scorePenalty: isExact ? 0 : 1,
          });
          i--;
          j--;
          continue;
        }
      }

      if (i > 0 && dp[i][j] === dp[i - 1][j] + GAP_PENALTY) {
        tokens.unshift({
          type: 'DELETION',
          referenceWord: ref[i - 1],
          spokenWord: null,
          phoneticMatch: false,
          scorePenalty: 1,
        });
        i--;
      } else {
        tokens.unshift({
          type: 'INSERTION',
          referenceWord: null,
          spokenWord: hyp[j - 1],
          phoneticMatch: false,
          scorePenalty: 1,
        });
        j--;
      }
    }

    // Compute stats
    let s = 0;
    let d = 0;
    let ins = 0;
    let phoneticMatches = 0;

    for (const t of tokens) {
      if (t.type === 'SUBSTITUTION') {
        s++;
        if (t.phoneticMatch) phoneticMatches++;
      } else if (t.type === 'DELETION') {
        d++;
      } else if (t.type === 'INSERTION') {
        ins++;
      }
    }

    const nRef = Math.max(1, ref.length);
    const wer = (s + d + ins) / nRef;
    const accuracy = Math.max(0, 100 * (1 - wer));

    return {
      tokens,
      totalReferenceWords: ref.length,
      wordErrorRate: Math.round(wer * 100) / 100,
      substitutions: s,
      deletions: d,
      insertions: ins,
      phoneticMatchesCount: phoneticMatches,
      accuracyPercentage: Math.round(accuracy * 10) / 10,
    };
  }
}
