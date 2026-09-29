# Quapp — Progress Tracker

Tick boxes as things land. Commit this file with the work it tracks, so the git history shows when each item was done.

Last updated: 2026-09-29

Team: 2 people. Brent builds the remaining screens and backend, and wires them. Partner improves the UI after handoff.

---

## Milestone 1 — UI and navigation (midterm) ✅ Done

- [x] Resource tokens locked first (`colors.xml`, `dimens.xml`, `themes.xml`)
- [x] Material 3 theme with custom teal palette
- [x] `SystemBars` insets helper (edge-to-edge, keyboard-aware variant)
- [x] Shared screens: Splash, Login, Register, Role Select
- [x] Queuer screens: Browse, Queue Detail, Join, Active Ticket
- [x] Owner screens: Dashboard, Create Queue, Live Console
- [x] RecyclerView + adapter/ViewHolder on Browse, Dashboard, Live Console
- [x] Active Ticket four-state screen with `CountDownTimer` grace period
- [x] Navigation wired with Intent extras (ids, not objects)
- [x] Dark theme fixed (night theme name mismatch)
- [x] Git commit, APK build, `DECISIONS.md` written

---

## Blockers (don't move on until clear)

- [ ] Confirm the R-class ID generation error from Browse is fully resolved and the project builds clean

---

## Milestone 2 — Team setup

- [ ] Push repo to GitHub, confirm `.gitignore` excludes `build/`, `.gradle/`, `.idea/`, `local.properties`
- [ ] Add teammate as collaborator; he clones and builds successfully
- [ ] Agree on branch naming (`feature/<screen>`) and pull-request flow into `main`
- [ ] Agree on string naming (prefix by screen)
- [ ] `CLAUDE.md`, `DECISIONS.md`, `PROGRESS.md` and `DESIGN.md` committed to repo root
- [ ] Decide: `QueueRepository` interface in front of `FakeData`? (see Open questions in DECISIONS)
- [ ] Write partner's UI-polish scope rules (what he may and may not touch)
- [ ] Agree who owns `themes.xml`, `colors.xml`, `dimens.xml` during polish
- [ ] Confirm with instructor that UI improvement counts as individual contribution
- [ ] Decide: hosted backend (Render etc.) or everyone runs it locally

---

## Milestone 3 — Backend and data (finals)

### Settle before writing the backend
- [x] `serviceHours`: free text → real start/end timestamps (MODELS.md schedule)
- [x] `proximityRadiusMeters` as a per-queue field (`joinRadiusMeters`)
- [ ] Constructor's four booleans → Builder or `VerificationConfig`
- [x] Written API contract (endpoints + JSON shapes) matching the model classes (MODELS.md)

### Backend (FastAPI)
Step-by-step plan: `BACKEND.md`. Schema: `MODELS.md` "Database (PostgreSQL)".
- [ ] Auth
- [ ] Queues CRUD
- [ ] Join queue
- [ ] Call next
- [ ] Rolling-average ETA from real service times

### Android integration
- [ ] Replace `FakeData` with Retrofit calls, screen by screen
- [ ] Loading and error states on every network-backed screen
- [ ] Replace `ActiveTicketStore` static field with fetch by ticket id
- [ ] `servedToday` from real data (currently hardcoded 18)
- [ ] Ticket removal by id instead of reference equality
- [ ] `DiffUtil` for list updates
- [ ] Form validation

### Grace period end to end
- [ ] Called state pushed from server (replaces long-press demo trigger)
- [ ] Notification when called
- [ ] Timer against real time

---

## Missing screens and features

All ten built 2026-09-21 and checked on the Pixel 8 emulator; unticked until Brent confirms them on his devices. The same pass also covered four Milestone 3 items (Builder instead of the four booleans, form validation, real `servedToday`, ticket removal by id) — see DECISIONS.md.

- [ ] Profile
- [ ] Queue history
- [ ] Category and location filters
- [ ] Search
- [ ] Cooldown state
- [ ] Active-ticket indicator on Browse
- [ ] Edit / pause / close queue
- [ ] Owner analytics
- [ ] Manual walk-in add
- [ ] Logout and session persistence

---

## Milestone 4 — Partner handoff

- [ ] All screens merged into `main`, building clean
- [ ] Partner clones, builds, and starts UI polish on `feature/ui-<screen>` branches

---

## Evaluation

- [ ] Python simulation harness: generated traffic with injected prank entries
- [ ] Verification strategy comparison (detection rate, false positives, throughput)
- [ ] Forecasting accuracy comparison: prequential error for rolling average vs trained-once model vs online model
- [ ] Wait-time model on FastAPI: `SGDRegressor` + `StandardScaler`, warm start on simulated days, `partial_fit` per served person, saved to disk
- [ ] Simulated days: arrivals, service times by hour and category, no-shows, move-backs, breaks
- [ ] `QueueStats.estimate_source` / `model_samples`; Insights shows where the estimate came from

---

