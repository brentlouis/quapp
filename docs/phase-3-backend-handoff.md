# Quapp Phase 3 — Backend Integration Handoff

Updated: 2026-10-21 (Asia/Manila). Status: **backend handoff prepared, with frontend/product and acceptance blockers in section 10 and the parity ledger; backend implementation and integration are not verified**. Earlier dated sections preserve historical evidence; section 10 describes the current whole-frontend integration boundary.

The frontend engineer owns the Android UI and client integration. The backend engineer supplies and implements the APIs and server-side guarantees below. Fill the `TBD` fields directly in this document, then review the contract with the frontend engineer before client changes.

This handoff does not by itself change product decisions, approve new roles, or mark integration complete. Existing process-local demonstrations are not proof of real server integration. The user has separated Phase 3 frontend acceptance from backend integration: unavailable APIs are integration-pending items, while unresolved frontend behavior remains a frontend blocker.

## 1. Backend engineer: complete these first

| Item | Backend answer |
|---|---|
| Engineer / contact | TBD |
| API documentation / OpenAPI location | TBD |
| Development/staging base URL | TBD |
| API version and availability date | TBD |
| Authentication flow / token expiry / refresh behavior | TBD |
| Safe test-account provisioning procedure | TBD — do not put passwords or tokens here |
| Owned public sharing domain and domain administrator | TBD |
| Queue public URL format and stable identifier | TBD |
| Recipient/organization identity lookup contract | TBD |
| Ownership-transfer API and authorization rules | TBD |
| Error schema, validation fields and retry rules | TBD |
| Timezone, date boundaries and timestamp format | TBD |
| Pagination, polling/rate limits and concurrency strategy | TBD |

## 2. Existing frontend references

- [MODELS.md](../MODELS.md): existing planned models, enums, JSON names and endpoint table. Treat this as a compatibility baseline, not evidence that a live server implements it.
- [DECISIONS.md](../DECISIONS.md): repository decisions. Check current live-vault decisions too; report conflicts rather than overriding them.
- [QueueStats.java](../app/src/main/java/com/example/quapp/QueueStats.java): current native statistics fields.
- [QueueLink.java](../app/src/main/java/com/example/quapp/QueueLink.java): currently uses the **placeholder** `quapp.app/q/` plus a queue-name slug. Domain ownership, real resolution and uniqueness are not established by that code.
- [QueueCounter.java](../app/src/main/java/com/example/quapp/QueueCounter.java): current counter representation; server persistence is not established.
- [FakeData.java](../app/src/main/java/com/example/quapp/FakeData.java): current local fixtures/behavior; do not treat them as a production backend contract.

`MODELS.md` documents bearer authentication from `/auth/login`, queue ownership through the organizer, `/queues/{id}/stats`, `/queues/{id}/line`, queue operations, walk-ins and ticket operations. Preserve agreed enum/JSON compatibility; propose and review changes before changing client or server contracts.

## 3. Sharing and QR links — external blocker

Backend/domain owner supplies:

- A real approved HTTPS domain and a stable queue-to-URL mapping. Define uniqueness, renames, invalid identifiers and deleted/closed/private queue behavior.
- The web/redirect destination and behavior when the Android app is absent. Queue names alone must not be assumed unique.
- Hosting for `/.well-known/assetlinks.json` if verified Android App Links are required. Agree package name and public signing-certificate fingerprints with the frontend/release owner; never exchange a signing private key in this document.
- Test URLs and access to a staging environment for real recipient-device delivery and link resolution.

Frontend engineer configures Android routing and sharing/QR UI after the domain and route contract are agreed. Copying text or opening Android's share sheet is not proof of external delivery or queue resolution.

| Contract field | Backend/domain-owner answer |
|---|---|
| Approved domain / verified owner | TBD |
| URL pattern / stable queue identifier | TBD |
| Queue-name collision and rename behavior | TBD |
| Web fallback / invalid or inaccessible queue behavior | TBD |
| assetlinks URL / package and certificate association | TBD |
| Staging example URLs and recipient test arrangement | TBD |

Acceptance: correct queue opens; renamed queues follow the agreed rule; invalid/missing/inaccessible queue errors are safe; app-installed and app-absent routes behave as agreed; QR resolution and actual recipient-device delivery are demonstrated.

## 4. Real ownership handover — external blocker

The recorded requirement is real ownership transfer. A local `managerId` mutation, fake success message or same-owner briefing is not a substitute.

Backend engineer must define:

- Trusted recipient discovery, stable user/organization identifiers, eligibility and the minimum public identity fields needed by the frontend.
- Server-side authorization: verify the authenticated caller owns the queue at execution time; reject unauthorized and ineligible recipients.
- Atomic transfer, concurrent ownership-change handling and idempotency/duplicate-request behavior.
- Whether recipient acknowledgement is required; exact pending/success/denial/failure states and when ownership actually changes.
- Timeout, retry, cancellation, status reconciliation and recovery behavior. After an uncertain response, the client must be able to determine the actual server state.
- What happens to current tickets, counters, announcements, public links and the previous owner's access. These effects require explicit agreement.

No endpoint names or DTO fields are invented here. Backend engineer fills the real contract:

