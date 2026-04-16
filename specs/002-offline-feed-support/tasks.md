# Tasks: Offline feed support

**Input**: Design documents from `/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/`  
**Prerequisites**: [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/plan.md), [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/spec.md), [research.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/research.md), [data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/data-model.md), [home-feed-offline-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/contracts/home-feed-offline-ui.md), [quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/quickstart.md)

**Tests**: Include targeted unit and instrumented coverage because the design explicitly requires verification for repository refresh behavior, offline UI states, and retry/reconnect flows.  
**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Every task includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add the persistence/tooling dependencies and create the offline package entry points the rest of the feature will build on.

- [X] T001 Add Room and Room Paging dependencies plus kapt compiler entries in /Users/henryvazquez/StudioProjects/DailyDose/app/build.gradle
- [X] T002 Create the offline persistence package entrypoint in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared offline data layer, storage helpers, and domain contracts required by all user stories.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T003 [P] Create retained feed and media Room entities in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineMediaAssetEntity.kt
- [X] T004 [P] Create sync-state entity and Room converters in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedSyncStateEntity.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedOfflineTypeConverters.kt
- [X] T005 [P] Create DAOs for retained feed, media assets, and sync state in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemDao.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineMediaAssetDao.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedSyncStateDao.kt
- [X] T006 Implement the Room database wiring in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt
- [X] T007 [P] Implement retained media file storage and eviction helpers in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedAssetStorage.kt
- [X] T008 [P] Implement remote-to-local and local-to-domain mappers in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt
- [X] T009 [P] Add shared offline feed state models in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/HomeFeedAvailabilityMode.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/HomeFeedSyncState.kt
- [X] T010 Update home feed repository and use case contracts for local-first paging plus sync status in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotsUseCase.kt
- [X] T011 Implement the shared refresh coordinator for remote sync, asset downloads, and bounded eviction in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeFeedRefreshCoordinator.kt
- [X] T012 Wire Room, asset storage, and refresh coordinator providers into DI in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/di/HomeRepositoryModule.kt

**Checkpoint**: Shared offline storage, sync models, and repository contracts are ready for story work.

---

## Phase 3: User Story 1 - Browse the last synced home feed offline (Priority: P1) 🎯 MVP

**Goal**: Let a previously synced user reopen the home feed offline and browse retained feed cards with working retained images.

**Independent Test**: Load the home feed online, disconnect the device, reopen the app, and confirm the retained feed items and their image previews still render in the last known order.

### Validation for User Story 1

- [X] T013 [P] [US1] Add repository tests for local-first paging and retained item ordering in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt
- [X] T014 [P] [US1] Add refresh coordinator tests for retained image download and bounded cache writes in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeFeedRefreshCoordinatorTest.kt

### Implementation for User Story 1

- [X] T015 [US1] Refactor the home repository to page retained feed items from Room and trigger remote sync updates in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T016 [US1] Update the home feed use case and snapshot domain model to expose retained media paths and retained ordering in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotsUseCaseImpl.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt
- [X] T017 [US1] Update the home view model to load retained feed data on launch and request the initial sync through the new repository APIs in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt
- [X] T018 [US1] Update feed card rendering, image opening, and share behavior to prefer retained local assets in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt

**Checkpoint**: User Story 1 is complete when retained home feed cards and retained images work offline without relying on live network calls.

---

## Phase 4: User Story 2 - Understand content freshness and limits while offline (Priority: P2)

**Goal**: Make offline state explicit by showing freshness details, limited-media fallbacks, and clear action restrictions.

**Independent Test**: Disconnect after a successful sync and confirm the home feed shows offline messaging, last successful refresh time, and limited-media fallbacks without blank image containers.

### Validation for User Story 2

- [X] T019 [P] [US2] Add view-model tests for offline-retained, offline-empty, and partial-media state mapping in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt

### Implementation for User Story 2

- [X] T020 [US2] Create the presentation model for offline availability, refresh metadata, and action policy in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeFeedUiState.kt
- [X] T021 [US2] Extend the home view model to expose last successful sync time, offline availability mode, and blocked action messaging in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt
- [X] T022 [US2] Update the home screen to render the offline banner, freshness timestamp, limited-media fallback, and offline-only action restrictions in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt
- [X] T023 [US2] Add offline-state, freshness, partial-media, and blocked-action copy in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml

**Checkpoint**: User Story 2 is complete when users can clearly tell they are offline, how fresh the retained feed is, and which content or actions are limited.

---

## Phase 5: User Story 3 - Recover smoothly when connectivity returns (Priority: P3)

**Goal**: Support retry/reconnect recovery, handle first-run offline empty states, and keep retained content isolated per signed-in account.

