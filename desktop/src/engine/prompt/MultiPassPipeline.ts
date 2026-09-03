/**
 * Type4Me Multi-Pass Orchestration Pipeline
 * Connects Speech Input -> Fast Phonetic Trie Repair -> Context Ingestion -> Gemini 3.7 Flash Synthesis.
 */

import { PhoneticTrie } from '../accent/PhoneticTrie';
import { UserAccentProfile } from '../accent/ConfusionMatrix';
import { DistilledContextSummary, TokenBudgeter } from '../context/TokenBudgeter';
import { PromptModifierTemplate } from './PromptModifierEngine';
import { GeminiClient, RefinementResponse } from './GeminiClient';

export interface PipelineExecutionResult {
  rawSpeech: string;
  phoneticallyRepaired: string;
  deterministicReplacements: Array<{ from: string; to: string }>;
  contextTokensEstimated: number;
  finalPrompt: string;
  source: string;
  latencyMs: number;
}

export class MultiPassPipeline {
  private phoneticTrie: PhoneticTrie;

  constructor(activeProfile: UserAccentProfile | null = null) {
    this.phoneticTrie = new PhoneticTrie(activeProfile?.substitutions || {});
  }

  public updateProfile(profile: UserAccentProfile | null): void {
    this.phoneticTrie.loadSubstitutions(profile?.substitutions || {});
  }

  public async execute(
    rawSpeech: string,
    template: PromptModifierTemplate,
    profile: UserAccentProfile | null,
    contextSummary: DistilledContextSummary | null,
    budgetType: 'lean' | 'balanced' | 'deep' = 'balanced',
    apiKey?: string
  ): Promise<PipelineExecutionResult> {
    const startTime = Date.now();

    // Pass 1: Fast Deterministic Phonetic Repair
    const trieResult = this.phoneticTrie.repair(rawSpeech);
    const phoneticallyRepaired = trieResult.repairedText;

    // Distill Context Header
    const contextHeader = TokenBudgeter.formatContextPrompt(contextSummary, budgetType, phoneticallyRepaired);
    const contextTokens = TokenBudgeter.estimateTokens(contextHeader);

    // Pass 2: Context-conditioned LLM synthesis with Gemini 3.7 Flash
    const geminiRes: RefinementResponse = await GeminiClient.refinePrompt({
      rawSpeech,
      phoneticallyRepaired,
      template,
      accentProfile: profile,
      contextHeader,
      apiKey,
    });

    return {
      rawSpeech,
      phoneticallyRepaired,
      deterministicReplacements: trieResult.replacements,
      contextTokensEstimated: contextTokens,
      finalPrompt: geminiRes.finalPrompt,
      source: geminiRes.source,
      latencyMs: Date.now() - startTime,
    };
  }
}
