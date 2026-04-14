# DailyDose Android Application - [linkedin.com/in/hva94](https://www.linkedin.com/in/hva94/)

## Android Application for sharing daily photo moments. <br>
Kotlin, Clean Architecture, ViewBinding, Dagger Hilt, Firebase Services...

## Architecture

DailyDose uses a **clean layered architecture** with three layers — presentation, domain, and data — and strict dependency rules (presentation→domain→nothing, data→domain).

- [Architecture overview](specs/001-clean-layered-architecture/ARCHITECTURE.md) — layer diagram, key files, dependency rule
- [Quickstart: adding a feature](specs/001-clean-layered-architecture/quickstart.md) — how to classify and implement a change
- [Capability map](specs/001-clean-layered-architecture/capability-map.md) — flows mapped to owning layers
- [Feature spec](specs/001-clean-layered-architecture/spec.md) — full requirements and success criteria

## Work in progress... 👨‍💻

- Master branch is the latest stable version.
- All updates will be documented in this README.
- Versioning: `Major.Minor.Patch` (e.g. `1.0.0`)

## 1.1.5
- Add Snapshot flow moved behind clean architecture: `AddSnapshotRepository`, `CreateSnapshotUseCase`, and `PostSnapshotOutcome` for upload vs. save failures with progress reporting.
- Profile screen logic moved into `ProfileViewModel` with domain use cases (`GetUserProfileUseCase`, `UpdateProfileNameUseCase`, `UploadProfilePhotoUseCase`) backed by `ProfileRepository` and `UserProfile`.
- `RemoteDatabaseService` extended with `publishSnapshot` implemented in `RemoteDatabaseServiceImpl` (Storage upload + Realtime Database write).
- Add screen UI state modeled as `AddPostUiState` (idle, uploading with percent, success, and failure variants).
- Dagger modules updated to bind the new repositories and interactors.
- Unit tests added for home-domain interactors: delete snapshot, get snapshots, and toggle like.
- Architecture specification pack added under `specs/001-clean-layered-architecture/` (overview, plan, tasks, contracts, and contributor docs).

## 1.1.4
- App versioning and dependencies updated.
- SDK versions updated (compileSdk 36, targetSdk 36).
- In-app update system implemented using Firebase Remote Config and DownloadManager.
- Profile name editing functionality added.
- Share functionality enhanced to include images.
- Home screen pagination and loading states improved.
- Add Snapshot screen updated to use modern Photo Picker.
- Layouts and UI resources updated for Profile, Add, and Home screens.
- Firebase Hosting configuration and landing page added.
- Code style and project structure settings updated.

## 1.1.2
- Master versioning started.
- Dark/Light mode handling.
- Like button updated.
- Default snapshot title added.
- Share button, first version added.

# 💬 Contact me on [linkedin.com/in/hva94](https://www.linkedin.com/in/hva94/)