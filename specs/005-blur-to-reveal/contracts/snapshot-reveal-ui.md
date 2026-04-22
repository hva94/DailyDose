# Contract: Snapshot reveal UI

## Purpose

Define the user-visible contract for blurred snapshot cards, reveal interaction, disabled pre-reveal actions, and owner/already-revealed behavior in the home feed.

## Feed Card Visibility Contract

The home feed must be able to render:

- A fully visible snapshot card for posts owned by the current user.
- A blurred snapshot card for posts from other users that the current viewer has not revealed.
- A fully visible snapshot card for posts from other users that the current viewer has already revealed.

### Visibility Rules

1. Owner posts render their image immediately with no blur treatment and no reveal overlay.
2. Unrevealed non-owner posts render a strong blur over the image area and a centered `Tap to reveal` overlay.
3. Revealed non-owner posts render the same card without the blur or overlay.
4. If reveal state is still being resolved, the product must prefer a hidden presentation over exposing the clear image.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `visibilityMode` | `VISIBLE_OWNER`, `VISIBLE_REVEALED`, `HIDDEN_UNREVEALED`, or `HIDDEN_PENDING_STATE`. |
| `isOwnerView` | Whether the signed-in user owns the snapshot. |
| `isRevealedForViewer` | Whether the current viewer has already revealed this snapshot. |
| `overlayLabel` | Copy shown over the image when hidden. |
| `isRevealAnimating` | Whether the blur removal transition is in progress. |

## Reveal Interaction Contract

### Expected Behavior

- Only the image area triggers reveal for unrevealed posts.
- The first successful tap on an unrevealed image reveals the image in place.
- Reveal does not navigate away from the feed and does not open the full-screen image viewer during that tap.

### Reveal Rules

1. Tapping the image area on an unrevealed non-owner post starts a smooth transition from blurred to visible.
2. The overlay fades out as the blur is removed.
3. Rapid repeated taps during reveal do not trigger duplicate reveal work or reset the animation.
4. Once reveal completes, the card stays visible for that viewer on later renders.
5. Tapping outside the image area does not reveal the post.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `canReveal` | Whether the image tap should trigger reveal instead of normal image viewing. |
| `hasRevealStarted` | Whether the current reveal interaction has already been accepted. |
| `revealSyncState` | `NONE`, `PENDING`, or `CONFIRMED` for the viewer's reveal record. |
| `canOpenExpandedImage` | Whether an image tap is allowed to open the existing expanded image viewer. |

## Interaction Controls Contract

### Expected Behavior

- Reaction, reply, and share controls remain visible on every card.
- Unrevealed non-owner posts show those controls in a disabled appearance.
- Owner and revealed posts show the same controls as active.

### Control Rules

1. Unrevealed non-owner posts do not allow reaction selection, reply sheet opening, or image sharing.
2. Disabled controls use muted styling that clearly distinguishes them from active controls.
3. When reveal completes, the same controls become active without requiring a screen reload.
4. Owner posts keep active controls at all times.

### State Inputs Expected From Presentation Logic

| Input | Description |
|-------|-------------|
| `canUseInteractions` | Shared gate for reaction, reply, and share actions. |
| `disabledControlReason` | Optional reason used only for analytics/debug logging, not user copy. |
| `hasPendingRevealSync` | Whether the reveal is locally visible but still syncing remotely. |

## Expanded Image Contract

### Expected Behavior

- Unrevealed posts do not open the current `ExpandedImageViewer`.
- Already revealed posts and owner posts may continue using the expanded viewer behavior after visibility is established.

### Rules

1. The first tap that reveals a hidden image performs only the reveal action.
2. The expanded image viewer is unavailable while the post remains hidden.
3. Once the post is visible, later image taps may follow the existing expanded-image flow.

## Failure and Availability Contract

- If reveal persistence cannot be confirmed immediately, the product still keeps the image visible locally after a valid reveal tap and retries synchronization in the background.
- If a retained offline post is not yet revealed for the current viewer, it remains blurred until local reveal cache or a new sync confirms visibility.
- If the current viewer changes accounts, reveal state must be resolved independently for the new account before any non-owner images are shown clearly.