**Independent Test**: Open the retained feed offline, restore connectivity, retry refresh, and confirm the feed updates in the same session; also verify a never-synced account sees the dedicated offline-empty state.

### Validation for User Story 3

- [X] T024 [P] [US3] Add repository tests for retry refresh, sync-state transitions, and account-scoped cache cleanup in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryRefreshTest.kt
- [X] T025 [P] [US3] Add an instrumented offline recovery test covering retry and offline-empty messaging in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/HomeOfflineRecoveryTest.kt

### Implementation for User Story 3

- [X] T026 [US3] Implement retry refresh orchestration and offline-empty sync-state handling in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt
- [X] T027 [US3] Update the home screen retry affordance and reconnect transition handling in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt
- [X] T028 [US3] Clear or rebuild retained feed data on account change and sign-out in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt

**Checkpoint**: User Story 3 is complete when retry/reconnect recovery works in-session and account changes never expose another user’s retained feed.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup, validation, and cross-story hardening.

- [X] T029 [P] Add cache-eviction guardrails and cleanup logging in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeFeedRefreshCoordinator.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedAssetStorage.kt
- [X] T030 [P] Record final manual validation coverage and any implementation-specific quickstart notes in /Users/henryvazquez/StudioProjects/DailyDose/specs/002-offline-feed-support/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and starts immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user stories.
- **Phase 3: User Story 1** depends on Phase 2 and is the MVP slice.
- **Phase 4: User Story 2** depends on Phase 2 and builds on the retained-feed foundation from US1 state/data.
- **Phase 5: User Story 3** depends on Phases 2, 3, and 4 because retry/recovery relies on retained data plus explicit offline state handling.
- **Phase 6: Polish** depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational work; no dependency on other user stories.
- **US2 (P2)**: Starts after foundational work but uses the retained-feed data path implemented for US1.
- **US3 (P3)**: Starts after US1 and US2 because reconnect recovery depends on retained data, sync metadata, and offline UI modes already existing.

### Task-Level Notes

- T003-T005 and T007-T009 can run in parallel after T002.
- T010 depends on T009.
- T011 depends on T003-T008.
- T012 depends on T006, T010, and T011.
- T015 depends on T010-T012.
- T016 depends on T015.
- T017 depends on T015-T016.
- T018 depends on T017.
- T020-T023 depend on T017-T018 and T019.
- T026-T028 depend on T021-T023 and T024-T025.
- T029-T030 depend on T028.

## Parallel Opportunities

- **Foundational parallel work**: T003, T004, T005, T007, T008, and T009 touch different files and can proceed together.
- **US1 validation parallel work**: T013 and T014 can run together because they target different unit-test files.
- **US3 validation parallel work**: T024 and T025 can run together because one is unit-level and the other is instrumented UI coverage.

## Parallel Example: User Story 1

```bash
# Launch repository-level verification in parallel
Task: "T013 Add repository tests for local-first paging and retained item ordering in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt"
Task: "T014 Add refresh coordinator tests for retained image download and bounded cache writes in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeFeedRefreshCoordinatorTest.kt"
```

## Parallel Example: User Story 2

```bash
# Split presentation-model and copy work once US1 is complete
Task: "T020 Create the presentation model for offline availability, refresh metadata, and action policy in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeFeedUiState.kt"
Task: "T023 Add offline-state, freshness, partial-media, and blocked-action copy in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml"
```

## Parallel Example: User Story 3

```bash
# Execute reconnect validation on separate tracks
Task: "T024 Add repository tests for retry refresh, sync-state transitions, and account-scoped cache cleanup in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryRefreshTest.kt"
Task: "T025 Add an instrumented offline recovery test covering retry and offline-empty messaging in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/HomeOfflineRecoveryTest.kt"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational offline data layer and repository contracts.
3. Complete Phase 3: User Story 1 retained-feed browsing.
4. Validate the independent US1 offline browse test before moving on.

### Incremental Delivery

1. Ship the retained-feed MVP from US1.
2. Add offline clarity and limited-content handling from US2.
3. Add reconnect recovery and account isolation from US3.
4. Finish with cross-cutting cleanup and validation notes.

### Parallel Team Strategy

1. One developer handles Room entities/DAOs while another handles storage helpers and domain sync models during Phase 2.
2. After US1 lands, presentation-focused work in US2 can split between UI-state modeling and string/copy updates.
3. US3 validation can split between repository tests and instrumented UI recovery coverage.

## Notes

- Every task follows the required checklist format: checkbox, task ID, optional `[P]`, optional story label, and exact file path.
- The suggested MVP scope is Phase 3 / User Story 1 only.
- Avoid starting US2 or US3 before the foundational offline data layer is stable, because both depend on the retained-feed contract created there.
