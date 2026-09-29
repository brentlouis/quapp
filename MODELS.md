# Quapp data models and API contract

The shared contract between the Android app (Java models, Retrofit) and the FastAPI backend.
Settled Sep 28, 2026 (see DECISIONS.md "Data models settled"). Change it here first, then in code.

Conventions:
- JSON is `snake_case`; Java fields are `camelCase`. Gson maps them with `@SerializedName`.
- Enums travel as their names in caps (`"OPEN"`). In Java they're enums, never strings.
- Times are ISO 8601 in **Asia/Manila**. A moment in time: `"2026-09-27T10:05:00+08:00"`. A date: `"2026-09-27"`. A time of day: `"08:00"`.
  In Java: `Instant`, `LocalDate`, `LocalTime` (java.time, through core library desugaring since min SDK is 24).
- `?` means the value can be null.
- Models are immutable (`final` fields, no setters). `Queue` keeps its Builder.
- IDs are strings.

---

## Enums

| Enum | Values |
|---|---|
| `Queue.Status` | `UPCOMING`, `OPEN`, `PAUSED`, `CLOSED` |
| `Category` | `RELIEF`, `MEDICAL`, `GOVERNMENT`, `EDUCATION`, `BILLS`, `IDS`, `JOBS`, `OTHER` |
| `Ticket.Status` | `WAITING`, `CALLED`, `SERVED`, `NO_SHOW`, `QUEUE_CLOSED`, `REMOVED` |
| `RemovalReason` | `PRANK`, `DUPLICATE`, `ASKED_TO_LEAVE` |
| `User.Status` | `ACTIVE`, `SUSPENDED` |
| `VerificationStatus` | `NONE`, `PENDING`, `VERIFIED`, `REJECTED`, `REVOKED` |
| `OrganizationType` | `LGU_OFFICE`, `BARANGAY`, `HEALTH`, `SCHOOL`, `COMMUNITY_GROUP`, `OTHER` |
| `ReportReason` | `FAKE`, `ASKED_FOR_MONEY`, `WRONG_PLACE_OR_TIME`, `OTHER` |
| `EstimateSource` | `ROLLING_AVERAGE`, `MODEL` |

Category labels in the app: Relief, Medical, Government, Education, Bills & Payments, IDs & Registration, Jobs, Other.

---

## Queue

What a queue is, when it runs, what checks it uses, and its live numbers.

| Field | JSON | Type | Notes |
|---|---|---|---|
| id | `id` | String | |
| organizerId | `organizer_id` | String | |
| organizerName | `organizer_name` | String | Shown as "Organized by …" |
| organizerVerified | `organizer_verified` | boolean | Badge on cards and detail |
| name | `name` | String | |
| category | `category` | Category | |
| shortDescription | `short_description` | String | Max 50 characters, one line on cards |
| details | `details` | String? | Full text on Queue detail |
| bring | `bring` | String? | "Barangay ID · claim stub" |
| venue | `venue` | String | |
| municipality | `municipality` | String | From the fixed Bohol list |
| latitude | `latitude` | double | Venue pin |
| longitude | `longitude` | double | |
| startDate | `start_date` | LocalDate | One-day queues: start = end |
| endDate | `end_date` | LocalDate | |
| opensAt | `opens_at` | LocalTime | Same hours every day of the run |
| closesAt | `closes_at` | LocalTime | Extending closing time changes this |
| status | `status` | Queue.Status | Server moves UPCOMING → OPEN → CLOSED on schedule. On a multi-day queue, each day's live tickets are released as QUEUE_CLOSED at that day's closing time; the queue stays OPEN between days |
| pausedAt | `paused_at` | Instant? | "No new joins since 10:05 AM" |
| closedAt | `closed_at` | Instant? | "Closed at 10:00 AM" |
| gracePeriodEnabled | `grace_period_enabled` | boolean | Presence confirmation, 3 min |
| noShowCooldownEnabled | `no_show_cooldown_enabled` | boolean | 2 no-shows → 30 min block |
| proximityCheckEnabled | `proximity_check_enabled` | boolean | |
| joinRadiusMeters | `join_radius_meters` | int | 500, 1000, 2000 or 5000; 0 when proximity is off |
| peopleWaiting | `people_waiting` | int | Live, server-computed |
| nowServing | `now_serving` | Integer? | Ticket number being served; null before the first call |
| estimatedWaitMinutes | `estimated_wait_minutes` | int | For someone joining now |

Removed: `serviceHours` (replaced by the schedule), `smsOtpEnabled` (SMS confirmation is future work; the app shows it as PLANNED without a field).

