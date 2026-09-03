# ADR-0003: Hierarchical Accent Calibration and Two-Tier Phonetic Repair

## Status
**Accepted** (2026-09-03)

## Context
Standard ASR engines (Whisper, Google Speech, Microsoft Cognitive Services) are predominantly trained on standard American and British English datasets. Non-native speakers and regional English speakers (e.g., South African English / Afrikaans, German English, Dutch English) encounter systematic acoustic distortions:
- Vowel centralization and raising (`/ɛ/ \rightarrow /ɪ/`, e.g., "pen" $\rightarrow$ "pin", "desk" $\rightarrow$ "disk").
- Devoicing of final plosives (`/d/ \rightarrow /t/`, `/v/ \rightarrow /f/`).
- Dental fricative substitutions (`/θ/, /ð/ \rightarrow /s/, /z/`).
- Technical engineering jargon misrecognitions (*Simulink, Stateflow, quaternions, scancodes, mutexes*).

A static dictionary is burdensome to populate manually, while training custom acoustic models from scratch requires excessive audio data and compute.

## Decision
We implement a **Hierarchical Accent Calibration and Two-Tier Phonetic Repair Architecture**:
1. **Base Regional Priors**:
   - The application ships with pre-populated baseline accent profiles (South African / Afrikaans English, German English, General ESL) containing known phoneme shift rules and engineering jargon mappings.
2. **Interactive Calibration Passages & Teleprompter**:
   - The user reads 1 to 3 curated, phonetically dense diagnostic scripts.
   - The system executes **Needleman-Wunsch Dynamic Programming Alignment** between the ground-truth text and spoken ASR transcript, computing Word Error Rate (WER) and phoneme-level substitutions.
3. **Personal Confusion Matrix**:
   - Systematic misrecognitions are extracted into a personal substitution dictionary with occurrence frequencies and confidence weights, saved as an incremental delta profile in local storage.
4. **Two-Tier Execution Pipeline**:
   - **Tier 1 (Deterministic Fast-Path)**: In-memory multi-word **Phonetic Trie** executes instantaneous (<1ms) phrase replacements for high-confidence terms before any network requests.
   - **Tier 2 (Accent-Conditioned LLM)**: Injects the speaker's top learned phonetic shift rules into **Gemini 3.7 Flash** system instructions to acoustically guide prompt reconstruction.

## Consequences
### Positive
*   Immediate out-of-the-box accuracy via regional priors with no initial setup required.
*   Rapid personalization: a single 25-second calibration passage customizes the profile to the user's specific vocal cadence.
*   Sub-millisecond local repair speed for known engineering jargon.
*   Zero manual dictionary entry required.

### Negative / Trade-offs
*   The calibration reader requires user participation (reading aloud) to compute initial error rates.
