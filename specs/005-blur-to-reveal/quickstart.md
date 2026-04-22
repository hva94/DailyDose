# Quickstart: Blur-to-Reveal

## Goal

Implement a per-viewer blur-to-reveal interaction in the home feed so other users' images start hidden, reveal in place on tap, and unlock reactions, replies, and sharing only after reveal, while owners always see their own posts normally.

## Implementation Sequence

1. Extend snapshot visibility models.
   - Add reveal-related fields to the domain snapshot model and any supporting enums for visibility and sync state.
   - Keep ownership and visibility resolution in one place so UI and repository logic use the same rule set.

2. Add reveal persistence to the data layer.
   - Introduce remote read/write support for per-user reveal records.
   - Add local cached reveal state for retained feed data and account-scoped cleanup.
   - If needed, extend pending-action infrastructure with a lightweight reveal sync action so offline reveals can reconcile later.

3. Resolve reveal state inside the home repository/use-case path.
   - Merge snapshot data with viewer reveal records before exposing feed items to presentation logic.
   - Make reveal writes idempotent and update local state immediately after a valid tap.
   - Keep owner posts visible without requiring reveal state reads.

4. Update home presentation and card UI.
   - Add hidden versus visible image rendering, overlay copy, and reveal animation to `SnapshotCard`.
   - Gate reaction, reply, and share controls from the resolved visibility state.
   - Prevent unrevealed image taps from opening `ExpandedImageViewer`.

5. Add verification coverage.
   - Add repository tests for reveal persistence, owner visibility, duplicate reveal protection, and cross-account separation.
   - Add UI/presentation tests for hidden-state rendering, enabled/disabled control states, and reveal-trigger behavior.
   - Add validation for retained offline feed behavior so unrevealed posts do not appear clear before reveal state is known.

## Manual Validation Script

1. Launch the app as user A and open the home feed with at least one post owned by user B.
2. Confirm user B's post image is blurred, the `Tap to reveal` overlay is centered on the image, and reaction, reply, and share controls are visible but inactive.
3. Confirm user A's own posts remain fully visible with active controls and no overlay.
4. Tap outside the hidden image area and confirm nothing reveals.
5. Tap the hidden image itself and confirm the blur fades away, the overlay disappears, and no navigation or full-screen viewer opens during that tap.
6. Confirm the reaction, reply, and share controls become active immediately after reveal.
7. Scroll away and back, then relaunch the screen, and confirm the same post remains visible for user A.
8. Sign in as user B or another viewer and confirm the same post still appears blurred until that viewer reveals it independently.
9. If retained offline content is available, switch offline and confirm unrevealed retained posts remain hidden while already revealed posts remain visible for that same account.
10. Rapidly tap an unrevealed image and confirm reveal triggers once without duplicate animations or inconsistent control states.

## Files Likely To Change

- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/PendingSnapshotAction.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/common/extension_functions/SnapshotExtensionFunctions.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/HomeContent.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/ExpandedImageViewer.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt`

## Suggested Validation Commands

- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest`
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest`

## Verification Notes

- Planning artifacts only: no build or test commands were run as part of this planning step.
- Manual follow-up should pay special attention to cross-account reveal isolation and retained offline feed behavior, because those are the two places most likely to cause accidental visibility regressions.

## Implementation Validation Notes

- 2026-04-22: `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest` passed.
- 2026-04-22: `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebugAndroidTest` passed.
- Added instrumented compose coverage for hidden overlay rendering, tap-to-reveal state transition, and disabled-to-enabled interaction controls in `SnapshotRevealTest` and `SnapshotRevealInteractionsTest`.
- Recommended manual follow-up remains the same: verify rapid repeated taps, retained offline resume, and cross-account reveal isolation on a device or emulator because those flows depend on real app state transitions rather than isolated composable or repository seams.
