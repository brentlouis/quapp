# Quapp proposal implementation — 2026-10-21

Result: **working local demo with the requested frontend changes implemented and bounded native acceptance passed**. Whole-app acceptance and production readiness remain incomplete. Real Google/phone authentication, recovery, reservations, staff membership and family issuance need genuine services.

## Source and preservation

Active source is `C:/dev/quapp`, branch `main`, HEAD `6b169efde5442d353f82f9fe3a278ac03948c821`. The supplied Documents directory contains source but is not a Git checkout. It was not edited or treated as synchronized.

Initial status contained 166 entries. Initial tracked diff and 163 dirty-file hashes are retained in `build/proposal-implementation/initial-*`. All 163 files remain present; intentional overlaps are enumerated in `final-metadata.json`. Directory entries were not recursively baseline-hashed. Unrelated tracked/individually hashed files remain unchanged. No reset, restore, clean, stash, staging, commit, push, data wipe, dependency change, Graphify maintenance or Obsidian setting change occurred.

## Requirement checklist

| Requirement | Evidence and status | Files / verification |
| --- | --- | --- |
| Single welcome | Verified within emulator scope: one screen, Create account, Sign in, guest Browse | OnboardingActivity/activity_onboarding; AuthProposalTest and OnboardingWorkflowTest |
| Completion migration / Back | Verified: established session bypasses welcome, existing completion still routes to auth; welcome/auth Back and guest access preserve session boundary | Splash retained; completionMigrationAndTicketEntry, welcomeAuthBackAndGuestWithoutSession |
| Registration restriction | Removed and verified editable with at least two local entries; existing history retained | RegisterActivity, Session; registrationUnlockedAndErrorsRecover |
| Authentication input | Verbatim password reads; name/phone rules unchanged; Next/Done share guarded submit; first error focused/revealed, edits clear stale errors | AuthForms, Login/Register; native keyboard, invalid and whitespace tests |
| Authentication lifecycle | Verified nonsecret recreation, password clearing, default masking, revealed-password exclusion from saved-state parcel, failure/repeated-submit guards, cancelled registration | AuthProposalTest; FakeData auth operations reject finishing/destroyed caller before session mutation |
| Recovery | Existing unavailable/local support boundary retained; no fake success added | ForgotPasswordActivity, Support, existing FinalFrontendAcceptanceTest |
| Google presentation | Verified disabled accessible native action with official current gradient G, Google Sans Medium, white surface and explicit unavailable copy | activity_login, official asset/font/license; source provenance in google-sign-in-assets.md |
| Genuine Google auth | Externally blocked | Missing OAuth clients, registered signing identities, libraries, network/session contract and deployed backend; see below |
| Preferences | Verified Reduce motion and Service order only; legacy sort values remain compatible | ProfilePreferences, ProfileSettingsActivity/ProfileFragment; ProfileParityTest persistence/sort/reset |
| System text | Custom multiplier, recursive traversal, global callbacks, size/area selectors and obsolete IDs removed; system sp/fontScale preserved | QuappApplication and Profile files; normal, 320dp/200%, landscape checks |
| Notifications | Working master/local queue-call switches and permission guards retained; inert service/appointment switches removed | CalledNotifier/NotificationPermissionActivity readers inspected; ProfileParityTest persistence and guards |
| Family drafts | Verified My tickets row includes draft-only subtitle in the same touch target; landing has exact planning-only disclosure; Prepare/Review/recreation work; live creation stays disabled | MyTicketsFragment/layout, Phase4Activity/strings; AuthProposalTest, Phase4FrontendTest, ProfileParityTest |
| Other existing routes | Existing Slate & Mint redesign and route implementations retained; bounded Browse, bookmark, preparation, reporting and organizer-console regressions passed | FinalFrontendAcceptanceTest, OrganizerWorkflowTest; prior route ledger remains the source for unchecked states |

`verified` above means the named automated checks and captured emulator states, not physical-device, TalkBack or every-route acceptance. No new policy was invented for capacity/cutoff, walk-in admission, terminal ticket recovery or ownership.

## Changes and retained behavior

Registration's dead form came from `Session.deviceAccountLimitReached()` and `RegisterActivity.bindDeviceLimit()`, not a keyboard interceptor. Both obsolete limit API and UI/resources were removed. `Session.register()` still preserves local registration history; no stored account was deleted.

