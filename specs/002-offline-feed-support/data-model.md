# Data Model: Offline feed support

## Entity: OfflineFeedItem

- **Purpose**: Represents one retained home feed entry available for offline browsing.
- **Primary Key**: `snapshotId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `snapshotId` | String | Stable feed item identifier from the remote source. |
| `accountId` | String | Signed-in user who owns this retained cache entry. |
| `ownerUserId` | String | Author of the feed item. |
| `title` | String | Title or caption shown in the card. |
| `publishedAt` | Long | Original publication timestamp. |
| `sortOrder` | Long | Value used to preserve the last known feed ordering. |
| `remotePhotoUrl` | String | Remote image URL for refresh/backfill use. |
| `mainImageAssetId` | String | Reference to the retained main image asset. |
| `ownerDisplayName` | String | Last known author name shown in the feed. |
| `ownerAvatarRemoteUrl` | String | Remote avatar URL used during sync. |
| `ownerAvatarAssetId` | String? | Reference to the retained avatar asset when available. |
| `likeCount` | Int | Last known like count shown offline. |
| `likedByCurrentUser` | Boolean | Last known current-user like state. |
| `availabilityStatus` | Enum | `FULLY_AVAILABLE` or `MEDIA_PARTIAL`. |
| `syncedAt` | Long | Timestamp of the sync session that last updated this row. |

### Validation Rules

- `snapshotId`, `accountId`, and `ownerUserId` must be non-empty.
- `sortOrder` must reflect descending feed order from the last successful sync.
- `availabilityStatus` becomes `MEDIA_PARTIAL` if any required retained asset is missing at render time.
- `mainImageAssetId` must reference a valid `OfflineMediaAsset` row unless the item is explicitly marked as partial.

### Relationships

- Many `OfflineFeedItem` rows belong to one `FeedSyncState` via `accountId`.
- Each `OfflineFeedItem` references one main `OfflineMediaAsset` and optionally one avatar `OfflineMediaAsset`.

### State Transitions

1. `Synced` -> created/updated from remote refresh.
2. `AvailableOffline` -> local files verified.
3. `PartialOffline` -> one or more files missing, but metadata retained.
4. `Evicted` -> removed because of retention cleanup, sign-out, or account switch.

## Entity: OfflineMediaAsset

- **Purpose**: Tracks a retained file needed to render feed content offline.
- **Primary Key**: `assetId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `assetId` | String | Stable local identifier for the retained asset. |
| `accountId` | String | Signed-in user whose cache owns the file. |
| `assetType` | Enum | `SNAPSHOT_IMAGE` or `USER_AVATAR`. |
| `sourceUrl` | String | Remote URL originally used to obtain the asset. |
| `localPath` | String | App-private file path or URI used for offline rendering. |
| `downloadStatus` | Enum | `READY`, `MISSING`, or `FAILED`. |
| `byteSize` | Long | File size used for cleanup and diagnostics. |
| `downloadedAt` | Long | When the retained file was last written successfully. |
| `lastReferencedAt` | Long | Last sync session or access used for cleanup decisions. |

### Validation Rules

- `localPath` must resolve to an app-private location.
- `downloadStatus = READY` requires the file to exist at `localPath`.
- Assets must never be shared across `accountId` boundaries.

### Relationships

- One `OfflineMediaAsset` may be referenced by multiple `OfflineFeedItem` rows when repeated avatars are deduplicated.

### State Transitions

1. `PendingDownload` -> created during sync.
2. `Ready` -> file downloaded and verified.
3. `MissingOrFailed` -> download failed or file later disappeared.
4. `Evicted` -> orphaned file and metadata removed by cleanup.

## Entity: FeedSyncState

- **Purpose**: Captures the retained feed freshness and availability state for one signed-in account.
- **Primary Key**: `accountId`

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `accountId` | String | Signed-in user this sync state belongs to. |
| `lastSuccessfulSyncAt` | Long? | Most recent successful feed refresh timestamp. |
| `lastRefreshAttemptAt` | Long? | Most recent refresh attempt timestamp. |
| `lastRefreshResult` | Enum | `SUCCESS`, `NETWORK_FAILURE`, `AUTH_FAILURE`, `UNKNOWN_FAILURE`, or `NEVER_SYNCED`. |
| `retainedItemCount` | Int | Number of currently retained items for the account. |
| `retentionLimit` | Int | Configured maximum retained item count, initially 50. |
| `hasRetainedContent` | Boolean | Whether offline feed content exists for this account. |

### Validation Rules

- `retentionLimit` must be positive.
- `hasRetainedContent` must match whether retained items exist for `accountId`.
- `lastSuccessfulSyncAt` remains null until the first successful sync completes.

### Derived View State

The home screen derives user-facing state from `FeedSyncState` plus current connectivity:

- `ONLINE_FRESH`: remote refresh succeeded for the current session.
- `OFFLINE_RETAINED`: no connectivity or refresh failure, but retained items exist.
- `OFFLINE_EMPTY`: no retained items exist yet for the account.
- `REFRESHING_FROM_OFFLINE`: retained items are visible while a refresh is in progress.

## Cross-Entity Rules

- All retained feed rows and retained asset rows must be deleted on account sign-out or account switch.
- Successful sync replaces the retained ordering with the newest remote ordering.
- Eviction removes orphaned `OfflineMediaAsset` rows and files after feed-item cleanup.
- If the main image file is missing, the feed item remains renderable as metadata-only content with explicit limited-availability UI.
