# ADR-0001: Hybrid Speech Capture Pipeline (Win+H Caret Focus + Native Push-to-Talk)

## Status
**Accepted** (2026-09-03)

## Context
The application is designed to ingest raw spoken developer instructions, repair accent-induced misrecognitions, and synthesize high-potency agentic prompts on Windows workstations. The user requested leveraging the native Windows Voice Typing shortcut (`Win+H`) directly in the app.

Live research on Windows 11 Voice Typing confirmed that:
1. `Win+H` is an operating system shell-level accessibility bar without a public developer API or programmatic hook.
2. Automating or hooking `Win+H` via UI Automation causes Text Services Framework (TSF) focus loss, causing the dictation bar to crash or drop trailing sentences.
3. `Win+H` relies on Microsoft Cognitive Services cloud streaming by default, creating network dependencies and latency in offline environments.

## Decision
We adopt a **Hybrid Speech Capture Strategy**:
1. **Primary Passive Capture (Win+H Caret Focus)**:
   - When the user summons the Floating HUD via global hotkey (`Ctrl+Shift+Space` or `Alt+\``), the app instantly focuses the input textarea.
   - The user can naturally press `Win+H` and dictate via the native Windows speech bar without any third-party hooking conflicts.
2. **Native Active Capture (Push-to-Talk Fallback)**:
   - The app implements native audio capture via Chromium Web Audio (`navigator.mediaDevices.getUserMedia`) triggered by holding a configurable hotkey (or mouse button).
   - Audio streams via WebSockets directly to **Gemini 3.5 Transcribe Live** (GA August 26, 2026), leveraging its **1,000-term custom vocabulary biasing** to pass project symbols and personal phonetic replacements directly to the speech engine.
   - Offline fallback is routed to a persistent local `whisper.cpp` (Vulkan) instance.

## Consequences
### Positive
*   Zero cognitive friction: retains native Windows `Win+H` muscle memory.
*   Reliability: push-to-talk bypasses Windows DWM overlay glitches and focus-stealing bugs.
*   Vocabulary Biasing: enables upfront phonetic biasing on the speech model layer rather than relying solely on post-hoc error correction.

### Negative / Trade-offs
*   The application requires microphone access permissions in Windows for the native push-to-talk feature.
