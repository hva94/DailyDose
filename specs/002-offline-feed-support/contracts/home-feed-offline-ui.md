# Contract: Home feed offline UI

## Purpose

Define the user-visible contract for the home feed when content is retained locally, connectivity is unavailable, or the app is recovering from offline mode.

## Screen States

### 1. Fresh online content

- Home feed shows the current retained list and may refresh in the background.
- Offline banner is hidden.
- Last refresh timestamp may exist internally but is not emphasized as an offline cue.

### 2. Offline retained content

- Home feed shows retained items in the last successful order.
- A visible offline indicator explains that the user is viewing retained content.
- The screen shows the last successful refresh time.
- Feed cards render from retained local image/avatar assets when available.

### 3. Offline retained content with limited media

- Feed metadata still renders.
- Any missing retained media uses an explicit fallback presentation instead of a blank or broken image container.
- The offline indicator remains visible.

### 4. Offline with no retained content

- The home feed does not show the generic online empty state.
- The screen explains that offline feed content is not available yet because the device has not completed a successful sync for this account.
- A retry action is visible for when connectivity returns.

### 5. Refreshing from offline

- Retained content stays visible while refresh is running.
- A refresh affordance communicates that the app is trying to restore fresh data.
- On success, the offline indicator is removed and the feed reflects the newest sync.

## Card-Level Contract

Each retained card must be able to provide:

- Main image preview, or a limited-availability fallback if the file is missing.
- Owner name.
- Owner avatar when retained, otherwise an avatar fallback.
- Title/caption.
- Relative or absolute time derived from the retained publication timestamp.

## Interaction Contract

- **Retry/refresh**: Always available from offline states; attempts to refresh remote content.
- **Open image**: Available only when the retained main image asset exists.
- **Share image**: Available when the retained main image asset exists locally.
- **Like**: Online-only in v1. When offline, the control is disabled or rejected with a clear message.
- **Delete**: Online-only in v1. When offline, the control is disabled or rejected with a clear message.

## State Inputs Expected From Presentation Logic

The home screen contract expects presentation logic to supply:

| Input | Description |
|-------|-------------|
| `items` | Paged retained feed items ready for rendering. |
| `availabilityMode` | `ONLINE_FRESH`, `OFFLINE_RETAINED`, `OFFLINE_EMPTY`, or `REFRESHING_FROM_OFFLINE`. |
| `lastSuccessfulSyncAt` | Nullable timestamp used for offline freshness messaging. |
| `isRefreshInFlight` | Whether a refresh attempt is running. |
| `offlineActionPolicy` | Whether mutating actions are allowed or blocked. |
| `itemMediaAvailability` | Whether each retained card has full or partial media availability. |

## Failure Handling

- Refresh failures while retained content exists must not clear the retained list.
- Refresh failures without retained content must route to the offline-empty state, not the generic unknown-error state.
- Missing local files must not crash image rendering or the share flow.
