<!-- quapp-master-implementation-20261021 -->
Current targeted override: single welcome, editable demo registration, native auth lifecycle/IME fixes, working-only Preferences/notifications and Family ticket drafts disclosure are implemented. Bounded acceptance passed; genuine authentication/backend and full physical/every-route acceptance remain incomplete. See `docs/onboarding-proposal-implementation.md`. Earlier dated evidence remains historical.

<!-- quapp-onboarding-followup-20261021 -->
Latest follow-up: welcome/four-slide onboarding, compatible New Queue simplification, draft/back/action repairs and Profile/privacy cleanup are implemented. Real Google sign-in, staff invitations, reservations and family membership remain blocked by absent services/contracts. Current evidence and limits: C:/dev/quapp/build/redesign-followup-20261021/REPORT.md. The earlier 47-route audit below is historical evidence, not a fresh every-state certification.

# Quapp professional UI redesign — 2026-10-21

Status: APP-WIDE UI IMPLEMENTATION COMPLETE; BOUNDED EMULATOR ACCEPTANCE PASSED. Full production and every-state/device acceptance remain unverified. This record distinguishes shared UI changes, native route mappings, automated execution and screenshots actually inspected. It does not certify production readiness or complete prototype behavior parity.

## Baseline and authority

- Repository `C:/dev/quapp`, branch `main`, HEAD `6b169efde5442d353f82f9fe3a278ac03948c821`. Initial porcelain: 130 entries, nothing staged. Baseline diff and 363 modified/untracked file hashes: `build/professional-redesign-20261021/initial-*`.
- Current handoff and targeted live DESIGN/DECISIONS/BUILD-MAP/DESIGN-CHECKPOINT sections take precedence over historical repository Civic Paper prose. The repository DESIGN now starts with an explicit Slate & Mint override; history remains intact.
- `PROJECT.md` and `ROUTING.md` were absent from the repository root and active project-note folder. Existing `PRODUCT.md`, `AGENTS.md`, route ledger, themes, color/type/dimension/style resources and relevant Java/XML were inspected. No Graphify query or index maintenance was needed.
- Verified inventory: 47 entries in `build/full-parity-20261021/prototype-routes.json`, matching the 47 rows in `docs/full-prototype-parity.md`. These include aliases, native adaptations, dialogs and conditional proposals, not 47 independent Activities.

## Implemented changes

| Before | After | Why |
| --- | --- | --- |
| 36/54sp screen headings and 24/36sp section titles | 29/36sp and 24/29sp roles | Clear hierarchy without crowding routine screens |
| 16/18sp supporting text; tracked uppercase labels | 16/22sp captions, normal sentence-case labels | Improve multiline reading and reduce visual noise |
| Statistics used proportional figures | Shared tabular figures | Keep updating quantities aligned |
| Button shape referenced card radius | Dedicated 12dp button radius; cards remain 16dp | Controls and containers have distinct purposes |
| Java applied textured, notched navigation chrome | Plain white bars and rounded active selection for both roles | Coherent native navigation; ticket motif remains on queue information |
| Java headers, docks and sheets retained grain/decorative chrome | Flat routine headers, docks and sheets; existing ticket texture retained | Match current Slate & Mint surfaces |
| Today had double outer padding and indented metrics | One 16dp gutter and aligned metric rows; queue-row chevron | Improve scanning and reveal the Insights destination |
| Full-width outlined Save/Remove before venue and queue facts | Compact text/bookmark action after preparation and description | Prioritize joining information and persistent Join action |
| Saved list repeated a generic hero above title/empty explanation | Remove redundant hero; retain local-storage and availability explanations | Reduce repetitive content |
| Four supporting actions and detail Join had fixed height | Content-driven height with retained minimum | Preserve full labels at enlarged text |
| Profile/settings helper hid disclosure arrows | Visible navigation arrows; disclosure direction follows expanded state | Make clickable rows distinguishable from static text |
| Counter actions touched; long announcement validation stretched editor | 8dp action gaps; 4–6-line scrollable native editor | Separate operations and keep publishing controls reachable |
| Mint dialog actions and field focus lacked sufficient contrast on light surfaces | Deep teal dialog text and field focus roles | Keep mint for filled actions and maintain readable controls |
| Location explanation could exceed a compact sheet | Native nested scrolling, expanded bounded sheet and growing labels | Allow/Not now reached by an actual swipe at 320dp/200%, no GPS request |
| Verification/support copy promised real office calls, review and replies | Explicit demo requests, simulated identity and disconnected support | Match implemented boundaries without changing validation or request rules |
| Mixed floating hints in organizer fields | External labels with labelFor for lookup, announcement and counter name | Prevent label/input overlap at 200% and preserve input semantics |
| Upcoming-card formatter passed an obsolete extra count | Match its one-placeholder resource | Fix the lint error while preserving no-early-join wording |

