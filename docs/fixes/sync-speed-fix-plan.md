# Speed Up Home Sync Without Losing Freshness

## Summary
- Keep the current local-first Home behavior, but split sync into a fast metadata check and a selective asset update pass.
- Optimize for warm opens with cached data: show the retained feed immediately, do a lightweight background refresh, and avoid re-downloading images/avatars unless something actually changed.
- Keep the current 10-minute auto-refresh staleness window, but make auto-sync mostly silent; only manual refresh should show an explicit refresh affordance by default.

## Key Changes
### Refresh strategy
- Change Home auto-refresh to a two-phase flow:
  - Phase 1: fetch the latest retained remote feed window and owner profile metadata, compare it to the current retained rows, and detect whether anything changed.
  - Phase 2: only if differences are found, update changed rows and reconcile only the affected assets.
- On warm opens where the retained remote signature matches local data, update sync timestamps/state and exit early without asset downloads or full Room rewrites.
- Keep first authenticated load, manual pull-to-refresh, and resume-after-staleness as the only auto-refresh entry points.

### Metadata and comparison
- Add a remote service helper to resolve owner profile data for a set of owner IDs in one refresh pass, instead of serial `getUserNameOnce` and `getUserPhotoUrlOnce` calls inside the item loop.
- Define the retained comparison signature as:
  - snapshot id
  - title
  - publish date
  - main photo URL
  - owner user id
  - owner display name
  - owner avatar URL
  - like count / liked-by-current-user state
  - retained ordering
- If the signature is unchanged, skip row churn and skip asset downloads.

### Asset reuse and download policy
- Reuse existing retained asset rows/files when the source URL is unchanged and the local file still exists.
- Re-download an asset only when:
  - the source URL changed,
  - the retained file is missing,
  - the retained asset record is missing for a required row.
- Keep asset reconciliation selective:
  - new/changed posts download only their main image,
  - avatar changes update only the affected owners’ avatar assets,
  - removed posts/assets are still pruned after a successful sync.
- If a changed asset download fails but an older local file is still valid for the previous URL, keep the old local file until the new asset is available rather than degrading the UI immediately.

### UI behavior
- Cached Home content should render immediately from Room on open.
- Auto-sync should not show a visible syncing state unless it exceeds a short threshold; default threshold: 1000 ms.
- Manual pull-to-refresh continues to show the explicit pull-to-refresh indicator immediately.
- No top linear loader is required for normal background auto-sync.
- The sync state should still update freshness metadata and offline/online availability correctly even when the refresh exits early.

## Public APIs / Interface Changes
- Add `RemoteDatabaseService.getUsersOnce(userIds: Set<String>): Map<String, User>` or equivalent bounded bulk profile fetch API for refresh use.
- Extend the refresh coordinator/asset storage collaboration so asset retention can evaluate an existing retained asset before downloading.
- No Room schema change is required if the existing `sourceUrl`, `localPath`, and asset identity fields continue to be used for reuse decisions.

## Test Plan
- Unit test: no-change warm refresh updates sync state but does not call asset download for unchanged retained images or avatars.
- Unit test: changed snapshot photo URL downloads only that snapshot asset and leaves unrelated retained assets untouched.
- Unit test: changed owner avatar URL updates only that owner’s avatar asset and reused rows keep their local files.
- Unit test: unchanged URLs with missing local files trigger re-download for only the missing assets.
- Unit test: unchanged warm refresh completes through the fast path and does not rewrite the retained feed rows unnecessarily.
- Unit test: removed remote posts still prune stale rows/assets after successful sync.
- Unit test: Home UI shows cached content immediately and does not surface a visible auto-sync indicator when refresh completes under the threshold.
- Performance acceptance target:
  - warm open with cached data and no retained changes should complete background sync in about 1 second or less on normal connectivity,
  - typical refresh with some changes should feel complete within 1–3 seconds,
  - anything above 5 seconds should be treated as a slow-path case.

## Assumptions
- The 10-minute staleness window for resume-triggered auto-refresh remains unchanged.
- Manual refresh still forces a remote metadata check, but not a full asset re-download of unchanged retained content.
- “Changed” means any visible retained feed content or avatar metadata that would affect what Home renders.
- Auto-sync remains best-effort and subtle; the user’s priority is fast open and stable cached interaction over visibly proving that a sync happened.
