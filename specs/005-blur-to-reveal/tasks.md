# Tasks: Blur-to-Reveal

**Input**: Design documents from `/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/`  
**Prerequisites**: [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/plan.md), [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/spec.md), [research.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/research.md), [data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/data-model.md), [snapshot-reveal-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/contracts/snapshot-reveal-ui.md), [quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/quickstart.md)

**Tests**: Include targeted unit and instrumented coverage because the plan and quickstart explicitly require repository, presentation, and retained-offline verification for reveal, interaction gating, and per-viewer persistence.  
**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently without deleting the existing spec or plan artifacts.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Every task includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Introduce reveal-specific constants, models, and local storage entry points that the rest of the feature builds on.

- [X] T001 Add reveal constants and viewer-facing reveal state enums in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/common/Constants.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt
- [X] T002 Create cached reveal Room primitives in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/CachedRevealStateEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/CachedRevealStateDao.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/FeedOfflineTypeConverters.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared reveal contracts, persistence seams, and repository plumbing required by every user story.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T003 [P] Extend snapshot visibility and interaction helpers in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/common/extension_functions/SnapshotExtensionFunctions.kt
- [X] T004 [P] Add reveal-specific repository and remote data source contracts in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt
- [X] T005 [P] Create remote reveal record DTOs and Firebase path support in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotRevealRecordDTO.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/common/Constants.kt
- [X] T006 Implement remote reveal read/write behavior and idempotent persistence in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt
- [X] T007 Implement local reveal cache mapping and retained-feed merge logic in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T008 Create reveal use-case entry points and dependency wiring in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/RevealSnapshotUseCase.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/RevealSnapshotUseCaseImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/di/InteractionModule.kt

**Checkpoint**: Shared reveal models, remote contracts, local cache primitives, and repository/use-case seams are ready for story work.

---

## Phase 3: User Story 1 - Reveal another user's post (Priority: P1) 🎯 MVP

**Goal**: Let a viewer deliberately reveal another user's hidden image in place, with a smooth transition and no navigation on the first tap.

**Independent Test**: Open the feed as a non-owner, confirm the image starts blurred with `Tap to reveal`, tap only the image area, and verify the image becomes visible without opening the full-screen viewer.

### Validation for User Story 1

- [X] T009 [P] [US1] Add repository and reveal use-case tests for owner visibility, hidden default state, and duplicate reveal no-op behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/domain/interactor/home/RevealSnapshotUseCaseImplTest.kt
- [X] T010 [P] [US1] Add instrumented UI coverage for blurred cards, overlay copy, and image-tap reveal behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotRevealTest.kt

### Implementation for User Story 1

- [X] T011 [US1] Implement reveal-state loading and reveal events in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeFeedUiState.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt
- [X] T012 [US1] Route unrevealed image taps through reveal instead of expanded-image navigation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/HomeContent.kt
- [X] T013 [US1] Build blurred image rendering, centered overlay text, and one-time reveal animation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt
- [X] T014 [US1] Add reveal copy, accessibility labels, and semantics for hidden images in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt
- [X] T015 [US1] Preserve expanded-image access only after visibility is established in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/ExpandedImageViewer.kt

**Checkpoint**: User Story 1 should now reveal hidden images in place, keep owner posts visible, and avoid navigation on the first reveal tap.

---

## Phase 4: User Story 2 - Interact only after reveal (Priority: P2)

**Goal**: Keep reaction, reply, and share controls visible but inactive before reveal, then activate them immediately after reveal or for owner posts.

**Independent Test**: Open an unrevealed non-owner post, confirm reaction/reply/share are visibly disabled and do nothing, reveal the post, and verify the same controls become active without reloading the feed.

### Validation for User Story 2

- [X] T016 [P] [US2] Add presentation and share-support tests for disabled-before-reveal and enabled-after-reveal behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotShareSupportTest.kt
- [X] T017 [P] [US2] Add instrumented UI coverage for muted control styling and post-reveal activation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotRevealInteractionsTest.kt

### Implementation for User Story 2

- [X] T018 [US2] Extend resolved snapshot state with shared interaction and image-view gating flags in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/common/extension_functions/SnapshotExtensionFunctions.kt
- [X] T019 [US2] Guard reaction and reply actions from unrevealed posts in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/RevealSnapshotUseCaseImpl.kt
- [X] T020 [US2] Render muted disabled controls and block pre-reveal sharing in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotShareSupport.kt
- [X] T021 [US2] Ensure only the image area triggers reveal while the rest of the card remains non-revealing in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/HomeContent.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt

**Checkpoint**: User Story 2 should now enforce disabled pre-reveal interactions and immediate post-reveal activation consistently across reaction, reply, and share surfaces.

---

## Phase 5: User Story 3 - Keep reveals personal and permanent (Priority: P3)

**Goal**: Persist reveal state per viewer and per post so a revealed post stays visible for that viewer across refreshes, retained offline sessions, and account changes without affecting anyone else.

**Independent Test**: Reveal a post as user A, refresh and relaunch the feed, confirm the post remains visible for user A, then switch to user B and verify the same post is still hidden until user B reveals it.

### Validation for User Story 3

- [X] T022 [P] [US3] Add remote and repository tests for per-viewer reveal persistence, retained offline visibility, and account-scoped isolation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt
- [X] T023 [P] [US3] Add home-state tests for revealed-post persistence across refresh, resume, and account changes in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt

### Implementation for User Story 3

