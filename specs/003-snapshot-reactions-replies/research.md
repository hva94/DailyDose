# Research: Reactions and Replies Upgrade for Snapshots

## Decision 1: Replace the like-specific interaction seam with a generic post-interactions seam

- **Decision**: Evolve the home/domain repository contract away from `toggleUserLike` toward interaction-focused operations that support reaction selection, reaction removal, reply retrieval, and reply submission.
- **Rationale**: The current code path is centered on a boolean like toggle, which does not map cleanly to one-of-many emoji reactions or reply workflows. A generic post-interactions seam keeps the home feed logic cohesive and avoids bolting new behavior onto misleading names.
- **Alternatives considered**:
  - Keep the current like API and add separate reaction/reply repositories beside it: rejected because it would scatter closely related feed interactions and leave legacy naming in the core path.
  - Implement reactions as a special case of likes with extra metadata: rejected because the behavior rules differ enough that the abstraction would become confusing.

## Decision 2: Use snapshot summary fields as the feed-facing source of truth

- **Decision**: Render feed-level reaction and reply activity from `reactionCount`, `reactionSummary`, and `replyCount` on the main snapshot record, while reading detailed reactions and replies only when needed for interaction or reply viewing.
- **Rationale**: The feature exists partly to make feed rendering faster and more stable. Summary fields let the feed remain lightweight, tolerate older posts with missing child data, and align with the spec’s migration and display requirements.
- **Alternatives considered**:
  - Recompute reaction and reply totals by reading child collections for every feed card: rejected because it would add more read cost and produce more complex loading/error states on the main feed.
  - Store only child collections and derive summaries locally after sync: rejected because it weakens remote consistency and complicates older-post fallback behavior.

## Decision 3: Treat legacy `likeList` as migration-only data and map it to a heart reaction

- **Decision**: Remove the like UI immediately, treat `likeList` as legacy migration input only, and convert migrated likes to a default heart reaction when historical engagement is preserved.
- **Rationale**: The product spec explicitly prefers one coherent feedback concept. Hiding likes from the UI prevents duplicate mental models, while mapping prior likes to hearts preserves intent better than dropping all historical engagement.
- **Alternatives considered**:
  - Show likes and reactions together during rollout: rejected because it creates duplicate concepts and contradicts the product goal.
  - Ignore `likeList` entirely and start all older posts at zero reactions: rejected because it discards prior engagement even when an obvious migration path exists.

## Decision 4: Store replies as flat, chronological records with denormalized author display data

- **Decision**: Keep replies as a single-level chronological list under each snapshot, capturing author display name and photo at creation time alongside the reply text and timestamp.
- **Rationale**: The product explicitly excludes threaded replies and requires graceful fallback when profile data is unavailable. Denormalized author display data keeps old replies readable and predictable even if profile values later change or cannot be fetched.
- **Alternatives considered**:
  - Resolve author name/photo live from the profile record every time a reply is shown: rejected because it introduces more read coupling and weaker offline/fallback behavior.
  - Add nested reply support now: rejected because it adds scope and UI complexity beyond the spec.

## Decision 5: Add a Room-backed pending interaction queue for offline reactions and replies

- **Decision**: Persist locally initiated reaction changes and reply submissions in a pending-action queue keyed per account and snapshot, optimistically update visible UI state, and flush those actions during refresh/reconnect.
- **Rationale**: The new product spec requires offline reactions and replies to appear pending until confirmed. The existing offline slice is read-only, so a durable local queue is the clearest way to survive process death, show pending state, and reconcile failures back to the last confirmed server state.
- **Alternatives considered**:
  - Keep reactions/replies online-only like the earlier like/delete behavior: rejected because it no longer matches the feature spec.
  - Store pending actions only in memory: rejected because queued work would be lost if the app is backgrounded or restarted before reconnect.

## Decision 6: Launch replies from the feed in a modal reply sheet

- **Decision**: Open replies in a modal bottom-sheet style surface from the feed rather than introducing a new full-screen destination.
- **Rationale**: The app currently uses simple bottom navigation without a deeper navigation stack. A reply sheet keeps the user anchored to the post they tapped, minimizes host-navigation changes, and matches the lightweight conversation scope in the spec.
- **Alternatives considered**:
  - Add a full-screen reply destination: rejected because it introduces more navigation plumbing for a smaller interaction surface.
  - Inline-expand replies inside feed cards: rejected because it complicates paging and feed performance, especially with multiple active conversations.

## Decision 7: Extend the retained feed cache with interaction summary and pending-state fields

- **Decision**: Add reaction summary, reaction count, reply count, current-user reaction, and pending-state markers to the locally retained feed item model so the feed can render consistently online and offline.
- **Rationale**: The existing retained feed cache only knows like count and like state. This feature needs summary activity and pending badges to remain visible in offline or reconnect scenarios without requiring a live network read.
- **Alternatives considered**:
  - Keep interaction summary only in remote snapshot reads: rejected because offline feed rendering would lose the activity signals the spec requires.
  - Reuse the existing like fields and reinterpret them for reactions: rejected because reaction summary and reply count need a richer representation than a boolean + integer pair.
