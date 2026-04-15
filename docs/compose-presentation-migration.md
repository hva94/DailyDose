# Compose Presentation Migration

## What Changed

- Replaced the `HostActivity` Fragment/ViewBinding presentation path with a Compose-first host built on `ComponentActivity` and `setContent`.
- Replaced the XML-backed Home, Add, and Profile screens with Compose screen implementations while preserving:
  - FirebaseUI authentication handoff
  - Remote Config update prompt/download flow
  - top-level Home/Add/Profile navigation and Home start destination
  - add-post validation, upload progress, and post success handoff back to Home
  - profile name/photo editing and sign-out
  - Home like/delete/share behavior and empty/error/loading states
- Removed the legacy Fragment, layout, RecyclerView adapter, ViewBinding, and bottom-nav XML presentation code after the Compose replacement compiled.
- Intentionally removed legacy image zoom from the Home feed during the migration.

## Why It Changed

- The app had Compose enabled in Gradle but still relied on a full view-era presentation stack.
- Migrating the host and primary user journeys to Compose removes duplicate UI paths and makes the presentation layer consistent with the project’s current direction.
- Keeping ViewModels, interactors, repositories, Firebase integration, and file-provider behavior stable minimized migration risk.

## Files Touched

- Host and theme:
  - `app/src/main/java/com/hvasoft/dailydose/presentation/HostActivity.kt`
  - `app/src/main/java/com/hvasoft/dailydose/presentation/MainDestination.kt`
  - `app/src/main/java/com/hvasoft/dailydose/presentation/theme/`
- Compose screens:
  - `app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeScreen.kt`
  - `app/src/main/java/com/hvasoft/dailydose/presentation/screens/add/AddScreen.kt`
  - `app/src/main/java/com/hvasoft/dailydose/presentation/screens/profile/ProfileScreen.kt`
- Presentation support:
  - `app/src/main/java/com/hvasoft/dailydose/presentation/screens/common/PresentationFormatters.kt`
  - `app/src/main/java/com/hvasoft/dailydose/presentation/screens/home/HomeViewModel.kt`
- Tests:
  - `app/src/androidTest/java/com/hvasoft/dailydose/presentation/screens/add/AddScreenContentTest.kt`
  - `app/src/test/java/com/hvasoft/dailydose/presentation/screens/common/PresentationFormattersTest.kt`

## Legacy Files Removed

- Fragments:
  - `AddFragment.kt`
  - `HomeFragment.kt`
  - `ProfileFragment.kt`
- Legacy presentation helpers:
  - `HomeState.kt`
  - `HomePagingAdapter.kt`
  - `OnClickListener.kt`
  - `HomeFragmentListener.kt`
  - `HostActivityListener.kt`
  - `UIExtensionFunctions.kt`
- XML/resources:
  - `activity_host.xml`
  - `fragment_add.xml`
  - `fragment_home.xml`
  - `fragment_profile.xml`
  - `empty_state_home.xml`
  - `item_snapshot.xml`
  - `bottom_nav_menu.xml`

## Dependency Cleanup Summary

- Added:
  - `androidx.activity:activity-compose`
  - Compose BOM and core Compose UI/runtime/tooling dependencies
  - `androidx.lifecycle:lifecycle-runtime-compose`
  - `androidx.paging:paging-compose`
  - `io.coil-kt:coil-compose`
  - Compose UI test dependencies
- Removed:
  - `androidx.fragment:fragment-ktx`
  - `androidx.constraintlayout:constraintlayout`
  - `androidx.navigation:navigation-fragment-ktx`
  - `androidx.navigation:navigation-ui-ktx`
  - Room runtime/ktx/compiler
  - Glide and Glide compiler
  - `com.github.MikeOrtiz:TouchImageView`
  - ViewBinding build feature
- Retained intentionally:
  - `androidx.appcompat:appcompat`
  - Material Components theme support used by the existing app theme and FirebaseUI auth theme

## Validation Steps And Results

- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebug`
  - Passed
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:testDebugUnitTest`
  - Passed
- `env JAVA_HOME=/Users/henryvazquez/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home GRADLE_USER_HOME=/tmp/dailydose-gradle-home-8_11 sh gradlew --no-daemon :app:assembleDebugAndroidTest`
  - Passed when run sequentially
  - An earlier parallel validation attempt hit a Kotlin build-cache collision and was discarded

## Remaining Follow-Up Work

- Run instrumentation tests on a device or emulator if end-to-end visual interaction coverage is needed beyond assembly.
- Reassess the in-app update install handoff path if the product should automatically continue from download completion into APK installation.
- Consider follow-up refinement of Compose previews and screen-specific UI tests once the new presentation layer settles.
