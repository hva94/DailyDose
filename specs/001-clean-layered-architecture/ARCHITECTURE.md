# DailyDose — Architecture overview

This document is the entry point for contributors and reviewers. All links below are relative to the repo root.

## The three layers

```
┌──────────────────────────────────────┐
│  presentation/                        │  Fragments, ViewModels, UI state,
│  HostActivity, screens/*             │  Navigation, Adapters, Glide
├──────────────────────────────────────┤
│  domain/                              │  Use cases, repository ports,
│  interactor/*, repository/*, model/* │  domain models — NO Android UI,
│                                      │  NO Firebase SDK
├──────────────────────────────────────┤
│  data/                                │  Repository implementations,
│  repository/*, network/*, paging/*   │  Firebase RTDB/Storage, DTOs,
│                                      │  Paging sources
└──────────────────────────────────────┘
```

**Dependency rule**: arrows only point inward.

- `presentation` → `domain` (via use cases)
- `domain` → nothing (pure Kotlin interfaces + models)
- `data` → `domain` (implements ports)

`di/` wires them together using Hilt — it is the only place allowed to reference both sides of a boundary.

## Key files

| File | What it does |
|------|-------------|
| `domain/repository/HomeRepository.kt` | Port for feed paging, like, delete |
| `domain/repository/AddSnapshotRepository.kt` | Port for image upload + snapshot record |
| `domain/repository/ProfileRepository.kt` | Port for user profile load / update |
| `domain/interactor/home/` | GetSnapshots, ToggleLike, DeleteSnapshot use cases |
| `domain/interactor/add/` | CreateSnapshot use case |
| `domain/interactor/profile/` | GetUserProfile, UpdateProfileName, UploadProfilePhoto use cases |
| `data/network/data_source/RemoteDatabaseService.kt` | All Firebase operations in one interface |
| `di/HomeRepositoryModule.kt` | Binds repositories + provides Firebase refs |
| `di/InteractionModule.kt` | Binds all use cases |

## Documented exceptions

See [`boundary-exceptions.md`](./boundary-exceptions.md) for deviations from strict layering and their rationale.

## Capability map

See [`capability-map.md`](./capability-map.md) for the flow → layer ownership table.

## Adding a feature

Follow the steps in [`quickstart.md`](./quickstart.md).

## Checking your understanding

Try the exercises in [`contributor-quiz.md`](./contributor-quiz.md).
