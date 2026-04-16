# Startup-Safe Auth Refactor for `currentUser` Crash

## Summary
Replace the global `lateinit` auth state with a small injected session abstraction, and stop constructing user-scoped Firebase dependencies during Hilt startup. This fixes the crash in `HomeRepositoryModule` at the root and removes the remaining startup-sensitive reads in home/add/profile entry points.

## Key Changes
- Add an `AuthSessionProvider` interface with a Firebase-backed implementation:
  - `fun currentUserOrNull(): FirebaseUser?`
  - `fun currentUserIdOrNull(): String?`
  - `fun requireCurrentUserId(): String`
  - Optional helper for UI: `fun currentUserSnapshotOrNull(): CurrentUserSnapshot?` where `CurrentUserSnapshot` contains `userId`, `displayName`, `email`, `photoUrl`.
- Remove mutable auth state from `Constants.kt`:
  - Delete `lateinit var currentUser`
  - Delete `currentUserIdOrNull()`
  - Keep only path/constants.
- Change DI in `HomeRepositoryModule.kt`:
  - Provide `FirebaseAuth` and `AuthSessionProvider` as singletons.
  - Remove `providesSnapshotsStorage()` entirely.
  - Keep only root storage `snapshots/` as a singleton dependency.
- Refactor `RemoteDatabaseServiceImpl` to derive user-scoped storage at call time:
  - Constructor takes root snapshots storage plus `AuthSessionProvider`.
  - `publishSnapshot(...)` writes to `snapshots/{currentUserId}/{snapshotKey}` using the session provider, not a DI-time user.
  - `deleteSnapshot(snapshot)` deletes from `snapshots/{snapshot.idUserOwner}/{snapshot.snapshotKey}` so it does not depend on ambient auth state.
  - `toggleUserLike(...)` resolves the current user id from `AuthSessionProvider`; if absent, return a controlled failure (`IllegalStateException("No signed-in user")`) instead of crashing.
- Move auth ownership out of Compose routes:
  - `HostActivity` stops assigning `Constants.currentUser`; `ensureCurrentUserRecord` takes the `FirebaseUser` from the auth listener as a parameter.
  - `HomeViewModel` injects `AuthSessionProvider` and uses it for `fetchSnapshots()` / `retrySync()` guards.
  - `AddViewModel.postSnapshot(...)` stops accepting `userId`; `CreateSnapshotUseCase` and `AddSnapshotRepository.publishSnapshot(...)` drop the `userId` parameter and resolve it internally through the session provider.
  - `ProfileRoute` stops reading `Constants.currentUser` in `rememberSaveable` and effects. `ProfileViewModel` owns current-user lookup via `AuthSessionProvider` and exposes current profile/load/update/upload actions without the route passing user id or auth fallback strings.
- Update helper logic that depends on “current user”:
  - `Snapshot.isLikedByCurrentUser()` and `isCurrentUserOwner()` should no longer read globals. Either pass `currentUserId` from the caller, or replace them with `snapshot.isLikedBy(userId: String?)` / `snapshot.isOwnedBy(userId: String?)`.
  - Use the session provider in the UI layer to supply the current id when rendering actions.

## Important Interface Changes
- New: `AuthSessionProvider` and `CurrentUserSnapshot`.
- Changed: `CreateSnapshotUseCase.invoke(...)` removes `userId`.
- Changed: `AddSnapshotRepository.publishSnapshot(...)` removes `userId`.
- Changed: profile-facing view-model API should become current-session based:
  - `loadCurrentProfile()`
  - `updateCurrentDisplayName(newName: String)`
  - `uploadCurrentProfilePhoto(imageUri: Uri, currentUserName: String)`
- Changed: snapshot helper functions accept a `userId` argument instead of consulting global state.

## Test Plan
- Add a startup regression test that launches the app with `FirebaseAuth.currentUser == null` and verifies the app reaches sign-in flow without throwing `UninitializedPropertyAccessException`.
- Add unit tests for `RemoteDatabaseServiceImpl`:
  - construction does not require a signed-in user
  - `publishSnapshot` fails gracefully when no session exists
  - `toggleUserLike` fails gracefully when no session exists
  - `deleteSnapshot` uses `snapshot.idUserOwner` for the storage path
- Update/add view-model tests:
  - `HomeViewModel.fetchSnapshots()` and `retrySync()` no-op safely when signed out
  - `AddViewModel.postSnapshot(...)` emits a controlled failure instead of crashing when no user is available
  - `ProfileViewModel.load/update/upload` emit failure/no-op when signed out and succeed when the provider returns a user
- Update any snapshot UI tests to pass explicit `currentUserId` into ownership/like helpers.

## Assumptions And Defaults
- Signed-out behavior stays the same: the app still requires sign-in and immediately launches FirebaseUI auth.
- Mutating actions are signed-in only. If the session is missing, they should fail predictably and surface an existing generic/auth error instead of crashing.
- Storage layout remains `snapshots/{userId}/{snapshotKey}`.
- This pass is intentionally staged: it removes `Constants.currentUser` completely and routes auth-sensitive startup/home/add/profile flows through `AuthSessionProvider`, but it does not redesign the overall navigation/auth experience.
