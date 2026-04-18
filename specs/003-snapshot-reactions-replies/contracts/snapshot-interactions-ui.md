# Contract: Snapshot interactions UI

## Purpose

Define the user-visible contract for reaction and reply behavior on snapshot cards and in the reply sheet, including offline pending states and migration-safe fallbacks.

## Feed Card Contract

Each snapshot card must be able to render:

- Main image, owner identity, title, and timestamp using existing feed behavior.
- A reaction entry point that shows the current user's selected emoji when present.
- Aggregate reaction display using total count plus compact emoji summary when reactions exist.
- A reply entry point that shows the current reply count and opens the reply sheet.
- Pending-state treatment when a local reaction or reply action is waiting on remote confirmation.

### Feed Card Rules

1. Missing `reactionCount`, `reactionSummary`, or `replyCount` resolves to a calm zero state rather than an error state.
2. Zero reactions do not render empty emoji chips or misleading placeholder counts.
3. Zero replies still render an affordance to open the reply sheet and start the conversation.
4. Legacy likes are never shown as a second feedback control alongside reactions.

## Reaction Picker Contract

### Expected Behavior

- Opening the reaction control presents the available emoji choices for the post.
- Selecting an emoji with no current reaction applies that emoji.
- Selecting the currently active emoji removes the user's reaction.
- Selecting a different emoji replaces the user's current reaction.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `currentUserReaction` | Nullable emoji currently selected by the signed-in user. |
| `reactionCount` | Aggregate active reaction total. |
| `reactionSummary` | Compact emoji totals for display. |
| `isReactionPending` | Whether the user's latest reaction action is still awaiting confirmation. |
| `canInteractOffline` | Whether offline pending is supported for the current session. |

### Failure Handling

- If local reaction intent is queued offline, the card stays interactive but shows a pending indicator.
- If queued reaction reconciliation fails, the UI restores the last confirmed reaction state and shows a clear failure message.

## Reply Sheet Contract

### Sheet Contents

- Snapshot header context so the user knows which post they are replying to.
- Chronological reply list ordered oldest to newest.
- Empty state when no replies exist yet.
- Composer with validation feedback and send action.
- Pending and failed delivery indicators for locally submitted replies.

### Reply Sheet Rules

1. The reply sheet opens from the tapped post without navigating away from the home feed tab.
2. Replies display using captured author name/photo when available, otherwise neutral fallbacks.
3. Blank or whitespace-only replies cannot be submitted.
4. Replies longer than the allowed limit are blocked before submission.
5. Pending replies remain visible in order while awaiting confirmation.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `snapshotId` | Selected post identifier. |
| `snapshotHeader` | Minimal post context shown above the replies. |
| `replies` | Ordered reply items including confirmed and pending local rows. |
| `replyCount` | Current total displayed for the post. |
| `composerText` | Current draft reply text. |
| `composerValidationMessage` | Current validation or submission guidance. |
| `isReplySheetLoading` | Whether reply data is still loading. |
| `isReplySubmissionPending` | Whether at least one local reply is waiting for confirmation. |

## Offline and Reconnect Contract

### When Offline With Retained Content

- Feed cards continue showing the last known reaction and reply counts.
- New reactions and replies can be initiated and appear as pending.
- Pending indicators remain visible until remote confirmation or failure.

### When Reconnect Succeeds

- Pending reaction and reply indicators clear after successful confirmation.
- Feed-level counts and reply-sheet totals reconcile to refreshed summary values.

### When Reconnect Fails

- Failed reaction reconciliation restores the last confirmed reaction state.
- Failed reply reconciliation removes or marks the unsent reply according to the final design, but always explains the failure to the user.
- Existing retained post content remains visible even if the latest interaction update fails.

## Failure Handling

- Reply data load failures must not close the sheet automatically; the user should see an error state with a retry option.
- Reaction summary load failures must not break the card layout or hide the post.
- Missing profile name/photo data must not block reply rendering.
