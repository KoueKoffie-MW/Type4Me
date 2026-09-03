# ADR-0004: Dual-Key Prompt Dispatch and Safety Review Mechanism

## Status
**Accepted** (2026-09-03)

## Context
Autonomous coding agents execute multi-step tools (file edits, bash commands, tests) based directly on user prompts. While developers require maximum velocity when dictating routine directives, blindly auto-pasting raw speech into an agent input box without validation carries the risk of executing hallucinated or misrecognized commands. Conversely, requiring mandatory manual review modals for every single prompt creates cognitive fatigue.

## Decision
We implement a **Dual-Key Prompt Dispatch and Safety Review Mechanism**:
1. **Rapid Execution Flow (`Enter`)**:
   - The pipeline executes speech recognition, deterministic Trie repair, context distillation, and Gemini 3.7 Flash prompt synthesis.
   - The Floating HUD automatically hides itself, restores foreground focus to the recorded target window handle (`HWND`), and simulates `Ctrl+V` to inject the prompt directly into the agent input box in under 500ms.
2. **Inspection & Safety Flow (`Ctrl+Enter` or Expand)**:
   - Halts auto-paste and displays the **3-Way Split Diff View**:
     - *Stage 1*: Raw transcribed speech.
     - *Stage 2*: Phonetically repaired text (highlighting accent substitutions).
     - *Stage 3*: Synthesized agent directive (with live markdown editing).
   - The user can review or modify the prompt and dispatch when satisfied.
3. **Configurable Default Mode**:
   - The user can toggle their default primary action between "Always Auto-Paste" and "Always Inspect Before Paste" in Settings.

## Consequences
### Positive
*   Zero-friction velocity for routine developer commands.
*   Zero-risk verification for complex architectural refactors or critical test generation.
*   Full visibility into the exact phonetic substitutions made by the engine.

### Negative / Trade-offs
*   Users must memorize the distinction between `Enter` (instant dispatch) and `Ctrl+Enter` (inspect).
