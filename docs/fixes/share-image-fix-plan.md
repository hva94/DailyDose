# Restore Image-First Sharing in Home Feed

## Summary
Rework `shareSnapshot` in [HomeScreen.kt](/Users/henryvazquez/StudioProjects/DailyDose/app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt) so it always shares a real image stream, matching the old `HomeFragment` behavior. The key change is to stop sharing the retained offline file directly and instead always create a dedicated shareable cache copy before building the `ACTION_SEND` intent.

## Key Changes
- Replace the current split behavior in `shareSnapshot(...)`:
  - Remove the branch that directly wraps `snapshot.localPhotoPath` with `FileProvider`.
  - Keep a single path that prepares a share-specific file in `cache/shared_images/` and returns its `content://` URI.
- Base the new implementation on the old fragment behavior:
  - If a retained local image exists, copy that file into `cache/shared_images/{snapshotKey}.jpg` and share the copy.
  - If no retained local image exists and remote fallback is allowed, fetch/decode the remote image and write it into the same share cache location.
  - If neither source is available, keep the current offline error behavior instead of attempting to share a path/string.
- Keep `ACTION_SEND` image sharing explicit:
  - `type = "image/*"`
  - `EXTRA_STREAM = shareable content URI`
  - `clipData = ClipData.newUri(...)`
  - `FLAG_GRANT_READ_URI_PERMISSION`
  - Preserve `EXTRA_TEXT` only as optional companion text, not as a substitute for the image.
- Extract the preparation logic into a small helper so the behavior is decision-free:
  - `prepareShareableImageUri(context, snapshot, allowRemoteFallback): Uri`
  - Internally delegates to:
    - copy retained local file to share cache, or
    - fetch/decode remote image into share cache.
- Keep the existing `FileProvider` authority and `cache-path` setup; no manifest/provider shape change is needed.

## Important Behavior / Interface Changes
- `shareSnapshot(...)` becomes image-first:
  - If the snapshot image is available locally or remotely, it must always produce a cache-backed `content://` URI for the actual image bytes.
  - It must never share the retained offline file directly.
- `createShareableImageUri(...)` should be widened from “remote URL only” to “prepare from local file or remote source”.
- `canShareImage(...)` should stay aligned with actual availability:
  - share is allowed only when a real image source exists now
  - no “path-only” or accidental text-only share for image posts

## Test Plan
- Add unit coverage around the share preparation helper:
  - local retained image -> returns a `content://` URI backed by a file in `cache/shared_images`
  - remote image with online fallback -> returns a `content://` URI backed by a generated cache copy
  - missing image while offline -> fails with the existing share error path
- Add a focused UI/intent test if practical:
  - tapping share on a retained offline image builds an `ACTION_SEND` intent with `image/*` and `EXTRA_STREAM`
  - the shared URI points to the share cache copy, not the original retained file path
- Regression-check the old user-visible behavior:
  - WhatsApp / Messages / compatible share target receives an actual image attachment, not a route/path string

## Assumptions
- The desired behavior is the same as the old fragment implementation: always share the actual image payload through a cache-backed `content://` URI.
- Keeping companion text is fine, but only when the image attachment is present for image posts.
- Offline support should remain intact: if the image was retained locally, sharing must still work without network.
