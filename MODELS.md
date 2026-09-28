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
| status | `status` | Queue.Status | Server moves UPCOMING → OPEN → CLOSED on schedule |
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
| position | `position` | int | Counts down; 1 = next |
| estimatedWaitMinutes | `estimated_wait_minutes` | int | |
| status | `status` | Ticket.Status | |
| joinedAt | `joined_at` | Instant | History dates, "joined 24 min ago" |
| calledAt | `called_at` | Instant? | Grace deadline = called_at + 3 min |
| finishedAt | `finished_at` | Instant? | Served, no-show, closed or removed |
| movedBack | `moved_back` | boolean | Once per ticket |
| movedBackAt | `moved_back_at` | Instant? | Console: "Moved at 10:05 AM" |
| removalReason | `removal_reason` | RemovalReason? | Only when status is REMOVED |

Rules the server enforces:
- **Several tickets, hours can't overlap.** A queuer can hold live tickets (WAITING or CALLED) in more than one queue, as long as no two of those queues' opening hours overlap on the same day.
- **Closed by the owner is not a no-show.** Closing a queue turns every WAITING or CALLED ticket into QUEUE_CLOSED and never counts toward the cooldown.
- **Removal.** PRANK counts as a no-show and goes to the admin; DUPLICATE and ASKED_TO_LEAVE don't count.

### Moving back: the estimator decides how far

"I need more time" asks how much time the queuer needs (5, 10, 15, 20, 30 or 45 min). The server turns time into places with the same rolling average that drives every ETA:

```
places = ceil(minutes_needed / average_service_minutes)
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

## Endpoints

Auth is a bearer token from `/auth/login`. "Owner" means the queue's organizer.

| Method | Path | Who | Does |
|---|---|---|---|
| POST | `/auth/register` | anyone | name, phone, password (min 8), device_install_id → token + User |
| POST | `/auth/login` | anyone | phone, password → token + User (403 if suspended) |
| GET | `/me` | user | the User |
| GET | `/queues?municipality=&category=&q=` | anyone | Browse: UPCOMING, OPEN, PAUSED (no CLOSED) |
| GET | `/queues/{id}` | anyone | one Queue |
| POST | `/queues` | user | create (unverified: one live queue at a time) |
| PATCH | `/queues/{id}` | owner | edit details, schedule, checks |
| POST | `/queues/{id}/pause` · `/resume` · `/close` · `/extend` | owner | status changes; extend takes a new `closes_at` |
| GET | `/queues/{id}/stats` | owner | QueueStats |
| GET | `/queues/{id}/line` | owner | now serving + waiting Tickets (phones masked) |
| POST | `/queues/{id}/call-next` | owner | marks the current one served, calls the next |
| POST | `/queues/{id}/no-show` | owner | marks the current one no-show, calls the next |
| POST | `/queues/{id}/walk-ins` | owner | name → Ticket |
| POST | `/tickets/{id}/remove` | owner | reason → TicketRemoval |
| POST | `/queues/{id}/tickets` | user | join (checks overlap, cooldown, radius with lat/lng) → Ticket |
| GET | `/me/tickets?live=true` | user | My tickets; `live=false` for History |
| POST | `/tickets/{id}/here` | holder | "I'm here" within the grace window |
| POST | `/tickets/{id}/move-back` | holder | minutes_needed → Ticket (once) |
| DELETE | `/tickets/{id}` | holder | leave the queue |
| POST | `/me/verification` | user | VerificationRequest |
| POST | `/queues/{id}/reports` | user | Report |
| GET/POST | `/admin/...` | admin | web page: verification requests, reports, suspend and revoke |
