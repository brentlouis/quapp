<!-- quapp-master-implementation-20261021 -->
Current targeted override: single welcome, editable demo registration, native auth lifecycle/IME fixes, working-only Preferences/notifications and Family ticket drafts disclosure are implemented. Bounded acceptance passed; genuine authentication/backend and full physical/every-route acceptance remain incomplete. See `docs/onboarding-proposal-implementation.md`. Earlier dated evidence remains historical.

# Full prototype parity — 2026-10-21

Status: IN PROGRESS. This is a traceability ledger, not acceptance of full parity.

Authoritative prototype: `C:/Users/JUSKWAA/.codex/visualizations/2026/10/14/01a1398c-051d-73a3-bb55-b678ce0e53a3/quapp-mode1` (`app.js`, `store.js`, `mock-data.js`, `styles.css`, `COVERAGE.md`, tests). Android: `C:/dev/quapp`, main, HEAD `6b169efde5442d353f82f9fe3a278ac03948c821`. Startup dirty-file hashes and Git inventory: `build/full-parity-20261021/initial-*`. Preserve every pre-existing change.

The 47-route inventory and prototype entry behaviors are captured in `build/full-parity-20261021/prototype-routes.json`. The first action extraction (36 literal comparisons) is `prototype-actions.txt`; it is not a complete handler/state inventory. Each row below also requires normal/error/loading/empty/offline/validation, navigation Back/recreation, persistence and visual comparison where applicable. Unlisted state checks remain NOT VERIFIED.

## Participant and shared routes

| Prototype route | Android source (under app/src/main/java/com/example/quapp) | Current evidence / remaining work |
| --- | --- | --- |
| welcome | SplashActivity, RoleSelectActivity | Native entry adaptation; prototype guest/welcome journey parity NOT VERIFIED |
| sign-in | LoginActivity | Native demo authentication; real authentication pending; error/routing audit needed |
| create-account | RegisterActivity | Native registration; demo OTP parity/validation audit needed |
| phone-verification | No separate native OTP Activity | GAP: simulated native registration does not establish practice OTP parity |
| password-recovery | ForgotPasswordActivity (mapping to verify) | NOT VERIFIED; no real recovery claim |
| home | BrowseFragment inside QueuerHomeActivity | Native three-tab adaptation; separate Home/Explore journey parity incomplete |
| explore | BrowseFragment | Native filters retained; pagination/save/state parity audit needed |
| service-details | QueueDetailActivity | Native schedule, proximity, cooldown, overlap; preparation checklist/capacity/cutoff missing or unresolved |
| saved-services | SavedServicesActivity, SavedServices, OpenNowAdapter, ProfileFragment, QueueDetailActivity | Core implemented: device-local account/guest bookmarks, compact rows/direct Unsave, Profile/detail entry, clear confirmation, empty/unavailable/loading/error states. SavedServicesTest covers recreation, opening, removal, clearing, unavailable bookmarks and default Browse-row isolation. Home/Explore direct bookmark controls remain GAP; error/retry, account isolation and process-death runtime checks remain NOT VERIFIED. |
| join-confirmation | JoinQueueActivity, FakeData.joinAsync/join | F01/F02 reproduced and repaired. QueueMutationSafetyTest covers stale async availability, unavailable/duplicate/concurrent join and finished-screen delivery. Issuance/registration are in-memory, not server/durable ticket persistence. |
| ticket-list | MyTicketsFragment, ActiveTicketStore | Exists; empty/grouping/actions audit needed |
| ticket-waiting | ActiveTicketActivity | Exists; more-time/leave/directions state audit needed |
| ticket-called | CalledActivity, MyTicketsFragment, HereReceiver, MyTicketsAdapter | User confirmed prototype rule in this chat: arrival stays CALLED until organizer completion. Shared confirmation, protected grace deadline, confirmed action and completion polling implemented. Safety tests cover UI/model/deadline. |
| ticket-directions | Directions | Native map intent adaptation; actual destination/permission verification needed |
| ticket-recovery | ActiveTicketActivity released/rejoin states | Recovery and duplicate/cooldown contract audit needed |
| ticket-receipt | ActiveTicketActivity terminal states; ProfileSettingsActivity receipt | Past-visit rows now open read-only receipts. Existing native history styling retained; broader terminal routing remains separate. |
| ticket-history | HistoryActivity / HistoryAdapter | Past visits opens native history; rows open receipts without restoring or mutating held tickets. |
| profile-overview | ProfileFragment / ProfileSettingsActivity | Prototype section order, compact borderless groups, pinned header, Details/Upcoming/Privacy sheets, collapsed organizer tools implemented. Native role navigation retained. |
| profile-preferences | ProfileSettingsActivity / ProfilePreferences | Device text size, reduced motion, manual area and service order persisted. Area does not filter services or change distances; order reaches both Browse branches. |
| profile-notifications | ProfileSettingsActivity / CalledNotifier | Master/category choices, announcement unread/read state implemented; native queue-call notifications respect master/call flags and Android permission. Service/appointment push still pending backend. |
| profile-help | ProfileSettingsActivity; existing ReportQueueActivity | Searchable FAQ disclosures, empty/reset search, demo reconnect, ticket recovery route, service reporting and account-scoped session reports implemented. Legacy HelpActivity preserved for other callers. |
| profile-verification | GetVerifiedActivity | Organizer verification exists; participant OTP versus organizer badge distinction needed |
| dialog-leave-queue | ActiveTicketActivity | Confirm/cancel/state effects NOT VERIFIED this job |
| dialog-report | ReportQueueActivity, FakeData reports | Native form retained; session history and validation/outcome audit needed |
| dialog-notification-permission | NotificationPermissionActivity | Native OS permission adaptation; denial/re-entry audit needed |
| dialog-location-permission | QueueDetailActivity, ProximityCheck | Native permission/location adaptation; denial/manual/offline audit needed |
| dialog-connection-recovery | LoadingStateHelper | Native loading/error/retry; stale snapshot and reconnect parity audit needed |
| dialog-service-change | ConsoleSheets, FakeData.setStatus | Native pause/close; postpone/cancel/reopen semantics audit needed |