| Operation | Method/path | Request fields | Success response | Error/status and permission rules |
|---|---|---|---|---|
| Discover eligible recipient | TBD | TBD | TBD | TBD |
| Request/execute transfer | TBD | TBD | TBD | TBD |
| Read transfer outcome/current ownership | TBD | TBD | TBD | TBD |
| Acknowledge or cancel, if the agreed flow requires it | TBD | TBD | TBD | TBD |

Acceptance fixtures must cover authorized success, unauthorized/foreign recipients, concurrent ownership changes, repeated submissions, retry after timeout, cancellation where supported and recovery after process/network interruption. Provide actual test evidence; frontend confirmation screens alone do not close this requirement.

### Approved frontend boundary — 2026-10-19

The user approved an owner-only, queue-specific Handover screen with an explicit recipient-service-unavailable state, Retry and Back, and Transfer ownership disabled. The frontend must not invent recipients, mutate ownership or show a successful transfer. Retry currently revalidates local queue ownership and redisplays unavailable status; it makes no pretend network request. This unavailable boundary may satisfy frontend scope while the real operations above remain integration pending. Recipient selection, confirmation and server-outcome behavior must be reviewed against the eventual contract before enabling transfer.

## 5. Today, Queues and Insights — frontend decisions approved

The user explicitly approved the existing native semantics on 2026-10-19. The prototype differences below are reference evidence, not instructions to replace the approved behavior. Backend mappings and implementations remain pending.

| Area | Current recorded native behavior | Prototype difference | User-approved frontend rule, 2026-10-19 |
|---|---|---|---|
| Today | Totals across owned queues; queue-specific Insights navigation | Selected-service counts; some counter counts use global demo state | Retain existing all-owned-queue totals and metrics; enter queue-specific actions through an owned queue. |
| Queues | Owned queues filtered by queue status | Services assigned through `managerId` | Assigned services mean existing owner-owned queues; retain status filters; no manager/staff role. |
| Insights | One queue's `QueueStats`, including existing wait forecast | Selected-service process-session counts, no live forecast | Retain queue-specific QueueStats and forecast; select through existing routes; no session-count replacement. |

Current `QueueStats` JSON baseline: `served_today`, `no_shows_today`, `waiting_now`, `average_service_minutes`, `service_sample_count`, `projected_wait_minutes`, `estimate_source`, `model_samples`. Do not assume prototype issued/completed/left/missed counters map directly to these fields.

| Approval item | Agreed rule / approver / date |
|---|---|
| Today scope and metric definitions | User approved 2026-10-19: retain all-owned-queues totals and existing metric definitions; enter queue-specific actions through an owned queue. Do not replace with selected-service/global-counter demo counts. |
| Owner versus manager/assigned-service model | User approved 2026-10-19: assigned services map to existing owner-owned queues; retain status filters; introduce no manager/staff role. |
| Insights metrics, selection and forecast behavior | User approved 2026-10-19: retain queue-specific QueueStats and forecast, selecting queues through existing routes; do not replace with prototype session-only counts. |

## 6. Backend support for other current demo workflows

Existing approved local behavior stays the baseline until a reviewed backend contract replaces it.

| Workflow | Backend contract or verification needed |
|---|---|
| Counter list / counter | Queue-owner-only resources; shared waiting line; next free counter calls next ticket. Define persistent state, atomic concurrent calls and cross-device consistency. No staff assignment is assumed. |
| Console / walk-in | Authenticated owner operations, masked phone data, validation, persisted results and safe retry/concurrency behavior; align with the existing line/call-next/no-show/walk-in/ticket-removal contracts. |
| Announcement | Existing frontend rule: one active plain-text message per queue, maximum 500 Unicode code points after trimming; owner publish/replace/clear; displayed on counters. Match that existing validation when agreeing the wire contract. No push, staff publishing or automatic expiry is assumed. |
| Organizer Preview | Read-only validated form snapshot; existing Create/Save performs writes. Persistent drafts and assignment are not required by the recorded contract. |
| Public Organizer / Organization | Explicit public projections for names, verification and public queues; authoritative identity/organization relationship. Never leak private contact, office, verification-request or account-history data. |
| Lookup | Owner-only, selected-queue exact ticket-number lookup; masked phone/status, finished and missing results. Enforce authorization server-side; no public or cross-queue search is assumed. |
| Display | Agreed serving/up-next/QR data and refresh/error rules. Device legibility and QR decoding need frontend/device verification too. |

For each implemented operation, supply method/path, request/response examples, auth/authorization, validation/errors, empty states, idempotency/concurrency where relevant, and a staging test fixture. Do not store credentials or production personal data here.

## 7. Delivery and completion checklist

- [ ] Backend engineer fills the TBD fields and provides API/OpenAPI documentation.
- [x] Product/frontend choices in section 5 are explicitly agreed by the user on 2026-10-19.
- [ ] DTOs, enum mappings, privacy rules and backwards compatibility are reviewed together.
- [ ] Frontend integrates only agreed contracts and implements loading/empty/error/success behavior.
- [ ] Sharing resolves real URLs on a recipient device; App Links setup is verified where required.
- [ ] Real handover and its denial/concurrency/retry/recovery cases pass against the backend.
- [ ] Remaining demo workflows have the required server authorization and persistence evidence.
- [ ] Frontend device/accessibility checks and backend integration results are recorded separately.
- [ ] Phase 3 acceptance is reviewed against the approved scope; no demo or document alone is labelled full integration completion.