No palette replacement, fonts, dependencies, business-rule refactor, backend capability, commit or push.

## Route checklist

All entries are tracked and their native mapping reviewed against the ledger and relevant source/resources. `Seen` means a rendered capture was inspected, not every state or gesture. `Pending` means no current capture has yet been inspected. Shared foundation changes apply to existing native implementations; the OTP gap remains a gap. Coverage: 47 tracked/source-reviewed entries; 45 rendered native route entries inspected, including aliases; one external Directions integration unverified and one missing native OTP route. These are route mappings, not 45 independent Activities. No new route feature was invented.

| Route | Native surface | Screenshot review / remaining boundary |
| --- | --- | --- |
| welcome | RoleSelect/Splash | Seen |
| sign-in | Login | Seen; demo authentication |
| create-account | Register | Seen; demo registration |
| phone-verification | No separate native OTP screen | Unimplemented; no fabricated OTP feature |
| password-recovery | ForgotPassword | Seen; support/recovery unavailable |
| home | Browse first visit | Seen; native shared-tab adaptation |
| explore | Browse selected town | Seen |
| service-details | QueueDetail | Seen; compact bookmark follows practical joining information |
| saved-services | SavedServices | Seen populated and empty |
| join-confirmation | JoinQueue | Seen |
| ticket-list | MyTickets | Seen |
| ticket-waiting | ActiveTicket | Seen |
| ticket-called | Called | Seen waiting/called/arrival-confirmed |
| ticket-directions | Directions map intent | External map/physical location unverified |
| ticket-recovery | ActiveTicket released/rejoin | Seen synthetic released state; no durable/server recovery claim |
| ticket-receipt | ProfileSettings/ActiveTicket | Seen read-only receipt |
| ticket-history | History | Seen |
| profile-overview | Profile | Seen; visible disclosure affordances inspected |
| profile-preferences | ProfileSettings | Seen |
| profile-notifications | ProfileSettings | Seen |
| profile-help | ProfileSettings/legacy Help | Seen |
| profile-verification | ProfileSettings/GetVerified | Seen; truthful demo request copy; normal and 200% |
| dialog-leave-queue | ActiveTicket confirmation | Seen settled normal/200%; real-touch open/cancel passes |
| dialog-report | ReportQueue | Seen |
| dialog-notification-permission | NotificationPermission | Seen; OS permission gestures unverified |
| dialog-location-permission | QueueDetail/ProximityCheck | Seen explanation; real GPS/permission journey unverified |
| dialog-connection-recovery | LoadingStateHelper | Seen loading/error/retry; automated retry coverage |
| dialog-service-change | ConsoleSheets | Seen options/removal sheets; compact expansion checked |
| organizer-today | Today | Seen |
| organizer-queues | OwnerQueues | Seen |
| organizer-create | CreateQueue | Seen |
| organizer-preview | OrganizerPreview | Seen |
| organizer-console | LiveConsole | Seen |
| organizer-counter-list | CounterList | Seen |
| organizer-counter | Counter | Seen; action spacing and reachability checked |
| organizer-walk-in | LiveConsole native dialog | Seen production-themed normal/200%; both actions fully visible and ≥48dp |
| organizer-announcement | Announcement | Seen; bounded editor after long validation input, normal/200% |
| organizer-insights | QueueAnalytics | Seen |
| organizer-sharing | ShareQueue | Seen; external link resolution unverified |
| organizer-display | CounterDisplay | Seen; narrow landscape QR clipping fixed and square/reachable checks pass |
| organizer-handover | OrganizerHandover | Seen unavailable/retry; transfer intentionally unavailable |
| organizer-lookup | OrganizerLookup | Seen |
| public-organizer | PublicOrganizer | Seen |
| public-organization | PublicOrganization | Seen |
| proposal-appointments | Phase4 local draft | Seen local draft; real scheduling unavailable |
| proposal-family-tickets | Phase4 local draft | Seen local draft; real group issuance unavailable |
| proposal-staff-roles | Phase4 local draft | Seen local draft; invitations/authorization unavailable |

