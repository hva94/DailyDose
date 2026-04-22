# Contract: Daily prompt UI

## Purpose

Define the user-visible contract for the shared daily prompt card, prompt-driven `Drop it` flow, generated title behavior, and feed rendering of prompt-driven posts.

## Home Feed Contract

The home feed must be able to render:

- The existing feed list and offline status panels.
- A daily prompt card near the top when a prompt assignment is available and the current user has not posted that day.
- Normal snapshot cards whether or not they include prompt context.

### Daily Prompt Card Rules

1. The card appears only when an active daily prompt assignment exists and the current user has not already posted that day.
2. The card shows the label `Daily Prompt`, the active prompt text, and the helper text `Tap to post your daily dose`.
3. If prompt data is unavailable, the feed omits the card instead of showing a broken placeholder.
4. After the user's first successful post of the day, the card disappears without requiring a full app restart.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `activeDailyPrompt` | Nullable prompt assignment for the shared day. |
| `hasPostedToday` | Whether the current user already completed a first post today. |
| `isPromptLoading` | Whether the home screen is still resolving prompt state. |
| `promptAvailabilityMode` | `AVAILABLE`, `UNAVAILABLE`, or `HIDDEN_BY_COMPLETION`. |

## Prompt-Driven Add Flow Contract

### Expected Behavior

- Tapping the daily prompt card opens the existing add flow in a prompt-aware mode.
- The screen header shows the active prompt text instead of the generic posting header.
- The flow offers an optional short-answer input before image selection/publish.
- Users can still publish without entering an answer.

### Title Field Rules

1. The title field starts in a system-managed state when the prompt flow is active.
2. If the answer is blank, the draft title comes from one random title pattern in the active combo.
3. If the answer is non-blank, the draft title comes from an answer-plus-time format.
4. When the user manually edits the title field, the product stops overwriting their text.
5. If the title is still system-managed at publish time, only the timestamp portion is refreshed to the actual publish time.
6. If the user has manually edited the title, the exact edited text is saved.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `promptHeaderText` | Header shown at the top of the add flow. |
| `answerText` | Current optional answer text. |
| `titleText` | Current editable title field value. |
| `isTitleUserEdited` | Whether the title field is user-managed. |
| `selectedImageUri` | Chosen image for the post. |
| `titlePreviewMode` | `PROMPT_PATTERN`, `ANSWER_FORMAT`, or `MANUAL_EDIT`. |

## Prompt-Driven Feed Card Contract

### Expected Behavior

- Prompt-driven posts show prompt context above the title and image.
- Non-prompt posts continue to render without prompt context.
- Long titles remain readable without truncating stored data.

### Feed Rendering Rules

1. Prompt-driven posts render prompt text, title, and image in that order.
2. Posts without prompt context render the existing feed card layout without an empty prompt gap.
3. Long titles render collapsed by default with a subtle expand affordance.
4. Expanding a long title reveals the full stored title inline.
5. Full-screen image viewing may keep using the saved title, but the feed remains the primary place where prompt context is visible.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `dailyPromptText` | Nullable prompt context attached to the snapshot. |
| `title` | Saved title text for the snapshot. |
| `isTitleExpanded` | Whether the user has expanded the title inline. |
| `canExpandTitle` | Whether the title exceeds the collapsed presentation space. |

## Failure and Availability Contract

- If the daily assignment cannot be loaded, the product hides the prompt card and continues to allow normal posting.
- If the user publishes a normal post first, the product still hides the daily prompt card for the rest of that day.
- If a prompt-driven post is displayed later after the active prompt changes, the saved prompt text continues to render from the post itself.
- If the add flow is opened from a prompt card and the shared assignment expires or changes before publish, the post keeps the prompt context originally loaded into that flow unless the user exits and re-enters.
