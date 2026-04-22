# Research: Blur-to-Reveal

## Decision 1: Store reveal state as viewer-owned metadata instead of mutating snapshots

- **Decision**: Persist reveal state in user-scoped records keyed by snapshot identity, rather than adding a shared "revealed" field to the snapshot itself.
- **Rationale**: The feature requires reveal state to be independent for each viewer and permanent for that viewer only. A viewer-owned record satisfies that rule directly and avoids coupling one person's reveal action to the shared post payload.
- **Alternatives considered**:
  - Add a shared reveal flag to each snapshot: rejected because it would make reveal global instead of personal.
  - Keep reveal state only on the device: rejected because the feature is defined per user, not per installation, and local-only state would be lost across reinstalls or account changes.

## Decision 2: Carry reveal visibility in the domain snapshot and retained offline cache

- **Decision**: Extend the feed domain model and local retained-feed storage with a resolved visibility state for the current viewer, backed by cached reveal records.
- **Rationale**: The app already serves feed content from Room-backed retained data. If reveal state lives only in remote reads, offline-retained posts could flash visible or remain permanently blocked when connectivity is unavailable. Caching reveal visibility keeps hidden content hidden until the viewer is known to own or have revealed the post.
- **Alternatives considered**:
  - Resolve reveal state only in Compose at render time from separate asynchronous reads: rejected because it risks visible-state flicker and scattered logic.
  - Recompute reveal state only during full feed refreshes: rejected because a just-revealed post needs immediate local state change without waiting for another refresh cycle.

## Decision 3: Make the first unrevealed image tap a reveal-only action

- **Decision**: Treat tapping an unrevealed image as a dedicated reveal action that removes the blur and overlay in place, with no full-screen viewer or navigation during that tap.
- **Rationale**: The spec explicitly wants reveal to feel intentional and immediate, and it forbids navigation or secondary actions on reveal. The current home feed opens `ExpandedImageViewer` on image tap, so the unrevealed case needs a different path that stops after revealing.
- **Alternatives considered**:
  - Keep the current full-screen viewer on the first tap and reveal inside it: rejected because it turns reveal into navigation instead of an in-feed action.
  - Disable all image taps until a separate reveal button is pressed: rejected because the feature specifically centers reveal on the image area itself.

## Decision 4: Drive all pre-reveal interaction gating from one visibility rule

- **Decision**: Use a single resolved `isInteractiveForViewer` rule derived from ownership or reveal state to control reaction, reply, and share availability.
- **Rationale**: The feed already exposes multiple actions with different enablement rules. One shared gate reduces the risk of mixed states where the image is still hidden but one engagement surface remains active.
- **Alternatives considered**:
  - Gate each control independently with custom rules: rejected because it increases drift risk and makes the UX harder to keep consistent.
  - Hide controls entirely before reveal: rejected because the spec wants the controls visible but clearly disabled.

## Decision 5: Reveal should be optimistic locally and idempotent remotely

- **Decision**: Mark the post revealed in local state as soon as the viewer taps the image, persist the reveal record durably, and treat repeated reveal attempts for the same viewer/post pair as no-op updates.
- **Rationale**: The interaction is intentionally lightweight and should feel immediate. A local-first update supports a smooth animation and avoids repeated reveals causing duplicate writes or visible glitches.
- **Alternatives considered**:
  - Wait for the remote write to succeed before revealing the image: rejected because network latency would undermine the intended “fast and smooth” transition.
  - Allow reveal to toggle back and forth: rejected because the feature defines reveal as permanent for that viewer.

## Decision 6: Preserve existing expanded-image behavior only after a post is already visible

- **Decision**: Keep the current expanded-image viewer available for owners and already-revealed posts, but block it for unrevealed posts.
- **Rationale**: This preserves existing functionality without violating the reveal interaction rules. Owners should see their normal post experience, and revealed posts can continue to support richer viewing without making reveal itself a navigation event.
- **Alternatives considered**:
  - Remove expanded-image viewing for all posts: rejected because it introduces a broader behavior regression unrelated to the new feature.
  - Allow expanded viewing immediately after the same tap that reveals: rejected because reveal would no longer be a single-purpose action.
