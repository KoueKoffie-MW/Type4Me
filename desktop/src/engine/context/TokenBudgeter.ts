/**
 * Type4Me Token Budgeter & Context Ingestion Engine
 * Distills extensive transcript and project files into a high-density,
 * token-capped context header for the LLM.
 */

export interface DistilledContextSummary {
  filePath: string | null;
  totalEntries: number;
  recentTurns: Array<{ role: string; content: string }>;
  activeGoals: string[];
  recentErrors: string[];
  extractedSymbols: string[];
}

export interface BudgetProfile {
  name: 'lean' | 'balanced' | 'deep';
  maxTokens: number;
  maxTurns: number;
  includeErrors: boolean;
  includeSymbols: boolean;
}

export const BUDGET_PROFILES: Record<'lean' | 'balanced' | 'deep', BudgetProfile> = {
  lean: {
    name: 'lean',
    maxTokens: 500,
    maxTurns: 2,
    includeErrors: true,
    includeSymbols: true,
  },
  balanced: {
    name: 'balanced',
    maxTokens: 2000,
    maxTurns: 4,
    includeErrors: true,
    includeSymbols: true,
  },
  deep: {
    name: 'deep',
    maxTokens: 8000,
    maxTurns: 10,
    includeErrors: true,
    includeSymbols: true,
  },
};

export class TokenBudgeter {
  /**
   * Approximates token count using character heuristic (1 token ~= 4 characters)
   */
  public static estimateTokens(text: string): number {
    return Math.ceil((text || '').length / 4);
  }

  /**
   * Formats distilled context into a crisp Markdown prompt section respecting the token budget
   */
  public static formatContextPrompt(
    summary: DistilledContextSummary | null,
    profileType: 'lean' | 'balanced' | 'deep' = 'balanced',
    userQuery: string = ''
  ): string {
    if (!summary || (!summary.recentTurns.length && !summary.recentErrors.length && !summary.extractedSymbols.length)) {
      return '';
    }

    const profile = BUDGET_PROFILES[profileType];
    const lines: string[] = [];
    lines.push('### Active Workstation Context');

    if (summary.filePath) {
      lines.push(`- **Context Source**: \`${summary.filePath}\``);
    }

    // Extracted target files / symbols
    if (profile.includeSymbols && summary.extractedSymbols.length > 0) {
      lines.push(`- **Referenced Files/Symbols**: ${summary.extractedSymbols.map((s) => `\`${s}\``).join(', ')}`);
    }

    // Recent errors
    if (profile.includeErrors && summary.recentErrors.length > 0) {
      lines.push('#### Recent Diagnostics / Errors:');
      for (const err of summary.recentErrors.slice(0, 2)) {
        lines.push(`> ${err.replace(/\n/g, ' ').slice(0, 250)}`);
      }
    }

    // Recent turns
    if (summary.recentTurns.length > 0) {
      lines.push('#### Recent Dialogue Momentum:');
      const turnsToInclude = summary.recentTurns.slice(-profile.maxTurns);
      for (const turn of turnsToInclude) {
        const roleLabel = turn.role === 'user' ? 'User' : 'Agent';
        lines.push(`- **${roleLabel}**: ${turn.content}`);
      }
    }

    let result = lines.join('\n');
    let tokens = this.estimateTokens(result);

    // Prune if exceeding budget
    if (tokens > profile.maxTokens) {
      const allowedChars = profile.maxTokens * 4;
      result = result.slice(0, allowedChars) + '\n... [Context truncated to respect token budget]';
    }

    return result;
  }
}