## Organizer and conditional routes

| Prototype route | Android source | Current evidence / remaining work |
| --- | --- | --- |
| organizer-today | TodayFragment | Approved all-owned totals adaptation (backend handoff section 5); runtime regression needed |
| organizer-queues | OwnerQueuesFragment | Approved owner-owned/status-filter adaptation; runtime regression needed |
| organizer-create | CreateQueueActivity | Form/preview implemented previously; capacity/cutoff/checklist contract missing |
| organizer-preview | OrganizerPreviewActivity, PreviewSnapshots | Read-only snapshot adaptation approved; current validation/navigation NOT VERIFIED |
| organizer-console | LiveConsoleActivity, WaitingTicketAdapter, FakeData | F03 reproduced and repaired. Waiting menu exposes Remove only; shared mutations cannot serve/no-show a waiting ticket. Called completion targets its actual counter. Safety and affected organizer workflow regressions pass. |
| organizer-counter-list | CounterListActivity | Existing owner-only shared-line implementation; prior CounterWorkflowTest evidence, current run needed |
| organizer-counter | CounterActivity | Existing separate call/complete/missed/open-close; boundary/runtime audit needed |
| organizer-walk-in | ConsoleSheets, LiveConsoleActivity | Native name dialog; counter/eligibility/recovery semantics audit needed |
| organizer-announcement | AnnouncementActivity | Existing owner-only 500-code-point/session notice; current publication/display test needed |
| organizer-insights | QueueAnalyticsActivity | Approved per-queue forecast adaptation (handoff section 5); current regression needed |
| organizer-sharing | ShareQueueActivity, QueueLink | Truthful local/demo sharing boundary approved; real link resolution external dependency |
| organizer-display | CounterDisplayActivity | Prior bounded visual evidence; multiple-counter/announcement state audit needed |
| organizer-handover | OrganizerHandoverActivity | Approved unavailable/retry/disabled transfer boundary; real identity/ownership backend dependency |
| organizer-lookup | OrganizerLookupActivity | Existing owner-only queue-specific lookup; missing/finished/error audit needed |
| public-organizer | PublicOrganizerActivity, PublicProfileActivity | Existing public projection; badge/report/service navigation audit needed |
| public-organization | PublicOrganizationActivity, PublicProfileActivity | Existing public projection; organization relationship backend dependency |
| proposal-appointments | Phase4Activity, Phase4Draft | Prior local draft/unavailable frontend accepted; real scheduling/capacity/timezone policy unresolved |
| proposal-family-tickets | Phase4Activity, Phase4Draft | Prior local draft/unavailable frontend accepted; group-place/member/eligibility policy unresolved |
| proposal-staff-roles | Phase4Activity, Phase4Draft | Prior exploratory/unavailable frontend accepted; roles/invitations/server authorization unresolved |

