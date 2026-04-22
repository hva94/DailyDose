# DailyDose Android Application - [linkedin.com/in/hva94](https://www.linkedin.com/in/hva94/)

## Android Application for sharing daily photo moments. <br>
Kotlin, Clean Architecture, Jetpack Compose, Dagger Hilt, Firebase Services...

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

## 1.4.0 - Version 5 release.
- Other users' photos now stay blurred until you tap to reveal them.
- Revealed photos now stay visible for you across refreshes and retained offline feed sessions.
- Reactions, replies, and sharing now unlock only after a photo has been revealed.
- The first tap on a hidden photo now reveals it in place instead of opening full screen.
- Hidden-photo controls now use a more muted disabled style for a clearer locked state.
- Reveal handling is now more consistent across reconnects, retained feed state, and account changes.

## 1.3.3
- The Home feed now features a shared daily prompt card that opens a guided posting flow.
- Daily prompt posts now generate time-based titles while still letting you edit them before publishing.
- Daily prompt visibility is now more reliable across day changes and after posting.
- The daily prompt card was redesigned with a cleaner publish-first layout.
- Snapshot cards now show daily prompt context with expandable long titles.
- Emoji reactions now support quick purple-heart taps with a long-press picker for other reactions.

## 1.3.2
- Replies now stay available offline and refresh more reliably when you reopen the feed.
- Pending replies now reconcile more cleanly after the app reconnects.
- The replies sheet now keeps earlier messages visible while newer ones continue loading.
- Snapshot cards now use a cleaner comment action with a compact reply count.
- The Add flow now prevents publishing before a photo is selected.
- Posting from Add is now clearer with a more prominent publish action.

## 1.3.1 - Version 4 release.
- App layouts now draw edge to edge more consistently across modern Android devices.
- The Home feed now keeps cards and empty states clear of the status and navigation bars.
- Add and Profile screens now keep content visible and easier to use while the keyboard is open.
- Reply composer spacing was improved so actions stay reachable above the keyboard.
- Full-screen image viewing now feels cleaner with better system bar handling.

## 1.3.0
- Likes were replaced with emoji reactions, including add, switch, and remove actions from the Home feed.
- Snapshot cards now show reaction summaries and reply counts, with a reply sheet for reading and posting in context.
- Reactions and replies now support offline pending state and reconnect reconciliation.
- Feed refresh and migration handling were hardened for older posts, missing summary fields, and partial profile data.
- Reply avatars now fall back more reliably, and the reply composer now stays above the keyboard.
- Test coverage was expanded for reactions, replies, offline sync, feed refresh regressions, and UI state handling.

## 1.2.1
- Home refresh is now more stable and local-first, so cached content stays visible while sync runs in the background.
- Posting, deleting, and liking now update the Home feed immediately for a faster and smoother feel.
- Sync performance was improved by skipping unnecessary rewrites and re-downloads when feed data has not changed.
- Profile offline support was improved with better fallback for name, email, and cached avatar data.
- Home and Profile image/cache behavior was cleaned up to make offline states more consistent.
- Test coverage was expanded for refresh, offline behavior, and cache-related regressions.

## 1.2.0 - Version 3 release.
- The app was fully migrated from Fragments/XML to **Jetpack Compose**.
- Home, Add, and Profile were rebuilt in Compose while keeping the main app behavior intact.
- Home gained offline support with a local-first cache using **Room** and retained media files.
- Sharing, auth startup, and general app stability were improved during the migration.
- Test coverage was expanded for the new Compose and offline flows.

## 1.1.5
- The Add Snapshot flow was moved into clean architecture with dedicated repository and use case layers.
- Profile logic was also moved into a ViewModel and domain/use case structure.
- Remote publishing and upload progress handling were improved and made more explicit.
- Dependency injection and unit tests were expanded to support the new architecture.
- A new architecture spec pack was added under `specs/001-clean-layered-architecture/`.

## 1.1.4 - Version 2 release.
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

## 1.1.2 - Version 1 release.
- Master versioning started.
- Dark/Light mode handling.
- Like button updated.
- Default snapshot title added.
- Share button, first version added.

# 💬 Contact me on [linkedin.com/in/hva94](https://www.linkedin.com/in/hva94/)
