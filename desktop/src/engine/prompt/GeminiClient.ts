/**
 * Type4Me Gemini API Client
 * Interfaces with Google's latest Gemini 3.7 Flash model (GA August 2026)
 * to perform accent-aware prompt synthesis and agentic transformation.
 */

import { UserAccentProfile } from '../accent/ConfusionMatrix';
import { PromptModifierTemplate } from './PromptModifierEngine';

export interface RefinementRequest {
  rawSpeech: string;
  phoneticallyRepaired: string;
  template: PromptModifierTemplate;
  accentProfile: UserAccentProfile | null;
  contextHeader: string;
  apiKey?: string;
}

export interface RefinementResponse {
  finalPrompt: string;
  source: 'gemini-3.7-flash' | 'deterministic-offline-fallback';
  latencyMs: number;
}

export class GeminiClient {
  public static async refinePrompt(req: RefinementRequest): Promise<RefinementResponse> {
    const startTime = Date.now();
    const apiKey = req.apiKey?.trim();

    if (!apiKey) {
      // Deterministic offline fallback if no API key is configured
      const fallbackPrompt = this.generateOfflineFallback(req);
      return {
        finalPrompt: fallbackPrompt,
        source: 'deterministic-offline-fallback',
        latencyMs: Date.now() - startTime,
      };
    }

    try {
      const systemPromptParts: string[] = [
        req.template.systemInstruction,
        '',
        '### Accent & Phonetic Repair Guidance:',
        'The speaker has a specific accent with known phonetic shifts.',
      ];

      if (req.accentProfile) {
        systemPromptParts.push(`- Speaker Accent Profile: ${req.accentProfile.name}`);
        const topRules = req.accentProfile.detailedRules.slice(0, 8);
        if (topRules.length > 0) {
          systemPromptParts.push('- Known Phonetic Substitutions:');
          for (const r of topRules) {
            systemPromptParts.push(`  * When ASR writes "${r.misrecognized}", the speaker usually meant "${r.intended}".`);
          }
        }
      }

      if (req.contextHeader) {
        systemPromptParts.push('', req.contextHeader);
      }

      const userMessage = [
        `Raw Transcribed Voice Input: "${req.rawSpeech}"`,
        `Deterministic Phonetic Repair: "${req.phoneticallyRepaired}"`,
        '',
        'Generate the final, optimal prompt directive for the target coding agent according to your instructions.',
      ].join('\n');

      // Call Gemini 3.7 Flash endpoint (GA August 2026)
      // Note: In Gemini 3.6+ and 3.7+, temperature, top_p, and top_k are deprecated and omitted
      const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=${apiKey}`;

      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          system_instruction: {
            parts: [{ text: systemPromptParts.join('\n') }],
          },
          contents: [
            {
              role: 'user',
              parts: [{ text: userMessage }],
            },
          ],
        }),
      });

      if (!response.ok) {
        const errText = await response.text();
        console.warn(`Gemini API returned ${response.status}: ${errText}`);
        return {
          finalPrompt: this.generateOfflineFallback(req),
          source: 'deterministic-offline-fallback',
          latencyMs: Date.now() - startTime,
        };
      }

      const data = await response.json();
      const generatedText = data.candidates?.[0]?.content?.parts?.[0]?.text;

      if (generatedText) {
        return {
          finalPrompt: generatedText.trim(),
          source: 'gemini-3.7-flash',
          latencyMs: Date.now() - startTime,
        };
      }
    } catch (err) {
      console.warn('Gemini API call failed, using deterministic fallback:', err);
    }

    return {
      finalPrompt: this.generateOfflineFallback(req),
      source: 'deterministic-offline-fallback',
      latencyMs: Date.now() - startTime,
    };
  }

  private static generateOfflineFallback(req: RefinementRequest): string {
    const text = req.phoneticallyRepaired || req.rawSpeech;
    if (req.template.id === 'raw_clean') {
      return text;
    }

    const sections: string[] = [];
    sections.push(`## Agent Directive: ${req.template.name}`);
    sections.push('');
    sections.push(`### Objective:`);
    sections.push(text);

    if (req.contextHeader) {
      sections.push('');
      sections.push(req.contextHeader);
    }

    return sections.join('\n');
  }
}
