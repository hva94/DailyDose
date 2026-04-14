# Internal layer contracts — DailyDose

No public HTTP API. "Contracts" here are the stable boundaries between layers.

## Repository ports (domain → implemented in data)

| Port | Location | Implementations |
|------|----------|-----------------|
| `HomeRepository` | `domain/repository/HomeRepository.kt` | `data/repository/HomeRepositoryImpl.kt` |
| `AddSnapshotRepository` | `domain/repository/AddSnapshotRepository.kt` | `data/repository/AddSnapshotRepositoryImpl.kt` |
| `ProfileRepository` | `domain/repository/ProfileRepository.kt` | `data/repository/ProfileRepositoryImpl.kt` |

**Rule**: Presentation calls use cases, not repositories directly.

## Use case ports (presentation → domain)

| Use case | Location | Responsibility |
|----------|----------|----------------|
| `GetSnapshotsUseCase` | `domain/interactor/home/` | Paged feed stream |
| `ToggleUserLikeUseCase` | `domain/interactor/home/` | Like / unlike |
| `DeleteSnapshotUseCase` | `domain/interactor/home/` | Delete snapshot |
| `CreateSnapshotUseCase` | `domain/interactor/add/` | Upload image + save record |
| `GetUserProfileUseCase` | `domain/interactor/profile/` | Load user profile |
| `UpdateProfileNameUseCase` | `domain/interactor/profile/` | Update display name |
| `UploadProfilePhotoUseCase` | `domain/interactor/profile/` | Upload avatar + save URL |

## Remote data source (data-internal)

| Type | Role |
|------|------|
| `RemoteDatabaseService` | Abstraction over Firebase operations |
| `RemoteDatabaseServiceImpl` | Concrete Firebase access |

**Rule**: Presentation must not reference these types.