## Implementation order and acceptance

Confirmed user reply in this chat: use prototype rules for both early joining and arrival. Only OPEN with its scheduled opening reached admits new remote joins. Upcoming join controls, Queue.acceptsJoins and console/card copy follow that choice. A future opening is rejected even if the status still says OPEN. No DECISIONS.md was edited. Proximity revalidation, closing/cutoff/capacity and walk-in admission remain separate audit work.

Evidence: `build/full-parity-20261021/p1-before.txt` (four reproduced failures), `p1-after.txt` (four pass), `batch2-final-runtime.txt` (13 pass), final build/UI logs and screenshot folders. Failed builds/fixture checks and rejected transition captures are preserved. Prototype store/UI scripts pass separately; this does not verify Android parity.

1. Reproduce F01 availability/duplicate, F02 delayed lifecycle delivery, F03 waiting terminal transitions. Repair once in shared mutation paths and adjust affected fixtures. Run focused regressions before broader checks.
2. Complete participant durable bookmarks and account/settings/help/history flows, with actual prototype handler/state audit and existing native conventions.
3. Reconcile preparation/capacity/cutoff/arrival semantics with approved native contracts. Ask only on concrete unresolved conflicts; continue independent work.
4. Regress all organizer/shared workflows and Phase 4 unavailable boundaries. Do not enable fake backend operations.
5. Compare representative actual Android/prototype screenshots, large fonts/short screen/keyboard/accessibility; run Phase 6 build/QA. Source presence alone is not runtime/visual evidence.

Phase 0–2: historical bounded completion; expanded full-parity acceptance pending. Phase 3: previous bounded frontend acceptance, expanded audit in progress. Phase 4: prior local-draft/unavailable frontend acceptance, backend policies pending. Phase 5: prior bounded motion completion, expanded screens pending. Phase 6: baseline cached build succeeds; full QA not complete. Backend integration not started.

## Verified checkpoint