## Skills and executable tools

- Read from the user-supplied instructions and applied: feature-decomposer, repository-cartographer, root-cause-investigator, safe-implementer, senior-pr-reviewer, regression-test-writer, Caveman, Ponytail, Emil and make-interfaces-feel-better. Applied to native Java/XML, not web styling. Focused regression assertions extend existing Today and Profile tests.
- Break-ui instructions and catalog inspected; adapted stress guidance using existing non-shipped instrumentation fixtures. This is not a claim of its entire browser/toggle workflow. Existing controls review has fixture selection; no production debug control was added.
- Impeccable project skill read; its actual Windows launcher executed `context --target app/src/main/res/values/styles.xml`, `--help`, and `detect` on three named XML files. Native platform, Operate, craft-floor and native-audit guidance inspected. Detection is regex-only for XML; it explicitly cannot establish rendered contrast or acceptance.
- Global UI/UX Pro Max skill and native checklist read. Global and project SKILL hashes differ; the registered global copy was used. Two bounded UX searches executed through the bundled Python. First was off-topic; retry returned web Text Reflow and Spacing. Content-driven sizing was adapted to Android; no Java/XML stack database exists. No new design-system output was persisted.
- Taste skill and app-screen guidance inspected for hierarchy, calm surfaces, useful density and subject-specific identity. Its Node/browser inspector and random direction draw were not run against native XML. Pinned Slate & Mint and Android controls govern.
- Project brand/design-system instructions inspected; semantic/component token separation applied in Android resources. Their web token-generation scripts were not run. Project design/ui-styling descriptions inspected; branding-assets and Radix/Tailwind workflows unsuitable here. banner-design/slides/Remotion irrelevant. Grill-me/grilling assessed as user-interview workflows and not executed because the user authorized autonomous implementation with a detailed brief. Graphify assessed and not run; targeted source searches answered scope questions.
- Catalog-listed global skills are registered. The grill-me file exists but is absent from this session catalog; it was not executed. Project files existing on disk do not prove registration; Impeccable CLI execution was independently verified. All 24 requested SKILL.md paths were verified to exist (skill-path-inventory.json); existence is distinct from reading or executing them. No skills/tools/dependencies installed, removed, updated or downloaded.

## Final verification

| Check | Result / evidence |
| --- | --- |
| Fresh debug/main + instrumentation APK builds and unit suite | PASS; 68 tasks executed, 39 tests / 0 failures / 0 skips (`build-tests.txt`) |
| Latest main/test APK builds, unit check and lint | PASS (`qr-build.txt`); 0 lint errors, 291 unsuppressed warnings; final test-only compilation PASS |
| Initial full normal instrumentation | 70/70 PASS, 547.84s, before final refinements (`instrumentation-normal.txt`) |
| Affected normal acceptance + final demo-copy checks | 25/25 and 4/4 PASS (`acceptance-normal.txt`, `copy-normal.txt`) |
| Corrected final normal / 320dp at 200% | 3/3 each PASS (`final-closure-normal.txt`, `final-closure-compact.txt`) |
| Final real-touch leave dialog, normal / 200% | 1/1 each PASS; settled captures inspected (`settled-dialog-normal.txt`, `settled-dialog-compact.txt`) |
| Repaired narrow landscape extended organizer workflow | 1/1 PASS, 159.683s (`landscape-final.txt`); visible bounds, square QR, public error/retry and existing operational assertions |
| Production-themed walk-in + shared sheets, 200% / normal | 1/1 each PASS (`walk-actions-compact.txt`, `walk-actions-normal.txt`); full positive/negative action bounds and ≥48dp targets |
| Contrast | Static semantic pairs 5.01:1–15.06:1; native dialog text contrast assertion ≥4.5:1 across default/walk-in overlays passed in affected campaigns |
| Final launch | Splash COLD launch PASS; foreground OwnerHomeActivity; inspected final screenshot from explicit display ID; crash buffer empty (0 bytes) |
| Preservation | All 363 baseline files present: 346 identical, 17 intentional overlaps; 38 task files including this report. 130→151 porcelain entries, nothing staged; branch/main HEAD unchanged. `diff --check` PASS |

