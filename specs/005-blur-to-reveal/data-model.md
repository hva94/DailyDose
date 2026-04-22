# Data Model: Blur-to-Reveal

## Entity: ViewerRevealRecord

- **Purpose**: Represents the durable fact that a specific viewer has revealed a specific snapshot.
- **Primary Key**: Composite of `viewerUserId` + `snapshotId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `viewerUserId` | String | Signed-in user who performed the reveal. |
| `snapshotId` | String | Snapshot/post the viewer revealed. |
| `revealedAt` | Long | When the viewer first revealed the snapshot. |
| `syncState` | Enum | Whether the record is confirmed remotely or still pending synchronization. |

### Validation Rules

- There must be at most one reveal record for each `viewerUserId` and `snapshotId` pair.
- `revealedAt` is set only on the first reveal and never reset.
- Duplicate reveal writes must preserve the existing revealed state rather than create a second record.

### Relationships

- One viewer may have many `ViewerRevealRecord` entries.
- One snapshot may have many `ViewerRevealRecord` entries, one per viewer.

### State Transitions

1. `Missing` -> the viewer has not revealed the snapshot.
2. `PendingSync` -> the viewer revealed the snapshot locally and the record still needs remote confirmation.
3. `Confirmed` -> the reveal record is durably stored for that viewer.

## Entity: SnapshotVisibilityState

- **Purpose**: Encapsulates whether a snapshot should render as hidden or visible for the current viewer.
- **Primary Key**: Derived from current viewer + snapshot

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `snapshotId` | String | Snapshot being rendered. |
| `viewerUserId` | String | Current signed-in viewer. |
| `ownerUserId` | String | Snapshot owner used to decide owner visibility. |
| `isOwnerView` | Boolean | Whether the viewer owns the snapshot. |
| `isRevealedForViewer` | Boolean | Whether the viewer has already revealed the snapshot. |
| `visibilityMode` | Enum | `VISIBLE_OWNER`, `VISIBLE_REVEALED`, `HIDDEN_UNREVEALED`, or `HIDDEN_PENDING_STATE`. |
| `isInteractiveForViewer` | Boolean | Whether reactions, replies, and sharing should be enabled. |

### Validation Rules

- `visibilityMode` must resolve to `VISIBLE_OWNER` when `isOwnerView` is true.
- `isInteractiveForViewer` must be true only when `visibilityMode` is `VISIBLE_OWNER` or `VISIBLE_REVEALED`.
- `HIDDEN_PENDING_STATE` must not expose the unblurred image while reveal state is still unresolved.

### Relationships

- Each rendered snapshot for a viewer has exactly one resolved `SnapshotVisibilityState`.

## Entity: SnapshotFeedItem

- **Purpose**: Extends the existing snapshot feed model with the visibility and interaction state required for blur-to-reveal.
- **Primary Key**: `snapshotId`

### Added/Changed Fields Relative to Current Snapshot

| Field | Type | Description |
|-------|------|-------------|
| `visibilityMode` | Enum | Current viewer-facing visibility state for the image. |
| `isRevealedForViewer` | Boolean | Whether the current viewer has revealed the post. |
| `isOwnerView` | Boolean | Whether the current viewer owns the post. |
| `revealSyncState` | Enum | `NONE`, `PENDING`, or `CONFIRMED` for the current viewer's reveal record. |
| `canOpenExpandedImage` | Boolean | Whether tapping the image may open the current expanded-image viewer. |
| `canUseInteractions` | Boolean | Whether reaction, reply, and share controls are active. |

### Validation Rules

- `canOpenExpandedImage` must be false while `visibilityMode` is hidden.
- `canUseInteractions` must be false while `visibilityMode` is hidden.
- `isOwnerView` and `isRevealedForViewer` together must always resolve to a visible state.

### Relationships

- Each `SnapshotFeedItem` is backed by zero or one `ViewerRevealRecord` for the current viewer.

## Entity: CachedRevealState

- **Purpose**: Stores reveal state locally so retained offline feed items can resolve hidden versus visible presentation without waiting for a fresh remote read.
- **Primary Key**: Composite of `accountId` + `snapshotId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `accountId` | String | Current account whose retained data is stored locally. |
| `snapshotId` | String | Snapshot/post the reveal state applies to. |
| `isRevealed` | Boolean | Whether the snapshot is already revealed for this account. |
| `revealedAt` | Long? | Timestamp of the reveal if it has occurred. |
| `syncState` | Enum | Current local synchronization state for the reveal record. |
| `updatedAt` | Long | Last time the cached reveal entry changed. |

### Validation Rules

- `isRevealed = false` records should not be required once the app can infer the default hidden state from absence.
- `revealedAt` must be present when `isRevealed` is true.
- Cache invalidation must be scoped per `accountId` so reveal history does not leak between users.

### Relationships

- One retained feed item may reference zero or one `CachedRevealState` record for the current account.

## Entity: RevealInteractionState

- **Purpose**: Local presentation state that coordinates blur animation, overlay visibility, and one-time tap handling while reveal is in progress.
- **Primary Key**: Local UI state only

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `snapshotId` | String | Snapshot currently being animated. |
| `hasRevealStarted` | Boolean | Whether the reveal transition has been triggered. |
| `isAnimating` | Boolean | Whether blur and overlay fade are still running. |
| `overlayLabel` | String | Copy shown over the unrevealed image, expected to be "Tap to reveal". |
| `tapGuardEnabled` | Boolean | Prevents repeated rapid taps from re-triggering reveal work. |

### Validation Rules

- `tapGuardEnabled` becomes true on the first reveal tap and remains true until local reveal state has been committed.
- `overlayLabel` must be absent once the image is fully visible.
- The animation path must end in a stable visible state rather than toggling back to hidden.

### State Transitions

1. `HiddenIdle` -> blurred image and overlay visible.
2. `RevealTriggered` -> first image tap accepted and duplicate taps suppressed.
3. `AnimatingVisible` -> blur reduces and overlay fades out.
4. `VisibleStable` -> image is fully visible and post interactions become active.
