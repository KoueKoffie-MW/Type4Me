/**
 * Type4Me Accent Priors
 * Pre-configured base accent profiles with regional phonetic shift rules
 * and common engineering homophone corrections.
 */

import { UserAccentProfile } from './ConfusionMatrix';

export const DEFAULT_ACCENT_PROFILES: UserAccentProfile[] = [
  {
    id: 'south_african_afrikaans',
    name: 'South African / Afrikaans English',
    baseAccentId: 'south_african',
    createdAt: '2026-09-01T00:00:00.000Z',
    updatedAt: '2026-09-01T00:00:00.000Z',
    substitutions: {
      // Vowel centralization / raising (/ɛ/ -> /ɪ/)
      'pin': 'pen',
      'disk': 'desk',
      'tin': 'ten',
      'min': 'men',
      'sind': 'send',
      'mish': 'mesh',
      
      // Devoicing of final plosives (/d/ -> /t/, /v/ -> /f/)
      'bat': 'bad',
      'sent': 'send',
      'recort': 'record',
      'foult': 'fault',
      'bort': 'board',
      
      // Labialization (/v/ -> /w/ or /f/)
      'wariable': 'variable',
      'wariables': 'variables',
      'walue': 'value',
      'walues': 'values',
      'vector': 'vector',
      
      // Engineering domain jargon
      'seem you link': 'Simulink',
      'sim you link': 'Simulink',
      'simyoulink': 'Simulink',
      'state flow': 'Stateflow',
      'stateflow': 'Stateflow',
      'sim scape': 'Simscape',
      'simscape': 'Simscape',
      'gherkin': 'Gherkin',
      'jerkin': 'Gherkin',
      'scan code': 'scancode',
      'scancodes': 'scancodes',
      'water neon': 'quaternion',
      'water neons': 'quaternions',
      'bowling': 'boolean',
      'bullion': 'boolean',
    },
    detailedRules: [
      { misrecognized: 'pin', intended: 'pen', occurrences: 12, confidenceScore: 0.95, phoneticKeyMatch: true },
      { misrecognized: 'disk', intended: 'desk', occurrences: 10, confidenceScore: 0.95, phoneticKeyMatch: true },
      { misrecognized: 'sim you link', intended: 'Simulink', occurrences: 15, confidenceScore: 0.99, phoneticKeyMatch: true },
      { misrecognized: 'wariable', intended: 'variable', occurrences: 8, confidenceScore: 0.92, phoneticKeyMatch: true },
      { misrecognized: 'water neon', intended: 'quaternion', occurrences: 6, confidenceScore: 0.90, phoneticKeyMatch: true },
    ],
    stats: {
      totalWordsRead: 450,
      averageWer: 0.12,
      calibrationPassagesCompleted: 3,
    },
  },
  {
    id: 'german_english',
    name: 'German English',
    baseAccentId: 'german',
    createdAt: '2026-09-01T00:00:00.000Z',
    updatedAt: '2026-09-01T00:00:00.000Z',
    substitutions: {
      // Dental fricatives (/θ/, /ð/ -> /s/, /z/)
      'sink': 'think',
      'sinking': 'thinking',
      'zis': 'this',
      'zat': 'that',
      'zose': 'those',
      'sank': 'thank',
      'sree': 'three',
      'tree': 'three',
      
      // /w/ vs /v/ confusion
      'went': 'vent',
      'vill': 'will',
      'vould': 'would',
      'wery': 'very',
      
      // Terminal devoicing
      'buck': 'bug',
      'tack': 'tag',
      'hant': 'hand',
      'repot': 'reboot',
      
      // Engineering jargon
      'simulink': 'Simulink',
      'gerkin': 'Gherkin',
      'qwertz': 'QWERTZ',
      'scancode': 'scancode',
    },
    detailedRules: [
      { misrecognized: 'sink', intended: 'think', occurrences: 14, confidenceScore: 0.95, phoneticKeyMatch: true },
      { misrecognized: 'tree', intended: 'three', occurrences: 11, confidenceScore: 0.92, phoneticKeyMatch: true },
      { misrecognized: 'buck', intended: 'bug', occurrences: 9, confidenceScore: 0.90, phoneticKeyMatch: true },
    ],
    stats: {
      totalWordsRead: 380,
      averageWer: 0.14,
      calibrationPassagesCompleted: 2,
    },
  },
  {
    id: 'general_developer_esl',
    name: 'General Developer ESL Priors',
    baseAccentId: 'general_esl',
    createdAt: '2026-09-01T00:00:00.000Z',
    updatedAt: '2026-09-01T00:00:00.000Z',
    substitutions: {
      'paser': 'parser',
      'syntex': 'syntax',
      'mutex': 'mutex',
      'mewtex': 'mutex',
      'repo': 'repository',
      'pr': 'pull request',
      'ast': 'AST',
      'jsonl': 'JSONL',
      'markdown': 'Markdown',
    },
    detailedRules: [
      { misrecognized: 'mewtex', intended: 'mutex', occurrences: 5, confidenceScore: 0.92, phoneticKeyMatch: true },
      { misrecognized: 'jsonl', intended: 'JSONL', occurrences: 7, confidenceScore: 0.95, phoneticKeyMatch: true },
    ],
    stats: {
      totalWordsRead: 200,
      averageWer: 0.10,
      calibrationPassagesCompleted: 1,
    },
  }
];