- `opening-final-build.txt`: latest incremental unit/APK/test-APK build PASS, 3 seconds; 39 unit tests, zero failures/errors in XML. `opening-final-runtime.txt`: final normal safety/bookmark suite PASS, 12 tests, 13.935 seconds.
- `batch2-final-runtime.txt`: affected organizer and earlier safety/bookmark suite PASS, 13 tests, 92.037 seconds. This predates the final saved-row presentation; owner flows were not repeatedly retested after that isolated change.
- `checkpoint-normal-ui.txt`: 11 tests PASS, 13.808 seconds; `checkpoint-large-ui.txt`: two PASS, 13.316 seconds; `checkpoint-landscape-ui.txt`: two PASS, 12.069 seconds. Later geometry/screenshot-bounds landscape rerun PASS, two tests. Display restored/read back: font 1.0, physical 1080x2400/density420, no size override, autorotation1/userRotation0. Normal launcher reopened.
- 24 final screen captures reviewed in `checkpoint-contact.jpg`, with individual populated/large-dialog/landscape inspection. Prototype empty and populated Saved services were inspected live and saved as `prototype-saved-empty.png` / `prototype-saved-populated.png`. Earlier native card presentation was replaced with the existing compact-row component. This is a representative visual comparison, not full visual acceptance.
- Important evidence mismatch: the confirmed-arrival badge remains clipped in landscape screenshots, although action geometry and screenshot-coordinate assertions pass. Treat visual scroll acceptance there as OPEN; do not infer manual scroll success from those assertions. Saved-services scroll-to-Explore is visible in the separate geometry capture. Physical device, spoken TalkBack, keyboard on other forms, full manual gestures, all-route visual QA and backend persistence are NOT VERIFIED.
- All 154 startup dirty/untracked files remain present; 141 byte-identical, 13 intentional overlaps. Seven previously clean files and seven new files bring the task change inventory to 27 files. Initial/final status: 92/106 porcelain entries, no staged entries, same main/HEAD. Changes were applied in targeted hunks; overlapping pre-existing changes were not discarded. Protected files remained byte-identical. No stage/commit/push/fetch/branch switch/Graphify/dependency operations.

This checkpoint does not finish the requested mission. The 47 routes are mapped, but the complete per-action/per-state audit and remaining implementation are unfinished. Next: finish participant bookmark entry points and profile/settings/notification/privacy/help/history gaps, then complete admission/preparation rules and broader QA. Existing external-service and conditional-feature policy boundaries still apply.

## Profile parity checkpoint — 2026-10-21

User-authorized Profile port follows local quapp-mode1 app.js/styles.css, not a new visual direction. Account details use existing session data, with no invented email. Guest bookmarks no longer expose the remembered signed-out account. Phone inputs remain required authentication identifiers; sample phone-number hints were removed from Login and Register only. Signup/login validation, account data and device-account limits remain.

Family draft access moved to Tickets, and expanded organizer tools include customer return for owners. Staff routes still require an owned service; appointments/staff/family remain explicit local drafts rather than fabricated backend operations. Native three-tab shells, existing history appearance and organizer verification are intentional reuse boundaries, distinct from a pixel-identical copy of every prototype route. Full 47-route parity remains incomplete. Evidence/checks/file inventory: build/profile-parity-20261021/REPORT.md.

## Final frontend audit ledger — 2026-10-21

This section is the current override for earlier GAP descriptions. Evidence root: `build/final-frontend-20261021/`. A passing test closes its asserted scenario only. No route below is labelled wholly verified merely because an Activity exists. Status vocabulary: **Verified frontend complete** (bounded asserted behavior), **Frontend implementation required**, **Frontend verification required**, **Approved native adaptation**, **Backend integration pending**, **Product decision required**, **Device/manual verification pending**.

Shared acceptance dimensions apply to **every row**: normal/empty/loading/error/unavailable; validation/permissions; Back/cancel/repeated actions; recreation/process death; persistence scope; portrait/compact 200%/landscape and accessibility. A dimension absent from the named test remains **Frontend verification required**, or **Device/manual verification pending** when it specifically needs human/device acceptance. Local-only screens do not need fabricated network loading or success states. Backend pending does not close a frontend defect.