### Backend engineer change log

| Date | Engineer | Contract/endpoint changed | Frontend impact | Agreed/tested status |
|---|---|---|---|---|
| TBD | TBD | TBD | TBD | Proposed / agreed / implemented / verified |

## 8. Frontend verification checkpoint — 2026-10-19

The user approved the section 5 native semantics and section 4 unavailable-service boundary. Phase 3 frontend acceptance is now separate from production integration. The Handover screen is reachable from the owner Console, rejects unavailable/non-owned queues, supports Back and Retry, and keeps transfer disabled without changing ownership or tickets. Retry does not call an invented service.

Final Handover fixture tests passed at normal portrait, 360x640dp/200% font and 640x360dp landscape. An inset/scroll clipping defect found at 200% was corrected using the existing outer-container pattern. Today navigation/filter preservation passed the same three configurations. A focused Sharing copy/back test also passed; placeholder link resolution is not claimed.

Evidence and separate per-workflow frontend/backend statuses: `build/phase3-organizer-closure-20261019/frontend-boundary-closure/WORKFLOW-MATRIX.txt` and its JSON companion. Full test logs, XML, screenshots, failed attempts and restoration evidence are retained alongside it.

The backend checklist above remains open. No endpoint, recipient DTO, authentication mechanism or transfer semantics has been fabricated. Before enabling real transfer, agree recipient data, authorization, outcome states and reconciliation behavior; then integrate and verify the real client/server flow. Frontend closure does not claim the integrated application is complete.

## 9. Phase 4 frontend boundaries and backend requirements — 2026-10-19

<!-- phase4-frontend-20261019 -->

Recorded at 21:02 UTC+08:00. The user explicitly authorized API-independent frontend implementation for Appointments, Family tickets and Staff/invitations. This does not approve the experimental prototype's product rules, new permissions, wire schemas or endpoints. Existing API/authentication/error/timezone/concurrency TBD fields above remain unresolved. Requirements below are proposed backend responsibilities, not implemented or approved endpoints.

### Current frontend and verification boundary

`Phase4Activity.java` provides internal, non-exported screens with existing Slate & Mint styles, keyboard insets and scrollable content. Profile opens Appointments and Family tickets; an owned queue's Console options open queue-scoped Appointments and Staff. Staff and owner-context Appointments recheck existing demo queue ownership on resume. These client checks are not server authorization. Existing individual tickets, ownership and session roles are unchanged.

Draft values are held in Activity state and saved in a Bundle for recreation, not persisted to a database or submitted. Family fields have a 4,000 UTF-16-code-unit presentation budget per field to bound saved state; this is not an approved group-size, identity or backend field rule. Do not enter sensitive identity/contact data. Recheck availability is a truthful local notice, not a network request. No asynchronous request is started, so no artificial loading, pending or confirmed result is shown. Real loading/error/empty/success distinctions must be wired to actual outcomes later.

Incremental `assembleDebug`, `assembleDebugAndroidTest` and fresh `testDebugUnitTest` passed together on 2026-10-19: 39 local tests, zero failures/errors, including two Phase4Draft tests. Three Phase4Frontend instrumentation tests compile but were not executed: no ADB/device authorization in this task. Normal portrait, 200% font, landscape, screenshots, pointer/picker behavior, TalkBack and process-death acceptance remain NOT VERIFIED. This section records implementation boundaries, not runtime or integrated acceptance.

### 9.1 Appointments

1. Implemented: discovery through existing demo services, local preferred date/time pickers, review, unavailable upcoming/status and details/management screens; owner entry fixes the selected queue. Reserve, cancel and reschedule remain disabled. Editing a preference cannot change a booking. No slots, bookings or organizer reservation records are fabricated.
2. Required operations, names/routes TBD: discover bookable services; obtain authoritative availability; reserve; list/read the authenticated person's reservations; cancel; reschedule atomically; and obtain permitted organizer views/actions. Organizer operations must be approved before enabling more than the present unavailable presentation.
3. Contract design needed: service/queue and slot identifiers, authoritative date/time/timezone, capacity and availability version, reservation ID/holder scope, lifecycle/status, appointment-to-ticket relationship, allowed actions, policy explanations and mutation outcome/reconciliation identifiers. Define validation, field errors, exhausted/stale slots, conflicts, denied/expired requests and unknown outcomes; no schema or enum is agreed here.
4. Require authenticated holder identity and server-enforced read/mutation ownership; authorize organizer access per queue and prevent access to another person's reservations. Define what organizer/public views may reveal. Current FakeData queue names are discovery fixtures, not trusted availability.
5. Persist bookings durably; prevent overbooking under concurrent requests; specify duplicate/replay/idempotency behavior, atomic rescheduling and capacity release. Define reservation expiry, attendance/no-show, cancellation and ticket creation/serving transitions. Never infer success from a timeout or an optimistic local record.
6. Offline/unavailable must preserve preferences without claiming a reservation. Distinguish no availability from failure. After uncertain mutation outcomes, reconcile by a stable operation/reservation identity before retrying; define whether retries may safely repeat. Show actual loading only while a request exists.
7. Fixtures/acceptance needed: no bookable service, no slots, long/Unicode service names, zero/full capacity, cross-timezone/day boundaries, stale slot, simultaneous last-slot booking, duplicate submission, timeout after commit, unauthorized access, cancellation races and reschedule rollback. Verify owner/participant projections and ticket linkage independently.
8. Product approval needed: Does booking create, reserve or later redeem an individual ticket? Who supplies slots and capacity? What timezone governs selection/display? What are lead time, eligibility, limits, lifecycle, cancellation/reschedule/no-show rules? Which organizer actions are allowed? Are local preferences durable drafts?
9. Presentation assumptions: any existing demo service can be selected to prepare a preference; any picker date/time is only a preference, not validated availability. Replace this source with approved availability and typed contracts; retain explicit unavailable states until authoritative operations and outcomes pass integration tests.