## Design overhaul (canvas "Quapp UI overhaul", Sep 26–27)
Designed on the Claude Design canvas; none of it is built. Decisions are in DECISIONS.md under "Design overhaul on the Design canvas", "Now Serving direction (Sep 28)" (structure still applies) and "Civic Paper direction (Sep 28)".
**Direction chosen (Sep 28): Civic Paper** (canvas "Quapp Civic Paper", written up in `DESIGN.md` in the repo root). The Now Serving and monochrome canvases are reference only.
### Decide first
- [x] Bottom navigation: hybrid. Fragments only for tab screens, in `QueuerHomeActivity` and `OwnerHomeActivity`; everything else stays an Activity
- [x] Proximity check back in scope: yes, per-queue radius, checked once at Join
- [x] Model changes: `Ticket.Status.QUEUE_CLOSED`, `Queue.Status.UPCOMING`, schedule fields replacing `serviceHours`, 8 categories (all in `MODELS.md`)
- [x] Multiple tickets with non-overlapping hours (replaces the one-queue-at-a-time rule)
- [x] Visual direction: Civic Paper, espresso + marigold (Sep 28; replaced Now Serving the same day)
- [x] Called screen stays the one loud moment (whole screen espresso, number and "I'm here" in marigold)
- [x] One logo (ticket mark), light theme only, 12dp spacing step, Upcoming neutral (see DECISIONS.md)
- [x] Settle the data models and FastAPI schema before any coding: `MODELS.md` (Sep 28)
- [x] "I need more time": queuer picks minutes, the estimator turns them into places
- [x] Organizer verification settled (phone check by hand, unverified may post with limits, suspend and revoke)
### Build: foundations first (DESIGN.md sections 2–4, 7, 8, 10)
- [x] Colours: Civic Paper tokens in `colors.xml`; M3 roles mapped in `Theme.Quapp` (light only, parent `Theme.Material3.Light.NoActionBar`); delete `values-night/themes.xml`
- [x] Fonts: DM Serif Display, IBM Plex Sans (400–700), IBM Plex Mono (500/600) in `res/font`; remove `barlow_semibold.ttf`
- [x] Type: `TextAppearance.Quapp.*` styles, M3 roles mapped, custom `textAppearanceTicketNumber` / `Stat` / `StatHero` attrs
- [x] Dimens: add `space_ms` (12dp), radii (`radius_sm/md/sheet`), punch sizes
- [x] Paper grain: `paper_grain.png` tile + `bg_paper` layer-list as `windowBackground`
- [x] `ThemeOverlay.Quapp.Spotlight` and `Theme.Quapp.Called`
- [ ] Punch `EdgeTreatment` (spotlight, nav tab, badge, selected chip) and the dashed tear-line drawable
- [x] Logo: ticket mark vector, splash on paper, adaptive launcher icon, notification small icon
- [x] Queue cards v5 (queuer and owner): status strip, identity, one-line description, footer band; faded paused/closed
- [ ] Ticket details: tear-off stub dock, kept-ticket stub + stamp on outcomes, receipt slip, serial line, barcode
- [ ] Help and support screen; location-declined state on Queue Detail
- [ ] Trust and safety screens: Get verified, organizer verification status, badge sheet, unverified queue note, Report a queue, Account suspended, device limit at signup, remove from line, one-live-queue limit
- [ ] Admin web page on FastAPI: verification review, reports, suspend / revoke
- [x] Queuer home states: first visit (town picker), town picked, in line
- [ ] Owner home: live-queue card, status chips, date / category / sort filters, Today tab
- [x] Create Queue: Schedule section, category sheet, verification switches
- [x] Live Console: Now serving panel as the spotlight (awaiting, confirmed, timed out), queue options sheet, extend closing time
- [ ] Ticket screens: called, served, slot released, queue closed, offline, leave confirmation
- [x] My tickets list (2+ tickets): called card (12) and still-in-line ticket (12b)
- [ ] Notification permission screen and "you're being called" notification with an "I'm here" action
- [ ] Share queue and counter display
- [ ] Nav hosts: `QueuerHomeActivity` + `OwnerHomeActivity` with `BottomNavigationView`; turn Browse, Profile and Owner dashboard into Fragments
### From the second review (open, see DECISIONS.md Open questions)
- [ ] Browse without an account; register at Join (mocked)
- [ ] Verified organizer badge (mocked)
- [ ] Map / directions link and "leave by" hint (mocked)
- [ ] "Bring" line on the ticket (mocked)
- [ ] Forgot password (mocked)
- [ ] Language setting
- [ ] Browse empty / no-results / error states (mocked)
- [x] "I need 10 more minutes" on the ticket (mocked)
- [x] Rename "My ticket" to "My tickets" everywhere (mocked)
---

## Polish and documentation

- [ ] Transitions on Active Ticket state changes (`TransitionManager.beginDelayedTransition`)
- [ ] Paper grain and contrast checked on the Xiaomi (low-end screens show grain darker)
- [ ] `DECISIONS.md` updated at each milestone
- [ ] Final APK build
