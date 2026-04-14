# Contributor self-test — DailyDose architecture

Answer each question using only [`ARCHITECTURE.md`](./ARCHITECTURE.md), [`quickstart.md`](./quickstart.md), [`capability-map.md`](./capability-map.md), and the source files under `app/src/main/java/com/hvasoft/dailydose/`.

---

## Exercise 1 — Rule-only change

**Scenario**: The product team wants to add a rule: *a user may not post more than 10 snapshots per day.*

- In which layer does the limit check belong?
- Which file(s) would you create or modify?
- Which layers should **not** change?

**Answer key**: The rule belongs in **domain** — create or extend a use case in `domain/interactor/add/CreateSnapshotUseCaseImpl.kt`. `data/` can surface the current count via `AddSnapshotRepository`. `presentation/` should not change unless there is a new UI state to display a limit-reached message (a new `AddPostUiState` variant is fine; Firebase logic in the fragment is not).

---

## Exercise 2 — UI-only change

**Scenario**: Design wants to replace the Add screen's dialog image picker with an inline bottom sheet.

- In which layer does this change belong?
- Which file(s) would you modify?
- Should `domain/` or `data/` change?

**Answer key**: This is entirely a **presentation** change. Modify `presentation/screens/add/AddFragment.kt` (replace the `MaterialAlertDialogBuilder` with a bottom sheet). `AddViewModel.kt` may get a small update if the sheet introduces a new UI state. `domain/` and `data/` must not change — the contract (`CreateSnapshotUseCase`) stays the same.

---

## Exercise 3 — Persistence-only change

**Scenario**: Engineering wants to swap the user profile storage from Firebase RTDB to a new REST endpoint.

- In which layer does this change belong?
- Which file(s) would you create or modify?
- What stays the same?

**Answer key**: This is a **data** change. Modify `data/repository/ProfileRepositoryImpl.kt` (replace Firebase RTDB calls with the new HTTP client). `domain/repository/ProfileRepository.kt` (the port) stays identical — it is the contract both sides agree on. `domain/interactor/profile/` use cases and `presentation/screens/profile/` must not change as long as `ProfileRepository`'s behaviour is preserved.

---

## Scoring

- 3/3: Ready to contribute independently.
- 2/3: Review [`quickstart.md`](./quickstart.md) step 1 (classify) again.
- 1/3 or 0/3: Read [`ARCHITECTURE.md`](./ARCHITECTURE.md) fully, then retry.
