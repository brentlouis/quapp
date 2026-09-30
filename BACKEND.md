# Quapp backend: the build plan

How to build the FastAPI server, one step at a time, from an empty `backend/` folder to the demo. Claude writes each step, explained so Brent can defend every line; Brent reviews and runs it before the next step starts (DECISIONS.md "Backend: PostgreSQL and SQLAlchemy, in backend/").

- **The contract is `MODELS.md`.** JSON shapes, enums, endpoints and the database tables all live there. If a step needs something the contract doesn't have, change `MODELS.md` first, then the code.
- **Every step ends with "Done when".** Don't start the next step until it's true, and commit at the end of each one.
- **Test as you go.** Every step with rules gets a pytest file. Run the whole suite before each commit.

---

## The stack

| Piece | Package | Why |
|---|---|---|
| Web framework | `fastapi`, `uvicorn[standard]` | Already known from the RAG project |
| Database | PostgreSQL + `psycopg[binary]` (v3) | Already installed on the desktop |
| ORM | `sqlalchemy` 2.x (the typed `Mapped[...]` style) | The usual FastAPI pairing |
| Settings | `pydantic-settings` | Reads `DATABASE_URL` and friends from `.env` |
| Passwords | `bcrypt` | Use it directly; `passlib` is unmaintained and warns with new bcrypt |
| Admin page | `jinja2`, `python-multipart` | Server-rendered HTML forms |
| Model | `scikit-learn`, `numpy`, `joblib` | `SGDRegressor`, `StandardScaler`, saving to disk |
| Tests | `pytest`, `httpx` | FastAPI's `TestClient` needs `httpx` |

## The folder

```
backend/
  app/
    main.py            the FastAPI app, routers included, /health
    config.py          Settings (DATABASE_URL, TOKEN_BYTES, MODEL_PATH, …)
    database.py        engine, SessionLocal, Base, get_db dependency
    enums.py           every enum from MODELS.md, as Python str Enums
    models.py          SQLAlchemy tables (MODELS.md "Database")
    schemas.py         Pydantic request/response models (MODELS.md JSON)
    deps.py            current_user, require_owner, require_admin
    timeutil.py        MANILA timezone, now(), today()
    routers/
      auth.py          register, login, logout, /me
      queues.py        browse, detail, create, edit, pause/resume/close/extend, stats
      tickets.py       join, my tickets, I'm here, move back, leave
      console.py       line, call next, no-show, walk-ins, remove
      trust.py         reports, verification requests
      admin.py         the admin web page
    services/
      schedule.py      moves a queue UPCOMING → OPEN → CLOSED on time
      line.py          positions, now serving, calling, closing a queue's tickets
      rules.py         join checks: overlap, cooldown, radius, one live ticket
      estimator.py     minutes per person: rolling average or the model
    ml/
      features.py      the model's features for one queue at one moment
      wait_model.py    load/save, predict, partial_fit
    templates/admin/   Jinja2 pages
  tests/
  seed.py              demo data (mirrors FakeData)
  simulate.py          generated traffic for the evaluation
  requirements.txt
  .env.example         DATABASE_URL=postgresql+psycopg://quapp:…@localhost:5432/quapp
```

`.env`, `__pycache__/`, `.venv/` and the saved model file go in `.gitignore`.

---

## Phase 0: Setup

### Step 0.1: The database and the Python environment
- In `psql`: create a user `quapp` with a password, a database `quapp` owned by it, and a second database `quapp_test` for the tests.
- In `backend/`: `python -m venv .venv`, activate it, install the stack, and `pip freeze > requirements.txt`.
- Write `.env` (real password, not committed) and `.env.example` (placeholder, committed).

**Done when:** `psql -U quapp -d quapp` connects, and `python -c "import fastapi, sqlalchemy, psycopg"` runs inside the venv.

### Step 0.2: Hello, phone
- `config.py`: a `Settings` class reading `.env`.
- `main.py`: the app, plus `GET /health` returning `{"status": "ok"}`.
- Run `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`.
- Reach it from the phone over mobile data, the same way as the RAG project (DECISIONS.md "Backend doesn't need shared Wi-Fi"). Write down the exact URL: it becomes the app's base URL.

**Done when:** the phone's browser, on mobile data, shows `{"status":"ok"}`, and `/docs` opens on the desktop.

---

