# Implementation Plan: Reactions and Replies Upgrade for Snapshots

**Branch**: `003-snapshot-reactions-replies` | **Date**: 2026-04-17 | **Spec**: [/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/spec.md)  
**Input**: Feature specification from `/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/spec.md`

## Summary

Upgrade the existing Android snapshot feed from a like-only interaction model to a richer post-interactions model that supports single-emoji reactions, flat chronological replies, and summary activity counts on each snapshot. The implementation should preserve visibility of older posts during migration, replace the like UI with reactions, and extend the existing offline-capable feed so reactions and replies can appear pending locally and reconcile when connectivity returns.

## Technical Context

**Language/Version**: Kotlin on JVM 17, Android app with minSdk 24 / targetSdk 36  
**Primary Dependencies**: Jetpack Compose Material3, Paging 3, Hilt, Firebase Realtime Database, Firebase Storage, Coil, AndroidX Room  
**Storage**: Firebase Realtime Database for snapshots/reactions/replies and summary fields; Firebase Storage for post images; Room plus app-private files for retained feed and offline action state  
**Testing**: JUnit 4, MockK, kotlinx-coroutines-test, Room testing, Paging testing, Android instrumented tests, Compose UI tests  
**Target Platform**: Android phones/tablets running API 24+  
**Project Type**: Single-module Android mobile app  
**Performance Goals**: Feed cards should continue to render from summary fields without opening reply data; reaction toggles and reply submissions should reflect local intent immediately; older posts missing new fields must still render without blocking errors  
**Constraints**: Preserve current authentication and post creation flows, keep a single feedback concept in UI, support offline pending behavior for reactions/replies, retain chronological replies, and avoid regressions to the existing home feed/offline cache  
**Scale/Scope**: One feature slice inside `app/`, centered on snapshot interactions for a private two-user app; changes span home feed cards, reply surface, repository/domain boundaries, Firebase snapshot structure, and offline cache metadata

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Status**: PASS
- `.specify/memory/constitution.md` is still the default placeholder template, so there are no ratified project-specific gates to enforce yet.
- Working standard applied for this plan: keep the change incremental, preserve post visibility during migration, and add verification coverage for reaction, reply, and offline reconciliation behavior before implementation is considered complete.

## Project Structure

### Documentation (this feature)

```text
/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── snapshot-interactions-ui.md
└── tasks.md
```

### Source Code (repository root)

```text
/Users/henryvazquez/StudioProjects/DailyDose/
├── app/
│   ├── build.gradle
│   ├── src/main/java/com/hvasoft/dailydose/
│   │   ├── data/
│   │   │   ├── auth/
│   │   │   ├── local/                      # extend offline feed storage and add pending interaction queue
│   │   │   ├── network/data_source/        # evolve Firebase interaction reads/writes
│   │   │   ├── network/model/              # snapshot/reaction/reply DTOs
│   │   │   └── repository/                 # home/interactions orchestration
│   │   ├── di/
│   │   ├── domain/
│   │   │   ├── interactor/home/
│   │   │   ├── model/
│   │   │   └── repository/
│   │   └── presentation/
│   │       ├── HostActivity.kt
│   │       └── screens/home/               # feed card interaction UI + reply sheet
│   ├── src/main/res/values/
│   ├── src/test/java/com/hvasoft/dailydose/
│   │   ├── data/
│   │   ├── domain/interactor/home/
│   │   └── presentation/screens/home/
│   └── src/androidTest/java/com/hvasoft/dailydose/
└── specs/
```

**Structure Decision**: Keep the work inside the existing single-module Android app. Extend the current home-feed stack rather than creating a new feature module: evolve `Snapshot`/DTO models, replace like-specific repository/use-case seams with post-interaction seams, add local persistence for pending offline actions, and keep UI changes concentrated in `presentation/screens/home` with minimal host-activity updates.

## Phase 0 Research Summary

- Replace like-specific repository and use-case methods with a post-interactions abstraction so reactions and replies fit cleanly beside the existing feed behavior.
- Store reaction and reply detail under each snapshot while treating `reactionCount`, `reactionSummary`, and `replyCount` as the feed-facing source of truth for fast rendering and migration safety.
- Denormalize reply author display fields at creation time so reply rows remain readable even if user profile data is unavailable later.
- Add a Room-backed pending action queue for offline reaction/reply intent, then reconcile it on refresh or reconnect so the UI can show pending state without blocking local interaction.
- Treat `likeList` as one-way legacy migration input to heart reactions and remove the like UI immediately to avoid duplicate concepts.
- Present replies in a modal reply sheet launched from the feed so users keep post context and existing bottom-navigation flow remains simple.

## Phase 1 Design Summary

- Data model is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/data-model.md).
- User-facing interaction contract is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/contracts/snapshot-interactions-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/contracts/snapshot-interactions-ui.md).
- Manual implementation and validation flow is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/003-snapshot-reactions-replies/quickstart.md).

## Post-Design Constitution Check

- **Status**: PASS
- Design remains incremental and contained within the existing app boundaries.
- The chosen approach preserves the original spec as source history and introduces no governance conflicts beyond the still-unratified constitution template.

## Complexity Tracking

No constitution violations or extra complexity exceptions were required for this design.