- [X] T024 [US3] Persist reveal records per viewer and keep repeated writes idempotent in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotRevealRecordDTO.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt
- [X] T025 [US3] Cache reveal state locally and scope cleanup by account in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/CachedRevealStateEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/CachedRevealStateDao.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/DailyDoseDatabase.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T026 [US3] Merge cached and remote reveal state into retained feed items without exposing hidden images prematurely in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedItemEntity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/local/OfflineFeedMapper.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T027 [US3] Keep revealed posts visible across refresh and retained offline resume while preserving per-viewer isolation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt

**Checkpoint**: User Story 3 should now preserve reveal state durably for each viewer while leaving other viewers’ copies of the same post hidden by default.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, cleanup, and documentation updates that span multiple user stories.

- [X] T028 [P] Finalize reveal-related copy and visual polish for hidden, animating, and visible states in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt
- [X] T029 [P] Record feature-specific manual validation notes and expected offline/account-switch checks in /Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/quickstart.md
- [X] T030 Run unit and instrumented validation for blur-to-reveal with /Users/henryvazquez/StudioProjects/DailyDose/gradlew and capture any follow-up implementation notes in /Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and can start immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user story work.
- **Phase 3: US1** depends on Phase 2 and is the MVP slice.
- **Phase 4: US2** depends on US1 because interaction gating builds on the reveal-state and image-tap behavior introduced there.
- **Phase 5: US3** depends on US1 because durable persistence extends the core reveal workflow and on Phase 2 because it uses the shared local/remote seams.
- **Phase 6: Polish** depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational work; no dependency on other user stories.
- **US2 (P2)**: Depends on US1 because controls should only become active after the reveal interaction already exists.
- **US3 (P3)**: Depends on US1 for the baseline reveal flow, then hardens persistence, privacy, and retained-offline behavior around that flow.

### Task-Level Notes

- T003, T004, and T005 can run in parallel after T001-T002.
- T006 depends on T004-T005.
- T007 depends on T002-T004 and T006.
- T008 depends on T003-T004 and can proceed alongside T007 once contracts are in place.
- T009-T010 depend on T006-T008.
- T011 depends on T007-T008.
- T012 depends on T011.
- T013-T014 depend on T011 and can run in parallel.
- T015 depends on T012-T013.
- T016-T017 depend on T015.
- T018 depends on T003 and T015.
- T019-T021 depend on T018 and can proceed after T016-T017 define expected behavior.
- T022-T023 depend on T024-T027 planning details but should be written before those implementations are completed.
- T024 depends on T006.
- T025 depends on T002 and T007.
- T026 depends on T024-T025.
- T027 depends on T011, T024, and T026.
- T028-T029 can run in parallel after T021 and T027.
- T030 depends on the completion of all desired implementation tasks.

## Parallel Opportunities

- **Foundational parallel work**: T003, T004, and T005 touch different model/contract files and can run together after setup.
- **US1 validation parallel work**: T009 and T010 can run together because one targets repository/use-case logic and the other targets instrumented UI behavior.
- **US1 implementation parallel work**: T013 and T014 can run together after reveal-state loading is available from T011.
- **US2 validation parallel work**: T016 and T017 can run together because one targets presentation/share logic and the other targets UI rendering.
- **US3 validation parallel work**: T022 and T023 can run together because one focuses on persistence layers and the other on home-state continuity.
- **Polish parallel work**: T028 and T029 can run together while T030 waits for implementation completion.

## Parallel Example: User Story 1

```bash
# Verify reveal mechanics on separate tracks
Task: "T009 Add repository and reveal use-case tests for owner visibility, hidden default state, and duplicate reveal no-op behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/domain/interactor/home/RevealSnapshotUseCaseImplTest.kt"
Task: "T010 Add instrumented UI coverage for blurred cards, overlay copy, and image-tap reveal behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotRevealTest.kt"
```

## Parallel Example: User Story 2

```bash
# Split interaction-gating work by verification layer
Task: "T016 Add presentation and share-support tests for disabled-before-reveal and enabled-after-reveal behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotShareSupportTest.kt"
Task: "T017 Add instrumented UI coverage for muted control styling and post-reveal activation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/SnapshotRevealInteractionsTest.kt"
```

## Parallel Example: User Story 3

```bash
# Validate persistence and account isolation in parallel
Task: "T022 Add remote and repository tests for per-viewer reveal persistence, retained offline visibility, and account-scoped isolation in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt"
Task: "T023 Add home-state tests for revealed-post persistence across refresh, resume, and account changes in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational reveal contracts, caching seams, and use-case wiring.
3. Complete Phase 3: User Story 1 reveal interaction.
4. Stop and validate the independent US1 reveal flow before moving on.

### Incremental Delivery

1. Ship the in-feed reveal interaction from US1.
2. Add pre-reveal interaction gating from US2.
3. Add durable per-viewer persistence and privacy hardening from US3.
4. Finish with cross-cutting validation and copy/UX polish.

### Parallel Team Strategy

1. One developer can own foundational remote/local reveal persistence work in T004-T007 while another prepares use-case wiring in T008.
2. After US1 starts, one developer can build the card animation in T013 while another wires the feed navigation changes in T012.
3. Once US1 is stable, US2 control gating and US3 persistence tests can be prepared in parallel before their implementation tasks land.

## Notes

- Every task follows the required checklist format: checkbox, task ID, optional `[P]`, optional story label, and exact file path.
- The suggested MVP scope is **Phase 3 / User Story 1**.
- The existing [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/plan.md) and [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/spec.md) remain in place as source history and should not be deleted as part of task generation.