| # / route | Entry → actions/result and Back path | Current state/persistence and evidence | Explicit remaining status |
|---|---|---|---|
| 01 welcome | Splash/role selection → guest Browse, Login, Register; Back exits/returns | Native role/splash adaptation; ParticipantVisualReviewTest covers auth layout/empty inputs | Approved native adaptation; Frontend verification required for full guest/relaunch journey |
| 02 sign-in | Phone/password demo → RoleSelect; recovery/register/Back | Verbatim secrets, saved-state exclusion, guarded submit/cancellation; AuthProposalTest/ProfileParityTest | Bounded normal/320dp200%/landscape/IME acceptance passed; real OAuth/phone auth and physical/TalkBack acceptance pending |
| 03 create-account | Editable name/phone/password/confirmation demo → RoleSelect; no local account-count lock | Existing history retained; nonsecret recreation, secret clearing/masking, validation and IME tested | Bounded frontend acceptance passed; genuine server eligibility/authentication pending |
| 04 phone-verification | No participant OTP route; registration does not verify phone | Native decision: SMS future work; practice OTP would not authenticate anyone | Approved native adaptation; Backend integration pending for real phone verification, no fabricated OTP success |
| 05 password-recovery | Login → recovery → unavailable support notice / Back | Support address blank; no message/reset; FinalFrontendAcceptanceTest checks no external launch | Verified frontend complete for unavailable support seam; Backend integration pending for inbox/recovery; manual route acceptance pending |
| 06 home | Queuer Browse tab → active ticket, categories, details | Combined native Home/Explore; local fixtures; BrowseJoinVisualReviewTest | Approved native adaptation; Frontend verification required for full pointer/filter journey |
| 07 explore | Browse search/town/category/order → results/detail; direct Save/Unsave | Full local result set, no server paging; both row/card bookmark controls now present; SavedServicesTest | Verified frontend complete for tested direct bookmarks; Backend integration pending for paging; Device/manual verification pending |
| 08 service-details | Browse/Saved/public queues → details, reminder, bookmark, organizer, directions, report, Join | Nullable bring free text rendered as one non-gating preparation check; Android view state handles recreation; local reminder only | Reminder recreation verified; structured requirements need contract review; capacity/cutoff explicitly deferred by user; location/device checks pending |
| 09 saved-services | Profile → saved list → details; remove/clear/cancel/Explore | Account/guest device prefs; missing saved ID distinguishable from no bookmarks; SavedServicesTest recreate/clear/cancel/isolation | Verified frontend complete within tested local scope; process-death/manual acceptance pending; no server sync claimed |
| 10 join-confirmation | Details admission/location → identity/edit/submit → ticket; Back cancels screen | Shared join checks latest availability/overlap/cooldown, registers before callback; QueueMutationSafetyTest duplicate/concurrent/lifecycle | Verified frontend complete for asserted safety; Capacity/cutoff explicitly deferred by user; Backend integration pending |
| 11 ticket-list | Tickets tab → live/current terminal ticket, family draft | Process held tickets, native grouping; ProfileParityTest family entry, organizer tests shared ticket updates | Frontend verification required for every list state/process restart; Backend integration pending |
| 12 ticket-waiting | Ticket list/banner → details/more-time/directions/leave | Process state, once-only move-back preview; TicketRulesTest + existing visual tests | Frontend verification required for full cancel/repeat/lifecycle matrix; Backend integration pending |
| 13 ticket-called | Notification/list/console → arrival confirmed; organizer later completes | CALLED retained; timer hidden/confirmation disabled; QueueMutationSafetyTest includes correct-counter and grace safety | Verified frontend complete for tested transitions; landscape screenshot/scroll and physical acceptance separately pending until evidence reviewed |
| 14 ticket-directions | Detail/active ticket → external geo map; Back from map returns | No permission needed to display venue; missing map handler gives snackbar; source Directions | Approved native adaptation; Device/manual verification pending for installed/absent map handler and actual destination |
| 15 ticket-recovery | Released/terminal ticket → read reason/rejoin if allowed | Native cooldown/overlap rules, process tickets; terminal safety retained | Product decision required for prototype unrestricted recovery conflict; Frontend verification required; Backend integration pending |
| 16 ticket-receipt | Terminal ticket/history row → read-only receipt → Back/Done | No restoring active tickets or mutating history; ProfileParityTest history receipt | Verified frontend complete for tested history entry; other terminal states Frontend verification required |
| 17 ticket-history | Profile Past visits → history → receipt | Process-only FakeData history; logout clears local history | Verified frontend complete for tested read-only navigation; Backend integration pending for durable/account-authorized history |
| 18 profile-overview | Profile → Details/Upcoming/Privacy/settings/history/organizer tools/logout | Guest identity guards, compact groups, native role return; ProfileParityTest | Verified frontend complete for tested functional scope; Device/manual verification pending |
| 19 profile-preferences | Profile → Reduce motion / Service order → Back | System text only; custom scaler/callbacks and duplicate area removed; legacy sort values preserved | Bounded persistence/sort/reset/200%/landscape acceptance passed; physical accessibility pending |
| 20 profile-notifications | Profile → local master / queue-call choices / announcement read state → Back | CalledNotifier permission/master/queue readers retained; inert switches removed | Bounded persistence and layout acceptance passed; real OS denial/re-entry and remote notifications pending |
| 21 profile-help | Profile → FAQ search/empty/reset/reconnect/recovery/reports | Reconnect is demo only; reports account-filtered process memory; ProfileParityTest | Verified frontend complete for tested help/search; Backend integration pending; pointer/keyboard manual acceptance pending |
| 22 profile-verification | Owner/Profile → GetVerified → local request/result | Manual organizer badge, not participant phone proof; native decision retained | Approved native adaptation; Backend integration pending; complete request/error/recreate Frontend verification required |
| 23 dialog-leave-queue | Active ticket → confirm leave/cancel → terminal/list | Existing local mutation; no server cancel/timeout reconciliation | Frontend verification required for repeated/cancel/rotation; Backend integration pending |
| 24 dialog-report | Details/Join/ticket/trust → reason/details/submit/Back | Explicit process-only outcome; duplicate in-flight guard; FinalFrontendAcceptanceTest + ProfileParityTest account attribution | Verified frontend complete for asserted validation/duplicate/outcome; Backend integration pending; keyboard/manual acceptance pending |
| 25 dialog-notification-permission | First join (API33+) → Allow/Not now → ticket underneath | Asked flag device prefs; native OS permission; no fake delivery | Approved native adaptation; Device/manual verification pending for actual deny/permanent deny/re-entry |
| 26 dialog-location-permission | Details → rationale/OS choice/settings/check again | One-shot framework location; deny has Settings/Browse; join location proof not persisted | Approved native adaptation; Device/manual verification pending; Backend integration pending for authoritative radius validation |
| 27 dialog-connection-recovery | Loading/error Retry and Profile demo reconnect | FakeData delayed failures; LoadingStateHelper; BrowseJoinVisualReviewTest error/loading/retry | Verified frontend complete for tested demo retry; Backend integration pending for stale versions/offline/real transport |
| 28 dialog-service-change | Console menu → pause/resume/close/extend/reopen | Native status lifecycle; owner-close QUEUE_CLOSED not no-show; OrganizerWorkflowTest | Approved native adaptation for native status enum; Product decision required for prototype postpone/cancel; Backend integration pending |
| 29 organizer-today | Owner Today → owned queue actions / Manage queues | Approved all-owned totals; TodayNavigationTest preserves selected filter | Verified frontend complete for asserted navigation; Approved native adaptation; backend pending |
| 30 organizer-queues | Owner Queues → filter/detail/Create | Owner-owned, not manager assignment; TodayNavigationTest/OrganizerWorkflowTest | Approved native adaptation; Frontend verification required for complete invalid/empty/error matrix |
| 31 organizer-create | Queues → validated Create/Edit → Preview or save | Native limited-unverified rule; local queues; CreateValidationNavigation/ExtendedOrganizer tests | User confirmed native creation; capacity/cutoff deferred to contract review; broader create QA pending; backend pending |
| 32 organizer-preview | Valid Create/Edit snapshot → fields → Back to form | Process-only PreviewSnapshots; missing snapshot unavailable; no publish action here | Approved native adaptation; ExtendedOrganizerWorkflowTest read-only/no-write checks; process-death visual acceptance pending |
| 33 organizer-console | Owned queue → line/call/finish/remove/options | WAITING completion guarded; called target preserved; QueueMutationSafetyTest and OrganizerWorkflowTest | Verified frontend complete for asserted mutation safety; full ownership/stale lifecycle matrix Frontend verification required |
| 34 organizer-counter-list | Console → counter list/add/select → Back | Owner-only process counters, shared waiting line; CounterWorkflowTest | Verified frontend complete for tested operations; Backend integration pending; manual acceptance pending |
| 35 organizer-counter | Counter list → call/finish/missed/open-close/display | CounterWorkflowTest/QueueMutationSafetyTest correct target/foreign/closed behavior | Verified frontend complete for asserted counter operations; backend concurrency/restart pending |
| 36 organizer-walk-in | Console → name dialog/validate/Cancel/Add → waiting tail | Native !CLOSED UI; shared mutation now rejects missing/foreign/CLOSED at execution; QueueMutationSafetyTest | Verified frontend complete for asserted guards; Product decision required for PAUSED/UPCOMING eligibility versus prototype; backend pending |
| 37 organizer-announcement | Console → text/publish/replace/clear → display | One plain-text 500-code-point process notice; ExtendedOrganizerWorkflowTest | Verified frontend complete for tested owner validation/display; Backend integration pending |
| 38 organizer-insights | Today/Console selected queue → QueueStats/forecast → Back | Approved per-queue native forecast; OrganizerWorkflowTest and prior bounded visual evidence | Approved native adaptation; full current visual acceptance Frontend verification required; backend pending |
| 39 organizer-sharing | Console → local/demo share/copy/QR → Back | QueueLink placeholder name slug; real resolution unavailable by design | Approved native adaptation; Backend integration pending; Device/manual verification pending for real delivery/QR resolution |
| 40 organizer-display | Console/counter → serving/up-next/notice/QR | Process polling, multi-counter projection; Counter/ExtendedOrganizer tests | Verified frontend complete for asserted state updates; distance legibility/QR/device acceptance pending |
| 41 organizer-handover | Console → unavailable recipients → Retry/Back; Transfer disabled | Owner rechecked on resume; isolated HandoverFrontendTest retains exact ownership/line assertions | Verified frontend complete for unavailable boundary; Backend integration pending |
| 42 organizer-lookup | Console → exact number → masked result/missing/error → Back | Owner-only selected queue; invalid/overflow inputs; ExtendedOrganizerWorkflowTest | Verified frontend complete for asserted lookup; backend/private projection and manual keyboard pending |
| 43 public-organizer | Detail/trust → public profile → organization/services/report | Public source projection; ExtendedOrganizerWorkflowTest; badge scope separate | Frontend verification required for full projection/error/Back matrix; Backend integration pending |
| 44 public-organization | Organizer → organization → public services/organizer | Relationship is demo-derived; no real organization directory | Frontend verification required; Backend integration pending for trusted relationships |
| 45 proposal-appointments | Profile or owner Console → preference/review/status/manage/clear | Bundle-only; reserve/cancel/reschedule disabled; Phase4FrontendTest | Verified frontend complete for unavailable/draft boundary; Product decision required; Backend integration pending |
| 46 proposal-family-tickets | My tickets → Family ticket drafts row → Prepare draft → Review local draft | Same-touch-target availability disclosure; exact planning-only landing; state/Back retained; live creation disabled | Bounded normal/compact200%/landscape checks passed; genuine group/membership/eligibility/capacity contracts pending |
| 47 proposal-staff-roles | Owned Console/tools → role description/review/unavailable | No invitations/members fabricated; no privilege from labels; Phase4FrontendTest | Verified frontend complete for tested unavailable boundary; Product decision required; Backend integration pending |