## Phase 1: Data

### Step 1.1: The connection
- `database.py`: `create_engine(settings.database_url)`, `SessionLocal = sessionmaker(...)`, `class Base(DeclarativeBase)`, and a `get_db()` dependency that yields a session and closes it.
- `timeutil.py`: `MANILA = ZoneInfo("Asia/Manila")`, `now()` (aware, UTC is fine), `today()` (the date in Manila). All "today" logic goes through `today()`, never `date.today()`, because the server's clock may not be in Manila time.

**Done when:** `GET /health` also runs `SELECT 1` through a session and still says ok.

### Step 1.2: Enums
- `enums.py`: one `class X(str, Enum)` per enum in MODELS.md, with the exact caps values (`OPEN`, `NO_SHOW`, …). `str` as a base class makes them serialize as plain strings.

**Done when:** each enum's values match the MODELS.md table letter for letter.

### Step 1.3: All seven tables
- `models.py`: `User`, `Token`, `Queue`, `Ticket`, `TicketRemoval`, `VerificationRequest`, `Report`, column by column from MODELS.md "Database (PostgreSQL)":
  - enums: `Enum(QueueStatus, native_enum=False, create_constraint=True)`
  - the CHECK constraints and indexes in `__table_args__`
  - the partial unique index on tickets: `Index(..., unique=True, postgresql_where=text("status IN ('WAITING','CALLED')"))`
  - ids: `default=lambda: str(uuid4())`
- `main.py` (for now): `Base.metadata.create_all(engine)` at startup. Switch to Alembic only if the schema starts changing after real data exists.

**Done when:** `\d tickets` in `psql` shows every column, the CHECKs and the indexes. A test inserts a queue with a 51-character short description and gets an `IntegrityError`.

### Step 1.4: Test setup
- `tests/conftest.py`: point the settings at `quapp_test`, create the tables once, give each test a session inside a transaction that rolls back at the end, and a `client` fixture (`TestClient` with `get_db` overridden to use that session).
- Add factory helpers (`make_user`, `make_queue`, `make_ticket`) so later tests read like the rule they check.

**Done when:** a trivial test that makes a user and reads it back passes, twice in a row, and the second run starts clean.

---

## Phase 2: Accounts

### Step 2.1: Response schemas
- `schemas.py`: Pydantic models for every JSON shape in MODELS.md, starting with `UserOut`.
- Field names are already `snake_case` in Python, so the JSON matches without aliases.
- `model_config = ConfigDict(from_attributes=True)` lets you return SQLAlchemy objects directly.
- Times: a serializer that turns every aware datetime into Manila time, so the app gets `"2026-09-27T10:05:00+08:00"`.

**Done when:** a `User` row converts to JSON with exactly the MODELS.md keys and nothing server-only (no `password_hash`, `device_install_id`, `phone_verified` or `is_admin`).

### Step 2.2: Register and log in
- `POST /auth/register` (name, phone, password, device_install_id):
  - validate the phone like the app does: `09` plus 9 digits (mirror `Validation.java`)
  - password at least 8 characters
  - phone taken → 409
  - two users already on this `device_install_id` → 409, with a code the app can show as the device-limit message
  - hash with `bcrypt.hashpw`, create a token (`secrets.token_urlsafe(32)`), return `{token, user}`
- `POST /auth/login` (phone, password):
  - wrong phone or password → 401, the same message for both
  - suspended → 403 with `suspended_reason` and `suspended_at`, so the app can open Account suspended
- `POST /auth/logout`: deletes the token. **Add it to MODELS.md first.**

**Done when:** tests cover register, a duplicate phone, the third account on one device, a short password, login with a wrong password, and login while suspended.

### Step 2.3: Who's asking
- `deps.py`: `current_user`. It reads `Authorization: Bearer …`, finds the token, loads the user, and returns 401 on a missing or unknown token. If the user is suspended, return 403 (and delete the token).
- `GET /me` returns the user.

**Done when:** `/me` works with a token, returns 401 without one, and returns 403 after the user is suspended mid-session.

### Step 2.4: Demo data
- `seed.py`: recreate what `FakeData` has, so the app looks the same once it's on the server:
  - the Tagbilaran queues and their tickets
  - Brgy. Poblacion Council, verified
  - a suspended account on 09180000000
  - one admin account
