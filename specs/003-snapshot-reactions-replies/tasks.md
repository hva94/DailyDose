# Tasks: Reactions and Replies Upgrade for Snapshots

**Input**: Design documents from `/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/`  
**Prerequisites**: [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/plan.md), [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/spec.md), [research.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/research.md), [data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/data-model.md), [snapshot-interactions-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/contracts/snapshot-interactions-ui.md), [quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/quickstart.md)

**Tests**: Include targeted unit and instrumented coverage because the plan and quickstart explicitly require repository, UI-state, and offline reconciliation verification.  
**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently without deleting the existing spec or plan artifacts.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`, `[US6]`)
- Every task includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared constants and new interaction entry points that the rest of the feature will build on.

- [X] T001 Add snapshot interaction path constants and composer limits in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/common/Constants.kt
- [X] T002 Create reaction/reply DTO and domain model entry points in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotReactionDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotReplyDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/SnapshotReply.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/PendingSnapshotAction.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared data, repository, and synchronization layer required by all reaction and reply stories.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T003 [P] Extend snapshot aggregate models with reaction and reply summary fields in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt
- [X] T004 [P] Add pending interaction Room storage in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/PendingSnapshotActionEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/PendingSnapshotActionDao.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt
- [X] T005 [P] Extend retained feed persistence for reaction summaries and reply counts in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemDao.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedOfflineTypeConverters.kt
- [X] T006 Replace like-specific repository and use-case contracts with interaction-focused APIs in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/SetSnapshotReactionUseCase.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/SetSnapshotReactionUseCaseImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotRepliesUseCase.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotRepliesUseCaseImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/AddSnapshotReplyUseCase.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/AddSnapshotReplyUseCaseImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/di/InteractionModule.kt
- [X] T007 Implement reaction/reply remote contracts plus legacy-like migration helpers in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt
- [X] T008 Implement shared interaction mapping and pending reconciliation helpers in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/SnapshotInteractionSyncCoordinator.kt

**Checkpoint**: Shared snapshot interaction data structures, local queueing, and remote contracts are ready for story work.

---

## Phase 3: User Story 1 - React to a post with an emoji (Priority: P1) 🎯 MVP

**Goal**: Let a signed-in user add their first emoji reaction to any visible snapshot post and see that choice reflected immediately.

**Independent Test**: Open the feed, add an emoji reaction to a post with no current-user reaction, and confirm the selected emoji and aggregate totals update without using replies.

### Validation for User Story 1

- [X] T009 [P] [US1] Add remote reaction creation tests in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt
- [X] T010 [P] [US1] Add repository and view-model tests for first-time reaction submission and optimistic pending state in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt

### Implementation for User Story 1

- [X] T011 [US1] Implement initial reaction creation and queue-aware repository writes in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/SnapshotInteractionSyncCoordinator.kt
- [X] T012 [US1] Wire reaction submission events through /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/SetSnapshotReactionUseCaseImpl.kt
- [X] T013 [US1] Replace the feed like control with a reaction picker entry point in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml

**Checkpoint**: User Story 1 is complete when a user can add an emoji reaction from the feed and see immediate visual confirmation.

---

## Phase 4: User Story 2 - Change or remove my reaction (Priority: P1)

**Goal**: Let a user tap the same emoji to remove a reaction or switch to a different emoji without ever holding more than one active reaction.

**Independent Test**: React to a post, tap the same emoji again to remove it, then choose a different emoji and confirm the reaction state and totals stay correct after each change.

### Validation for User Story 2

- [X] T014 [P] [US2] Add repository tests for same-emoji removal and emoji replacement in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt
- [X] T015 [P] [US2] Add remote synchronization tests for reaction replacement and removal writes in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt

### Implementation for User Story 2

- [X] T016 [US2] Implement single-reaction replacement and removal compression in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/PendingSnapshotActionDao.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T017 [US2] Update reaction picker state for selected, removed, and pending reactions in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt

**Checkpoint**: User Story 2 is complete when reaction removal and replacement work reliably and never create duplicate current-user reactions.

---

## Phase 5: User Story 3 - View reaction totals on a post (Priority: P2)

**Goal**: Show feed-level aggregate reaction totals and emoji summaries without exposing which specific user chose each emoji.

**Independent Test**: Open the feed for posts with and without reactions and confirm compact emoji totals and overall reaction counts render correctly without any user identity list.

### Validation for User Story 3

- [X] T018 [P] [US3] Add mapper and repository tests for reaction summary normalization and legacy-field fallback in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt

### Implementation for User Story 3

- [X] T019 [US3] Persist reaction summary aggregates in retained feed storage and mapping in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemDao.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedOfflineTypeConverters.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt
- [X] T020 [US3] Normalize missing summary fields and legacy-like migration defaults in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T021 [US3] Render compact reaction totals without reactor identities in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/common/PresentationFormatters.kt

**Checkpoint**: User Story 3 is complete when feed cards show aggregate reaction activity only, including calm zero states for older posts with missing summary fields.

---

## Phase 6: User Story 4 - Open a post's replies (Priority: P2)

**Goal**: Let a user open a dedicated reply sheet from the feed and read replies in chronological order with clear loading, empty, and error states.

**Independent Test**: Open the replies for a post from the feed and confirm the sheet shows post context plus oldest-to-newest replies, or an empty state when none exist.

### Validation for User Story 4

- [X] T022 [P] [US4] Add view-model tests for reply sheet loading, empty, and error states in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt

### Implementation for User Story 4

- [X] T023 [US4] Implement reply retrieval contracts and models in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotReplyDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/SnapshotReply.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt
- [X] T024 [US4] Add reply loading orchestration in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotRepliesUseCase.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotRepliesUseCaseImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt
- [X] T025 [US4] Add the modal reply sheet with loading, empty, and retry states in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml

**Checkpoint**: User Story 4 is complete when replies open from the feed in a dedicated sheet and always preserve post context.

---

## Phase 7: User Story 5 - Add a reply to a post (Priority: P2)

**Goal**: Let a user submit a valid reply, prevent blank or over-limit replies, and preserve pending/failure behavior when offline.

**Independent Test**: Open the reply sheet, submit a valid reply, verify it appears at the end of the list and increments the count, then confirm blank and over-limit replies are blocked.

### Validation for User Story 5

- [X] T026 [P] [US5] Add repository tests for valid reply submission, blank validation, and offline pending replies in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt
- [X] T027 [P] [US5] Add remote write tests for reply creation and denormalized author fallback fields in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt

### Implementation for User Story 5

- [X] T028 [US5] Implement reply submission, validation, and optimistic pending rows in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/AddSnapshotReplyUseCase.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/AddSnapshotReplyUseCaseImpl.kt
- [X] T029 [US5] Persist pending reply queue state and reconciliation metadata in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/PendingSnapshotActionEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/PendingSnapshotActionDao.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt
- [X] T030 [US5] Wire reply composer validation, send actions, and pending/failure UI states in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml

**Checkpoint**: User Story 5 is complete when replies can be posted, validated, and reconciled cleanly even through offline-to-online transitions.

---

## Phase 8: User Story 6 - View reply counts in the feed (Priority: P3)

**Goal**: Show reply counts on feed cards so users can spot active conversations before opening the reply sheet.

**Independent Test**: View feed cards for posts with zero and non-zero replies and confirm reply counts update after a successful new reply without opening a second screen.

### Validation for User Story 6

- [X] T031 [P] [US6] Add repository and UI-state tests for replyCount propagation to feed cards in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt

### Implementation for User Story 6

- [X] T032 [US6] Store and refresh replyCount in snapshot aggregates and retained feed rows in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt
- [X] T033 [US6] Surface reply counts and zero-reply call-to-action on feed cards in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/common/PresentationFormatters.kt

**Checkpoint**: User Story 6 is complete when reply activity is visible directly from the feed, including zero-reply invitation states.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Finish cross-story hardening, full-flow validation, and implementation notes.

- [X] T034 [P] Add instrumented interaction coverage for the reaction picker and reply sheet flows in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotInteractionsTest.kt
- [X] T035 [P] Harden reconnect rollback and account cleanup for queued reactions and replies in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T036 Record implementation-specific validation notes in /Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and can start immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user story work.
- **Phase 3: US1** depends on Phase 2 and is the MVP slice.
- **Phase 4: US2** depends on US1 because change/remove behavior extends the first reaction flow.
- **Phase 5: US3** depends on Phase 2 and benefits from US1-US2 because summary display should reflect the reaction model already in use.
- **Phase 6: US4** depends on Phase 2 and can start once shared reply contracts are ready.
- **Phase 7: US5** depends on US4 because reply submission extends the reply sheet and reply retrieval flow.
- **Phase 8: US6** depends on US4-US5 because feed-level reply counts should reflect the same reply pipeline already used by the reply sheet.
- **Phase 9: Polish** depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational work; no dependency on other stories.
- **US2 (P1)**: Depends on US1’s initial reaction selection path.
- **US3 (P2)**: Depends on foundational summary storage and should land after reaction selection/removal behavior is stable.
- **US4 (P2)**: Starts after foundational work; independent from reactions.
- **US5 (P2)**: Depends on US4’s reply-view foundation.
- **US6 (P3)**: Depends on the reply loading/submission flow from US4-US5.

### Task-Level Notes

- T003-T005 can run in parallel after T001-T002.
- T006 depends on T003-T005.
- T007 depends on T001-T003.
- T008 depends on T004-T007.
- T009-T010 depend on T006-T008.
- T011 depends on T009-T010.
- T012 depends on T011.
- T013 depends on T012.
- T014-T015 depend on T011-T013.
- T016 depends on T014-T015.
- T017 depends on T016.
- T018 depends on T003, T005, and T008.
- T019-T021 depend on T018 and should land after T017.
- T022 depends on T006-T008.
- T023 depends on T022.
- T024 depends on T023.
- T025 depends on T024.
- T026-T027 depend on T023-T025.
- T028 depends on T026-T027.
- T029 depends on T028 and T004.
- T030 depends on T028-T029.
- T031 depends on T024-T030.
- T032 depends on T031.
- T033 depends on T032.
- T034-T036 depend on T033.

## Parallel Opportunities

- **Foundational parallel work**: T003, T004, and T005 touch different storage/model files and can run together.
- **US1 validation parallel work**: T009 and T010 can run together because they target different test layers.
- **US2 validation parallel work**: T014 and T015 can run together because one is repository-focused and the other is remote-write focused.
- **US5 validation parallel work**: T026 and T027 can run together because they cover repository and remote layers separately.
- **Polish parallel work**: T034 and T035 can run together while T036 waits for final validation results.

## Parallel Example: User Story 1

```bash
# Launch first-reaction coverage in parallel
Task: "T009 Add remote reaction creation tests in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt"
Task: "T010 Add repository and view-model tests for first-time reaction submission and optimistic pending state in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt"
```

## Parallel Example: User Story 4

```bash
# Split reply viewing work once foundational contracts are ready
Task: "T022 Add view-model tests for reply sheet loading, empty, and error states in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt"
Task: "T023 Implement reply retrieval contracts and models in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotReplyDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/SnapshotReply.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt"
```

## Parallel Example: User Story 5

```bash
# Verify reply submission on separate tracks
Task: "T026 Add repository tests for valid reply submission, blank validation, and offline pending replies in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt"
Task: "T027 Add remote write tests for reply creation and denormalized author fallback fields in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational interaction storage, contracts, and reconciliation support.
3. Complete Phase 3: User Story 1 reaction creation.
4. Stop and validate the independent US1 reaction flow before moving on.

### Incremental Delivery

1. Ship the first reaction flow from US1.
2. Add reaction correction/removal from US2.
3. Add feed-level reaction summaries from US3.
4. Add reply viewing from US4.
5. Add reply submission from US5.
6. Add feed-level reply counts from US6.
7. Finish with cross-cutting validation and rollback hardening.

### Parallel Team Strategy

1. One developer can own storage/model work in T003-T005 while another owns remote/repository contracts in T006-T008.
2. After US1 lands, a second developer can work on US4 reply viewing while the first stabilizes reaction summary behavior in US2-US3.
3. Once the reply sheet exists, reply submission validation in US5 can split between repository and remote-write coverage.

## Notes

- Every task follows the required checklist format: checkbox, task ID, optional `[P]`, optional story label, and exact file path.
- The suggested MVP scope is **Phase 3 / User Story 1**.
- The existing [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/spec.md) and [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/plan.md) remain in place as history and should not be deleted as part of task generation.
