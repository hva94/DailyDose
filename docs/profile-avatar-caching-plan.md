# Add Local-First Profile Avatar Caching

## Summary
- Make the Profile screen use a retained local avatar file, not just the backend `photoUrl`, so it behaves like Home for the current user.
- Share the retained current-user avatar asset with Home using the same asset identity, so both screens stay visually consistent.
- Keep the current image visible during upload; only switch to the new avatar after upload succeeds.
- Remove the duplicate avatar rendering in Profile and use a single image composable with the same neutral placeholder style as Home.

## Key Changes
### Profile data and cache behavior
- Extend the Profile model returned to the UI so it carries both:
  - the backend `photoUrl`
  - the retained local avatar path for the current user, if present
- Add a Profile-side avatar model helper equivalent to Home’s avatar selection:
  - prefer retained local file first
  - then backend `users/{uid}/photoUrl`
  - then `CurrentUserSnapshot.photoUrl` as a last fallback
  - then neutral placeholder
- Reuse the existing `offline_media_assets` table and `FeedAssetStorage`; do not add a new table or schema.
- Use the shared asset id `avatar-$accountId-$userId` for the current user so Profile and Home reference the same retained avatar asset.

### Repository and upload flow
- Update `ProfileRepositoryImpl` so `loadUserProfile()` resolves the current user’s retained avatar asset from `offline_media_assets` and includes that local path in the returned profile data.
- After profile-photo upload succeeds and the backend user record is updated, immediately retain the new remote avatar URL into the shared avatar asset entry and return the refreshed profile data/state.
- If the new avatar retention download fails after a successful upload, keep the previous retained local avatar file if it is still valid rather than dropping straight to a broken state.
- Do not change the upload UX to a local pre-success preview: keep the current avatar visible until upload success, then swap to the new cached avatar.

### Profile UI behavior
- In `ProfileScreen`, replace the stacked `AsyncImage` + `SubcomposeAsyncImage` with a single `SubcomposeAsyncImage`.
- Render the avatar from the profile’s preferred image model, not directly from the raw remote URL.
- Keep shimmer while loading, and use the same neutral avatar placeholder on empty/error instead of the broken-image look.
- Preserve the existing progress indicator during upload.

## Public API / Type Changes
- Extend `UserProfile` with `localPhotoPath: String?` and a small helper like `preferredPhotoModel()` or equivalent UI-facing image selection logic.
- Update `ProfileRepository.loadUserProfile()` so it returns profile data with retained-avatar information, not only the backend URL.
- Add internal Profile repository support for resolving and updating the shared current-user avatar asset in `offline_media_assets`.
- No Room schema change is required; this plan reuses the existing asset store and avatar asset id convention.

## Test Plan
- Unit test: loading Profile returns retained local avatar path when the shared avatar asset exists.
- Unit test: Profile falls back from retained local file to backend `photoUrl`, then to auth photo URL, then placeholder.
- Unit test: successful photo upload updates the backend record and refreshes the shared retained avatar asset.
- Unit test: if post-upload avatar retention fails, the previous valid retained avatar remains usable.
- Unit test: Profile upload keeps showing the old avatar until success, then switches to the new one.
- UI/unit test: Profile renders only one avatar image composable path and uses the neutral placeholder on empty/error.
- Regression test: Home can continue using the same shared current-user avatar asset without behavior changes.

## Assumptions
- The current user’s Profile avatar should be available offline from a retained local file, matching Home’s local-first behavior as closely as possible.
- The retained avatar asset is shared with Home through the existing `avatar-$accountId-$userId` asset id.
- Upload behavior stays conservative: no immediate local preview before success.
- The app should not introduce a new dedicated Profile cache store when the existing offline asset system already fits the need.
