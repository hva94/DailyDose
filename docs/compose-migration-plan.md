# DailyDose Compose Presentation Migration Plan

## Summary
- Migrate the app’s only presentation path from `AppCompatActivity` + manual Fragment orchestration + XML/ViewBinding/RecyclerView to a Compose-first host inside the existing `HostActivity`.
- Preserve current product behavior: FirebaseUI auth handoff, Remote Config update prompt/download/install flow, the three top-level sections (`Pool`, `Drop it`, `Myself`), home refresh/share/delete/like behavior, add snapshot validation/upload/progress flow, profile edit/photo upload/sign-out flow, and all existing backend/platform integrations.
- Intentionally remove image zoom during the migration per your choice, so the Compose home feed will use a non-zoomable image and the legacy zoom dependency will be deleted.

## Key Changes
- Keep `HostActivity` as the single entry activity, but replace `ActivityHostBinding`/`activity_host.xml` and fragment transactions with `setContent {}` and a Compose `Scaffold` containing:
  - a Compose `NavigationBar` for the existing three top-level sections
  - activity-owned selected-tab state matching the current start destination (`home`)
  - a Compose `SnackbarHost` replacing fragment/activity snackbar plumbing
  - a Compose `AlertDialog` for the Remote Config update prompt
- Do not introduce `navigation-compose`. Use a simple activity-owned `MainDestination` enum/sealed type because the current app only has three top-level tabs and no nested back stack; this matches the existing behavior and avoids a second navigation model.
- Keep domain/data/use cases stable. Reuse `HomeViewModel`, `AddViewModel`, and `ProfileViewModel` rather than moving repository/platform logic into composables. Compose routes/screens will own only UI-local state such as selected image URI, dialog visibility, text field state, and currently selected tab.
- Replace the legacy screens with Compose equivalents:
  - Home: `LazyColumn` + `paging-compose`, Compose snapshot card, compose empty state, delete confirmation dialog, like/share actions, and refresh behavior when returning to Home or after posting.
  - Add: Compose image picker/camera chooser dialog, title field validation, square image preview, progress indicator, success/failure messaging, and post success handoff back to Home.
  - Profile: Compose profile image, edit/save/cancel name flow, upload progress, image source chooser, sign-out confirmation, and auth-driven reset back to Home.
- Add a minimal Compose theme layer that mirrors current XML colors/typography closely enough for parity, while keeping the existing XML `LoginTheme` for FirebaseUI auth.
- Replace XML/image loading helpers with Compose-friendly equivalents:
  - adopt `coil-compose` for Compose image rendering
  - use the same app cache + `FileProvider` sharing approach for shared images
  - move any remaining non-UI helpers needed by Home sharing/date labels into plain Kotlin utilities
- Remove obsolete presentation code after the Compose path builds and the flows are verified:
  - delete `HomeFragment`, `AddFragment`, `ProfileFragment`
  - delete `HomePagingAdapter`, `OnClickListener`, `HomeFragmentListener`, `HostActivityListener`, and `UIExtensionFunctions` once fully replaced
  - delete `activity_host.xml`, `fragment_home.xml`, `fragment_add.xml`, `fragment_profile.xml`, `empty_state_home.xml`, `item_snapshot.xml`, and `bottom_nav_menu.xml`
- Dependency cleanup to perform as part of the migration:
  - add Compose runtime/UI/tooling dependencies needed for a real Compose app (`activity-compose`, core Compose UI/runtime/tooling, `paging-compose`, lifecycle compose interop, Compose UI test deps)
  - add `coil-compose`
  - remove `androidx.fragment:fragment-ktx`
  - remove `androidx.constraintlayout:constraintlayout`
  - remove `androidx.navigation:navigation-fragment-ktx`
  - remove `androidx.navigation:navigation-ui-ktx`
  - remove `com.github.MikeOrtiz:TouchImageView`
  - remove Glide and its compiler if share/image rendering is fully moved off Glide
  - remove Room runtime/ktx/compiler if the repo-wide usage search stays empty during implementation

## Public Interfaces / Internal Contracts
- No domain or repository interfaces should change.
- Presentation-only changes:
  - introduce a small top-level destination type for Compose tab selection
  - keep existing ViewModel entry points where possible
  - add Compose screen functions and previews for the host, home card/list state, add screen, and profile screen
- Add a markdown migration report at `/Users/henryvazquez/StudioProjects/DailyDose/docs/compose-presentation-migration.md` covering:
  - what changed
  - why it changed
  - files touched/deleted
  - dependency cleanup
  - validation commands/results
  - remaining follow-up work

## Validation Plan
- Baseline already confirmed: `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest` succeeded on April 15, 2026.
- Final implementation validation should run:
  - `:app:testDebugUnitTest`
  - `:app:assembleDebug`
  - `:app:assembleDebugAndroidTest` if Compose UI/instrumentation tests are added
- Add or update tests for the migrated presentation layer:
  - a Compose UI test covering Add-screen validation and post button enable/disable behavior
  - a Compose UI test covering Home empty/loading/content rendering at the composable level
  - a small JVM/unit test for any extracted presentation helper logic such as relative time labels or snackbar/event mapping
- Explicitly report any validation not run, especially emulator/device-only execution of instrumentation tests.

## Assumptions
- Image zoom is intentionally removed from the home feed during this migration.
- The current top-level UX remains a three-tab app with Home as the start destination and no nested navigation stack.
- FirebaseUI auth, Remote Config update handling, camera/gallery contracts, and `FileProvider` sharing remain implemented in the activity/screen layer and are preserved behaviorally.
- The migration will prefer removing duplicate/unused presentation dependencies immediately once the Compose replacement compiles and the affected flows are validated.
