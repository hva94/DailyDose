# Plan: Move Daily Prompt Copy into Android Resources Without Leaking Android into Domain

## Summary

Refactor the daily prompt catalog so user-facing prompt/title copy lives in Android resource XML, while `domain` stays pure Kotlin with no `Context`, `Resources`, or Android-specific APIs.

Use `strings.xml` plus `arrays.xml` as the resource source of truth, and introduce one Android-backed provider in the app/data layer that converts those resources into pure `DailyPromptCombo` models. At the same time, remove the current version-drift risk by persisting the resolved prompt generation strings in the shared daily assignment, so prompt-title generation no longer depends on local app catalog content after assignment creation.

## Key Changes

### 1. Resource-backed catalog, not hardcoded domain copy
- Move prompt questions into `strings.xml`.
- Move grouped title patterns and shared answer formats into `arrays.xml`.
- Keep only stable IDs and pure data models in `domain`; do not keep user-facing English strings in `DailyPromptCatalog`.
- Replace the current hardcoded `DailyPromptCatalog` object with:
  - a pure `DailyPromptCombo` model
  - a pure rotation/selection helper that works from a provided `List<DailyPromptCombo>`
- Do not use fallback hardcoded copy like `"A moment at %time"`; if resource config is invalid, treat the prompt as unavailable and hide the card.

### 2. Android boundary lives below domain
- Add a new Android-backed catalog provider in `data` or app-level config code, injected with `@ApplicationContext`.
- Provider responsibility:
  - load prompt text and pattern arrays from resources
  - map them into pure `DailyPromptCombo` instances
  - validate that required arrays are non-empty and aligned with expected combo IDs
- Shared interface shape:
  - `fun getDailyPromptCombos(): List<DailyPromptCombo>`
  - no Android types in the interface signature
- `RemoteDatabaseServiceImpl` depends on this provider to build the shared daily assignment.
- `PromptTitleGenerator` should stop doing `comboId -> local catalog lookup`; it should generate from the already-resolved assignment payload.

### 3. Freeze generation strings in the shared assignment
- Expand `DailyPromptAssignment` and `DailyPromptAssignmentDTO` to include:
  - `titlePatterns: List<String>`
  - `answerFormats: List<String>`
- Daily assignment creation flow:
  - provider returns pure combos from resources
  - selection helper chooses the combo for the day
  - assignment persists `comboId`, `promptText`, `titlePatterns`, `answerFormats`, `assignedAt`, `previousComboId`
- Result:
  - all prompt-driven generation for that day uses the assignment payload
  - changing resources in a future app version does not change generation behavior for an already-assigned day

### 4. Prompt generation and publish flow updates
- `PromptTitleGenerator` reads `promptText`, `titlePatterns`, and `answerFormats` directly from `DailyPromptAssignment`.
- Keep `PromptComposerState` storing the chosen prompt pattern / answer format for the current compose session so publish-time timestamp refresh uses the same template the user saw.
- No new Android dependency is needed in `PromptTitleGenerator`.
- Keep persisted snapshot behavior the same at publish time:
  - save final `title`
  - save `dailyPromptId`, `dailyPromptText`, and `titleGenerationMode`
- Do not add selected-template fields to snapshots unless implementation discovers a concrete need; the assignment-level freeze is the chosen drift fix.

## Public Interface / Type Changes

- `DailyPromptCatalog` hardcoded singleton is removed or reduced to a pure selection helper with no embedded copy.
- `DailyPromptAssignment`
  - add `titlePatterns: List<String>`
  - add `answerFormats: List<String>`
- `DailyPromptAssignmentDTO`
  - add matching persisted fields for `titlePatterns` and `answerFormats`
- New pure abstraction for catalog loading, implemented in Android-backed `data` code:
  - recommended name: `DailyPromptCatalogProvider`

## Test Plan

- Unit test the Android-backed provider:
  - resources map to exactly 7 combos
  - each combo has the expected prompt text and 2 title patterns
  - shared answer formats are loaded correctly
  - invalid or missing arrays fail closed
- Unit test rotation logic:
  - same inputs return same combo
  - previous combo is excluded when alternatives exist
- Unit test `RemoteDatabaseServiceImpl`:
  - created daily assignment persists prompt text, title patterns, and answer formats from provider output
- Unit test `PromptTitleGenerator`:
  - generation uses assignment payload only
  - no local catalog lookup remains
  - publish-time timestamp refresh preserves the previously selected template
- Regression test:
  - if assignment payload is missing generation arrays, prompt card/add flow degrades to unavailable instead of using hardcoded fallback text

## Assumptions

- “Move to strings.xml” is interpreted as “move to Android resources,” with `arrays.xml` allowed for grouped prompt/title pattern data.
- Additive Firebase Realtime Database schema changes are acceptable for daily assignment records.
- Localization readiness is a goal of this refactor, but no translation rollout is required in the same change.
- Existing prompt-driven post schema remains sufficient once assignment-level generation strings are frozen.
