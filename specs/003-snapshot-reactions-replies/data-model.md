# Data Model: Reactions and Replies Upgrade for Snapshots

## Entity: SnapshotAggregate

- **Purpose**: Represents the feed-facing snapshot record used for card rendering, migration fallback, and summary activity display.
- **Primary Key**: `snapshotId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `snapshotId` | String | Stable identifier for the snapshot/post. |
| `ownerUserId` | String | User who created the post. |
| `title` | String | Post title or caption. |
| `photoUrl` | String | Main image URL for the snapshot. |
| `publishedAt` | Long | Post creation timestamp. |
| `reactionCount` | Int | Total number of active reactions on the post. |
| `reactionSummary` | Map<String, Int> | Aggregate totals by emoji for feed display. |
| `replyCount` | Int | Total number of replies on the post. |
| `currentUserReaction` | String? | Current signed-in user's confirmed or pending emoji on this post. |
| `interactionSyncState` | Enum | `CONFIRMED`, `PENDING_REACTION`, `PENDING_REPLY`, or `PENDING_BOTH`. |
| `legacyLikeState` | Enum | `UNMIGRATED`, `MIGRATED_TO_HEART`, or `IGNORED`. |

### Validation Rules

- `reactionCount` must equal the sum of `reactionSummary` values when summary data is present.
- Missing `reactionCount`, `reactionSummary`, or `replyCount` must be normalized to zero/empty fallback values for rendering.
- `currentUserReaction` must contain at most one emoji value.
- `interactionSyncState` must reflect whether local pending actions exist for the snapshot.

### Relationships

- One `SnapshotAggregate` may have zero or many `PostReaction` records.
- One `SnapshotAggregate` may have zero or many `PostReply` records.
- One `SnapshotAggregate` may have zero or many queued `PendingSnapshotAction` records for offline reconciliation.

### State Transitions

1. `LegacyVisible` -> older post loads with missing summary fields and zero/empty fallbacks.
2. `ConfirmedSummary` -> remote summary fields become available or are refreshed.
3. `PendingInteraction` -> local reaction or reply intent is queued while offline.
4. `Reconciled` -> pending actions are confirmed remotely and summary fields refresh.

## Entity: PostReaction

- **Purpose**: Represents one user's active emoji reaction for a specific snapshot.
- **Primary Key**: Composite of `snapshotId` + `userId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `snapshotId` | String | Snapshot this reaction belongs to. |
| `userId` | String | User who owns the reaction. |
| `emoji` | String | Active emoji for the reaction. |
| `createdAt` | Long | When the user's reaction was first created. |
| `updatedAt` | Long | When the user's active emoji was last changed. |
| `isPendingLocalChange` | Boolean | Whether the visible reaction state is waiting on remote confirmation. |

### Validation Rules

- Each `snapshotId` + `userId` pair may have at most one active reaction.
- Selecting the same emoji twice removes the record instead of creating a duplicate.
- Replacing one emoji with another must preserve a single record and update `updatedAt`.

### Relationships

- Many `PostReaction` records belong to one `SnapshotAggregate`.

### State Transitions

1. `Absent` -> user has no active reaction.
2. `ActiveConfirmed` -> user reaction exists and remote summary is aligned.
3. `ActivePending` -> user changed reaction locally while offline and reconciliation is outstanding.
4. `Removed` -> reaction is deleted by same-emoji toggle or explicit removal.

## Entity: PostReply

