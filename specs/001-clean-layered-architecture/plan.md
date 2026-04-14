# Implementation Plan: Clean layered architecture foundation

**Branch**: `001-clean-layered-architecture` | **Date**: 2026-04-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-clean-layered-architecture/spec.md`

## Summary

Harden the existing `presentation → domain → data` split so live users keep current behavior (FR-001, FR-007) while new work lands in clear boundaries. Approach: incremental strangler — tighten dependency rules, expand tests, document a capability map, and defer Gradle multi-module extraction until baselines are stable (R7 in research.md).

## Technical Context

**Language/Version**: Kotlin 1.9.x (JVM 17)
**Primary Dependencies**: Android Gradle Plugin 8.9.x, AndroidX (AppCompat, Fragment, Navigation, Lifecycle, Paging 3, Room), Jetpack Compose (Material3) alongside ViewBinding, Dagger Hilt 2.48, Firebase (Auth, Realtime Database, Storage, Config), Glide
**Storage / remote**: Firebase Realtime Database & Storage; Room on classpath
**Testing**: JUnit 4, MockK, Truth, kotlinx-coroutines-test, Hilt testing, Espresso
**Target Platform**: Android minSdk 24, targetSdk / compileSdk 36
**Project Type**: Single-module mobile application (`:app`)
**Performance Goals**: Preserve current perceived performance; no new numeric SLA
**Constraints**: Production app — no extended big-bang freeze (FR-006)
**Scale/Scope**: ~37 Kotlin source files; primary flows Host → Home / Add / Profile

## Constitution Check

`.specify/memory/constitution.md` is still a template (not ratified). Effective gates come from the spec: satisfy FR-001–FR-007 and SC-001–SC-003. No constitution violations to track.

## Project Structure

### Documentation (this feature)

```text
specs/001-clean-layered-architecture/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/README.md
├── spec.md
├── checklists/requirements.md
├── regression-baseline.md
├── capability-map.md
├── boundary-exceptions.md
├── ARCHITECTURE.md
├── contributor-quiz.md
└── tasks.md
```

### Source Code

```text
app/src/main/java/com/hvasoft/dailydose/
├── presentation/
│   ├── HostActivity.kt
│   └── screens/
│       ├── home/                     # HomeFragment, HomeViewModel, HomeState, adapter
│       ├── add/                      # AddFragment, AddViewModel, AddPostUiState
│       ├── profile/                  # ProfileFragment, ProfileViewModel
│       └── common/
├── domain/
│   ├── model/                        # Snapshot, UserProfile, PostSnapshotOutcome
│   ├── repository/                   # HomeRepository, AddSnapshotRepository, ProfileRepository
│   ├── interactor/
│   │   ├── home/                     # Get/Toggle/Delete use cases
│   │   ├── add/                      # CreateSnapshotUseCase
│   │   └── profile/                  # Get/UpdateName/UploadPhoto use cases
│   └── common/
├── data/
│   ├── repository/                   # HomeRepositoryImpl, AddSnapshotRepositoryImpl, ProfileRepositoryImpl
│   ├── network/data_source/          # RemoteDatabaseService + Impl
│   ├── paging/
│   └── common/
└── di/                               # HomeRepositoryModule, InteractionModule, DispatchersModule
```

**Structure Decision**: Stay with single `:app` module; Gradle extraction deferred (research.md R7).

## Phase 0 & Phase 1 outputs

| Artifact | Path | Status |
|----------|------|--------|
| Research | research.md | Complete |
| Data model | data-model.md | Complete |
| Contracts | contracts/README.md | Complete |
| Quickstart | quickstart.md | Complete |
