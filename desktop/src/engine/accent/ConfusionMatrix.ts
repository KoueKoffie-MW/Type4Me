/**
 * Type4Me Phonetic Engine: Confusion Matrix & Substitution Dictionary
 * Analyzes alignment errors to build personal phonetic substitution rules
 * and compute accent distortion patterns.
 */

import { AlignedToken } from './NeedlemanWunsch';

export interface PhoneticSubstitution {
  misrecognized: string;
  intended: string;
  occurrences: number;
  confidenceScore: number;
  phoneticKeyMatch: boolean;
}

export interface UserAccentProfile {
  id: string;
  name: string;
  baseAccentId: 'south_african' | 'german' | 'general_esl' | 'custom';
  createdAt: string;
  updatedAt: string;
  substitutions: Record<string, string>; // misrecognized -> intended
  detailedRules: PhoneticSubstitution[];
  stats: {
    totalWordsRead: number;
    averageWer: number;
    calibrationPassagesCompleted: number;
  };
}

export class ConfusionMatrixBuilder {
  /**
   * Extracts substitutions from alignment tokens
   */
  public static buildSubstitutions(tokens: AlignedToken[]): PhoneticSubstitution[] {
    const pairCounts: Map<string, { misrecognized: string; intended: string; count: number; isPhonetic: boolean }> = new Map();

    for (const token of tokens) {
      if (token.type === 'SUBSTITUTION' && token.referenceWord && token.spokenWord) {
        const key = `${token.spokenWord}-->${token.referenceWord}`;
        const existing = pairCounts.get(key);
        if (existing) {
          existing.count++;
        } else {
          pairCounts.set(key, {
            misrecognized: token.spokenWord,
            intended: token.referenceWord,
            count: 1,
            isPhonetic: token.phoneticMatch,
          });
        }
      }
    }

    const results: PhoneticSubstitution[] = [];
    for (const entry of pairCounts.values()) {
      results.push({
        misrecognized: entry.misrecognized,
        intended: entry.intended,
        occurrences: entry.count,
        confidenceScore: entry.isPhonetic ? 0.95 : 0.75,
        phoneticKeyMatch: entry.isPhonetic,
      });
    }

    // Sort by occurrences descending
    return results.sort((a, b) => b.occurrences - a.occurrences);
  }

  /**
   * Merges newly discovered substitutions into an existing user accent profile
   */
  public static updateProfile(
    profile: UserAccentProfile,
    newSubstitutions: PhoneticSubstitution[],
    wer: number,
    wordCount: number
  ): UserAccentProfile {
    const updatedDict = { ...profile.substitutions };
    const ruleMap = new Map<string, PhoneticSubstitution>();

    // Load existing
    for (const rule of profile.detailedRules) {
      ruleMap.set(`${rule.misrecognized}-->${rule.intended}`, { ...rule });
    }

    // Merge new
    for (const sub of newSubstitutions) {
      const key = `${sub.misrecognized}-->${sub.intended}`;
      if (ruleMap.has(key)) {
        const item = ruleMap.get(key)!;
        item.occurrences += sub.occurrences;
        item.confidenceScore = Math.min(0.99, item.confidenceScore + 0.05);
      } else {
        ruleMap.set(key, { ...sub });
      }
      updatedDict[sub.misrecognized.toLowerCase()] = sub.intended;
    }

    const currentCompleted = profile.stats.calibrationPassagesCompleted || 0;
    const currentWer = profile.stats.averageWer || wer;
    const newAverageWer = Math.round(((currentWer * currentCompleted + wer) / (currentCompleted + 1)) * 100) / 100;

    return {
      ...profile,
      updatedAt: new Date().toISOString(),
      substitutions: updatedDict,
      detailedRules: Array.from(ruleMap.values()).sort((a, b) => b.occurrences - a.occurrences),
      stats: {
        totalWordsRead: (profile.stats.totalWordsRead || 0) + wordCount,
        averageWer: newAverageWer,
        calibrationPassagesCompleted: currentCompleted + 1,
      },
    };
  }
}