## QueueStats

An owner's numbers for one queue today. Separate from `Queue` because only the organizer gets them.

| Field | JSON | Type |
|---|---|---|
| servedToday | `served_today` | int |
| noShowsToday | `no_shows_today` | int |
| waitingNow | `waiting_now` | int |
| averageServiceMinutes | `average_service_minutes` | double |
| serviceSampleCount | `service_sample_count` | int |
| projectedWaitMinutes | `projected_wait_minutes` | int |
| estimateSource | `estimate_source` | EstimateSource |
| modelSamples | `model_samples` | int |

`estimate_source` says where today's minutes-per-person came from: the rolling average while a queue has little history, the learning model after that. Insights shows it ("Learning model · 142 people"). `model_samples` is how many served people the model has learned from in total.

---

## Ticket

One person's place in one queue.

| Field | JSON | Type | Notes |
|---|---|---|---|
| id | `id` | String | |
| queueId | `queue_id` | String | |
| queueName | `queue_name` | String | Copied so lists don't need the queue |
| venue | `venue` | String | |
| holderName | `holder_name` | String | |
| holderPhone | `holder_phone` | String? | Masked by the server for organizers ("0917 ••• 0002"); null for walk-ins |
| walkIn | `walk_in` | boolean | Added at the counter by the organizer |
| ticketNumber | `ticket_number` | int | Fixed for good |
| position | `position` | int | Counts down; 1 = next. 0 when not WAITING (called or finished) |
| estimatedWaitMinutes | `estimated_wait_minutes` | int | |
| status | `status` | Ticket.Status | |
| joinedAt | `joined_at` | Instant | History dates, "joined 24 min ago" |
| calledAt | `called_at` | Instant? | Grace deadline = called_at + 3 min |
| finishedAt | `finished_at` | Instant? | Served, no-show, closed or removed |
| movedBack | `moved_back` | boolean | Once per ticket |
| movedBackAt | `moved_back_at` | Instant? | Console: "Moved at 10:05 AM" |
| removalReason | `removal_reason` | RemovalReason? | Only when status is REMOVED |

Rules the server enforces:
- **Cooldown.** On queues with the no-show penalty, strikes are NO_SHOW tickets and PRANK removals on penalty queues. Every second strike starts 30 minutes in which penalty queues refuse joins; the count then starts again (DECISIONS.md "No-show cooldown").
- **Radius.** Distance is measured with the haversine formula. The app measures with Android's `Location.distanceBetween`, which can differ by up to about 0.5%, so the server allows 1% over the radius.
- **Several tickets, hours can't overlap.** A queuer can hold live tickets (WAITING or CALLED) in more than one queue, as long as no two of those queues' opening hours overlap on the same day. A ticket is for one day: today once the queue has started, otherwise its first day, and each queue is compared on that day (DECISIONS.md "A ticket is for one day").
- **Closed by the owner is not a no-show.** Closing a queue turns every WAITING or CALLED ticket into QUEUE_CLOSED and never counts toward the cooldown.
- **Removal.** PRANK counts as a no-show and goes to the admin; DUPLICATE and ASKED_TO_LEAVE don't count.

### The estimator

Every wait in the app is **predicted minutes per person × people ahead**. The minutes per person come from the wait-time model (DECISIONS.md "Wait-time estimation learns online"), or from the rolling average of the last 5 service times while a queue has fewer than 10 served people today. The same number drives the move-back distance below.

### Moving back: the estimator decides how far

"I need more time" asks how much time the queuer needs (5, 10, 15, 20, 30 or 45 min). The server turns time into places with the same rolling average that drives every ETA:

```
places = ceil(minutes_needed / predicted_minutes_per_person)
places = min(places, people_behind)          // at most, to the end of the line
```

The ticket keeps its number; only the call order changes. Once per ticket, never a no-show. The sheet previews the result before confirming: new position, new wait, new "Be there by". With no one served yet, the default 5 min per person is used.

---

## User

The account. One type for everyone; which side of the app you use is a local choice.

| Field | JSON | Type | Notes |
|---|---|---|---|
| id | `id` | String | |
| name | `name` | String | |
| phone | `phone` | String | Unique. Not confirmed until SMS exists |
| status | `status` | User.Status | SUSPENDED blocks login |
| suspendedReason | `suspended_reason` | String? | Shown on "Account suspended" |
| suspendedAt | `suspended_at` | Instant? | |
| verificationStatus | `verification_status` | VerificationStatus | Organizer badge |
| organizationName | `organization_name` | String? | Set when verified; "Posting as …" |

