# Plan: Tap-for-Purple, Long-Press-for-Picker on `ReactionPickerButton`

## Summary

Change the reaction button interaction so a simple tap applies the default purple heart behavior, while a long press opens the emoji dropdown picker.

Lock the tap semantics as:
- If the user has no current reaction, simple tap applies the purple heart.
- If the user already has any reaction selected, simple tap removes that reaction, regardless of which emoji it was.
- Long press keeps the current picker behavior and opens the dropdown menu without applying any default reaction first.

## Key Changes

### Interaction behavior
- Update `ReactionPickerButton` in the Home snapshot card to use `combinedClickable` instead of a plain click-only trigger.
- Map gestures as:
  - `onClick`: default reaction action
  - `onLongClick`: open dropdown menu
- Keep the dropdown menu selection behavior unchanged:
  - selecting an emoji from the menu applies that emoji
  - tapping outside dismisses the menu
- Do not open the dropdown on simple tap anymore.

### Reaction logic wiring
- Introduce a small UI-side resolver for the tap action inside the snapshot card layer:
  - if `currentUserReaction == null`, emit the purple heart emoji
  - otherwise emit `null` to clear the reaction
- Propagate nullable reaction selection through the Home presentation chain where needed:
  - `ReactionPickerButton`
  - `PostInteractionRow`
  - `HomeContent`
  - `HomeScreen`
  - `HomeViewModel`
- Update `HomeViewModel.setSnapshotReaction(...)` to accept `String?`, matching the already-nullable domain/use-case/repository interfaces.
- Reuse the existing purple heart constant/value already used by the reaction system instead of redefining a new default.

### UI and accessibility details
- Keep the existing visual appearance of the reaction summary row unless there is already a pending visual redesign.
- Add long-press semantics labels where practical so accessibility services can still surface the picker intent.
- Preserve current menu anchoring and sizing behavior.

## Public Interface / Type Changes
- `HomeViewModel.setSnapshotReaction(snapshot, emoji)` changes from non-null `String` to nullable `String?`.
- Presentation-layer reaction callbacks should also become nullable where they represent “apply or clear reaction.”
- Domain/use-case/data interfaces do not need behavioral redesign because they already support nullable emoji values.

## Test Plan

### Unit / behavior checks
- `ReactionPickerButton` tap with no current reaction emits purple heart.
- `ReactionPickerButton` tap with purple heart selected emits `null`.
- `ReactionPickerButton` tap with any non-purple reaction selected also emits `null`.
- Long press opens the dropdown menu without changing the current reaction.
- Selecting an emoji from the dropdown still emits that emoji and closes the menu.

### Presentation regression checks
- `HomeViewModel` accepts `null` reaction values and forwards them through `SetSnapshotReactionUseCase`.
- Existing reaction error handling remains unchanged when the use case fails.

### Acceptance scenarios
- From an unreacted snapshot: tap once -> purple heart is applied.
- From a snapshot reacted with purple heart: tap once -> reaction is removed.
- From a snapshot reacted with another emoji: tap once -> reaction is removed.
- From any snapshot: long press -> emoji picker appears; selecting an emoji applies it.

## Assumptions
- The “default” reaction is the existing purple heart shown in the current UI, not the backend legacy heart.
- Simple tap should behave like a binary “quick react / clear react” shortcut, independent of the currently selected emoji.
- No additional visual affordance is required in this change beyond the gesture behavior itself.