Auth passwords bypass `Forms.text()` so the shared trimming behavior remains unchanged elsewhere. Secret fields and their Material layouts disable saved state, including revealed-toggle restoration. Passwords clear and remask when auth stops. No password is passed into registration storage, preferences or logging. Existing backup configuration was inspected; no new secret data is written for backup to collect. Login remains explicitly simulated; FakeData does not verify a real password.

The first compact keyboard run reproduced a zero-width field after the fixed dock consumed the viewport. Both existing action docks now live inside their existing native form scroll containers. Existing keyboard/system inset handling remains unchanged. No keyboard-height heuristic, ViewModel or dependency was introduced.

Welcome uses the existing mark, color/type resources, native buttons and scroll. It has no new animation. System disabled animations and Reduce motion still govern the unchanged QuappMotion helpers. Family entry reuses `view_list_row`/`ListRow.bind`; individual tickets and existing draft state/navigation remain separate.

## Verification actually performed

From `C:/dev/quapp`, using existing JBR, Gradle cache and connected `emulator-5554`:

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:GRADLE_USER_HOME='C:/Users/JUSKWAA/.gradle'
./gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline --console=plain
```

- 39 unit tests, zero failures/errors/skips. Both debug APK builds passed.
- Final build/lint after the family-row refinement passed: `final-row-build.txt`; lint **0 errors, 297 warnings**. Warnings remain; they are not presented as a clean lint result.
- Auth/onboarding/Profile normal suite: **24/24 passed**, `final-normal.txt`.
- Expanded final normal auth + Browse/preparation/report + organizer-console suite: **14/14 passed**, `verified-normal.txt`.
- Final expanded compact 320dp / system fontScale 2.0: **9/9 passed**, `verified-compact200.txt`.
- Final expanded 640dp × 360dp landscape: **9/9 passed**, `verified-landscape.txt`.
- Reduced-motion checks: **2/2 passed**, `native-reduced.txt`.
- Isolated existing draft/staff boundary suite: **3/3 passed**, `verified-phase4.txt`.
- Last family-row refinement: **2/2 normal**, **1/1 compact 200%**, **1/1 landscape**, `final-row-*.txt`. The whole row, including subtitle, must be fully visible before entry.
- The previously blocked participant visual test now passes **1/1**, `final-participant-visual.txt`; the final empty registration screenshot was captured and compared.
- `git diff --check` passed. APK installs used `-r`; app data was not wiped.

These runs overlap; do not add them as unique coverage totals. The 39-unit run predates only the final view-row replacement; affected native checks and both APK/lint builds were rerun afterward.

Native input used Espresso touch/input injection and the installed IME. It is not human manual typing or hardware-keyboard testing. Actual IME Next/Done, password toggles, first-error focus and field visibility were asserted. At landscape, Android's native fullscreen extract editor is expected and was captured.

Failed runs retained: initial Gradle home misresolution; test compile attempt using a non-public Material icon method; stale slide-count/switch-count expectations; initial compact keyboard failure; an earlier combined run reporting `Process crashed.` at a staff test. The crash buffer did not establish a cause. Isolated Phase4 suite subsequently passed 3/3; no speculative production fix was added for that process crash. A pre-fix replay of new tests against reverted source was not performed.

## Screenshot audit and native visual QA

Accepted captures live in `build/proposal-implementation/verified-*-screens/`, plus final `final-family-row-*.png`. Each set was saved, opened as contact sheets and inspected. Earlier captures retain intermediate defects for comparison. Native screenshots, not browser rendering, provide the evidence.

| Step | Screen | Health in inspected states |
| --- | --- | --- |
| 1 | Welcome / scrolled actions | Clear three-action entry; all actions reachable at enlarged text and landscape |
| 2 | Sign in / failure / unavailable Google | Honest local boundary; readable failure; Google remains disabled |
| 3 | Register / errors / IME / recreated | Editable, masked by default, nonsecret state retained; compact viewport repair verified |
| 4 | Preferences / scrolled order | Only two connected settings; native scrolling preserves action reachability |
| 5 | Notifications | Only working local switches; real OS permission still separate |
| 6 | My tickets / Family draft entry | Combined title/subtitle fully visible together after final row repair |
| 7 | Family landing / review | Planning-only disclosure precedes entry; Unicode labels survive recreation; no group creation |

The supplied board and native welcome were compared together in `proposal-native-comparison.png`. Board typography/device shell are approximations; native typography, platform bars, existing backgrounds and password icons remain authoritative. This is a native layout-specification translation, not a pixel-identical website or a Sites prototype. The bundled Design QA comparison checklist was applied to this native adaptation; project-root `design-qa.md` records the passed scoped comparison and intentional differences. No browser verification or web score is claimed.

### Interface polish review — full requested scope

| Category | Evidence inspected | Result |
| --- | --- | --- |
| Typography | Java/XML text roles, 200% captures, long Unicode name/draft fields | Native roles retained; single-line fields scroll; summaries wrap |
| Surfaces | Welcome panel, auth fields/actions, Preferences, family row | Existing resources reused; accidental pill-shaped welcome panel corrected |
| Animations | QuappMotion consumers, no new welcome animation, reduced/system-disabled checks | Native ripple and reduced-motion behavior retained; slow-motion/human motion audit not performed |
| Icons | Back/password Material icons, official Google download, family row | No imitation logo; no new icon family |
| Performance | Scaler callers, application lifecycle callbacks, dynamic settings | Recursive text-size work removed; no measured speed claim |

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| HIGH | RegisterActivity / Session | Two local entries disable four fields | Editable demo form; local history retained | Remove the confirmed dead-form cause |
| HIGH | AuthForms.java:24 | New single-line setup reset password transformation | Preserve transformation and assert default masking | Keep secrets hidden by default |
| HIGH | activity_login.xml / activity_register.xml | Fixed dock collapses compact IME viewport | Actions scroll with fields | Keep inputs and submission reachable |
| MEDIUM | MyTicketsFragment.java:63 / fragment_my_tickets.xml | Subtitle could fall below visible button | Shared two-line navigation row | Show draft-only availability before entry |
| MEDIUM | OnboardingActivity / activity_onboarding | Repeated welcome and slides | One useful orientation screen | Reduce navigation steps |
| MEDIUM | ProfilePreferences / QuappApplication / Profile settings | Custom text scaling, duplicate area and inert switches | System text scaling and connected controls only | Remove misleading settings and traversal work |

Considered and rejected: a new auth ViewModel/dependency (native saved-state policy suffices); wiring another municipality owner (Browse already owns it); bespoke press scaling/springs (existing ripple preserves stable hit targets); live group or Google session simulation (no authoritative service).

### Break UI

| # | Severity | Field / value | What broke | Fix and verification |
| --- | --- | --- | --- | --- |
| 1 | Broken | Register IME at 320dp/200% | Fixed dock consumed form viewport; field width became zero | Move both auth docks into native scroll; activity_register.xml and activity_login.xml; same keyboard test passed |
| 2 | Broken | Password after single-line setup | Default transformation was lost | AuthForms.java:24 preserves transformation; native toggle/default-mask and saved-parcel tests pass |
| 3 | Fragile | Family row at 200% | Button could be seen before separate draft-only subtitle | MyTicketsFragment.java:63 and fragment_my_tickets.xml use one shared row; full-row bounds pass in three sizes |

What held up: `李` satisfies the unchanged nonblank name rule; long Polish/diacritic/emoji names retain native horizontal editing; family labels `Jo`, `家族 🌿`, `Nguyễn Thị Minh Khai` wrap and recreate; empty fields show local errors; password trailing space remains significant; empty My tickets and absent remote membership remain truthful. Name and draft-label limits are unbounded under existing validation; no invented Western-name or length restriction was added.

Fixtures remain only in `AuthProposalTest`, not production. Native test methods replace the web-only demo/worst-case toggle. No new counts, media, timestamps, remote collections or translated locales were introduced by these slices. Unchanged huge lists/count formatting, dark/RTL and localization are not claimed as newly stress-tested.

Decisions remaining: none needed for these local corrections. Production identity, family eligibility/capacity and membership require real contracts.

## Final correctness and simplicity review

What this change does: replaces repeated onboarding with a direct entry, repairs native demo authentication and removes inert settings. It preserves the existing queue app and makes family planning explicitly draft-only.

Senior PR Reviewer inspected affected callers, lifecycle callbacks, session writes, saved-state behavior, settings consumers, layouts and meaningful regression coverage. Confirmed findings above were fixed; no remaining actionable blocker was identified in the changed local paths. This does not certify unrelated dirty changes or production security.

Ponytail Review: existing native scroll/insets/Material fields, ListRow, sorting, notification guards and motion tokens are reused. AuthForms has two actual callers and centralizes their shared secret/IME/error policy. No speculative endpoint, wrapper framework, dependency, new product screen or shipped test switch was added. The small official font resource is required by current Google branding, scoped to that action and licensed in the APK.

Verdict: ship for local demo review. Production release remains blocked. Not checked: physical hardware, spoken TalkBack, all 47 routes/states, older Android versions, dark/RTL/localization, physical QR/maps, real OS denial/re-entry, genuine OAuth, remote APIs, durable ticket recovery and background process-death acceptance. No 10%-speed animation audit was needed for newly added motion because none was added; existing app-wide motion is not recertified here.

## Exact external prerequisites

- Android OAuth client for `com.example.quapp`, including approved debug/release signing fingerprints, plus Web client/server audience and configured consent/test-user access. Only local debug signing information was verified; production signing/OAuth registration is absent.
- Stable compatible Credential Manager / Play Services Auth / Google ID versions chosen against the actual app. No libraries were added while backend prerequisites are missing.
- INTERNET permission and approved HTTPS base URL; deployed token verification of issuer/audience/expiry/replay policy; app-session issue/expiry/refresh/revocation/sign-out contract; secure client storage and failure/retry/unknown-outcome handling.
- Real phone authentication/recovery and server authorization. Current FakeData accepts local demo inputs; it must never be advertised as production authentication.
- Family eligibility, capacity, member identity/membership and atomic group creation; staff invitation/roles/ownership guarantees; real reservation availability/mutation contracts; durable ticket recovery. Existing handoff lists remain authoritative; no endpoint is invented by this report.

Official sources: [Google branding](https://developers.google.com/identity/branding-guidelines), [Credential Manager implementation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [Google Sans metadata](https://raw.githubusercontent.com/google/fonts/main/ofl/googlesans/METADATA.pb). Current Android examples include alpha versions; they were deliberately not adopted.

## Skills applied and boundaries

Ponytail and Caveman: scope/reuse and concise chat. Feature Decomposer: repository requirement checklist. Root Cause Investigator: account lock, secret state and compact IME defect. Safe Implementer: coherent native slices and preservation. Regression Test Writer: AuthProposalTest and affected existing expectations. UI/UX Pro Max: targeted inline-validation search and native accessibility checklist. Emil and Make Interfaces Feel Better: hierarchy, native ripple, restrained motion, spacing/type/state review. Break UI: realistic native-only fixtures and enlarged/compact/landscape checks. Senior PR Reviewer and Ponytail Review: connected-code correctness and final simplicity review. Product Design index/audit/image-to-code: proposal interpretation and screenshot-first native guidance; web scaffolding/canvas/deployment not applicable.

Computer Use instructions were read, but Windows desktop automation was unnecessary because native ADB/instrumentation was available. OpenAI Docs was not applied: no OpenAI product question arose. No subagents ran. The bundled Design QA helper was read and applied through native source/screenshot comparisons; web scaffolding and browser checks do not apply.

APK: `C:/dev/quapp/app/build/outputs/apk/debug/app-debug.apk`.
Evidence/logs/metadata: `C:/dev/quapp/build/proposal-implementation/`.
Next effort: **Medium** for physical-device/broader acceptance or real authentication integration; **Light** for record-only changes.


## Recordkeeping and final delivery

Active-vault PROGRESS, Daily/2026-10-21, DESIGN, overview, DECISIONS, BUILD-MAP and HANDOFF were saved and readback-verified at 2026-10-21 22:59 UTC+08:00 (Model unknown). The earlier approval-pending checkpoint is preserved as history; local-limit portions of earlier decisions carry links to their partial supersession. No historical Documents-vault write occurred. Verification receipts are in build/proposal-implementation/record-readback.json.

Final preservation check: 163 initially hashed dirty files remain; 137 are byte-identical and 26 are intentional overlaps. Three initial directory entries were not recursively baseline-hashed. There are 36 task files including resources, tests and documentation; exact paths/hashes are in final-metadata.json and task-file-hashes.json. HEAD remains unchanged and nothing is staged.

Delivery copy: C:/dev/quapp/build/proposal-implementation/Quapp-proposal-debug.apk (11,526,471 bytes).
SHA-256: BC90711D9D1FD621790D1682918D594DD86ED6730CF0DC2131D4ABDCF08D8434.