Server only (never sent to the app): password hash, `device_install_id` (max 2 accounts per install), `phone_verified` (false until an SMS gateway exists).

## VerificationRequest

An organizer asking for the badge. The answers are deleted after the decision; only the result and the admin note stay.

| Field | JSON | Type |
|---|---|---|
| id | `id` | String |
| userId | `user_id` | String |
| organizationName | `organization_name` | String |
| organizationType | `organization_type` | OrganizationType |
| position | `position` | String |
| officePhone | `office_phone` | String |
| status | `status` | VerificationStatus |
| adminNote | `admin_note` | String? |
| createdAt | `created_at` | Instant |
| decidedAt | `decided_at` | Instant? |

## Report

Anyone reporting a queue. The organizer never sees who reported.

| Field | JSON | Type |
|---|---|---|
| id | `id` | String |
| queueId | `queue_id` | String |
| reason | `reason` | ReportReason |
| details | `details` | String? |
| createdAt | `created_at` | Instant |

Server only: `reporter_id` (to stop one person flooding reports).

## Line

What the Live console shows: `GET /queues/{id}/line` and the calling endpoints return it. Holder phones are masked (`0917 ••• 0002`).

| Field | JSON | Type | Notes |
|---|---|---|---|
| nowServing | `now_serving` | Ticket? | The CALLED ticket; null when nobody is at the counter |
| nowServingHereAt | `now_serving_here_at` | Instant? | When they tapped "I'm here" (the console's confirmed state) |
| nowServingTimedOut | `now_serving_timed_out` | boolean | Grace period on, 3 minutes since the call, no "I'm here". The organizer decides: no-show, or serve anyway. The server never marks it by itself |
| waiting | `waiting` | Ticket[] | Today's WAITING tickets in call order |

## TicketRemoval

An organizer taking someone out of the line, with a reason.

| Field | JSON | Type |
|---|---|---|
| id | `id` | String |
| ticketId | `ticket_id` | String |
| queueId | `queue_id` | String |
| reason | `reason` | RemovalReason |
| removedBy | `removed_by` | String |
| createdAt | `created_at` | Instant |

---

## Errors

Every error has the same JSON body, so the app can pick a message by `error` without parsing text:

```json
{"error": "DEVICE_LIMIT", "message": "This phone already has 2 accounts."}
```

`message` is plain English the app may show as-is. Some errors add fields (listed below).

| HTTP | `error` | When | Extra fields |
|---|---|---|---|
| 401 | `NOT_SIGNED_IN` | No token, or a token that was logged out | |
| 401 | `WRONG_CREDENTIALS` | Login with an unknown phone or a wrong password (the same answer for both) | |
| 403 | `SUSPENDED` | Login, or any signed-in call, by a suspended user | `suspended_reason`, `suspended_at` |
| 409 | `PHONE_TAKEN` | Register with a phone that has an account | |
| 409 | `DEVICE_LIMIT` | Register a third account on one install | |
| 422 | `INVALID_INPUT` | A field fails validation (bad phone, short password, missing field, a closing time before the opening time) | `fields`: `{"phone": "…"}` |
| 404 | `QUEUE_NOT_FOUND` | No queue with that id | |
| 403 | `NOT_OWNER` | An organizer-only call on someone else's queue | |
| 409 | `ONE_LIVE_QUEUE` | An unverified organizer opening a second live (open or paused) queue; an upcoming one is allowed | |
| 409 | `WRONG_STATUS` | A status change that doesn't fit ("Only an open queue can be paused"), or editing a closed queue | |
| 409 | `QUEUE_PAUSED` | Joining a paused queue | |
| 409 | `QUEUE_NOT_OPEN` | Joining a closed queue, or an open one after today's closing time | |
| 409 | `ALREADY_IN_LINE` | Joining a queue you already have a live ticket in | `ticket_id` |
| 409 | `HOURS_OVERLAP` | Joining while holding a live ticket for a queue on the same day with overlapping hours | `other_queue_id`, `other_queue_name` |
| 409 | `COOLDOWN` | Joining a queue with the no-show penalty during a cooldown | `until` |
| 422 | `LOCATION_NEEDED` | Joining a queue with the proximity check without `latitude`/`longitude` | |
| 409 | `TOO_FAR` | Joining from farther than the queue's radius | `distance_meters`, `join_radius_meters` |
| 404 | `TICKET_NOT_FOUND` | No such ticket, or not yours | |
| 409 | `NOBODY_CALLED` | No-show with nobody at the counter | |
| 409 | `NOT_CALLED` | "I'm here" on a ticket that isn't being called | |
| 409 | `GRACE_OVER` | "I'm here" after the 3-minute grace window | |
| 409 | `ALREADY_MOVED_BACK` | A second "I need more time" on one ticket | |
| 409 | `LAST_IN_LINE` | "I need more time" with nobody behind to let ahead | |

Later steps add their own codes here.

## Endpoints

Auth is a bearer token from `/auth/login`. "Owner" means the queue's organizer.

| Method | Path | Who | Does |
|---|---|---|---|
| POST | `/auth/register` | anyone | name, phone, password (8 to 72 characters), device_install_id → token + User. Spaces and dashes in the phone are ignored (`0917 123 4567`) |
| POST | `/auth/login` | anyone | phone, password → token + User (403 if suspended) |
| POST | `/auth/logout` | user | ends this token's session → 204 |
| GET | `/me` | user | the User |
| GET | `/queues?municipality=&category=&q=` | anyone | Browse: UPCOMING, OPEN, PAUSED (no CLOSED) |
| GET | `/queues/{id}` | anyone | one Queue |
| GET | `/me/queues` | user | the organizer's own queues, all statuses: live first, then upcoming, then closed |
| POST | `/queues` | user | create (unverified: one live queue at a time) |
| PATCH | `/queues/{id}` | owner | edit details, schedule, checks |
| POST | `/queues/{id}/pause` · `/resume` · `/close` · `/extend` | owner | status changes, each returning the Queue; extend takes a later `closes_at` |
| GET | `/queues/{id}/stats` | owner | QueueStats |
| GET | `/queues/{id}/line` | owner | Line (below): who's at the counter and who's waiting, phones masked |
| POST | `/queues/{id}/call-next` | owner | marks the one at the counter SERVED (if any), calls the first waiting for today → Line. Open or paused queues only (a paused queue still serves its line) |
| POST | `/queues/{id}/no-show` | owner | marks the one at the counter NO_SHOW, calls the next → Line. 409 `NOBODY_CALLED` if nobody is at the counter |
| POST | `/queues/{id}/walk-ins` | owner | `{name}` → Ticket: numbered like a join, no account, no phone, at the back of today's line |
| POST | `/tickets/{id}/remove` | owner | `{reason}` → TicketRemoval; the ticket becomes REMOVED. Waiting or called tickets only |
| POST | `/queues/{id}/tickets` | user | join; body `{latitude, longitude}` (needed only when the queue checks proximity) → Ticket. Checks, in order: the queue takes joins now, not already in it, cooldown, overlap, radius |
| GET | `/me/tickets?live=true` | user | My tickets; `live=false` for History |
| POST | `/tickets/{id}/here` | holder | "I'm here" while CALLED, within 3 minutes of `called_at` when the queue has the grace period → Ticket |
| POST | `/tickets/{id}/move-back?dry_run=` | holder | `{minutes_needed}` (5, 10, 15, 20, 30 or 45) → Ticket at its new place (once). Waiting or called: a called ticket hands the counter back and rejoins the line. `dry_run=true` returns the same Ticket without saving, for the sheet's preview |
| GET | `/tickets/{id}` | holder | one of my tickets, with its position and wait now |
| DELETE | `/tickets/{id}` | holder | leave the queue (waiting or called) → 204. The ticket is deleted: not a no-show, not in history (FakeData.leave) |
| POST | `/me/verification` | user | VerificationRequest |
| POST | `/queues/{id}/reports` | user | Report |
| GET/POST | `/admin/...` | admin | web page: verification requests, reports, suspend and revoke |

---

## Database (PostgreSQL)

The server's tables, written with SQLAlchemy models in `backend/`. Settled Sep 29, 2026 (DECISIONS.md "Backend: PostgreSQL and SQLAlchemy, in backend/").

**What isn't stored:** anything the server can count from other rows. `people_waiting`, `now_serving`, `position`, every `estimated_wait_minutes`, all of `QueueStats`, the no-show cooldown, and a queue's `organizer_name` / `organizer_verified` (joined from `users`) are computed per request, so they can't drift out of step.

Types:
- IDs are `TEXT`, a uuid4 made by the server (the API says IDs are strings).
- Moments are `TIMESTAMPTZ` (Postgres keeps UTC; the API sends them with `+08:00`). Dates are `DATE`, times of day `TIME`.
- Enums are `VARCHAR` plus a `CHECK` on the allowed values: SQLAlchemy `Enum(..., native_enum=False, create_constraint=True)`. Easier to add a value later than a native Postgres enum type.
- Booleans `BOOLEAN NOT NULL DEFAULT false` unless noted.

### users

| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | |
| name | TEXT NOT NULL | |
| phone | TEXT NOT NULL UNIQUE | |
| password_hash | TEXT NOT NULL | bcrypt; never leaves the server |
| status | User.Status NOT NULL | default `ACTIVE` |
| suspended_reason | TEXT | |
| suspended_at | TIMESTAMPTZ | |
| verification_status | VerificationStatus NOT NULL | default `NONE` |
| organization_name | TEXT | set when verified |
| device_install_id | TEXT NOT NULL | the install that registered it; at most 2 users per value (checked in code) |
| phone_verified | BOOLEAN | false until an SMS gateway exists |
| is_admin | BOOLEAN | who can open `/admin` |
| created_at | TIMESTAMPTZ NOT NULL | |

### tokens

| Column | Type | Notes |
|---|---|---|
| token | TEXT PK | random, from `secrets.token_urlsafe` |
| user_id | TEXT NOT NULL → users ON DELETE CASCADE | |
| created_at | TIMESTAMPTZ NOT NULL | |

Login adds a row, logout deletes it. Suspending a user deletes all of theirs, which signs them out everywhere.

### queues

Every `Queue` field that isn't computed, plus:

| Column | Type | Notes |
|---|---|---|
| organizer_id | TEXT NOT NULL → users | |
| category | Category NOT NULL | |
| short_description | TEXT NOT NULL | `CHECK (length(short_description) <= 50)` |
| latitude, longitude | DOUBLE PRECISION NOT NULL | |
| start_date, end_date | DATE NOT NULL | `CHECK (end_date >= start_date)` |
| opens_at, closes_at | TIME NOT NULL | `CHECK (closes_at > opens_at)` |
| status | Queue.Status NOT NULL | |
| paused_at, closed_at | TIMESTAMPTZ | |
| join_radius_meters | INTEGER NOT NULL | `CHECK (join_radius_meters IN (0, 500, 1000, 2000, 5000))` |
| next_ticket_number | INTEGER NOT NULL DEFAULT 1 | hands out #1, #2, … and never reuses one |
| created_at | TIMESTAMPTZ NOT NULL | |

Index: `(municipality, status)` for Browse.

### tickets

| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | |
| queue_id | TEXT NOT NULL → queues | |
| user_id | TEXT → users | null for walk-ins |
| holder_name | TEXT NOT NULL | |
| holder_phone | TEXT | null for walk-ins |
| walk_in | BOOLEAN | |
| ticket_number | INTEGER NOT NULL | `UNIQUE (queue_id, ticket_number)` |
| line_order | DOUBLE PRECISION NOT NULL | the call order; starts equal to `ticket_number` |
| service_date | DATE NOT NULL | a ticket is for one day (the overlap rule) |
| status | Ticket.Status NOT NULL | |
| joined_at | TIMESTAMPTZ NOT NULL | |
| called_at, here_at, finished_at | TIMESTAMPTZ | `here_at` is when they tapped "I'm here" |
| moved_back | BOOLEAN | once per ticket |
| moved_back_at | TIMESTAMPTZ | |
| removal_reason | RemovalReason | only when status is `REMOVED` |

- **Position** is the number of `WAITING` tickets in the queue with a smaller `line_order`, plus 1.
- **Moving back** changes only `line_order`: to a value between the two tickets it lands between (moving behind #45 when #46 is next gives 45.5). The ticket number never changes.
- **Service time** is the time between one call and the next, from `called_at`. The rolling average and the model learn from it; no separate table.
- **One live ticket per queue per person:** a partial unique index, `UNIQUE (queue_id, user_id) WHERE status IN ('WAITING', 'CALLED')`.
- Indexes: `(queue_id, status, line_order)` for the line and positions, `(user_id, status)` for My tickets.

### ticket_removals

`id` TEXT PK, `ticket_id` TEXT NOT NULL UNIQUE → tickets, `queue_id` → queues, `reason` RemovalReason NOT NULL, `removed_by` → users, `created_at` TIMESTAMPTZ NOT NULL.

### verification_requests

The `VerificationRequest` fields, with `user_id` → users and `decided_by` TEXT → users. `organization_name`, `organization_type`, `position` and `office_phone` are nullable: they're wiped after the decision, and only the status and `admin_note` stay.

### reports

The `Report` fields, plus `reporter_id` TEXT NOT NULL → users (never sent to the organizer) and `handled_at` TIMESTAMPTZ for the admin page. `UNIQUE (queue_id, reporter_id)`: one report per person per queue.

### Not in the database

The wait-time model's weights and scaler are saved as a file next to the server (PROGRESS.md, milestone 3).