### Frontend blockers and disposition

- **Confirmed native adaptation / deferred contract work:** user answered on 2026-10-21: keep native creation; add capacity/cutoff after backend/product contract review. Limited unverified creation stays. Prototype capacity/cutoff/verified-only behavior is not a current implementation blocker; future capacity semantics and wire fields remain unresolved.
- **Product decision required:** whether walk-ins follow OPEN/opening restrictions or retain current native non-CLOSED admission; whether prototype postpone/cancel and unrestricted recovery replace native terminal/cooldown rules. Native policies remain until explicitly changed.
- **Frontend verification required:** route-specific untested lifecycle/permission/terminal branches in the table; full per-action acceptance remains open. Current tests establish bounded behavior only.
- **Device/manual verification pending:** actual touch, spoken TalkBack, physical-device keyboard/gestures, second-device QR/external link resolution and background-process recovery. Exact script is in backend handoff section 10.6.
- **Backend integration pending:** authentication/token/recovery, durable tickets/history, authoritative admission/ownership/concurrency/time, reports/verification, push, trusted public identity/domain/transfer and conditional features. These are separate from the frontend blockers above.

Implemented in this audit: direct Browse bookmarks in both row/card branches (owner cards unchanged); optional preparation reminder using the existing free-text Bring requirement; truthful local report/recovery outcomes and no placeholder support recipient; in-flight report duplicate guard; walk-in mutation-time missing/owner/closed checks. Handover's old live-fixture count failure was corrected by a disposable queue, preserving exact count and ownership assertions.