Final suite scope is focused regression closure, not a second complete final 70-test run. Prior failures were retained and investigated: full compact campaign had 64 passes, one failure, then the clipboard test stalled the guest with four later tests unrun. Restarted this task's emulator without a wipe; clipboard restoration after that stall is unverified. The later 26-test compact campaign had 23 passes/3 failures (two sheets plus a timing fixture); landscape had 25 passes/1 failure. The seven-test closure was normal6/7, compact5/7; its landscape run was intentionally force-stopped for a discovered label fix, so its runner “Process crashed” is not counted as an unexplained app crash. The next landscape closure was2/3 until the QR repair. All corresponding raw logs remain; final focused checks above close the relevant failures. No test skip is silently called a pass.

### Defects fixed during rendered review

- Organizer lookup's disabled floating-hint style still had an inline hint; it overlapped content at 200%. Lookup, announcement and counter-name dialog now use external FieldLabel TextViews with labelFor. IDs, input types and validation preserved. Label/layout geometry assertions pass; normal and 200% captures inspected.
- Console operational sheets and location explanation now open expanded within 90% of the viewport; location root scrolls and action labels grow. Location geometry measured a 1449px viewport/1753px content at 320dp/200%. Espresso scrollTo remained unreliable; a real native upward swipe passed and both visible actions were inspected. Temporary geometry logging removed.
- A fixed 168dp landscape display QR exceeded its narrow column: measured width441px vs visible238px. Native wrap_content, adjustViewBounds and existing maximum dimensions now keep it square and fully visible. The strict bounds assertion was retained and augmented with a square check; repaired screenshot inspected. Earlier assumption that this was only a simultaneous-visibility test issue was incomplete.
- Loading fixtures were too short for enlarged/rotating screens. Browse/ticket fixtures now use a 5000ms delay and bounded settling. Public-profile one-shot failure is injected via the existing reload listener after orientation recreation; otherwise the old Activity consumed it. Production loading behavior unchanged.
- Leave-dialog capture initially caught a pre-draw background frame. Test now opens it through an actual button click, waits for visible dialog controls and captures the settled result; then cancels and verifies fixture restoration. Normal/200% screenshots inspected.
- An old walk-in presentation fixture omitted the production walk-in theme, producing a clipped Cancel capture. It now uses the same overlay as LiveConsoleActivity; strict full-button visibility and 48dp checks pass. No speculative production workaround was added. Earlier DESIGN-CHECKPOINT Cancel clipping evidence is superseded for this tested production theme; physical/fullscreen IME acceptance remains open.

### Screenshots actually inspected

Forty initial normal captures through ten contact sheets, twelve supplementary route/state captures, enlarged Browse/detail/join images, final Profile/verification/announcement/location/recovery/handover images, final compact contact sheet, settled leave dialogs at both sizes, production-themed large-text walk-in, repaired landscape QR, and final OwnerHome launch image. This represents the 45 native route mappings above, not every state or end-to-end production journey. Old pulled files were excluded from freshness claims; `adb pull -a` preserves guest timestamps. Some pre-gesture/pre-draw frames showed the preceding Activity and were not accepted as dialog evidence.

Selected evidence: `final-compact-review.jpg`, `settled-dialog-captures/`, `settled-dialog-compact-captures/`, `walk-actions-captures/`, `landscape-final-captures/`, `final-startup.png` under `build/professional-redesign-20261021/`.

## Emulator and final artifact

Existing Android Studio/JBR, SDK platforms36/36.1, ADB, emulator and installed android-37.1 Google APIs x86_64 image verified. Reused/started existing `Resizable_Experimental`, serial emulator-5554; boot_completed=1. No SDK/system-image/skill/dependency install or download. Application com.example.quapp, launcher .SplashActivity, debug variant; both APKs installed successfully.