- Make it idempotent: wipe the tables and refill them, so `python seed.py` resets a demo in one command.

**Done when:** after `python seed.py`, `psql` shows the same queues the app shows from FakeData.

---

## Phase 3: Queues

### Step 3.1: Browse and detail
- `GET /queues?municipality=&category=&q=`: UPCOMING, OPEN and PAUSED only; `q` matches the name or venue, case-insensitive (`ilike`).
- `GET /queues/{id}`: one queue, CLOSED included (history links to it).
- The computed fields, in `services/line.py`:
  - `people_waiting`: count of WAITING tickets
  - `now_serving`: the ticket number of the most recently called ticket today, or null
  - `organizer_name`, `organizer_verified`: from the organizer's user row (the verified organization name when there is one)
  - `estimated_wait_minutes`: minutes per person × people waiting, with minutes per person fixed at 5 until Step 6.1
- Watch for N+1 queries: Browse must not run one query per card. Count WAITING tickets for all listed queues in one `GROUP BY` query.

**Done when:** Browse with each filter returns the right queues, and the SQLAlchemy echo log shows a fixed number of queries no matter how many queues there are.

### Step 3.2: The schedule
- `services/schedule.py`: `refresh(queue, now)`:
  - UPCOMING becomes OPEN once `start_date` + `opens_at` has passed
  - OPEN or PAUSED becomes CLOSED once `end_date` + `closes_at` has passed, and closing turns every WAITING or CALLED ticket into QUEUE_CLOSED
  - on multi-day queues, hours between closing and the next day's opening count as closed for joining, but the queue stays OPEN
- Call it lazily: before any endpoint reads or changes a queue. No background scheduler to explain or crash. The lag is at most one request.

**Done when:** tests freeze the time (pass `now` in) and check each transition, including closing, which releases tickets as QUEUE_CLOSED and never counts them as no-shows.

