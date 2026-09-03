# ADR-0002: Context Distillation and Token Budgeting via Sliding Window and Entity Biasing

## Status
**Accepted** (2026-09-03)

## Context
Developer workflows with autonomous coding agents generate large conversational transcripts (e.g., `transcript.jsonl`) that quickly exceed 50,000 to 250,000 tokens. Ingesting these raw logs into every voice prompt refinement call causes:
1. Significant API latency (1.5 to 3 seconds per request).
2. Prohibitive token costs and rapid budget exhaustion.
3. LLM attention dilution ("lost-in-the-middle" effect), which degrades the quality of synthesized prompt directives.

Vector RAG approaches (Option B) introduce heavy embedding overhead and cold-start latency for local log files undergoing active, continuous append operations from the IDE.

## Decision
We implement a **4-Tier Context Distillation & Token Budgeting Engine**:
1. **Sliding Dialogue Window**:
   - Backward traversal extracts only the most recent $N$ dialogue turns (2 to 4 turns), preserving immediate conversational momentum.
2. **Diagnostic Error Trap**:
   - Detects tool errors, stack traces, and test failures in recent transcript steps and formats them into an explicit diagnostic context block.
3. **Semantic Entity & Symbol Matcher**:
   - Performs fast lexical/BM25 token scanning on the user's spoken words to pull relevant file references, AST identifiers, and historical turns containing matching terms.
4. **Enforced Token Budget Allocator**:
   - Provides user-selectable budget profiles (*Lean: 500 tokens*, *Balanced: 2,000 tokens*, *Deep: 8,000 tokens*) that deterministically truncate context headers before API dispatch.

## Consequences
### Positive
*   Predictable, low latency (<10ms context extraction).
*   Guaranteed containment within user-selected token budgets.
*   Focused, high-density context headers that directly inform agent prompt synthesis.

### Negative / Trade-offs
*   Long-term historical conversation beyond the sliding window and entity matcher is omitted to conserve tokens.
