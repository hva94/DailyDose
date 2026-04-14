# Regression baseline — DailyDose

Manual (and optional automated) checks required before merging structural PRs. Aligns with spec FR-007 / SC-001.

## Scenarios

### §1 — Host bootstrap & auth (`HostActivity.kt`)

| # | Action | Expected |
|---|--------|----------|
| 1.1 | Cold-start app | Splash / host loads without crash |
| 1.2 | Complete sign-in if prompted | User reaches main navigation |
| 1.3 | Remote Config / auth listener active | No ANR; bottom navigation usable |

### §2 — Home feed (`HomeFragment.kt`)

| # | Action | Expected |
|---|--------|----------|
| 2.1 | Open Home tab | Feed loads or shows empty/error state without crash |
| 2.2 | Scroll feed | Paging loads next page; no duplicate fatal errors |
| 2.3 | Toggle like on an item | UI reflects like state |
| 2.4 | Delete own item | Item removes or shows expected error |

### §3 — Add snapshot (`AddFragment.kt`)

| # | Action | Expected |
|---|--------|----------|
| 3.1 | Open Add tab | Layout visible; select image enabled |
| 3.2 | Pick image (gallery) | Preview shows; title field appears |
| 3.3 | Post with valid title | Progress bar → success message; home refreshes |
| 3.4 | Post with empty title | Validation error shown; no silent post |

### §4 — Profile (`ProfileFragment.kt`)

| # | Action | Expected |
|---|--------|----------|
| 4.1 | Open Profile tab | Name, email, avatar area load |
| 4.2 | Change display name and save | Name updates; success feedback |
| 4.3 | Change profile photo | Upload progress; image updates on success |
| 4.4 | Log out | Returns to signed-out state |

---

## Baseline run log

| Date | Build | Tester | §1 | §2 | §3 | §4 | Notes |
|------|-------|--------|----|----|----|----|-------|
| 2026-04-13 | debug | CI | ✓ compile+unit | — | — | — | Manual device run required; unit tests pass |

---

## Post-merge verification template

After each structural PR touching `presentation/`, `domain/`, or `data/`:

1. Re-run all scenarios on a debug or release-candidate build.
2. Append a row to the table above: date, branch/SHA, tester, pass (✓) / fail (✗) per section, notes.
3. Do **not** merge to the release branch with any unexplained failure.
