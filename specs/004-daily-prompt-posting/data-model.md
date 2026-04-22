# Data Model: Daily Prompt Posting

## Entity: DailyPromptCombo

- **Purpose**: Represents one reusable prompt package available to the product for daily assignment.
- **Primary Key**: `comboId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `comboId` | String | Stable identifier for the combo. |
| `promptText` | String | Shared question shown in home and prompt-driven posting. |
| `titlePatterns` | List<String> | Title templates that include the `%time` placeholder for prompt-only title generation. |
| `answerFormats` | List<String> | Answer-based formats such as `{answer} · %time` and `{answer} at %time`. |
| `isEnabled` | Boolean | Whether the combo can be selected for assignment. |

### Validation Rules

- `titlePatterns` must contain at least one pattern with `%time`.
- `answerFormats` must contain only formats that include both `{answer}` and `%time`.
- Disabled combos must be ignored by daily assignment logic.

### Relationships

- One `DailyPromptCombo` may be referenced by many `DailyPromptAssignment` records over time.

## Entity: DailyPromptAssignment

- **Purpose**: Represents the single shared prompt combo selected for a given day.
- **Primary Key**: `dateKey`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `dateKey` | String | Shared calendar-day key used to resolve the active assignment. |
| `comboId` | String | Selected combo for that day. |
| `promptText` | String | Prompt text stored with the assignment so all users see the same wording. |
| `assignedAt` | Long | When the assignment record was created. |
| `previousComboId` | String? | Prior assignment used only to enforce the no-consecutive-repeat rule. |

### Validation Rules

- There must be at most one assignment per `dateKey`.
- `comboId` must reference an enabled combo.
- `comboId` must not equal `previousComboId` when a previous assignment exists.

### Relationships

- One `DailyPromptAssignment` resolves to one `DailyPromptCombo`.
- One `DailyPromptAssignment` may influence many prompt-driven posts created on that day.

### State Transitions

1. `Missing` -> no assignment exists yet for the shared day.
2. `Assigned` -> the day receives a combo and prompt text.
3. `Unavailable` -> assignment cannot be loaded, so the product omits the prompt card and allows normal posting.

## Entity: UserPostingStatus

- **Purpose**: Tracks the current user's most recent successful publish time so home can decide whether the daily prompt is already completed for that day.
- **Primary Key**: `userId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | User whose posting status is being tracked. |
| `lastPostedAt` | Long? | Most recent successful publish timestamp. |
| `lastPromptComboId` | String? | Optional last prompt combo used when the first post came from the prompt flow. |

### Validation Rules

- `lastPostedAt` updates only after a successful publish completes.
- Prompt-card visibility treats a same-day `lastPostedAt` as completed regardless of `lastPromptComboId`.

### Relationships

- One `UserPostingStatus` belongs to one signed-in user.

## Entity: PromptDrivenSnapshot

- **Purpose**: Extends the existing snapshot/post model with prompt context for posts created through the daily prompt flow.
- **Primary Key**: `snapshotId`

### Added/Changed Fields Relative to Current Snapshot

| Field | Type | Description |
|-------|------|-------------|
| `title` | String | Final post title shown in feed and detail surfaces. |
| `publishedAt` | Long | Actual publish timestamp used for the saved post. |
| `dailyPromptId` | String? | Prompt combo identifier when the post came from the daily prompt flow. |
| `dailyPromptText` | String? | Prompt wording attached to the post for later feed display. |
| `titleGenerationMode` | Enum | `PROMPT_PATTERN`, `ANSWER_FORMAT`, or `MANUAL_EDIT`. |

### Validation Rules

- `dailyPromptId` and `dailyPromptText` must either both be present or both be absent.
- `titleGenerationMode` is `PROMPT_PATTERN` only when the answer was blank and the title stayed system-managed.
- `titleGenerationMode` is `ANSWER_FORMAT` only when a non-blank answer influenced the saved title and the field stayed system-managed.
- `titleGenerationMode` is `MANUAL_EDIT` when the user overrides the generated draft before publishing.

### Relationships

- Many `PromptDrivenSnapshot` records may reference the same `DailyPromptCombo`.

## Entity: PromptComposerState

- **Purpose**: Local add-screen state that coordinates prompt display, optional answer input, generated title draft, and manual-title takeover.
- **Primary Key**: Local UI state only

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `activePrompt` | DailyPromptAssignment? | Assignment currently driving the prompt-aware add flow. |
| `answerText` | String | Optional short answer entered by the user. |
| `draftTitle` | String | Current editable title text shown in the field. |
| `isTitleUserEdited` | Boolean | Whether the user has manually taken ownership of the title field. |
| `selectedImageUri` | String? | Chosen image pending publish. |

### Validation Rules

- `draftTitle` may auto-update only while `isTitleUserEdited` is false.
- Publishing requires `selectedImageUri` to exist.
- If `activePrompt` is null, the add flow falls back to the existing non-prompt behavior.

### State Transitions

1. `Idle` -> no prompt-driven state active.
2. `PromptLoaded` -> active prompt available and shown in the add flow.
3. `DraftManaged` -> title draft follows prompt/answer changes.
4. `ManualTitle` -> user edits the title and auto-generation stops.
5. `Published` -> final title and prompt context are committed to the snapshot.
