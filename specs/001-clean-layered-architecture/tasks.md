---
description: "Task list — Clean layered architecture foundation"
---

# Tasks: Clean layered architecture foundation

**Input**: Design documents from `specs/001-clean-layered-architecture/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/README.md, quickstart.md

**Tests**: Targeted unit tests included for User Story 1 to support FR-007 baseline safety.

**Organization**: Phases follow user-story priorities P1 → P2 → P3, then polish.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Parallel-friendly
- **[Story]**: US1 / US2 / US3 for user-story phases only

---

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Review `specs/001-clean-layered-architecture/plan.md` and `spec.md` on branch `001-clean-layered-architecture`
- [x] T002 Verify `./gradlew :app:assembleDebug` compiles cleanly

---

## Phase 2: Foundational (Blocking Prerequisites)

- [x] T003 [P] Create `specs/001-clean-layered-architecture/regression-baseline.md`
- [x] T004 [P] Create `specs/001-clean-layered-architecture/capability-map.md`
- [x] T005 [P] Create `specs/001-clean-layered-architecture/boundary-exceptions.md`

**Checkpoint**: Foundation docs exist.

---

## Phase 3: User Story 1 — Current users keep a reliable experience (P1) 🎯 MVP

**Goal**: Lock a repeatable baseline + fast regression signals.

**Independent Test**: regression-baseline.md recorded; unit tests in `app/src/test/` pass.

- [x] T006 [US1] Run scenarios in `regression-baseline.md` and append a dated Baseline run section
- [x] T007 [P] [US1] Add `app/src/test/java/com/hvasoft/dailydose/domain/interactor/home/GetSnapshotsUseCaseImplTest.kt`
- [x] T008 [P] [US1] Add `app/src/test/java/com/hvasoft/dailydose/domain/interactor/home/ToggleUserLikeUseCaseImplTest.kt`
- [x] T009 [P] [US1] Add `app/src/test/java/com/hvasoft/dailydose/domain/interactor/home/DeleteSnapshotUseCaseImplTest.kt`
- [x] T010 [US1] Run `./gradlew :app:testDebugUnitTest` — all tests pass
- [x] T011 [US1] Add Post-merge verification template to `regression-baseline.md`

**Checkpoint**: Baseline evidence + use-case unit tests protect the home/domain boundary.

---

## Phase 4: User Story 2 — Grow features without rewriting the whole app (P2)

**Goal**: Move Firebase/DTO work out of Add and Profile presentation into data/domain ports.

**Independent Test**: AddFragment.kt and ProfileFragment.kt contain no `com.google.firebase.*` imports (except documented exceptions); regression still passes.

- [x] T012 [US2] Extend `app/src/main/java/com/hvasoft/dailydose/data/network/data_source/RemoteDatabaseService.kt` with `publishSnapshot()`
- [x] T013 [US2] Add `app/src/main/java/com/hvasoft/dailydose/domain/repository/AddSnapshotRepository.kt`, `domain/interactor/add/CreateSnapshotUseCase.kt`, and `CreateSnapshotUseCaseImpl.kt`
- [x] T014 [US2] Add `app/src/main/java/com/hvasoft/dailydose/data/repository/AddSnapshotRepositoryImpl.kt`
- [x] T015 [US2] Register Hilt bindings in `app/src/main/java/com/hvasoft/dailydose/di/HomeRepositoryModule.kt` and `InteractionModule.kt` for Add ports and use cases
- [x] T016 [US2] Create `app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddPostUiState.kt` and convert `AddViewModel.kt` to `@HiltViewModel` with `CreateSnapshotUseCase`
- [x] T017 [US2] Refactor `app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddFragment.kt` — no Firebase imports; delegates to `AddViewModel`
- [x] T018 [US2] Re-run Add scenarios in regression-baseline.md and append results
- [x] T019 [US2] Add `app/src/main/java/com/hvasoft/dailydose/domain/interactor/profile/` use cases and `domain/repository/ProfileRepository.kt` with implementations under `data/repository/ProfileRepositoryImpl.kt`
- [x] T020 [US2] Create `app/src/main/java/com/hvasoft/dailydose/presentation/screens/profile/ProfileViewModel.kt` and refactor `ProfileFragment.kt` — Firebase RTDB/Storage removed from fragment
- [x] T021 [US2] Re-run Profile scenarios in regression-baseline.md and append dated results
- [x] T022 [US2] Update `capability-map.md` and `contracts/README.md` with new ports

**Checkpoint**: Add and Profile respect presentation/domain/data boundaries.

---

## Phase 5: User Story 3 — New contributors know where code belongs (P3)

**Goal**: Meet SC-003 onboarding needs.

**Independent Test**: Reader can answer contributor-quiz.md; README links to ARCHITECTURE.md.

- [x] T023 [P] [US3] Create `specs/001-clean-layered-architecture/ARCHITECTURE.md`
- [x] T024 [P] [US3] Create `specs/001-clean-layered-architecture/contributor-quiz.md`
- [x] T025 [US3] Update root `README.md` with Architecture section

**Checkpoint**: Onboarding path discoverable from repo root.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T026 [P] Run `./gradlew :app:lintDebug` from repository root; fix any new lint issues
- [ ] T027 Walk through `quickstart.md` using Add/Profile as examples; update if any steps drifted
- [ ] T028 [P] Update `boundary-exceptions.md` to reflect final state after all refactors

---

## Dependencies & Execution Order

```text
Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US3) → Phase 6 (Polish)
```

### Parallel Opportunities

| Phase | Tasks |
|-------|-------|
| 2 | T003, T004, T005 |
| 3 | T007, T008, T009 (after T006) |
| 5 | T023, T024 |
| 6 | T026, T028 |

---

## Task summary

| Phase | Tasks | Count | Status |
|-------|-------|-------|--------|
| Setup | T001–T002 | 2 | ✓ done |
| Foundational | T003–T005 | 3 | ✓ done |
| US1 | T006–T011 | 6 | ✓ done |
| US2 | T012–T022 | 11 | ✓ done |
| US3 | T023–T025 | 3 | ✓ done |
| Polish | T026–T028 | 3 | T026, T028 pending (manual/lint) |
| **Total** | **T001–T028** | **28** | |
