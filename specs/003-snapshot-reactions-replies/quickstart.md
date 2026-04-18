# Quickstart: Reactions and Replies Upgrade for Snapshots

## Goal

Implement snapshot reactions and replies by replacing the current like flow, adding reply viewing/submission from the feed, preserving a coherent migration from legacy likes, and extending offline support so local interaction intent can survive reconnects.

## Implementation Sequence

1. Evolve the core snapshot interaction models.
   - Extend snapshot/domain/network models with reaction and reply summary fields.
   - Introduce DTOs/models for per-user reactions, replies, and pending interaction state.
   - Replace like-specific repository/use-case names with interaction-oriented names.

2. Update Firebase interaction orchestration.
   - Extend the remote data source to read/write reaction detail, reply detail, and snapshot summary fields.
   - Add migration-aware handling for older posts and legacy `likeList`.
   - Keep the main feed driven by snapshot summary fields.

3. Extend local persistence and offline reconciliation.
   - Update retained feed rows to store reaction summary, reply count, and current-user reaction state.
   - Add Room-backed pending action storage for offline reaction and reply intent.
   - Reconcile pending actions during refresh/reconnect and roll back optimistic state on confirmed failure.

4. Update home feed and reply UI.
   - Replace like controls with reaction controls on feed cards.
   - Add the reply entry point and modal reply sheet from the home feed.
   - Surface loading, empty, pending, and error states for replies and reactions.

5. Add verification coverage.
   - Add repository tests for reaction replacement/removal, reply count updates, migration fallback, and pending queue reconciliation.
   - Add view-model tests for feed summary state and reply sheet state.
   - Add Compose/UI or instrumented coverage for the reaction control, reply sheet, and offline pending behavior.

## Manual Validation Script

1. Launch the app online and open the home feed.
2. Confirm each snapshot card shows reaction and reply affordances instead of the legacy like control.
3. Add a reaction to a post, change it to another emoji, then tap the same emoji again to remove it.
4. Verify feed-level reaction totals and emoji summary stay aligned after each reaction change.
5. Open the reply sheet for a post with no replies and confirm the empty state invites the first reply.
6. Submit a valid reply and confirm it appears at the end of the list and increments the feed reply count.
7. Attempt to submit a blank reply and an over-limit reply and confirm both are blocked with clear feedback.
8. Load older posts that are missing new summary fields and confirm they still render with zero/empty interaction state instead of failing.
9. Put the device offline, add a reaction and a reply, and confirm both appear as pending while retained content remains visible.
10. Restore connectivity, trigger refresh, and confirm pending states clear on success or roll back to the last confirmed state with an error message on failure.

## Files Likely To Change

- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/*`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/*`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/di/InteractionModule.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml`

## Suggested Validation Commands

- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest`
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest`

## Verification Notes

- Existing offline tests were written around read-only behavior, so this feature needs explicit new coverage for queued offline mutations and reconciliation.
- Full device execution for connected Android tests still requires an attached emulator or device.
- Implementation validation completed on April 17, 2026.
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest` passed after updating the repository, remote, view-model, and reply/reaction queue coverage.
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest` passed to confirm the new Compose interaction test and Android-test sources compile cleanly.
- The Compose interaction test currently validates reaction picker selection and reply-sheet composer submission at compile/build time; executing the Android test itself still requires an emulator or connected device.