### Step 3.3: Creating and running a queue
- `POST /queues`: validate the MODELS.md rules (radius values, dates, hours). If the organizer isn't verified and already has a live queue (UPCOMING, OPEN or PAUSED), return 409 with a code for the one-live-queue sheet.
- `deps.require_owner`: 403 unless `current_user` is the queue's organizer.
- `PATCH /queues/{id}`: details, schedule and checks.
- `POST /queues/{id}/pause` · `/resume` · `/close` · `/extend`:
  - pause sets `paused_at`
  - close sets `closed_at` and releases the line (Step 3.2's code)
  - extend takes a later `closes_at` on the last day

**Done when:** tests cover the one-live-queue limit (and that verified organizers skip it), a non-owner getting 403, and close releasing the line.

---

## Phase 4: Tickets

### Step 4.1: Joining
- `POST /queues/{id}/tickets` with `lat`/`lng` when the queue checks proximity. Checks in `services/rules.py`, each with its own error code for the app:
  1. The queue is OPEN or UPCOMING (joining early) and not PAUSED.
  2. **One live ticket per queue.** The partial unique index backs this up, but check first so the error is readable.
  3. **Overlap:** no other live ticket whose queue's hours overlap this one's on the ticket's day (`service_date`, from DECISIONS.md "A ticket is for one day").
  4. **Cooldown:** 2 no-shows (PRANK removals count, QUEUE_CLOSED doesn't) → blocked until 30 minutes after the second one.
  5. **Radius:** haversine distance from the venue ≤ `join_radius_meters`. The app uses Android's `Location.distanceBetween` (`ProximityCheck.java`), which measures on an ellipsoid, so the two can differ by up to about 0.5%. Give the server a small allowance (say 1%) so nobody the app let through gets refused at the edge.
- Port the overlap and cooldown cases from `TicketRulesTest.java` into the pytest file, so the server keeps the rules the app already tested.
- **Numbering safely:** lock the queue row (`select(Queue).where(...).with_for_update()`), take `next_ticket_number`, increment it, insert the ticket with `line_order = ticket_number`, and commit. Without the lock, two people joining at the same moment can get the same number.

**Done when:** a test per rule, plus a concurrency test in which 20 threads join at once and get 20 different numbers.

### Step 4.2: My tickets, leaving
- `GET /me/tickets?live=true`: WAITING and CALLED tickets, with `position` and `estimated_wait_minutes` computed. `live=false` gives history (everything else, newest first).
- `DELETE /tickets/{id}`: the holder leaves. Only WAITING, and it isn't a no-show.
- `position` in `services/line.py`: the number of WAITING tickets with a smaller `line_order`, plus 1.

**Done when:** three people join, the middle one leaves, and the third one's position goes from 3 to 2.

---

## Phase 5: The console

### Step 5.1: The line
- `GET /queues/{id}/line` (owner):
  - now serving: the CALLED ticket, with `timed_out` true when the grace period is on, `called_at` + 3 min has passed and `here_at` is empty
  - the WAITING tickets in `line_order`
  - phones masked (`0917 ••• 0002`)
  - **Add `timed_out` to MODELS.md first.**
- The organizer decides a timeout is a no-show (the console's timed-out state already asks them). The server never marks it by itself.

**Done when:** the line comes back in call order with masked phones, and `timed_out` turns true 3 minutes after a call with no "I'm here".

### Step 5.2: Calling
- `POST /queues/{id}/call-next`, in one transaction:
  1. the CALLED ticket (if any) becomes SERVED, with `finished_at` set
  2. the first WAITING ticket by `line_order` becomes CALLED, with `called_at` set
  3. return the new now-serving ticket
- `POST /queues/{id}/no-show`: the same, but step 1 marks NO_SHOW.
- `POST /queues/{id}/walk-ins` (name): a ticket with `walk_in = true`, no user, numbered like a join (same lock).
- `POST /tickets/{id}/remove` (reason): status REMOVED, `removal_reason` set, a `ticket_removals` row. PRANK counts toward the cooldown (Step 4.1).

**Done when:** a scripted run of join ×3, call, call, no-show leaves the tickets as SERVED, NO_SHOW and CALLED with the right timestamps, and the stats agree.

### Step 5.3: The queuer's side of a call
- `POST /tickets/{id}/here`: only while CALLED and within the grace window. Sets `here_at`.
- `POST /tickets/{id}/move-back` (`minutes_needed`: 5, 10, 15, 20, 30 or 45):
  - `places = ceil(minutes / minutes_per_person)`, at most the people behind
  - the new `line_order` is the midpoint between the ticket it lands behind and the one after (or last + 1 at the end of the line)
  - `moved_back`, `moved_back_at`; only once, and only while WAITING
- **Preview:** the sheet shows the result before confirming. Add `?dry_run=true`: it returns the new position, wait and "be there by" without saving. **Add it to MODELS.md first.**

**Done when:** tests cover moving back 10 minutes with 2 min per person (5 places), moving back past the end of the line (capped), a second move-back (refused), and the dry run leaving the ticket unchanged.

### Step 5.4: How the phone learns it's been called
- The server can't push to the app yet. Options:
  - **Polling (recommended for now):** My tickets and Active ticket re-fetch every 10 seconds while open, and the console every 5. Simple, nothing new to explain, and fine for a demo.
  - **FCM push:** real notifications with the app closed. Needs a Firebase project, `google-services.json` and a server key. Worth it later if time allows.
- Decide, and add a DECISIONS.md entry.

**Done when:** the decision is written down. Polling needs nothing on the server: the endpoints already exist.

---

## Phase 6: Numbers

### Step 6.1: The rolling average
- `services/estimator.py`: `minutes_per_person(queue, now)`:
  - service times are the gaps between consecutive `called_at` values today
  - gaps over 30 minutes are breaks and are skipped
  - the average of the last 5 gaps; 5 minutes when there are none yet
- Port `RollingAverage.java` and its unit tests (DECISIONS.md "RollingAverage … moves to the FastAPI side as a straight port").
- Every `estimated_wait_minutes` and the move-back switch to it.

**Done when:** the ported tests pass, and the waits on Browse change after a few calls.

### Step 6.2: Stats
- `GET /queues/{id}/stats` (owner): the `QueueStats` fields, counted from today's tickets. `estimate_source` is `ROLLING_AVERAGE` until Phase 9.

**Done when:** after the Step 5.2 script, the stats say 1 served, 1 no-show, and the right waiting count.

---

## Phase 7: Trust and safety

### Step 7.1: Reports
- `POST /queues/{id}/reports`: saves the reporter (never returned to the organizer). A second report from the same person on the same queue → 409 (the UNIQUE backs it up).

### Step 7.2: Verification requests
- `POST /me/verification`: one PENDING request at a time. Validate the office phone like `Validation.isValidOfficePhone`. The user's `verification_status` becomes PENDING.
- Add `GET /me/verification` if the Get verified screen needs to show a pending request's details. **Check with the app and MODELS.md first.**

**Done when:** tests cover a duplicate report, a second pending request (refused), and the user's status changing to PENDING.

---

## Phase 8: The admin page

### Step 8.1: Signing in
- `/admin/login` is a form with phone and password. Only `is_admin` users get in.
- On success, the server sets a cookie holding a token from the same `tokens` table (`httponly`, `samesite=lax`), and `require_admin` reads it.

### Step 8.2: The pages
- **Verification:** PENDING requests, each with the answers and the phone to call.
  - Approve: the request becomes VERIFIED, the user becomes VERIFIED, and `organization_name` is copied to the user.
  - Reject: an admin note.
  - Either way the answers are wiped (MODELS.md).
- **Reports:** grouped by queue, newest first, with a "handled" button.
- **Users:** search by phone.
  - Suspend with a reason: this also deletes their tokens, so they're signed out.
  - Unsuspend.
  - Revoke a badge (REVOKED).
- Every form is a POST that redirects back to the page, so a refresh doesn't submit twice.

**Done when:** you approve a request and the badge appears in the app, and you suspend a user and their app hits Account suspended on its next call.

---

## Phase 9: The learning model

> **Deferred (Sep 29).** Waits use the rolling average from phase 6 for now; training on synthetic data takes more time than there is right now (DECISIONS.md "Wait-time estimation learns online"). The steps below stay as the plan for when it's picked up. Phases 10 and 11 don't depend on it.

### Step 9.1: Simulated days
- `simulate.py` generates days of queue traffic:
  - arrivals through the day
  - service times that depend on the hour and category
  - no-shows, move-backs and breaks
  - injected prank entries (for the verification comparison)
- It writes them as `served` events: the features at that moment, plus the real service time. PROGRESS.md "Evaluation" lists what it needs.

### Step 9.2: The model
- `ml/features.py`: the DECISIONS.md "Wait-time estimation learns online" features for a queue at a moment:
  - rolling average
  - served in the last 15 minutes
  - people in line
  - no-shows in the last hour
  - moved-backs waiting
  - hour of day and day of week
  - category, one-hot
  - minutes since the queue opened
  - whether grace period and proximity are on
- `ml/wait_model.py`: a `StandardScaler` and an `SGDRegressor` (small learning rate) saved together with `joblib` at `MODEL_PATH`. Functions:
  - `warm_start(events)`
  - `predict(features)`
  - `learn(features, minutes)`, which runs `partial_fit` on both and saves

### Step 9.3: Online, in the server
- Warm-start once from simulated days and save.
- After each served person (in call-next), `learn(...)` with the real service time. Skip breaks over 30 minutes.
- `estimator.minutes_per_person`:
  - a queue with fewer than 10 served people today uses the rolling average
  - otherwise it uses `predict`, clamped to a sensible range
- `QueueStats.estimate_source` and `model_samples` report which one ran.

### Step 9.4: The evaluation
- Prequential ("predict, then learn") over held-out simulated days: the mean error in minutes of the rolling average, a model trained once, and the online model, plus a chart of the online model's error falling as it learns.
- Then the verification comparison from the same harness.

**Done when:** the numbers and charts exist for the report, and restarting the server keeps what the model learned.

---

## Phase 10: The Android app on the server

One screen at a time, in this order, each one working before the next.

### Step 10.1: The client
- Gradle: add `retrofit`, `converter-gson`, `okhttp` and its `logging-interceptor`.
- `ApiClient`: the base URL in `BuildConfig` (from `local.properties`, so it isn't hard-coded), an interceptor that adds `Authorization: Bearer <token>` from `Session`, and Gson type adapters for `Instant`, `LocalDate` and `LocalTime`.
- `QuappApi`: the endpoints as a Retrofit interface. Add `@SerializedName` on every model field (MODELS.md conventions).
- Decide the `QueueRepository` question from DECISIONS.md Open questions now. An interface with a `FakeData` version and a Retrofit version lets you switch one screen at a time.

### Step 10.2: The screens
1. Register and login, `/me`, suspended, device limit
2. Browse and Queue detail
3. Join, My tickets, Active ticket (with polling), leave, I'm here, move back
4. Create and edit queue, Owner home, Live console (with polling), walk-ins, remove, pause/close/extend
5. Stats, Insights, Today
6. Report, Get verified
- Every network-backed screen gets loading, empty, error and populated states (CLAUDE.md). The error state has a Retry button.
- Delete each `FakeData` method once nothing calls it.

**Done when:** `FakeData` is gone, or holds only preview data, and the PROGRESS.md "Android integration" items are ticked.

**Status (Sep 30):** built and emulator-tested in four batches: accounts (bc79bcf), the queuer side (7ad0feb), then the organizer side and verification together. `FakeData` and the app's own rolling average are deleted. The PROGRESS.md ticks wait for Brent's run on the phone.

---

## Phase 11: The demo

### Step 11.1: Running it for real

**Hosted (Render + Neon), the way to release** (DECISIONS.md "Hosting: Render and Neon"):
1. **Neon:** sign up at neon.tech, create a project (region: Singapore, closest to Bohol) with a database named `quapp`. Copy the connection string from the dashboard. Use the direct one (the host without `-pooler`), with `?sslmode=require` on the end.
2. **Fill it once from the PC:** in PowerShell, from `backend/`, `$env:DATABASE_URL = "<the Neon string>"; .venv\Scripts\python seed.py`. The environment variable wins over `.env`, so your local database is untouched. Close that terminal afterwards.
3. **Render:** sign up at render.com with GitHub, then New > Blueprint and pick the `quapp` repo. Render reads `render.yaml` and asks for `DATABASE_URL`: paste the Neon string. The first build takes a few minutes; then `https://quapp-api.onrender.com/health` answers `{"status": "ok"}` (the name may differ if it's taken).
4. **The app:** `quapp.apiUrl=https://quapp-api.onrender.com/` in `local.properties`, then build the release APK.
5. **The admin page:** the same address plus `/admin`, signed in as the seeded admin account.
- Render deploys the branch named in `render.yaml` (`feature/civic-paper-foundations` for now); change it to `master` after merging.
- Reseeding later is step 2 again. `seed.py` wipes the demo data, so don't run it once real people are using it.

**On the PC** (for development, or a demo without internet on the server side):
- On the desktop: Postgres running, and `uvicorn app.main:app --host 0.0.0.0 --port 8000` without `--reload`.
- Phones on mobile data reach it through a Cloudflare quick tunnel: `cloudflared tunnel --url http://localhost:8000` prints an `https://….trycloudflare.com` address. Put it in `local.properties` as `quapp.apiUrl=https://….trycloudflare.com/` (the trailing slash matters to Retrofit), then build and install again: the address is baked into the app at build time (`BuildConfig.API_URL`). A quick tunnel gets a new address every time `cloudflared` starts, so each restart means a rebuild; a named tunnel keeps one address. The admin page is the same address plus `/admin`.
- The emulator and a phone on USB can skip the tunnel: leave `quapp.apiUrl` out and run `adb reverse tcp:8000 tcp:8000`.
- Keep it running across the demo day: a start script, and switch off sleep.
- Before each rehearsal: `pg_dump` a backup, then `python seed.py` for a clean start.

### Step 11.2: The end-to-end rehearsal
- Two phones on mobile data: an organizer on the Xiaomi and a queuer on another phone (or the emulator).
- Run the full flow:
  - join
  - call
  - "I'm here" within 3 minutes
  - serve
  - a no-show
  - a move-back
  - report the queue
  - approve verification on the admin page
  - suspend a test account
- Time it. Write down anything that lags or breaks, and fix it in one batch.

**Done when:** the whole flow runs twice in a row with no fixes in between.

---

## Things to settle along the way

Each needs a line in MODELS.md or DECISIONS.md before its step:

| Question | Step |
|---|---|
| `POST /auth/logout` in the contract | 2.2 |
| `timed_out` on the console's now-serving ticket | 5.1 |
| `dry_run` preview for move-back | 5.3 |
| Polling or FCM for "you've been called" | 5.4 |
| `GET /me/verification` for the pending card | 7.2 |
| `QueueRepository` interface in front of FakeData | 10.1 |
| Hosting: the desktop (working assumption) or a cloud host | 11.1 |
| The Builder or `VerificationConfig` for Queue's four booleans (PROGRESS.md "Settle before writing the backend") | 10.1 |