### 9.2 Family tickets

1. Implemented: local holder/member labels, nonblank validation, multiline review, unavailable ticket status and member-management presentation; edit/clear affects local draft only. Create group and leave group remain disabled. No existing individual-ticket model, serving state or history is mutated.
2. Required operations, routes TBD: determine group eligibility; create/read a group ticket; list/status members; add/remove/leave where allowed; obtain serving/partial-completion status. Establish the queue context and relationship to existing individual tickets before enabling submission.
3. Contract design needed: group/ticket/queue identifiers, authenticated responsible holder, trusted member references or approved dependent representation, public/private projections, allowed actions, per-member/group status, versions and operation outcomes. Define invalid member, duplicate, capacity, ineligible, authorization and stale-state errors. Local labels are not identity fields or an agreed request payload.
4. Server must establish holder authority and member consent/eligibility, define minors/dependents, and protect private member data. Define who can see or change membership and what a public display exposes. Client labels do not prove identity or provide a privacy guarantee.
5. Define whether capacity counts people or groups, maximum membership, duplicate participation across queues/groups, concurrency on member changes and partial service, durable membership and idempotent creation/removal. Preserve individual-ticket compatibility; specify atomic transitions and recovery when part of a group is served.
6. Separate no linked group from fetch failure; keep local labels on recoverable failure without claiming saved membership. Define offline reading/privacy and uncertain create/add/remove/leave outcomes with authoritative reconciliation before retry. Clearing a local draft must never delete a server group.
7. Fixtures/acceptance needed: empty/whitespace labels, long Unicode/multiline labels, absent group, maximum and over-limit membership once approved, duplicate/dependent identity, denied member access, simultaneous join/removal, people-versus-group capacity, partial service, offline failure and replay after committed mutation. Verify existing individual tickets are unchanged.
8. Product approval needed: Who is eligible and responsible? Are members accounts, dependents or labels? What consent/privacy rules apply? What is the maximum size? How is capacity counted? Can members join multiple groups? Who may add/remove/leave, including after partial service? What lifecycle/status and queue linkage should be presented?
9. Presentation assumptions: holder/member labels support reviewing an unsubmitted local draft only; each member is displayed as entered rather than parsed into trusted entities. Replace label fields with the approved identity/eligibility workflow, keeping server mutations disabled until contracts exist. The local text budget is not a membership policy.

### 9.3 Staff roles and invitations

1. Implemented: owned-queue entry, unavailable member/invitation list, explicitly exploratory prototype role descriptions, invitation review, unavailable status and role-change/revocation presentation. No recipient is invented; Send, Change role and Revoke remain disabled. Exploring a description never grants a role or permission.
2. Required operations, routes TBD: load authorized members and action matrix; resolve trusted recipients; create/read/accept/decline invitations; expire/resend/revoke invitations as approved; change/revoke membership. Ownership transfer/removal remains a separate agreed security operation, not an extension of this local screen.
3. Contract design needed: membership scope and IDs, trusted recipient identity/projection, server-supported role/action descriptions, invitation token/ID and lifecycle, expiry, inviter identity, allowed actions, version and mutation/reconciliation outcomes. Define existing-member, duplicate/expired invite, wrong recipient, denied, conflict and unknown-outcome errors. Prototype labels are not role enum values or DTOs.
4. Authentication must bind acceptance to the intended recipient; enforce invitation/member reads and mutations server-side per approved scope. Define queue versus organization membership and private projections. Owner-only demo guards must not be treated as sufficient authorization; client extras cannot establish permission.
5. Persist memberships/invitations, protect the last owner, prevent unauthorized owner transfer/removal, define concurrent role changes and revocation effects on active sessions. Specify single-use acceptance, token/replay protection, idempotent resend/revoke behavior, audit records and retention. Define capacity only if a staff/invitation limit is approved.
6. Distinguish empty list from failed/unavailable lookup. Do not show pending until the server confirms invitation creation; delivery and acceptance are separate outcomes. Define timeout reconciliation, retry/resend restrictions and offline permission behavior. Revocation/role changes cannot be optimistically reported as effective without authoritative confirmation.
7. Fixtures/acceptance needed: no members, missing trusted recipient, long Unicode names, duplicate/self/expired invitation, wrong-account acceptance, repeated token use, denied cross-queue access, simultaneous acceptance/revoke, role-change race, last-owner removal attempt, timeout after commit and audited recovery. Verify no new privilege is obtained by opening or editing the UI.
8. Product approval needed: What exact role/action matrix applies? Is membership queue- or organization-scoped? Which trusted recipient directory exists? Who may invite/change/revoke and at which roles? What are acceptance, expiry, resend/revoke rules? What are owner-transfer/removal and last-owner protections, delivery guarantees and audit requirements?
9. Presentation assumptions: Counter operator, Queue manager and Viewer are explicitly proposed prototype descriptions only, with no permission implementation. Replace these with approved server-supported roles; add trusted recipient selection and validations only from the agreed identity contract. Keep mutation buttons disabled until authorization and outcome handling are integrated.

