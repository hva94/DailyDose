# Implementation Plan: Daily Prompt Posting

**Branch**: `004-daily-prompt-posting` | **Date**: 2026-04-20 | **Spec**: [/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/spec.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/spec.md)  
**Input**: Feature specification from `/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/spec.md`

## Summary

Add a shared daily prompt to the home feed that opens a prompt-aware version of the existing `Drop it` flow, generates an editable time-based default title, and stores prompt context on prompt-driven posts. The implementation should keep `spec.md` intact, preserve normal non-prompt posting, keep prompt visibility personal to each user, and use a shared prompt assignment so both users see the same prompt on the same day.

## Technical Context

**Language/Version**: Kotlin on JVM 17, Android app with minSdk 24 / targetSdk 36  
**Primary Dependencies**: Jetpack Compose Material3, Paging 3, Hilt, Firebase Realtime Database, Firebase Storage, Coil, AndroidX Room  
**Storage**: Firebase Realtime Database for snapshots, daily prompt assignment, and user posting metadata; Firebase Storage for post images; Room plus app-private files for retained feed and offline home state  
**Testing**: JUnit 4, MockK, kotlinx-coroutines-test, Paging testing, Compose UI tests, Android instrumented tests  
**Target Platform**: Android phones/tablets running API 24+  
**Project Type**: Single-module Android mobile app  
**Performance Goals**: The home feed should render the prompt card without blocking feed pagination, prompt-title draft generation should feel instantaneous, and the prompt card should disappear immediately after a successful first post of the day  
**Constraints**: Preserve `spec.md` as source documentation, keep normal posting available when no prompt is present, keep generated titles editable, ensure the same daily prompt is shared by all users, and avoid regressions to existing upload/offline feed behavior  
**Scale/Scope**: One feature slice inside `app/`, centered on home feed prompt visibility, add-screen title orchestration, snapshot metadata, and a lightweight shared daily-prompt assignment for a private two-user app

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Status**: PASS
- `.specify/memory/constitution.md` is still the default placeholder template, so there are no ratified project-specific gates to enforce yet.
- Working standard applied for this plan: keep the change incremental, preserve the existing specification file, avoid introducing prompt-related blocking states, and add verification around shared prompt selection, prompt visibility, and title-generation behavior before implementation is considered complete.

## Project Structure

### Documentation (this feature)

```text
/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── daily-prompt-ui.md
└── tasks.md
```

### Source Code (repository root)

```text
/Users/henryvazquez/StudioProjects/DailyDose/
├── app/
│   ├── src/main/java/com/hvasoft/dailydose/
│   │   ├── data/
│   │   │   ├── auth/
│   │   │   ├── local/                      # retain daily-prompt visibility/supporting cache if needed
│   │   │   ├── network/data_source/        # extend snapshot publish + daily prompt assignment reads/writes
│   │   │   ├── network/model/              # snapshot/user/daily-prompt DTOs
│   │   │   └── repository/                 # home/add repository orchestration
│   │   ├── di/
│   │   ├── domain/
│   │   │   ├── interactor/add/             # extend publish inputs and title generation support
│   │   │   ├── interactor/home/            # prompt loading/completion state for home
│   │   │   ├── model/                      # snapshot + prompt domain models
│   │   │   └── repository/
│   │   └── presentation/
│   │       ├── HostActivity.kt
│   │       ├── MainDestination.kt
│   │       └── screens/
│   │           ├── add/                    # prompt-aware create flow and editable generated titles
│   │           └── home/                   # prompt card, feed prompt rendering, and home state
│   ├── src/main/res/values/
│   ├── src/test/java/com/hvasoft/dailydose/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   └── src/androidTest/java/com/hvasoft/dailydose/
├── specs/
└── AGENTS.md
```

**Structure Decision**: Keep the work inside the existing single-module Android app. Extend the current home and add flows rather than creating a new feature module: add shared daily-prompt assignment and user-posting metadata in the existing Firebase-backed data layer, evolve snapshot publish/read models to carry prompt context, and keep most UI changes concentrated in `presentation/screens/home` and `presentation/screens/add`.

## Phase 0 Research Summary

- Use a shared daily prompt assignment record in Firebase Realtime Database so all users resolve the same combo for the same day without needing a separate backend service.
- Keep the prompt combo catalog app-owned and stable, while storing the selected combo identity and prompt text with the day assignment so prompt rendering does not depend on a full remote catalog.
- Track daily prompt completion from per-user last-post metadata updated on every successful publish, because any first post of the day must hide the prompt card whether the prompt was used or not.
- Store prompt context on prompt-driven snapshots, but treat the optional short answer as title-generation input rather than a separately displayed feed field.
- Resolve the “publish-time title but still editable” tension with a system-managed draft title that keeps updating until the user edits it, then finalizes the timestamp at publish only when the title is still auto-managed.
- Handle long feed titles with collapsed/expanded rendering in the snapshot card so the full stored title remains available without truncating the data.

## Phase 1 Design Summary

- Data model is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/data-model.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/data-model.md).
- User-facing UI contract is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/contracts/daily-prompt-ui.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/contracts/daily-prompt-ui.md).
- Manual implementation and validation flow is defined in [/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/quickstart.md](/Users/henryvazquez/StudioProjects/DailyDose/specs/004-daily-prompt-posting/quickstart.md).

## Post-Design Constitution Check

- **Status**: PASS
- The design remains incremental and contained within the existing app boundaries.
- The chosen approach preserves the existing `spec.md` artifact, adds no governance conflicts beyond the still-unratified constitution template, and keeps the feature optional for users when prompt data is unavailable.

## Complexity Tracking

No constitution violations or extra complexity exceptions were required for this design.
