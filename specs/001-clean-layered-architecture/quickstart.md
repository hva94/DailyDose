# Quickstart: Adding a feature under presentation / domain / data

## 1. Classify the change

| If the change mainly… | Start in… |
|----------------------|-----------|
| Alters screens, navigation, strings | `presentation/screens/…` |
| Alters rules, validation, orchestration | `domain/interactor/…` |
| Alters Firebase, DTOs, paging, IO | `data/…` |

If it spans layers, note which boundary is crossed in the PR description (FR-005).

## 2. Domain-first sketch

1. Name the use case (verb + noun), e.g. `CreateSnapshot`, `UploadProfilePhoto`.
2. Define or reuse a repository port in `domain/repository/`.
3. Keep domain models free of Android UI and Firebase annotations.

## 3. Data layer

1. Implement the port in `data/repository/` (suffix `Impl`).
2. Map DTO ↔ domain next to the implementation.
3. Keep Paging / Firebase / Room types out of Fragments.

## 4. Presentation layer

1. Inject use cases into the ViewModel with Hilt — avoid injecting `*RepositoryImpl` directly.
2. Expose UI state (`StateFlow` / `SharedFlow`) from the ViewModel.

## 5. DI

1. Add `@Binds` or `@Provides` in `di/` modules.
2. Sanity check: domain packages must not import `presentation` or `com.google.firebase`.

## 6. Verify

1. Run unit tests for new/changed use cases.
2. Run baseline regression before merge to release branch.

## 7. Documentation

1. Update `capability-map.md` when adding or significantly changing a user journey.