### 9.4 Next integration and acceptance gate

Backend engineer and product owner must resolve the questions above and the earlier TBD register, provide agreed operations/schemas plus non-sensitive fixtures, and verify authorization, persistence, concurrency and uncertain-outcome handling before frontend mutations are enabled. No permanent product decision is established by this section. Frontend device acceptance remains separate; authorize the existing emulator checks before executing the compiled instrumentation tests and three layout configurations. Backend integration remains pending for all three features.

### 9.5 Frontend acceptance clarification — 2026-10-19

The API-independent frontend passed automated emulator acceptance at normal portrait, 200% font/small portrait and landscape; backend integration remains pending. Evidence: `build/phase4-acceptance-20261019/FINAL-HANDOFF.txt`. Newly actionable integration requirement: return field-addressable validation errors so holder/member errors can attach to their respective labelled inputs instead of a transient global message. Keep draft preference values when reopening date/time controls; returned authoritative availability must remain distinct from those preferences. These are presentation/error-mapping requirements, not an approved schema, identity rule, slot guarantee or endpoint. The earlier unverified-device checkpoint describes the earlier implementation task; this later checkpoint supersedes that frontend test status only. Physical devices, TalkBack, process death and real service operations remain unverified.

## 10. User-approved prototype parity corrections — 2026-10-21

In the full prototype parity chat, the user explicitly chose prototype rules for both early joining and participant arrival. This supersedes earlier native early-join and participant-local Served behavior only; section 5's Today/Queues/Insights adaptations remain unchanged.

- Only OPEN after the scheduled opening admits new remote joins. UPCOMING is not an early reservation, and an OPEN flag cannot bypass a future opening. The local mutation rechecks availability, duplicate held ticket, schedule overlap and cooldown before allocating a number; repeated joins return the existing held ticket. Server admission must enforce its agreed rules atomically with number allocation and registration. Capacity/cutoff/proximity revalidation still require the remaining parity work and backend contract review.
- Ticket registration now belongs to the in-memory issuance mutation, before the Activity callback. This survives Activity finish/recreation within the process; it is not durable ticket persistence, server idempotency or cross-device consistency.
- Participant arrival confirms presence while status stays CALLED. Only organizer completion produces SERVED. Confirmed arrival must survive the original grace deadline and target the correct counter. Waiting tickets cannot be marked Served/No-show; they must be called first. The future backend remains authoritative for identities, authorization and transitions; no endpoint/schema is invented here.
- Saved services are durable SharedPreferences bookmarks on this device, scoped by the existing account/guest session. They reserve no ticket, grant no role and create no alerts. Operational queues/tickets remain process-local FakeData. Settings, privacy beyond bookmark clearing, notification preferences, help/history parity and broader QA remain unfinished.

Evidence and unresolved work: `docs/full-prototype-parity.md` and `build/full-parity-20261021/`. No protected decision file was edited.

## 10. Whole-frontend integration handoff — 2026-10-21

This section supersedes historical verification counts and entry-point descriptions above where later source differs. Status: backend handoff prepared; full frontend acceptance remains open in [the 47-route ledger](full-prototype-parity.md). Backend integration and production readiness are **not** claimed. Evidence for this audit is `build/final-frontend-20261021/`; older evidence remains dated evidence, not a current test result.

### 10.1 Architecture and replacement seams

All source paths below are relative to `app/src/main/java/com/example/quapp/`.

