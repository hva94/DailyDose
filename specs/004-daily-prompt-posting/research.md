# Research: Daily Prompt Posting

## Decision 1: Use a shared daily assignment record in Firebase Realtime Database

- **Decision**: Resolve the active prompt from a shared daily assignment record keyed by a shared date value, creating the record transactionally when it does not already exist.
- **Rationale**: The feature requires all users to see the same prompt on the same day and forbids consecutive-day repeats. A shared assignment record is the most reliable way to coordinate that behavior in the current Firebase-backed architecture without adding a separate backend service.
- **Alternatives considered**:
  - Compute the prompt locally from the date on each device: rejected because different device time zones or clock drift could cause users to see different prompts.
  - Hard-code the “current prompt” remotely and rotate it manually: rejected because it adds operational dependency for a simple daily rule set.

## Decision 2: Keep the prompt combo catalog app-owned and store only the selected assignment remotely

- **Decision**: Keep the seven prompt/title combos as app-owned configuration, while storing the selected `comboId`, prompt text, and assignment date in the shared daily assignment record.
- **Rationale**: The prompt set is small, stable, and product-owned. Keeping the catalog in the app avoids a second remote content-management surface, while storing the resolved assignment remotely prevents cross-user drift if app versions are temporarily out of sync.
- **Alternatives considered**:
  - Store the full prompt catalog remotely and fetch it on every launch: rejected because it adds more failure modes and content-sync scope than the feature needs.
  - Store only `comboId` remotely and derive all text locally: rejected because future app-version drift could make the shared assignment ambiguous.

## Decision 3: Drive prompt-card visibility from per-user last-post metadata

- **Decision**: Determine whether the current user has “posted today” from a per-user last-post timestamp updated after every successful publish, rather than from a prompt-specific completion record.
- **Rationale**: The spec says the first post of the day hides the prompt card even if the user did not use the prompt. A generic last-post marker satisfies that rule directly and avoids introducing streak-like or prompt-only completion state.
- **Alternatives considered**:
  - Infer completion by scanning loaded feed snapshots for a same-day post from the current user: rejected because paged or offline data might not contain the full answer early enough.
  - Create a dedicated prompt-completion node: rejected because it duplicates information already implied by first-post-of-day behavior.

## Decision 4: Persist prompt context on prompt-driven snapshots, but do not persist the optional answer separately

- **Decision**: Extend prompt-driven snapshots with prompt metadata such as prompt identity and prompt text, while treating the optional answer as input to title generation rather than a separately rendered feed field.
- **Rationale**: The feed must remain understandable after the daily prompt rotates, so prompt context needs to travel with the post. The optional answer only affects the generated title in the current product scope, so persisting it separately would add storage and future-support expectations without user-facing value.
- **Alternatives considered**:
  - Persist no prompt metadata on the snapshot: rejected because older prompt-driven posts would lose their context once the active day changes.
  - Persist prompt answer as another visible post field: rejected because the spec does not call for displaying a separate answer field in the feed.

## Decision 5: Use a system-managed draft title that hands control to the user on manual edit

- **Decision**: In the prompt-driven add flow, generate a draft title from the active prompt and optional answer, keep that draft synchronized while the title remains system-managed, and replace only the timestamp fragment at publish time if the user has not manually taken over the field.
- **Rationale**: The product wants the default title to reflect the actual publish moment while still being editable by the user. A system-managed draft resolves that tension cleanly without blocking users from changing the title before publishing.
- **Alternatives considered**:
  - Generate the title only after the user taps publish: rejected because it would not be meaningfully editable before final confirmation.
  - Generate the title once when entering the screen and never touch it again: rejected because it would drift away from the true publish time.

## Decision 6: Expand long titles inline in the feed instead of truncating saved content

- **Decision**: Render prompt-driven titles in a collapsed state by default with a subtle expand affordance, then reveal the full text inline when requested.
- **Rationale**: The spec explicitly forbids silently truncating saved titles while still asking for a reasonable default visible length. Inline expansion fits the current snapshot-card UI better than a separate dialog or marquee behavior.
- **Alternatives considered**:
  - Hard truncate the title and rely on full-screen image view for the rest: rejected because it hides saved content and does not match the spec.
  - Always show the full title in the feed: rejected because it risks oversized cards and poor scanability for longer titles.
