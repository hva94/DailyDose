# Phase 0 Research: Clean layered architecture foundation

## R1 — Layering model and dependency direction

**Decision**: `domain` is the center. `data` implements domain ports (repository interfaces). `presentation` depends on domain via use cases only.

**Rationale**: Matches FR-002–FR-004 and the existing package structure.

**Alternatives considered**: Full Gradle multi-module immediately — rejected (high churn on live app, deferred to R7).

## R2 — AndroidX Paging at the domain boundary

**Decision**: `HomeRepository` in `domain.repository` keeps `PagingData` in its signature — accepted short-term coupling.

**Rationale**: Replacing Paging 3 with a pure-Kotlin abstraction is a large refactor; spec mandates incremental delivery (FR-006).

## R3 — Firebase, Glide, and platform APIs

**Decision**: All Firebase (Realtime Database / Storage / Auth) usage stays under `data`. Glide and ViewBinding stay in `presentation`. DTO→domain mapping happens in `data`, never in Fragments.

## R4 — Dependency injection (Hilt)

**Decision**: Keep Hilt modules in `di` package. Bind implementations→interfaces there. Domain stays free of Hilt annotations on models.

## R5 — Verification and regression (FR-007, SC-001)

**Decision**: Baseline = (1) use-case unit tests with MockK fakes; (2) instrumented smoke paths when feasible; (3) short manual checklist for releases.

## R6 — Capability map (SC-003)

**Decision**: Maintain `capability-map.md` in the spec directory; update it when merging each vertical slice.

## R7 — Future modularization

**Decision**: No new Gradle modules in the first planning wave. Re-evaluate after baselines are stable and one vertical is cleanly migrated.
