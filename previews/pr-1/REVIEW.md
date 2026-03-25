# Codebase Review

## Identified Issues and Inefficiencies

1. **Inconsistent tense keys** – The verb quiz listed “perfeito composto” in the UI but looked up `perfeito composto` in the data. The JSON source, however, stores the key as `perfeito_composto`, so the tense was never loaded.
2. **Translation direction bug** – The vocabulary quiz treated the selected translation direction as a string, making the Portuguese → English option unreachable because both "true" and "false" evaluated to truthy values.
3. **Stringly-typed prompt identifiers** – Both quizzes used brittle string parsing (splitting on `-` or `_`) to recover verb persons or vocabulary terms. This broke if content contained those characters and led to repetitive parsing logic.
4. **State not reset between runs** – Score, timer and progress counters were only zeroed on initial load. Without a full page refresh, any second run would reuse stale values.
5. **Inefficient iteration and bookkeeping** – Prompts were rebuilt by nesting the verbs loop inside the tense loop, performing redundant passes through the data. Version fetching logic was repeated on every page.
6. **Accessibility and feedback gaps** – Feedback messages lacked ARIA live regions, and the UI relied on toggling inline styles instead of semantic `hidden` attributes.
7. **Visual inconsistency** – The previous styling relied on default fonts, inline layout tweaks and duplicated declarations, producing a dated look that was difficult to scale.

## Remediations Implemented

- Normalized tense metadata with explicit keys/labels so that UI copy and data lookups stay aligned.
- Converted translation direction handling to use real booleans and reworked vocabulary prompts to store structured objects.
- Centralised quiz state management (score, timer, progress) with predictable resets and clamped updates.
- Introduced resilient prompt identifiers and lookup tables to avoid fragile string parsing.
- Reduced redundant iterations while generating prompts and extracted shared version loading into `version.js`.
- Adopted `hidden` toggles, polite live regions and improved keyboard shortcuts for better accessibility.
- Delivered a full visual refresh with a cohesive design system, responsive layout and consistent component styling.