- `QuappApplication` installs notification callbacks and applies saved text preferences. `SplashActivity` routes into authentication/role homes. `Session.homeIntent` clears the navigation task when changing authentication/role destinations.
- `QueuerHomeActivity`: Browse, Tickets, Profile fragments. `OwnerHomeActivity`: Today, owned Queues, Profile. The native shells intentionally combine prototype Home/Explore. Details, Join, ActiveTicket, Called and organizer tools use Activities and explicit ID extras. Treat intent IDs as untrusted when integrating.
- Activities call static `FakeData` read/mutation methods and callback-based `*Async` methods directly. `dispatchAsync` uses a delayed main-thread task and injected failures; it is not HTTP, an offline cache, or a durable outbox. Existing synchronous mutation methods support tests/demo console updates. Inventory all callers before replacing a method; leaving synchronous callers on FakeData creates split state.
- `FakeData` owns mutable queue lists, owned IDs, per-queue waiting/serving/counter state, finished tickets, history, announcements, verification and reports in process memory. `ActiveTicketStore` holds the participant's selected tickets and derives fresh status from FakeData. It is **not durable** and is not a server identity/authorization boundary. `Cooldown` is also local. Process restart can restore fixtures while losing actual demo activity.
- `Session`: device SharedPreferences (`quapp_session`, `quapp_device`); local logged-in/name/phone/role/town and install-account count. No bearer token, server password verification, refresh, revocation or secure token store exists. Password fields are validated but passwords are not persisted. Role QUEUER/OWNER is a UI choice, never server authority.
- `SavedServices`: SharedPreferences `quapp_saved_services`; account phone key versus guest key, device-only. Browse cards/compact rows, Details and Saved screen share this store. Production identity must use authoritative stable user IDs rather than treating a phone string as an authorization credential. Decide migration/sync explicitly.
- `ProfilePreferences`: SharedPreferences `quapp_profile_preferences`; device text/motion, manual area, order and alert flags/read state. Manual area does not calculate distance or filter queues. Reports are process-local records filtered by submitting phone; sign-out/history and public projection privacy require server enforcement.
- `Phase4Activity`: Bundle-backed unsubmitted preferences/labels only; restored when Android supplies saved instance state, not a durable booking/group/invitation store. Family entry now lives in Tickets; Appointments in Profile and owned Console; Staff in owned tools. `PreviewSnapshots` uses a process-only map: missing snapshots show unavailable and Back, never publish silently.
- `LoadingStateHelper` supplies loading/content/empty/error and Retry; `QuappMotion` respects the stored reduced-motion preference and system animator settings. Keep lifecycle guards in async callbacks. Loading/error UI alone does not implement cancellation, response ordering or mutation reconciliation.
- `CalledNotifier`/`HereReceiver` are local notification delivery/actions; no FCM registration or server push channel. `Directions` sends a geo intent and handles absent map apps. `Support` now refuses an empty support address; no message/reset is sent. Supply an approved inbox before populating `forgot_support_email`.

### 10.2 Feature-to-backend matrix

| Feature / client seam | Current source and persistence | Backend capability / existing reference | Remaining client integration and acceptance |
|---|---|---|---|
| Auth/register/recovery; Login/Register/Session/Support | Validated demo session, device prefs; recovery unavailable | MODELS auth/register/login/me; recovery/token lifecycle/inbox TBD | Replace demo success; real validation/error fields, secure token handling, expiry/revocation/account switch; never store credentials in docs |
| Discovery/details; Browse/QueueDetail | All local fixtures, search/category/town/order; no remote page cursor | Existing GET queues and queue by ID | Query cancellation/order, pagination contract, loading/empty/error/stale timestamps, missing/closed IDs; compare selected filters after Back |
| Bookmarks/preferences | Device preferences, guest/account isolation; not server sync | No bookmark/settings endpoint established | Decide local-only versus sync/migration; account IDs, deletion/export; restart and account-switch tests |
| Preparation/admission; QueueDetail/Join/FakeData.join | Bring text with local reminder; OPEN and scheduled opening, overlap/duplicate/cooldown local checks | Existing POST queue tickets with lat/lng; capacity/cutoff absent from MODELS | Product resolution of capacity/cutoff/eligibility; fresh location proof server validation; authoritative admission errors, reconcile uncertain issuance |
| Tickets/arrival/recovery/history | Process-only held/live/finished/history; arrival remains CALLED | Existing me/tickets, here, move-back, delete | Re-fetch after restart/sign-in; holder authorization; explicit arrival confirmation field; no automatic reissue on timeout; terminal receipt is read-only |
| Owner queues/create/preview | Local owned IDs and validated editable form; preview process map | Existing queue create/patch and ownership | Server validation, ownership, unverified one-live limit, stable IDs, atomic save; product conflicts below; lost preview must stay unavailable |
| Console/counters/walk-ins | Shared process line; closed/missing/foreign walk-ins rejected at execution | Existing line/call-next/no-show/walk-ins/removal; counter-specific contract TBD | Authorize every operation; queue/counter target, stale version, serialization, idempotency and result re-fetch; preserve native paused/upcoming walk-in policy until reviewed |
| Announcements/display/insights | One process notice, per-counter serving; queue-specific QueueStats | Existing stats; notice/counter read/write contracts TBD | Owner write/public projection, refresh/failure/last-updated, 500-code-point validation; no prototype session-count substitution |
| Reports/verification/public identity | Session reports and demo verification; public projections assembled locally | Existing reports/me verification; public org projection TBD | Truthful acknowledgement, duplicate policy, field validation, privacy/redaction, admin outcomes/deletion and public relationship authenticity |
| Notifications | Local callbacks and OS permission; saved category flags | Push registration/delivery/ack contracts TBD | Token rotation, sign-out unbind, consent, deduplication, stale notification/deep-link fetch; deny permission without losing ticket |
| Sharing/handover | Explicit local/unavailable boundaries | Sections 3–4; all external values TBD | Resolve real domain/ID/App Links/QR and real recipient/transfer outcome before enabling operations |
| Appointments/family/staff | Unsubmitted Bundle drafts; disabled backend actions | Section 9; no approved endpoint/role model | Product policies first, then authoritative operations; draft clear never mutates individual tickets or privileges |

### 10.3 Contract compatibility findings

`MODELS.md` is the existing planned baseline, not a verified deployed API. Its opening claim that Gson/Retrofit map these models does not match current app code/dependencies: Java models have no `@SerializedName` annotations, and the app Gradle file contains no Gson/Retrofit dependency or configured base URL. Implement explicit DTO/mapping/date adapters as a reviewed integration change; do not serialize the current objects and assume snake_case compatibility.