- **Purpose**: Represents a flat reply attached directly to a snapshot conversation.
- **Primary Key**: `replyId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `replyId` | String | Stable identifier for the reply; local temporary IDs may exist before reconciliation. |
| `snapshotId` | String | Snapshot this reply belongs to. |
| `ownerUserId` | String | User who authored the reply. |
| `ownerDisplayName` | String | Captured display name shown in the reply list. |
| `ownerPhotoUrl` | String? | Captured author photo URL shown in the reply list. |
| `text` | String | Reply body text. |
| `publishedAt` | Long | Reply timestamp. |
| `deliveryState` | Enum | `CONFIRMED`, `PENDING`, or `FAILED`. |

### Validation Rules

- `text` must contain at least one non-whitespace character.
- `text` must not exceed 280 characters.
- Replies are displayed sorted by `publishedAt` ascending, then `replyId` as a tie-breaker.
- Missing `ownerDisplayName` or `ownerPhotoUrl` must fall back to neutral UI placeholders without blocking rendering.

### Relationships

- Many `PostReply` records belong to one `SnapshotAggregate`.

### State Transitions

1. `Draft` -> local composer text before submission.
2. `Pending` -> reply submitted locally and waiting for remote confirmation.
3. `Confirmed` -> reply accepted remotely and summary counts refreshed.
4. `Failed` -> reply could not be confirmed after reconnect and requires visible rollback or retry guidance.

## Entity: PendingSnapshotAction

- **Purpose**: Durable local queue entry for offline reaction/reply intent that has not yet been reconciled with Firebase.
- **Primary Key**: `actionId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `actionId` | String | Stable local identifier for the queued action. |
| `accountId` | String | Signed-in user who owns the pending action queue entry. |
| `snapshotId` | String | Snapshot targeted by the pending action. |
| `actionType` | Enum | `SET_REACTION`, `REMOVE_REACTION`, or `ADD_REPLY`. |
| `payload` | String | Serialized action details such as emoji or reply body. |
| `createdAt` | Long | When the pending action was queued. |
| `lastAttemptAt` | Long? | When reconciliation last tried to send the action. |
| `attemptCount` | Int | Number of send attempts so far. |
| `queueState` | Enum | `QUEUED`, `IN_FLIGHT`, `FAILED`, or `DISCARDED`. |
| `supersedesActionId` | String? | Prior queued reaction action replaced by a later one for the same user/post. |

### Validation Rules

- Pending actions must be isolated per signed-in account.
- For reactions, only the latest queued action for the same account + snapshot should remain active after local compression.
- Failed actions must leave enough metadata to restore the last confirmed server state or explain failure to the user.

### Relationships

- Many `PendingSnapshotAction` records may target one `SnapshotAggregate`.
- `ADD_REPLY` actions may produce a temporary `PostReply` row before reconciliation.

### State Transitions

1. `Queued` -> created offline or while remote confirmation is deferred.
2. `InFlight` -> reconciliation is attempting remote write.
3. `Confirmed` -> local queue entry removed after successful confirmation.
4. `FailedOrDiscarded` -> queue entry marked failed and local optimistic state rolled back or replaced.

## Entity: RetainedSnapshotFeedItem

- **Purpose**: Extends the current Room-cached feed row so offline cards can render interaction summary and pending state.
- **Primary Key**: Composite of `accountId` + `snapshotId`

### Added/Changed Fields Relative to Current Cache

| Field | Type | Description |
|-------|------|-------------|
| `reactionCount` | Int | Last known or locally derived active reaction total. |
| `reactionSummary` | Map<String, Int> | Compact emoji totals used on feed cards. |
| `currentUserReaction` | String? | The current user's confirmed or optimistic emoji. |
| `replyCount` | Int | Last known or locally derived reply total. |
| `hasPendingReaction` | Boolean | Whether the current user's reaction change is awaiting confirmation. |
| `hasPendingReply` | Boolean | Whether one or more local replies are awaiting confirmation. |
| `legacyLikeCount` | Int? | Optional migration-only field used during backfill and then removable. |

### Cross-Entity Rules

- Feed rendering must use retained summary values plus pending markers instead of reaching into reply detail collections.
- When pending actions fail after reconnect, retained snapshot state must roll back to the last confirmed server snapshot.
- Summary fields remain optional during migration, but the local cache should normalize them to stable zero/empty values once loaded.
