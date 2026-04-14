# Capability map — DailyDose

| Capability | Presentation | Domain | Data |
|------------|-------------|--------|------|
| Host bootstrap, auth, Remote Config | `presentation/HostActivity.kt` | — | Firebase via activity (exception — see boundary-exceptions.md) |
| Home feed, like, delete | `presentation/screens/home/` | `domain/interactor/home/`, `domain/repository/HomeRepository.kt` | `data/repository/HomeRepositoryImpl.kt`, `data/paging/`, `data/network/` |
| Add snapshot | `presentation/screens/add/AddFragment.kt`, `AddViewModel.kt` | `domain/interactor/add/CreateSnapshotUseCase*`, `domain/repository/AddSnapshotRepository.kt` | `data/repository/AddSnapshotRepositoryImpl.kt`, `data/network/data_source/RemoteDatabaseService*.kt` |
| Profile (name, photo, RTDB user doc, logout) | `presentation/screens/profile/ProfileFragment.kt`, `ProfileViewModel.kt` | `domain/interactor/profile/*`, `domain/repository/ProfileRepository.kt` | `data/repository/ProfileRepositoryImpl.kt` |

## Cross-cutting

- **Firebase AuthUI** sign-in / sign-out: `HostActivity.kt` / `ProfileFragment.kt` — documented exception.
- **Glide** image loading: stays in presentation.
- **Constants.currentUser**: `data/common/Constants.kt` — cross-cutting for now; migrate to a domain-level auth port in a future feature.