- String IDs and enum names must remain compatible: Queue UPCOMING/OPEN/PAUSED/CLOSED; Ticket WAITING/CALLED/SERVED/NO_SHOW/QUEUE_CLOSED/REMOVED; role UI QUEUER/OWNER; eight native Category values RELIEF/MEDICAL/GOVERNMENT/EDUCATION/BILLS/IDS/JOBS/OTHER. Prototype's 13 labels are not an agreed wire taxonomy. Unknown enum handling needs review.
- MODELS lists required fields and nullable `?` fields; Java Builders contain defaults and do not replace transport validation. Validate required IDs/schedule/coordinates/status and nested records at the trust boundary; do not coerce unknown data into an apparently successful queue.
- Queue capacity, distinct admission cutoff, eligibility/audience and structured preparation items are not established Queue fields. `bring` is nullable free text; a checked local reminder does not attest eligibility. No capacity endpoint/schema has been agreed.
- Ticket JSON lacks a client-visible arrival confirmation/counter identity although FakeData keeps confirmation and counter slots separately. Define how a fresh client learns `here` succeeded and which counter is serving it. `here_at` exists in the planned database, not the Ticket wire table. This mismatch blocks durable confirmed-arrival rendering.
- Planned queue-level call-next/no-show automatically finish one ticket and call the next; current multiple-counter UI also has separate call/finish operations. Review exact targeting and response semantics before wiring existing endpoint names. Do not silently map all counters to the main counter.
- User verification differs from participant phone verification: organizer badge is manual organization verification; SMS/OTP is planned/unavailable. Do not equate the prototype practice OTP with a trusted phone or organizer badge.
- `Report.reporterPhone` is local attribution added for session filtering. It is not a public JSON field. The server derives reporter identity from authentication, masks holder phone for organizer views, and excludes private user/verification/history data from public projections.
- Dates/times: preserve ISO dates and local daily hours plus authoritative instants; MODELS uses Asia/Manila and `+08:00`. Agree inclusive date bounds, exact closing/cutoff boundaries, multi-day schedules and server clock skew. The demo clock is not authoritative.

### 10.4 Required server guarantees and unresolved product choices

Confirmed client rules to preserve: remote joins need OPEN and opening reached; repeated issuance must not allocate another live ticket; held hours cannot overlap; arrival stays CALLED until organizer completion; WAITING cannot be served/no-show via alternate calls; completion targets the actual counter; owner closure releases tickets as QUEUE_CLOSED without a no-show strike. Existing cooldown baseline is two counted no-shows then 30 minutes for penalty-enabled queues; removal PRANK counts while DUPLICATE/ASKED_TO_LEAVE does not. Server must enforce these independently of UI controls.

Server obligations: authenticate/authorize every read and mutation; reject foreign queue/counter/ticket IDs; atomically allocate unique numbers and line positions; serialize competing calls and admission; return authoritative state/version/timestamps; define conflict errors and retryability. Idempotency keys, version fields and reconciliation lookup are **required capabilities with design TBD**, not agreed field names. A timeout after commit is an unknown result, not failure or permission to issue again. Reconcile before retry. Never trust client owner flags, local capacity, claimed proximity or device account counts as server proof.

User confirmed in this task on 2026-10-21: **keep native creation; add capacity/cutoff after backend/product contract review**. This retains limited unverified creation; prototype verified-only creation is not the current requirement. Capacity/cutoff implementation is explicitly deferred, not an unapproved frontend omission. Future review still must settle counts (issued/live/people/groups), capacity release, distinct cutoff versus closing, and fields/errors. Other unresolved future policies include walk-ins during PAUSED/UPCOMING versus remote admission, participant recovery eligibility, retention/export/deletion, push semantics and offline cache privacy. Existing native behavior remains until explicitly superseded; the prototype does not settle these contracts.

Define pagination ordering/cursors, polling interval or realtime subscription/sequence/resume, rate limits and stale versions before replacing local refresh loops. Define durable notification delivery versus display, duplicate handling and unread acknowledgement. Define logout revocation, token expiry/refresh races, suspended-user handling and safe account switching. A transport retry must not duplicate reports, tickets, calls, bookings or transfers.

### 10.5 Integration setup and dependency order

No API host/token configuration exists in the current app. Backend engineer supplies development/staging HTTPS URLs, OpenAPI version, sanitized request/response/error fixtures, account provisioning for participant/owner/foreign owner/suspended/unverified users, and test queues on known dates/timezones. Values remain TBD. Put environment selection in a reviewed build configuration and secrets in approved local/CI secret storage; never commit credentials, signing keys or production personal data. Confirm package `com.example.quapp`, debug versus release certificates, min SDK 24/target 36 and actual hosting before App Links work.

1. Resolve the product conflicts and contract mismatches in 10.3–10.4; complete section 1 and share versioned OpenAPI/fixtures.
2. Establish auth/session lifecycle, identity, authorization and DTO/date/error mapping. Unlock truthful sign-in, role shells and account-scoped reads.
3. Read-only discovery/details/public projections, paging, stale/error behavior and bookmark identity migration. Unlock participant browsing without writes.
4. Atomic admission/ticket list/history, reconciliation and arrival/move-back/leave. Unlock participant lifecycle and process-death recovery.
5. Owner queue mutations, line/counters, walk-ins, completion/removal/announcements/stats. Unlock cross-device organizer/participant consistency.
6. Push registration and notification/deep-link recovery; real sharing domain/App Links/QR. Unlock delivery and external routing.
7. Trusted recipient transfer and verification/admin/report outcomes. Enable transfer only after atomic/authorized outcome tests.
8. Conditional scheduling/groups/staff only after separate approved policies and server authorization. Keep current draft actions disabled until then.

