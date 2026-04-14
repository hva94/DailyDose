# Boundary exceptions — DailyDose

Known deviations from strict layering. Each must be reviewed when the relevant area is next touched.

| Location | Violation | Status / plan |
|----------|-----------|---------------|
| `domain/repository/HomeRepository.kt` | Uses `androidx.paging.PagingData` in domain port | Accepted (research.md R2); defer until a domain-owned paging abstraction is warranted |
| `presentation/HostActivity.kt` | Direct Firebase Auth, RTDB, Remote Config | Deferred — cross-cutting bootstrap; migrate behind a facade when auth is next touched |
| `presentation/screens/profile/ProfileFragment.kt` | `AuthUI` sign-out call | Documented acceptable UX boundary; RTDB / Storage now moved to `ProfileViewModel` + `ProfileRepository` |
| `data/common/Constants.currentUser` | `FirebaseUser` object stored in a `data` layer constant used everywhere | Accepted short-term; replace with a domain-level `CurrentUserProvider` port in future |
