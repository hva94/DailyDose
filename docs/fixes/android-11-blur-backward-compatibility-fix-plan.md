# Android 11 Tap-to-Reveal Blur Backward-Compatibility Plan

## Summary
Implement an API-gated hidden-image rendering path for snapshot cards.

Keep the existing `Modifier.blur(...)` behavior on Android 12+ where Compose blur is supported. For Android 11 and lower, replace the hidden-state image render with a pre-blurred image request through Coil so the image is still obscured before reveal without relying on unsupported platform blur APIs.

This is the safest option because it preserves the current behavior on supported devices, limits the fallback to the hidden-image branch only, and avoids changing reveal state, data flow, or backend behavior.

## Implementation Changes
- Add a small API-level decision in the snapshot image rendering path:
  - Android 12+ (`SDK_INT >= 31`): keep the current Compose `Modifier.blur(Constants.SNAPSHOT_REVEAL_BLUR_DP.dp)`.
  - Android 11 and lower: load the same image model with a blurred Coil request instead of Compose blur.
- Keep the fallback scoped only to the unrevealed hidden state.
  - Revealed images continue using the normal unblurred request.
  - Owner-visible and already-revealed snapshots stay unchanged.
- Build the older-device fallback from the existing `mainImageModel` (`File` or remote URL) so no repository, domain model, or navigation changes are needed.
- Reuse the current overlay and “Tap to reveal” label exactly as-is.
  - No changes to reveal tap behavior.
  - No changes to `hasRevealStarted`, visibility rules, or reveal sync state.
- Encapsulate the branching in the home snapshot UI layer so the rest of the feed remains unaware of API differences.
- Do not change constants unless testing shows the Coil blur radius needs a small calibration to approximate the current 18dp visual treatment on API 26-30.

## Public APIs / Interfaces
No public API, repository, database, or domain-model changes.

Internal UI-only addition:
- Introduce a private/internal helper for “hidden image rendering strategy” in the snapshot card UI layer, or a small helper that builds the blurred Coil request for pre-31 devices.

## Test Plan
- Update/add UI tests for hidden snapshot rendering behavior:
  - Hidden snapshot on API 31+ still shows the reveal overlay and uses the existing hidden-state path.
  - Hidden snapshot on API 26-30 uses the fallback blurred request path instead of Compose blur.
- Add a unit-level test for the API-gated decision helper if extracted.
- Regression-test unchanged behavior:
  - Owner snapshot never shows hidden treatment.
  - Revealed snapshot opens normally and is not blurred.
  - Tapping a hidden snapshot still triggers reveal flow exactly once.
- Manual verification on two targets:
  - Android 11 emulator or device: hidden image is visibly obscured before reveal.
  - Android 16 device/emulator: current blur appearance remains intact.

## Assumptions
- Priority is safety and backward compatibility over pixel-perfect parity with Android 12+ blur.
- Slight visual difference between Compose blur and Coil blur on older devices is acceptable as long as the image is clearly obscured.
- The fallback is only required in the feed card tap-to-reveal treatment; expanded image and other image surfaces remain unchanged.