Final settings read back: physical1080×2400, density420, font1.0, accelerometer_rotation1, user_rotation0, animator_duration_scale key null, boot_completed1. Keep emulator running. Its Windows window was discovered, activated and actual Quapp UI captured with the Computer Use skill (`@oai/sky` via node_repl). At final focus attempt the user-input guard stopped activation; no further desktop input was sent. The emulator remains open and can be selected on the taskbar as **Android Emulator - Resizable_Experimental:5554**. Foreground Android Activity is OwnerHomeActivity; another PC window may cover the emulator.

Latest APK: `C:/dev/quapp/app/build/outputs/apk/debug/app-debug.apk`, debug/demo. SHA256 `0E14A2451333835324ABEEADB889451DF0016F8C4B297324BC9C4C6504F68C48`. The earlier delivered APK under build/apk-delivery was deliberately not overwritten.

## Commands and tools

Use `C:/dev/quapp`, JAVA_HOME=`C:/Program Files/Android/Android Studio/jbr`, GRADLE_USER_HOME=`C:/Users/JUSKWAA/.gradle`, and the existing SDK platform-tools ADB. Main exact build commands:

```powershell
.\gradlew.bat --offline assembleDebug assembleDebugAndroidTest testDebugUnitTest --rerun-tasks --console=plain
.\gradlew.bat --offline assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --console=plain
.\gradlew.bat --offline assembleDebugAndroidTest --console=plain
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r com.example.quapp.test/androidx.test.runner.AndroidJUnitRunner
```

Corrected three-test closure class argument: `com.example.quapp.BrowseJoinVisualReviewTest#locationExplanationKeepsActionsReachable,com.example.quapp.TicketVisualReviewTest,com.example.quapp.ExtendedOrganizerWorkflowTest`, with `-e displayOrientation portrait` or `landscape` and `-e reviewConfig final-normal`/`final-compact`/`final-landscape`. Final landscape command:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e class com.example.quapp.ExtendedOrganizerWorkflowTest -e displayOrientation landscape com.example.quapp.test/androidx.test.runner.AndroidJUnitRunner
```

Final dialog commands use `-e class com.example.quapp.TicketVisualReviewTest` and `-e class com.example.quapp.ConsoleSheetsVisualReviewTest` with the same runner. Compact: wm size840x1680, density420 retained, font_scale2.0. Landscape: wm945x1680, font1.0, auto-rotation0/user-rotation1 plus explicit landscape request; actual short letterboxed configuration360×270dp recorded in filenames. Restore: wm size reset, font1.0, user_rotation0, accelerometer_rotation1. Final launch: `adb -s emulator-5554 shell am start -W -n com.example.quapp/.SplashActivity`; topResumedActivity and empty crash buffer read back. Final screenshot explicitly uses SurfaceFlinger display4619827259835644672 after a multi-display warning on the first default-display capture.

Other executed tools: targeted rg/source reads, Git status/diff/hash audits, Impeccable Windows launcher (context/help/XML detect), UI/UX Pro Max's Python search, bundled Pillow contact sheets, ADB install/instrument/screenshots/logs, emulator launcher, Windows Computer Use discovery/activation/capture. Computer Use SKILL/guidance/API/confirmation policy read. No Windows security/privacy/authentication UI automated.

## Remaining acceptance limits and next step

No unresolved defect in the final checked fixtures. Full production acceptance is incomplete: native OTP absent; external Directions/maps and physical QR scanning unverified; backend/session-only capabilities remain explicitly unavailable. Physical hardware, spoken TalkBack, manual gesture/keyboard acceptance, older Android versions and dark-theme behavior were not certified. Earlier report-editor IME/physical-keyboard limitations remain; no broad durability/process-death claim. Emulator clipboard restoration after its earlier stalled test remains unverified.

Next: inspect the running build manually on a physical device, especially TalkBack, IME, external maps/QR and true queue lifecycle once authorized backend contracts exist. Do not restart this redesign or repeat its baseline audit. Reasoning recommendation: Medium for remaining device/acceptance work; Light for notes. No commits, pushes, staging, stash/reset/clean, branch change, dependency/skill installs, Graphify rebuild/export or Obsidian setting changes.

Live-vault checkpoint saved and read-back verified at 2026-10-21 15:36 UTC+08:00: PROGRESS, Daily, DESIGN, DESIGN-CHECKPOINT, BUILD-MAP, overview, confirmed scope decision and HANDOFF. HANDOFF updated once; all previous history retained byte-for-byte. Historical Documents vault untouched.
