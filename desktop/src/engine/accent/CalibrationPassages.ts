/**
 * Type4Me Calibration Passages
 * Curated phonetically dense diagnostic scripts designed to stress-test
 * vowel raising, final consonant devoicing, dental fricatives, and engineering jargon.
 */

export interface CalibrationPassage {
  id: string;
  title: string;
  targetFeatures: string[];
  estimatedDurationSeconds: number;
  text: string;
}

export const CALIBRATION_PASSAGES: CalibrationPassage[] = [
  {
    id: 'passage_engineering_core',
    title: 'Passage 1: Engineering & Architecture Stress Test',
    targetFeatures: ['Engineering jargon', 'Vowel raising (/ɛ/ -> /ɪ/)', 'Labial fricatives (/v/ vs /w/)'],
    estimatedDurationSeconds: 25,
    text: `Please inspect the Simulink model and ensure the Stateflow chart correctly handles variable quaternion rotation vectors. We must verify that the boolean flag on the desk board does not report a false fault before writing the record to disk.`,
  },
  {
    id: 'passage_fricatives_plosives',
    title: 'Passage 2: Fricatives, Plosives & Dental Transitions',
    targetFeatures: ['Dental fricatives (/θ/, /ð/)', 'Terminal devoicing (/d/ -> /t/, /g/ -> /k/)', 'Consonant clusters'],
    estimatedDurationSeconds: 30,
    text: `Thirty-three engineers thought that the third bug was caused by a dead-lock in the mutex thread. When the second test failed, they sent a patch to prevent the bad tag from corrupting the send buffer in the repo.`,
  },
  {
    id: 'passage_agentic_workflow',
    title: 'Passage 3: Agentic Directives & Gherkin Specifications',
    targetFeatures: ['Gherkin syntax', 'AST & JSONL terminology', 'Imperative cadence'],
    estimatedDurationSeconds: 30,
    text: `Generate a Gherkin test scenario covering all edge cases. Read the active transcript in JSONL format, extract the AST node descriptors, and apply a surgical diff without breaking any invariant constraints or existing comments.`,
  },
];
