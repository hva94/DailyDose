# Data model: Clean layered architecture foundation

## Domain entities

### Snapshot
| Field | Type | Notes |
|-------|------|-------|
| title | String? | User-provided caption |
| dateTime | Long? | Epoch ms |
| photoUrl | String? | Storage URL |
| likeList | Map<String,Boolean>? | userId→liked |
| idUserOwner | String? | Owner's Firebase UID |
| paginationId | Int | Paging helper |
| snapshotKey | String | Remote record key |
| userName / userPhotoUrl | String? | Denormalized for feed display |
| isLikedByCurrentUser | Boolean | Derived |
| likeCount | String | Display string |

### UserProfile (new)
| Field | Type | Notes |
|-------|------|-------|
| userId | String | Firebase UID |
| displayName | String | Shown in UI |
| photoUrl | String | Avatar URL |
| email | String | Read-only display |

### PostSnapshotOutcome (new enum)
Values: `SUCCESS`, `IMAGE_UPLOAD_FAILED`, `SAVE_FAILED`

## Data entities

### SnapshotDTO
Wire-format mirror of Snapshot for Firebase. `@IgnoreExtraProperties`. `@Exclude` fields not stored. Maps to Snapshot in `data` layer only.

### User
| Field | Notes |
|-------|-------|
| id | Firebase UID (excluded from DB write) |
| userName | Display name |
| photoUrl | Avatar URL |

## Boundary ownership

| Concern | Layer |
|---------|-------|
| Firebase paths, DTO serialization | data |
| Like/delete/create policies | domain |
| HomeState, adapters, navigation | presentation |
| AddPostUiState | presentation |
| ProfileViewModel.Event | presentation |
