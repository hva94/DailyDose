# Quickstart: Daily Prompt Posting

## Goal

Implement a shared daily prompt on home that launches a prompt-aware version of the existing `Drop it` flow, generates editable time-based default titles, and stores prompt context on prompt-driven posts while preserving normal posting behavior.

## Implementation Sequence

1. Add prompt assignment and completion domain models.
   - Introduce daily prompt combo/assignment models and any per-user posting-status model needed for visibility checks.
   - Extend the snapshot domain/network models with prompt metadata fields.

2. Extend the Firebase-backed data layer.
   - Add read/write support for shared daily prompt assignment.
   - Add or update user posting metadata used to determine `hasPostedToday`.
   - Extend snapshot publish to persist prompt metadata when the prompt flow is used.

3. Update posting use cases and add-flow orchestration.
   - Extend the add repository/use case inputs beyond `title` plus image so prompt context can be carried through publish.
   - Implement system-managed draft title logic with manual-title takeover.
   - Keep non-prompt posting behavior working for the regular `Drop it` path.

4. Update home feed UI and state.
   - Load prompt assignment plus per-user completion state into home presentation logic.
   - Render the daily prompt card near the top of the feed when eligible.
   - Hide the card after the first successful post of the day.

5. Update feed card rendering for prompt-driven posts.
   - Show prompt text above the title when prompt context exists.
   - Add collapsed/expanded title presentation so full titles remain accessible.

6. Add verification coverage.
   - Add repository/use-case tests for daily prompt assignment, consecutive-day rotation safety, and prompt completion visibility.
   - Add add-flow tests for answer-based title drafts, publish-time timestamp finalization, and manual title override behavior.
   - Add home/feed UI tests for prompt-card visibility and prompt-context rendering.

## Manual Validation Script

1. Launch the app with prompt data available and open the home feed as a user who has not posted today.
2. Confirm the `Daily Prompt` card appears near the top with the correct prompt text and helper copy.
3. Tap the prompt card and verify the add flow opens with the prompt as the screen header.
4. Leave the answer blank, pick an image, and confirm the title field uses one of the active prompt patterns.
5. Publish the post and confirm the saved title includes the actual publish-time label.
6. Reopen the home feed and confirm the daily prompt card is now hidden for that user.
7. On another user account that has not posted yet, confirm the same prompt is shown for the same day.
8. Publish a prompt-driven post with a short answer and confirm the title uses the answer-plus-time style.
9. Manually edit the title before publishing another prompt-driven post and confirm the exact edited title is saved.
10. View prompt-driven posts in the feed and confirm prompt text appears above the title and image.
11. Verify a long title initially appears collapsed and can expand to reveal the full saved text.
12. Disable prompt availability or simulate missing assignment data and confirm the home feed omits the prompt card while normal posting still works.

## Files Likely To Change

- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/User.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/AddSnapshotRepositoryImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/AddSnapshotRepository.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/add/CreateSnapshotUseCase.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/add/CreateSnapshotUseCaseImpl.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddScreen.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddViewModel.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeFeedUiState.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/HomeContent.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt`
- `/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml`

## Suggested Validation Commands

- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest`
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest`

## Verification Notes

- Unit validation completed with:
  `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest`
- Android-test compilation completed with:
  `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest`
- Manual follow-up checks should still confirm the cross-account behavior on real signed-in users, especially that one user posting hides only their own prompt card while the other user keeps seeing the same day assignment.
- The prompt add flow now keeps a system-managed draft title until the user edits it, and feed cards render saved prompt context plus inline title expansion for longer generated titles.
