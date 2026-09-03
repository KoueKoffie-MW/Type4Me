/**
 * Type4Me Phonetic Engine: Double Metaphone Algorithm
 * Encodes words into primary and alternate phonetic representations
 * to resolve accent homophones and pronunciation shifts.
 */

export interface MetaphoneResult {
  primary: string;
  alternate: string;
}

export class DoubleMetaphone {
  public static encode(rawWord: string): MetaphoneResult {
    const word = rawWord.toUpperCase().trim();
    if (!word) return { primary: '', alternate: '' };

    let primary = '';
    let alternate = '';
    let current = 0;
    const length = word.length;

    // Helper functions
    const charAt = (pos: number) => (pos >= 0 && pos < length ? word[pos] : '');
    const isVowel = (pos: number) => ['A', 'E', 'I', 'O', 'U', 'Y'].includes(charAt(pos));

    // Handle initial letters
    if (word.startsWith('GN') || word.startsWith('KN') || word.startsWith('PN') || word.startsWith('WR') || word.startsWith('PS')) {
      current++;
    } else if (word.startsWith('X')) {
      primary += 'S';
      alternate += 'S';
      current++;
    }

    while (primary.length < 6 && current < length) {
      const ch = charAt(current);

      switch (ch) {
        case 'A':
        case 'E':
        case 'I':
        case 'O':
        case 'U':
        case 'Y':
          if (current === 0) {
            primary += 'A';
            alternate += 'A';
          }
          current++;
          break;

        case 'B':
          primary += 'P';
          alternate += 'P';
          current += charAt(current + 1) === 'B' ? 2 : 1;
          break;

        case 'C':
          if (charAt(current + 1) === 'H') {
            primary += 'X';
            alternate += 'K';
            current += 2;
          } else if (['I', 'E', 'Y'].includes(charAt(current + 1))) {
            primary += 'S';
            alternate += 'S';
            current += 2;
          } else {
            primary += 'K';
            alternate += 'K';
            current += charAt(current + 1) === 'C' ? 2 : 1;
          }
          break;

        case 'D':
          if (charAt(current + 1) === 'G') {
            primary += 'J';
            alternate += 'J';
            current += 2;
          } else {
            primary += 'T';
            alternate += 'T';
            current += charAt(current + 1) === 'D' ? 2 : 1;
          }
          break;

        case 'F':
          primary += 'F';
          alternate += 'F';
          current += charAt(current + 1) === 'F' ? 2 : 1;
          break;

        case 'G':
          if (charAt(current + 1) === 'H') {
            current += 2;
          } else if (['I', 'E', 'Y'].includes(charAt(current + 1))) {
            primary += 'J';
            alternate += 'K';
            current += 2;
          } else {
            primary += 'K';
            alternate += 'K';
            current += charAt(current + 1) === 'G' ? 2 : 1;
          }
          break;

        case 'H':
          if (isVowel(current + 1) && !isVowel(current - 1)) {
            primary += 'H';
            alternate += 'H';
            current += 2;
          } else {
            current++;
          }
          break;

        case 'J':
          primary += 'J';
          alternate += 'A';
          current += charAt(current + 1) === 'J' ? 2 : 1;
          break;

        case 'K':
          primary += 'K';
          alternate += 'K';
          current += charAt(current + 1) === 'K' ? 2 : 1;
          break;

        case 'L':
          primary += 'L';
          alternate += 'L';
          current += charAt(current + 1) === 'L' ? 2 : 1;
          break;

        case 'M':
          primary += 'M';
          alternate += 'M';
          current += charAt(current + 1) === 'M' ? 2 : 1;
          break;

        case 'N':
          primary += 'N';
          alternate += 'N';
          current += charAt(current + 1) === 'N' ? 2 : 1;
          break;

        case 'P':
          if (charAt(current + 1) === 'H') {
            primary += 'F';
            alternate += 'F';
            current += 2;
          } else {
            primary += 'P';
            alternate += 'P';
            current += charAt(current + 1) === 'P' ? 2 : 1;
          }
          break;

        case 'R':
          primary += 'R';
          alternate += 'R';
          current += charAt(current + 1) === 'R' ? 2 : 1;
          break;

        case 'S':
          if (charAt(current + 1) === 'H') {
            primary += 'X';
            alternate += 'X';
            current += 2;
          } else if (charAt(current + 1) === 'C' && charAt(current + 2) === 'H') {
            primary += 'X';
            alternate += 'SK';
            current += 3;
          } else {
            primary += 'S';
            alternate += 'S';
            current += charAt(current + 1) === 'S' ? 2 : 1;
          }
          break;

        case 'T':
          if (charAt(current + 1) === 'H') {
            primary += '0'; // Theta sound
            alternate += 'T';
            current += 2;
          } else {
            primary += 'T';
            alternate += 'T';
            current += charAt(current + 1) === 'T' ? 2 : 1;
          }
          break;

        case 'V':
          primary += 'F';
          alternate += 'F';
          current += charAt(current + 1) === 'V' ? 2 : 1;
          break;

        case 'W':
          if (isVowel(current + 1)) {
            primary += 'A';
            alternate += 'F';
          }
          current++;
          break;

        case 'Z':
          primary += 'S';
          alternate += 'S';
          current += charAt(current + 1) === 'Z' ? 2 : 1;
          break;

        default:
          current++;
          break;
      }
    }

    return {
      primary: primary.slice(0, 4),
      alternate: alternate.slice(0, 4),
    };
  }

  /**
   * Compares two words phonetically (returns true if either primary or alternate match)
   */
  public static arePhoneticallySimilar(wordA: string, wordB: string): boolean {
    const a = this.encode(wordA);
    const b = this.encode(wordB);
    return (
      (a.primary && (a.primary === b.primary || a.primary === b.alternate)) ||
      (a.alternate && (a.alternate === b.primary || a.alternate === b.alternate))
    );
  }
}
