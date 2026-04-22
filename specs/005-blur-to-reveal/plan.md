# Implementation Plan: Blur-to-Reveal

**Branch**: `005-blur-to-reveal` | **Date**: 2026-04-22 | **Spec**: [/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/spec.md)  
**Input**: Feature specification from `/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/spec.md`

## Summary

Add a per-viewer blur-to-reveal layer to the home feed so other users' images stay hidden until deliberately tapped, while owners keep their images fully visible. The implementation should persist reveal state per user and per post, keep reaction/reply/share controls disabled until reveal, preserve the retained offline feed, and prevent the current image tap from opening the full-screen viewer when a post is still unrevealed.

## Technical Context

**Language/Version**: Kotlin on JVM 17, Android app with minSdk 24 / targetSdk 36  
**Primary Dependencies**: Jetpack Compose Material3, Paging 3, Hilt, Firebase Realtime Database, Firebase Storage, Coil, AndroidX Room  
**Storage**: Firebase Realtime Database for snapshots, daily prompt assignment, user posting metadata, reactions/replies, and per-user reveal records; Firebase Storage for post images; Room plus app-private files for retained feed, offline action state, and cached reveal visibility  
**Testing**: JUnit 4, MockK, kotlinx-coroutines-test, Paging testing, Room testing, Compose UI tests, Android instrumented tests  
**Target Platform**: Android phones/tablets running API 24+  
**Project Type**: Single-module Android mobile app  
**Performance Goals**: Blur-to-visible transitions should feel immediate, reveal state should be available before an image is exposed, and feed pagination should continue without waiting on per-card blocking work  
**Constraints**: Preserve `spec.md` as source documentation, never blur the viewer's own posts, disable reaction/reply/share until reveal, avoid accidental content exposure while reveal state is loading, keep the first unrevealed image tap navigation-free, and avoid regressions to retained offline feed behavior  
**Scale/Scope**: One feature slice inside `app/`, centered on home feed visibility, per-user reveal persistence, interaction gating, Room cache updates, and the current expanded-image behavior for snapshot cards

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Status**: PASS
- `.specify/memory/constitution.md` is still the default placeholder template, so there are no ratified project-specific gates to enforce yet.
- Working standard applied for this plan: keep the change incremental, preserve the existing specification file, prevent hidden content from flashing visible before reveal state is known, and add verification around owner visibility, per-user persistence, interaction gating, and offline-retained behavior before implementation is considered complete.

## Project Structure

### Documentation (this feature)

```text
/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── snapshot-reveal-ui.md
└── tasks.md
```

### Source Code (repository root)

```text
/Users/henryvazquez/StudioProjects/DailyDose/
├── app/
│   ├── src/main/java/com/hvasoft/dailydose/
│   │   ├── data/
│   │   │   ├── auth/
│   │   │   ├── common/
│   │   │   ├── local/                      # extend cached feed state and local reveal persistence
│   │   │   ├── network/data_source/        # load/store per-user reveal records with feed data
│   │   │   ├── network/model/              # keep snapshot DTOs aligned with reveal-related fields when needed
│   │   │   └── repository/                 # home feed reveal orchestration and sync
│   │   ├── di/
│   │   ├── domain/
│   │   │   ├── common/extension_functions/
│   │   │   ├── interactor/home/           # reveal-state reads/writes and feed refresh behavior
│   │   │   ├── model/                     # snapshot visibility state and reveal records
│   │   │   └── repository/
│   │   └── presentation/
│   │       ├── HostActivity.kt
│   │       └── screens/home/
│   │           ├── HomeScreen.kt
│   │           ├── HomeViewModel.kt
│   │           └── ui/                    # snapshot blur overlay, disabled controls, reveal animation
│   ├── src/main/res/values/
│   ├── src/test/java/com/hvasoft/dailydose/
│   │   ├── data/
│   │   ├── domain/interactor/home/
│   │   └── presentation/screens/home/
│   └── src/androidTest/java/com/hvasoft/dailydose/
├── specs/
└── AGENTS.md
```

**Structure Decision**: Keep the work inside the existing single-module Android app. Extend the current home feed stack rather than creating a new module: add per-user reveal state at the repository/data-source boundary, cache that state alongside retained feed data, and keep the visible product changes concentrated in `presentation/screens/home` plus its card UI.

## Phase 0 Research Summary

- Store reveal state as user-scoped metadata keyed by snapshot so reveal persistence remains independent per viewer and does not mutate the shared post record.
- Materialize reveal state in the feed domain model and Room cache so retained offline content can stay hidden until the current viewer is known to have revealed it.
- Reserve the first tap on an unrevealed image for reveal only, then allow existing image-view behavior only after the post is already visible.
- Gate reactions, replies, and sharing from one shared visibility flag so controls cannot drift into mixed enabled/disabled states.
- Treat reveal as a durable, one-way user action: write it optimistically to local state, synchronize it remotely, and ignore duplicate reveal attempts.

## Phase 1 Design Summary

- Data model is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/data-model.md).
- User-facing reveal contract is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/contracts/snapshot-reveal-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/contracts/snapshot-reveal-ui.md).
- Manual implementation and validation flow is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/005-blur-to-reveal/quickstart.md).

## Post-Design Constitution Check

- **Status**: PASS
- The design remains incremental and contained within the existing app boundaries.
- The chosen approach preserves `spec.md`, aligns with the current offline-retained feed design, and avoids governance conflicts beyond the still-unratified constitution template.

## Complexity Tracking

No constitution violations or extra complexity exceptions were required for this design.