### 10.6 Integration acceptance scenarios

| Scenario | Required observable outcome |
|---|---|
| Happy path | Participant authenticates, discovers a queue, joins once, organizer calls correct counter, arrival remains CALLED, organizer completes, read-only receipt/history agrees on both devices |
| Unauthorized / cross-queue | Foreign user cannot read private line/stats or mutate queue/ticket/counter; wrong queue/counter target rejected without changing either resource |
| Empty / failed / offline | Empty list distinct from failed request; stale data labelled; retry fetches actual state; writes never show success without confirmed result |
| Duplicate submit / replay | Same operation after repeated tap, rotation or retry creates one result; validation errors do not consume ticket numbers |
| Concurrent last capacity | With approved capacity policy, two independent callers compete for one place; exactly one succeeds; both clients reconcile authoritative remaining capacity |
| Stale availability | Queue pauses/closes/cutoff passes between details and issuance; server rejects stale request without ticket; UI refreshes explanation |
| Timeout after commit | Drop response after ticket/transfer/report commit; client reconciles same operation and does not issue a second write |
| Recreation / process death | Recreate pending screens and kill background process; restore authoritative ticket/status after auth; missing local preview/draft cannot fabricate server success |
| Account switch / sign-out | Prior user's tickets/reports/private projections inaccessible; guest bookmarks separate; push tokens unbound; old callback cannot repopulate new account |
| Correct transitions | WAITING serve/no-show rejected; confirmed CALLED does not expire; finishing counter B cannot finish A; closure is not a cooldown strike |
| Notifications / links | Denied permission retains usable ticket; stale/duplicate/foreign push resolves safely after fetch; valid real link/QR opens exact stable queue; missing/renamed/private queue has agreed outcome |
| External/conditional features | Handover remains disabled absent trusted recipient; no fake delivery; no booking/group/staff privilege from editing draft; server policy tests precede enabling each action |

Manual acceptance script: on a physical min-supported/recent device, exercise touch/Back/keyboard on Login, Register, Create, Join, Report and Lookup; repeat representative portrait, 360x640 at 200% font and 640x360 landscape with long Unicode/empty/error data. Scroll the confirmed-arrival action fully into view; it must read confirmed, remain disabled, retain CALLED, and permit Back. Use spoken TalkBack to verify labels, reading/focus order and modal return; verify reduced motion on affected screens; scan the QR with a second device only after domain resolution exists. Record screenshots/logs and restore settings. Dark theme is outside the recorded light-only product scope. Emulator geometry assertions alone do not close these human/device checks.

### 10.7 Final audit result and current acceptance limits

Current frontend fixes and all47route dispositions are in [full-prototype-parity.md](full-prototype-parity.md), with commands, failures, screenshots and preservation evidence in `build/final-frontend-20261021/REPORT.md`. Unit39/39, both APK builds and bounded participant/organizer regressions pass. Actual compact receipt clipping and landscape Browse target clipping were repaired; confirmed-arrival is fully reachable by programmatic fullScroll. Human gestures/spoken TalkBack/physical-device and full route action-state acceptance remain pending. A force-stop/new-process smoke check preserves four local preference files; it cannot restore process-only demo tickets. This handoff is prepared with those acceptance blockers; frontend completeness/backend integration/production readiness remain distinct and are not claimed.

Confirmed user decision: retain native creation; capacity/cutoff only after backend/product contract review. Do not require verified-only creation or implement prototype capacity fields as an implied part of this handoff. Review the deferred policy/DTO fields before enabling them.


## 11. Authentication and onboarding correction — 2026-10-21

<!-- quapp-master-implementation-20261021 -->
The user approved the single welcome/auth/settings proposal and removal of the local two-account registration restriction. Existing local registration history remains; it no longer establishes eligibility. Do not recreate this demo count as a server policy without an agreed requirement.

Phone sign-in/registration remain local FakeData demonstrations. Password reads preserve whitespace; secrets are excluded from automatic saved state and clear when authentication stops. Cancellation/recreation guards now run before delayed local session writes. These frontend fixes do not implement production authentication.

Google's official current artwork and Google Sans Medium are bundled in an explicitly disabled action. OAuth clients, registered package/signing identities, compatible stable Credential Manager/Google ID dependencies, INTERNET permission, HTTPS backend token verification, issued app sessions, secure storage/refresh/revocation and sign-out contracts remain absent. The package is com.example.quapp; local debug fingerprint evidence is in build/proposal-implementation/final-build.txt. Release signing registration is not verified. Do not infer an endpoint or service from this section.

Preferences now exposes Reduce motion and Service order only. Browse remains the municipality owner; system font scaling remains native. Notifications retains only functional local master/queue-call switches. Family ticket drafts are planning only; no issued group or membership is implied.

Current scoped verification, exact remaining prerequisites and acceptance limits: docs/onboarding-proposal-implementation.md. This updates the frontend status only, not production readiness, server security or the unresolved contracts in sections 1–10.
