# Research: Offline feed support

## Decision 1: Use Room as the offline source for retained home feed metadata

- **Decision**: Add Room-backed local persistence for retained home feed items, sync metadata, and media references.
- **Rationale**: The current home feed is built from Firebase flows inside a custom paging source, which means the UI has no durable local list to read when the network is unavailable. Room fits Paging well, survives process death, supports per-user queries, and gives explicit control over retention/eviction.
- **Alternatives considered**:
  - Firebase Realtime Database offline persistence: rejected because it does not fully address retained image files, explicit freshness metadata, or clear per-user eviction rules for this feature.
  - SharedPreferences/DataStore only: rejected because the feed is a structured list with ordering, timestamps, and retention behavior that outgrows key-value storage.
  - Raw JSON file snapshots: rejected because updates, filtering, and bounded cleanup would become brittle and harder to test.

## Decision 2: Make the home feed local-first and sync-driven

- **Decision**: Refactor the home repository so home feed reads come from local paging storage while a refresh path fetches the latest Firebase feed and writes it into local storage.
- **Rationale**: A local-first read path keeps online and offline rendering unified. The UI can show retained items immediately, then refresh in place when connectivity allows, which directly matches the spec’s browse-offline and recover-on-reconnect goals.
- **Alternatives considered**:
  - Keep the existing remote `PagingSource` and bolt on a fallback file read: rejected because it would duplicate load-state logic and make offline/online transitions harder to reason about.
  - Let the `ViewModel` merge remote and local streams directly: rejected because repository ownership is the better place to encapsulate synchronization and cache invalidation.

## Decision 3: Retain image assets in app-private files tracked by metadata

- **Decision**: Download and retain main feed images and user avatars into app-private storage, then store metadata references to those files in local persistence.
- **Rationale**: The spec explicitly prioritizes offline image availability. App-private retained files make availability explicit, allow deterministic cleanup, and let the UI choose a guaranteed local source before falling back to a remote URL.
- **Alternatives considered**:
  - Rely only on Coil’s default remote disk cache: rejected because retention and per-account invalidation are less explicit, making it harder to guarantee behavior in a bounded offline feature.
  - Store image blobs directly inside the database: rejected because it inflates local database size and complicates paging/performance.

## Decision 4: Persist sync freshness separately from feed items

- **Decision**: Add a dedicated sync-state record per user to track last successful refresh time, last attempted refresh time, and whether retained feed content exists.
- **Rationale**: The UI needs to distinguish between offline retained content and first-run offline empty states. That state should not be inferred only from item count because item count alone does not explain freshness or recovery.
- **Alternatives considered**:
  - Infer freshness only from the newest feed item timestamp: rejected because publishing time and sync time answer different questions.
  - Keep sync state only in memory: rejected because the user may kill and reopen the app while still offline.

## Decision 5: Bound retention to a recent window of 50 items per signed-in account

- **Decision**: Retain the latest 50 home feed items and their required preview/avatar assets for each signed-in user, evicting older items and orphaned files after successful sync or sign-out.
- **Rationale**: The feature needs predictable storage usage and clear account isolation. A fixed recent window is simple to explain, simple to test, and aligns with the “recent feed” expectation for offline browsing.
- **Alternatives considered**:
  - Unlimited retention: rejected because storage usage becomes unpredictable.
  - Time-based retention only: rejected because a time window alone can still leave too many retained items for heavy users.
  - A much smaller window such as 10 items: rejected because it weakens the value of offline browsing for the home feed.

## Decision 6: Keep offline v1 read-only for server-mutating actions

- **Decision**: Offline home feed v1 supports browsing retained content and sharing retained images, but like/delete actions remain online-only and should be disabled or clearly rejected while offline.
- **Rationale**: The spec assumptions already bound this release to read-only offline behavior. Deferring mutation queues reduces risk while still delivering the main user value quickly.
- **Alternatives considered**:
  - Queue likes immediately in v1: rejected because it introduces reconciliation rules, duplicate protection, and user trust questions that are not necessary for the first offline slice.
  - Hide all actions offline: rejected because sharing a retained image can still work without server mutation and remains useful to the user.
