# Stabilize Home Feed Refresh And Mutations

## Summary
- Make Home feel local-first at all times: the Room-backed feed stays visible and interactive while network sync runs in the background.
- Stop forcing a full refresh on normal tab changes. Auto-refresh only on first authenticated load, explicit manual refresh, and app resume after a 10-minute staleness threshold.
- Make add, delete, and like update the local Home cache immediately after success so the feed reflects the user action before any remote re-sync finishes.
- Replace the large refresh status card with a slim top `LinearProgressIndicator` during active refresh. Keep offline messaging only for real offline/empty states.

## Implementation Changes
### Navigation and refresh triggers
- In [HostActivity](/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt), stop bumping `homeRefreshSignal` when simply switching back to `HOME` or after a successful post.
- Keep initial authenticated load refresh.
- Add an app-resume refresh gate: refresh Home only if it is the selected destination and the last successful sync is older than 10 minutes.
- Reuse Home tab reselect only for scroll-to-top behavior, not for network refresh.

### Home UI and state model
- In `HomeViewModel` and `HomeFeedUiState`, separate `isBackgroundRefreshing` from offline availability.
- Do not switch to read-only just because a refresh started. `READ_ONLY_OFFLINE` should apply only when the last known feed state is actually offline/retained or offline/empty.
- In [HomeScreen.kt](/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt), remove the refresh card during active sync and show a top linear loader instead.
- Add pull-to-refresh on Home and route it to the existing manual retry/refresh path.
- Remove the screen-local `optimisticLikes` map once likes are persisted into the local cache, so the UI is driven by one source of truth.

### Local write-through for mutations
- Change the add flow so a successful publish returns the created snapshot payload, not just a success enum.
- Update `CreateSnapshotUseCase` / `AddSnapshotRepository.publishSnapshot` to return `CreateSnapshotResult`, with `Success(createdSnapshot: Snapshot)` plus the existing failure cases.
- Add `HomeRepository.cachePostedSnapshot(snapshot: Snapshot)` and call it before navigating from Add to Home. The new item should be inserted at the top of the local feed by shifting existing `sortOrder` values and inserting the new row with `sortOrder = 0`.
- Keep the success snackbar and immediate navigation to Home, but do not force a full refresh afterward.
- Change `HomeRepository.deleteSnapshot` so on remote success it immediately removes the item from the local cache and cleans up unreferenced retained assets for that snapshot. Do not trigger a full refresh after delete.
- Change `HomeRepository.toggleUserLike` so it updates the local cache immediately, sends the remote mutation, and reverts the local like state if the remote call fails.

### Refresh coordinator safety
- In `HomeFeedRefreshCoordinator`, replace the delete-all-then-reinsert pattern with a single Room transaction that:
  - upserts the retained remote feed rows,
  - deletes rows no longer present in the retained window,
  - upserts asset rows,
  - prunes stale asset records/files,
  - updates sync state last.
- This prevents transient empty/partial feed states while refresh runs and avoids the visible “freeze then jump” behavior.

## Public API / Type Changes
- Replace `PostSnapshotOutcome` return usage in add flow with a new `CreateSnapshotResult`.
- Add `HomeRepository.cachePostedSnapshot(snapshot: Snapshot)`.
- Add `isBackgroundRefreshing` to `HomeFeedUiState`.
- Keep `HomeFeedAvailabilityMode` focused on availability only; do not use refresh-in-flight to synthesize a blocking mode.

## Test Plan
- Unit test: returning to Home from Add/Profile does not call refresh.
- Unit test: app resume triggers refresh only when Home is selected and the last successful sync is older than 10 minutes.
- Unit test: `HomeFeedUiState` stays `FULL_ACCESS` during background refresh when the last known state is online-fresh.
- Unit test: posting returns `CreateSnapshotResult.Success`, caches the created snapshot locally, and the cached item appears at the top without a follow-up refresh.
- Unit test: delete removes the item from the local feed immediately after remote success and does not invoke `refreshSnapshots`.
- Unit test: like updates local count/state immediately and reverts on remote failure.
- Repository test: refresh coordinator transaction updates the retained feed without exposing an empty list between delete/upsert phases.
- UI/instrumented test: Home shows a top linear loader during refresh while like/delete remain enabled in online-fresh state.
- UI/instrumented test: after posting from Add, navigation to Home shows the new item immediately.
- UI/instrumented test: after deleting the newest item, the next valid item remains visible immediately and no stale deleted item reappears.

## Assumptions
- Offline browsing remains read-only when the last known feed state is offline-retained or offline-empty.
- The default staleness threshold for resume-based auto-refresh is 10 minutes.
- Manual refresh is provided via pull-to-refresh plus the existing retry affordance for failure/empty offline states.
