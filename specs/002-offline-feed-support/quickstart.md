# Quickstart: Offline feed support

## Goal

Implement and validate the first offline home feed slice: retained feed browsing with retained images, explicit offline/freshness messaging, and recovery to fresh content after reconnect.

## Implementation Sequence

1. Add local persistence dependencies and wiring.
   - Add Room dependencies to `app/build.gradle`.
   - Create `data/local` types for retained feed items, media assets, sync state, DAOs, and database wiring.
   - Register new providers in DI.

2. Refactor the home feed repository to be local-first.
   - Replace the current remote-only home feed paging path with a Room-backed pager.
   - Add a refresh coordinator that fetches remote feed data, downloads required assets, writes local rows, and evicts overflow/orphaned data.
   - Keep Firebase as the online source of truth.

3. Update domain and presentation behavior.
   - Extend the home feed use case/view model to expose offline availability mode and last successful sync time.
   - Update `HomeScreen` states so offline-retained, offline-empty, refreshing-from-offline, and partial-media states are distinct.
   - Disable or clearly reject like/delete while offline; keep share available only when a retained image file exists.

4. Add verification coverage.
   - Add unit tests for local-first repository refresh behavior and retention cleanup.
   - Add unit tests for view-model state transitions across online, offline-retained, and offline-empty cases.
   - Add at least one UI/instrumented test that asserts offline messaging and retry behavior.

## Manual Validation Script

1. Launch the app online and open the home feed.
2. Confirm the recent feed loads and retained image previews are created.
3. Put the device in airplane mode.
4. Reopen the app and verify the home feed still shows retained items with images.
5. Confirm the screen displays offline messaging and last refresh information.
6. Verify that:
   - Opening an image works only when the retained asset exists.
   - Sharing works only when the retained asset exists locally.
   - Like/delete are blocked or disabled while offline.
7. Clear retained data for the current account or use a fresh account, then open the app offline and confirm the dedicated offline-empty state appears.
8. Restore connectivity, trigger refresh, and confirm the feed updates without restarting the app.

## Files Likely To Change

- `/Users/henryvazquez/StudioProjects/DailyDose/app/build.gradle`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/*`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/paging/*`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/di/HomeRepositoryModule.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/*`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml`

## Suggested Validation Commands

- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest`
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest`

## Verification Notes

- Unit coverage now exercises the local-first repository flow, refresh coordinator, sync-state delegation, and home view-model offline behavior.
- The Android test target compiles with offline-empty and reconnect-status UI checks in `HomeOfflineRecoveryTest`.
- Full device execution for `connectedDebugAndroidTest` still requires an attached emulator or device.
