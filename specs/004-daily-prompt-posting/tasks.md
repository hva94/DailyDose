# Tasks: Daily Prompt Posting

**Input**: Design documents from `/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/`  
**Prerequisites**: [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/plan.md), [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/spec.md), [research.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/research.md), [data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/data-model.md), [daily-prompt-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/contracts/daily-prompt-ui.md), [quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/quickstart.md)

**Tests**: Include targeted unit and instrumented coverage because the plan and quickstart explicitly require repository, add-flow, home-state, and feed-rendering verification.  
**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently without deleting the existing spec or plan artifacts.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`)
- Every task includes exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Introduce the daily-prompt constants and model entry points the rest of the feature builds on.

- [X] T001 Add daily prompt constants and prompt catalog definitions in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/common/Constants.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/DailyPromptCombo.kt
- [X] T002 Create daily prompt assignment and posting-status DTO/domain models in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/DailyPromptAssignmentDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/UserPostingStatusDTO.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/DailyPromptAssignment.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/UserPostingStatus.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared prompt data, publish contracts, and presentation state required by every user story.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T003 [P] Extend snapshot publish/read models with prompt metadata and title-generation mode in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt
- [X] T004 [P] Extend prompt-aware repository and use-case contracts in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/HomeRepository.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/repository/AddSnapshotRepository.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/add/CreateSnapshotUseCase.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt
- [X] T005 [P] Add daily prompt home/use-case entry points in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetActiveDailyPromptUseCase.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetActiveDailyPromptUseCaseImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/ObservePromptCompletionUseCase.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/ObservePromptCompletionUseCaseImpl.kt
- [X] T006 Implement Firebase mapping for shared daily assignment, user posting status, and prompt-aware snapshot persistence in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/User.kt
- [X] T007 Implement prompt-aware repository and publish orchestration in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/AddSnapshotRepositoryImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/add/CreateSnapshotUseCaseImpl.kt
- [X] T008 Create shared prompt composer/title-generation helpers in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/PromptComposerState.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/PromptTitleGenerator.kt
- [X] T009 Update shared home/add UI state containers for prompt support in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeFeedUiState.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddViewModel.kt

**Checkpoint**: Shared daily-prompt models, remote contracts, publish seams, and presentation state are ready for story work.

---

## Phase 3: User Story 1 - See today's prompt in home (Priority: P1) 🎯 MVP

**Goal**: Show an eligible user the shared daily prompt card near the top of the home feed and hide it once that user has completed the day with a first post.

**Independent Test**: Open the home feed as a user with no post today and confirm the `Daily Prompt` card appears with the label, prompt text, and helper copy. Publish once, return to the feed, and confirm the card is hidden for that user.

### Validation for User Story 1

- [X] T010 [P] [US1] Add repository and home-state tests for prompt availability, missing-assignment fallback, and hidden-after-post behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt
- [X] T011 [P] [US1] Add instrumented prompt-card rendering coverage in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/DailyPromptHomeTest.kt

### Implementation for User Story 1

- [X] T012 [US1] Implement active daily prompt loading and prompt-availability mapping in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/home/GetActiveDailyPromptUseCaseImpl.kt
- [X] T013 [US1] Wire prompt visibility and refresh-after-post state through /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeFeedUiState.kt
- [X] T014 [US1] Render the daily prompt card and tap callback near the top of the feed in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/HomeContent.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml

**Checkpoint**: User Story 1 is complete when the prompt card appears only for eligible users and disappears after the first successful post of the day.

---

## Phase 4: User Story 2 - Create a prompt-driven post (Priority: P1)

**Goal**: Let a user tap the prompt card, enter an optional short answer, and publish a prompt-driven post with an editable time-based default title.

**Independent Test**: Tap the prompt card, verify the same prompt appears in the add flow, publish one post with no answer and one with an answer, and confirm the title rules and manual override behavior work correctly.

### Validation for User Story 2

- [X] T015 [P] [US2] Add add-flow tests for prompt header state, answer-based title drafts, publish-time timestamp finalization, and manual title override in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/add/AddViewModelTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/add/PromptTitleGeneratorTest.kt
- [X] T016 [P] [US2] Add prompt-aware publish and posting-status write tests in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt

### Implementation for User Story 2

- [X] T017 [US2] Extend prompt-navigation and add-flow entry state in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddViewModel.kt
- [X] T018 [US2] Implement prompt-aware composer UI, optional answer input, and system-managed title behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddScreen.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/PromptTitleGenerator.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml
- [X] T019 [US2] Persist prompt-driven publish inputs and update per-user last-post metadata in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/AddSnapshotRepositoryImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/interactor/add/CreateSnapshotUseCaseImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt

**Checkpoint**: User Story 2 is complete when prompt-driven posting works end to end with blank-answer titles, answer-based titles, and manual title overrides.

---

## Phase 5: User Story 3 - Read prompt context in the feed (Priority: P2)

**Goal**: Show prompt context on prompt-driven posts in the feed and keep long generated titles readable without truncating saved content.

**Independent Test**: View the feed after prompt-driven posts are published and confirm each such post shows prompt text, title, and image in order, with long titles expandable inline.

### Validation for User Story 3

- [X] T020 [P] [US3] Add feed-card state tests for prompt-context snapshots and non-prompt fallbacks in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt
- [X] T021 [P] [US3] Add instrumented feed-card coverage for prompt text and expandable long titles in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/DailyPromptFeedCardTest.kt

### Implementation for User Story 3

- [X] T022 [US3] Expose prompt metadata from snapshot mapping and posted-snapshot caching in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/Snapshot.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/SnapshotDTO.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T023 [US3] Render prompt text above snapshot titles while preserving non-prompt cards in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/HomeContent.kt
- [X] T024 [US3] Implement collapsed and expanded long-title behavior with prompt-aware copy in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml

**Checkpoint**: User Story 3 is complete when prompt-driven posts visibly carry prompt context and long titles can be expanded without losing any stored text.

---

## Phase 6: User Story 4 - Keep prompt visibility personal and day-based (Priority: P2)

**Goal**: Ensure all users see the same prompt on the same day while prompt visibility remains controlled by each user’s own posting activity.

**Independent Test**: Compare two users on the same day where one has posted and the other has not, and confirm they share the same prompt assignment while only the user without a post still sees the card.

### Validation for User Story 4

- [X] T025 [P] [US4] Add remote assignment tests for same-day shared combo selection and no-consecutive-repeat behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt
- [X] T026 [P] [US4] Add repository and home-state tests for per-user first-post completion across prompt-driven and normal posts in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt

### Implementation for User Story 4

- [X] T027 [US4] Implement shared daily assignment creation and consecutive-day exclusion in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImpl.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/DailyPromptAssignmentDTO.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/domain/model/DailyPromptAssignment.kt
- [X] T028 [US4] Implement per-user last-post tracking for prompt completion in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/network/model/User.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/AddSnapshotRepositoryImpl.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImpl.kt
- [X] T029 [US4] Refresh prompt visibility after successful first posts while preserving normal-post completion semantics in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt, /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt, and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddViewModel.kt

**Checkpoint**: User Story 4 is complete when prompt assignment is shared across users while completion remains personal and tied to each user’s first successful post of the day.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Finish full-flow validation, copy hardening, and implementation notes that affect multiple stories.

- [X] T030 [P] Add end-to-end prompt flow instrumented coverage in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/DailyPromptFlowTest.kt
- [X] T031 [P] Finalize prompt-related copy and accessibility labels in /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/res/values/strings.xml and /Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/ui/SnapshotCard.kt
- [X] T032 Record implementation-specific validation notes and final manual checks in /Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and can start immediately.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user story work.
- **Phase 3: US1** depends on Phase 2 and is the MVP slice.
- **Phase 4: US2** depends on US1 because the prompt-driven creation flow starts from the home prompt entry point.
- **Phase 5: US3** depends on US2 because feed prompt context requires prompt-driven posts to exist first.
- **Phase 6: US4** depends on US1-US2 because it hardens shared-day assignment and first-post completion semantics across the already-working prompt card and prompt publish flow.
- **Phase 7: Polish** depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Starts after foundational work; no dependency on other user stories.
- **US2 (P1)**: Depends on US1’s prompt-card entry point and extends the add flow from there.
- **US3 (P2)**: Depends on US2 because it renders prompt-driven post metadata in the feed.
- **US4 (P2)**: Depends on the basic home/add prompt flow from US1-US2, then hardens multi-user and first-post-of-day rules.

### Task-Level Notes

- T003-T005 can run in parallel after T001-T002.
- T006 depends on T001-T005.
- T007 depends on T003-T006.
- T008 depends on T001-T004.
- T009 depends on T004, T007, and T008.
- T010-T011 depend on T006-T009.
- T012 depends on T007 and T009.
- T013 depends on T012.
- T014 depends on T013.
- T015-T016 depend on T007-T009 and can run in parallel.
- T017 depends on T014 and T015.
- T018 depends on T008, T009, and T017.
- T019 depends on T016-T018.
- T020-T021 depend on T019 and can run in parallel.
- T022 depends on T019.
- T023 depends on T022.
- T024 depends on T023.
- T025-T026 depend on T019 and can run in parallel.
- T027 depends on T025.
- T028 depends on T026 and T027.
- T029 depends on T028 and T014.
- T030-T031 depend on T024 and T029 and can run in parallel.
- T032 depends on T030-T031.

## Parallel Opportunities

- **Foundational parallel work**: T003, T004, and T005 touch different model/contract files and can run together after setup.
- **US1 validation parallel work**: T010 and T011 can run together because one targets repository/view-model logic and the other targets instrumented UI rendering.
- **US2 validation parallel work**: T015 and T016 can run together because they cover separate add-flow and remote-publish layers.
- **US4 validation parallel work**: T025 and T026 can run together because one focuses on shared assignment logic and the other on per-user completion semantics.
- **Polish parallel work**: T030 and T031 can run together while T032 waits for final validation output.

## Parallel Example: User Story 1

```bash
# Verify prompt-card logic on separate tracks
Task: "T010 Add repository and home-state tests for prompt availability, missing-assignment fallback, and hidden-after-post behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt"
Task: "T011 Add instrumented prompt-card rendering coverage in /Users/henryvazquez/StudioProjects/DailyDose/app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/home/DailyPromptHomeTest.kt"
```

## Parallel Example: User Story 2

```bash
# Split prompt-driven post validation by layer
Task: "T015 Add add-flow tests for prompt header state, answer-based title drafts, publish-time timestamp finalization, and manual title override in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/add/AddViewModelTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/add/PromptTitleGeneratorTest.kt"
Task: "T016 Add prompt-aware publish and posting-status write tests in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt"
```

## Parallel Example: User Story 4

```bash
# Validate shared-day and personal-completion behavior in parallel
Task: "T025 Add remote assignment tests for same-day shared combo selection and no-consecutive-repeat behavior in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseServiceImplTest.kt"
Task: "T026 Add repository and home-state tests for per-user first-post completion across prompt-driven and normal posts in /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/data/repository/HomeRepositoryImplTest.kt and /Users/henryvazquez/StudioProjects/DailyDose/app/src/test/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModelTest.kt"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational prompt data, publish contracts, and state support.
3. Complete Phase 3: User Story 1 home prompt visibility.
4. Stop and validate the independent US1 prompt-card flow before moving on.

### Incremental Delivery

1. Ship the home prompt card from US1.
2. Add prompt-driven posting and title generation from US2.
3. Add feed prompt context and long-title expansion from US3.
4. Add shared-day and per-user completion hardening from US4.
5. Finish with cross-cutting validation and copy hardening.

### Parallel Team Strategy

1. One developer can own foundational remote/repository work in T004-T007 while another prepares prompt composer helpers in T008-T009.
2. After US1 lands, one developer can work on add-flow behavior in US2 while another prepares feed-card coverage for US3.
3. Once prompt-driven publish works, shared-assignment hardening in US4 can split between remote assignment logic and repository/home completion tests.

## Notes

- Every task follows the required checklist format: checkbox, task ID, optional `[P]`, optional story label, and exact file path.
- The suggested MVP scope is **Phase 3 / User Story 1**.
- The existing [plan.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/plan.md) and [spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/spec.md) remain in place as history and should not be deleted as part of task generation.
