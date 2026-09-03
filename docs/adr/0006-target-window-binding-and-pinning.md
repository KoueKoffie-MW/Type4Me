# ADR-0006: Target Window Binding and 1-Click Process Pinning

## Status
**Accepted** (2026-09-03)

## Context
Developers routinely operate across multi-monitor workstations, referencing documentation, browser consoles, MATLAB/Simulink visualization windows, or PDFs on secondary displays while running an AI coding agent in a primary IDE (e.g. Antigravity IDE, Cursor, VS Code). 

If a floating dictation tool strictly auto-pastes into the immediate previous foreground window, developers must first click back into their IDE before summoning dictation. This introduces unnecessary window juggling and breaks cognitive flow.

## Decision
We implement a **Dynamic Auto-Tracking with 1-Click Process Pinning Strategy**:
1. **Dynamic Default Tracking**:
   - The application automatically tracks and records the handle (`HWND`), title, and process name of the active foreground window immediately before the Floating HUD is summoned.
2. **1-Click Target Pinning (Lock)**:
   - The user can click a "Pin" icon in the HUD header next to the target application (e.g., `Target: Antigravity IDE [PINNED]`).
   - When pinned, the HUD locks its dispatch target to that specific window/process handle.
3. **Cross-Window Dispatch**:
   - When pinned, the user can actively read documentation in a web browser or terminal, summon the HUD (`Ctrl+Shift+Space`), dictate a prompt, and hitting `Enter` will **automatically bring the pinned IDE to the foreground and simulate `Ctrl+V`**, eliminating all manual focus-switching.

## Consequences
### Positive
*   Eliminates focus switching friction when reading external documentation or diagnostics.
*   Enables true ambient prompt authoring across multi-monitor workstations.
*   Retains dynamic fallback for quick pasting into ad-hoc terminal windows when unpinned.

### Negative / Trade-offs
*   If a pinned window is closed by the user, the app must gracefully fall back to dynamic auto-tracking.
