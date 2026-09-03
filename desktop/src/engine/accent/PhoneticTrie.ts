/**
 * Type4Me Fast Phonetic Trie
 * High-speed deterministic phrase replacement engine executing in sub-millisecond time
 * before LLM reasoning.
 */

interface TrieNode {
  children: Map<string, TrieNode>;
  replacement?: string;
}

export class PhoneticTrie {
  private root: TrieNode = { children: new Map() };

  constructor(substitutions: Record<string, string> = {}) {
    this.loadSubstitutions(substitutions);
  }

  public loadSubstitutions(substitutions: Record<string, string>): void {
    this.root = { children: new Map() };
    for (const [key, value] of Object.entries(substitutions)) {
      this.insert(key.toLowerCase().trim(), value);
    }
  }

  public insert(phrase: string, replacement: string): void {
    const words = phrase.split(/\s+/);
    let current = this.root;

    for (const word of words) {
      if (!current.children.has(word)) {
        current.children.set(word, { children: new Map() });
      }
      current = current.children.get(word)!;
    }
    current.replacement = replacement;
  }

  /**
   * Replaces known accent / jargon misrecognitions in raw transcribed text
   */
  public repair(rawText: string): { repairedText: string; replacementCount: number; replacements: Array<{ from: string; to: string }> } {
    if (!rawText) return { repairedText: '', replacementCount: 0, replacements: [] };

    // Tokenize preserving original punctuation & casing
    const tokens = rawText.split(/(\s+|[.,!?;:()\[\]])/);
    const resultTokens: string[] = [];
    const replacements: Array<{ from: string; to: string }> = [];

    let i = 0;
    while (i < tokens.length) {
      const token = tokens[i];

      // Skip whitespace or pure punctuation for trie head matching
      if (/^\s+$/.test(token) || /^[.,!?;:()\[\]]$/.test(token)) {
        resultTokens.push(token);
        i++;
        continue;
      }

      // Check for longest multi-word match in Trie
      let matchedNode: TrieNode | null = null;
      let matchEndIndex = i;
      let current = this.root;
      let matchedTokensCount = 0;

      for (let j = i; j < tokens.length; j++) {
        const candidate = tokens[j];
        if (/^\s+$/.test(candidate) || /^[.,!?;:()\[\]]$/.test(candidate)) continue;

        const normalized = candidate.toLowerCase();
        if (current.children.has(normalized)) {
          current = current.children.get(normalized)!;
          if (current.replacement) {
            matchedNode = current;
            matchEndIndex = j;
            matchedTokensCount++;
          }
        } else {
          break;
        }
      }

      if (matchedNode && matchedNode.replacement) {
        // Collect original slice
        const originalPhrase = tokens.slice(i, matchEndIndex + 1).join('').trim();
        replacements.push({ from: originalPhrase, to: matchedNode.replacement });
        resultTokens.push(matchedNode.replacement);
        i = matchEndIndex + 1;
      } else {
        resultTokens.push(token);
        i++;
      }
    }

    return {
      repairedText: resultTokens.join(''),
      replacementCount: replacements.length,
      replacements,
    };
  }
}