Preparation is intentionally non-gating. One checkbox preserves the complete native free-text requirement rather than guessing that punctuation separates independent eligibility items. Structured per-item requirements and server attestation need a reviewed model. Its checked state is view saved state, not a durable server claim. Missing bring text hides the check; it never implies that no requirements exist.

### Final verification and confirmed scope override

User confirmed 2026-10-21: retain native creation and defer capacity/cutoff to backend/product contract review. This supersedes earlier rows describing those choices as unanswered. Reminder recreation is verified; leave Cancel preserves the ticket and Confirm releases it; notification Not now retains the ticket. These tests do not simulate a real OS denial or server recovery.

A further actual frontend defect was reproduced: at 640x360, pinned Browse filters left only 98px of the 126px bookmark target visible. BrowseFragment now lets the filters scroll/return on windows shorter than480dp. Normal/compact pinning remains. Exact same target visibility assertion passes after repair. Landscape final suite `short-window-landscape.txt` 5/5 PASS; dedicated normal2.748s and compact2.711s checks each1/1 PASS. Actual final normal, compact and repaired landscape bookmark screenshots inspected. Earlier failed `delivered-landscape.txt` is retained.

Other evidence: 39unit tests zero failures/errors; both debug APK builds PASS after production changes; final-normal31/31 PASS; affected organizer/safety17/17 PASS; corrected compact reminder/report/recreation3/3 PASS; system reduced-motion3/3 PASS. The compact line-width failure used trailing whitespace; corrected test checks visible Layout.getLineMax, no ellipsis and line bounds. Earlier failed logs remain in the evidence root. Receipt hours now wrap visibly at200% font. History/Profile shared-slip regression passed in the normal suite.

Landscape arrival: initial requestRectangleOnScreen capture remains clipped despite its assertions; `scroll-screens/scroll-landscape-arrival-confirmed-full-scroll.png` shows the entire action after actual fullScroll. This closes programmatic scroll reachability, not human gesture acceptance. No production Called layout change was required.

Process restart: confirmed running PID20293 stopped, absent PID verified, new PID20385 launched via Splash with COLD status. Four local preference file hashes match before/after. This is a force-stop/cold-launch smoke check, not Android background saved-state restoration or durable ticket recovery. FakeData/held tickets remain process-only. Emulator restored physical1080x2400/density420/font1.0/autorotation1/userRotation0; originally absent animator_duration_scale restored by deleting temporary setting and readbacknull.

Full47route acceptance remains INCOMPLETE: remaining route-specific permission/recovery/terminal/lifecycle checks are explicitly listed above; physical gestures, spoken TalkBack, older-device behavior and external/backend flows remain pending. This audit prepared the backend handoff and repaired observed defects; it does not certify frontend complete or production ready. Full report and exact file/verification evidence: `build/final-frontend-20261021/REPORT.md`.
