# Repository map

## System at a glance

Quapp is a single-module Android application (`:app`) using Java 11 source compatibility, XML views, Material Components, and API levels 24–36. `SplashActivity` is the launcher. The app currently uses `FakeData` and local stores; no production network client or backend is present.

```text
Android entry
  SplashActivity
    -> Session.homeIntent()
       -> QueuerHomeActivity -> Browse / My Tickets / Profile
       -> OwnerHomeActivity  -> Queues / Today / Profile

Screen or Fragment
  -> FakeData asynchronous callback
  -> LoadingStateHelper (loading / content / empty / error)
  -> Adapter or detail view
  -> local state in Session / ActiveTicketStore
```

## Build and configuration

| Path | Role |
|---|---|
| `settings.gradle` | Repository and plugin resolution; includes the `:app` module. |
| `build.gradle` | Root Android plugin declaration. |
| `app/build.gradle` | Application ID, SDK levels, Java compatibility, and dependencies. |
| `gradle/libs.versions.toml` | Version catalog for AndroidX, Material, tests, and ZXing. |
| `gradle.properties` | Gradle JVM and configuration-cache settings. |
| `local.properties` | Local Android SDK path; ignored and machine-specific. |
| `app/src/main/AndroidManifest.xml` | Permissions, launcher, activities, receiver, and themes. |

## Runtime entry points

- `QuappApplication` initializes app-wide behavior.
- `SplashActivity` displays the centered ticket mark, reads `Session`, then routes to the correct home.
- `QueuerHomeActivity` hosts Browse, My Tickets, and Profile tabs.
- `OwnerHomeActivity` hosts Queues, Today, and Profile tabs.
- `HereReceiver` handles the notification action for confirming arrival.
- `CalledNotifier` creates the local called notification.

## Feature areas

### Authentication and session

`LoginActivity`, `RegisterActivity`, `ForgotPasswordActivity`, `RoleSelectActivity`, `Session`, `User`, and `Validation` handle the mocked account flow and local session routing.

### Queue discovery and joining

`BrowseFragment`, `QueueAdapter`, `QueueFilter`, `QueueDetailActivity`, `JoinQueueActivity`, `ProximityCheck`, and `Directions` cover discovery, filtering, location checks, and joining.

### Ticket lifecycle

`MyTicketsFragment`, `MyTicketsAdapter`, `ActiveTicketActivity`, `ActiveTicketStore`, `MoreTimeSheet`, `CalledActivity`, `CalledNotifier`, `Cooldown`, and `TicketRules` cover waiting, being called, grace time, moving back, and outcomes.

### Owner workflow

`OwnerQueuesFragment`, `TodayFragment`, `CreateQueueActivity`, `LiveConsoleActivity`, `ConsoleSheets`, `WaitingTicketAdapter`, `QueueAnalyticsActivity`, and `QueueStats` cover queue creation and live operation.

### Trust and support

`GetVerifiedActivity`, `ReportQueueActivity`, `AccountSuspendedActivity`, `TrustSheets`, `VerificationRequest`, `VerificationStatus`, `Report`, `HelpActivity`, and `Support` cover organizer verification, reporting, and support UI.

### Sharing and display

`ShareQueueActivity`, `CounterDisplayActivity`, and `QueueLink` generate and display queue links and QR codes.

## Data and state

| Component | Responsibility |
|---|---|
| `FakeData` | In-memory source for users, queues, tickets, reports, async callbacks, and debug simulation. |
| `Queue`, `Ticket`, `User`, `Report` | Core domain models. |
| `Session` | Logged-in identity and role in SharedPreferences. |
| `ActiveTicketStore` | Active-ticket persistence and transitions. |
| `RollingAverage`, `QueueStats` | Wait-time estimation and queue analytics. |
| `LoadingStateHelper` | Four-state UI lifecycle for async screens. |
| `Connectivity` | Network availability observation for offline UI. |

`MODELS.md` is the planned backend contract. Updating a domain model requires reviewing that document first.

## UI and resources

- `res/layout/activity_*` contains full-screen activities.
- `res/layout/fragment_*` contains bottom-navigation tabs.
- `res/layout/item_*` contains RecyclerView rows and cards.
- `res/layout/view_*` contains reusable UI pieces and states.
- `res/layout/sheet_*` contains bottom sheets and choices.
- `res/values/colors.xml`, `themes.xml`, `styles.xml`, `type.xml`, and `dimens.xml` implement Civic Paper tokens.
- `res/drawable` contains icons, ticket shapes, surfaces, dividers, and paper treatments.
- `Grain`, `QuappViewInflater`, `TicketShapes`, `PunchEdgeTreatment`, `NotchCornerTreatment`, and `TearLineView` provide custom rendering.

The repository currently has 69 Java source files, 55 layout resources, 78 XML drawables, six local unit-test files, and one instrumentation-test placeholder.

## Test map

| Test | Coverage |
|---|---|
| `ValidationTest` | Input validation. |
| `TicketRulesTest` | Ticket overlap and lifecycle rules. |
| `RollingAverageTest` | Forecast calculation. |
| `QueueLinkTest` | Queue-link encoding and parsing. |
| `QueueFilterTest` | Browse filtering. |
| `ExampleUnitTest` | Template test; low project value. |
| `ExampleInstrumentedTest` | Instrumentation template; not feature coverage. |

Run `scripts/check.ps1` for the repeatable local check sequence.

## Visual and knowledge assets

- `artifacts/` contains the complete offline Civic Paper gallery and 64 original board exports.
- `design-experiment/` contains a modernist experiment and prompt; it is not the selected direction.
- `tools/` contains source generators for logo and paper-grain assets.
- `obsidian-starter/` contains vault structure and pending records when direct vault writes fail.

## Deployment touchpoints

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` after `assembleDebug`.
- Release optimization is currently disabled; no signing or publishing configuration is defined.
- The backend, hosted base URL, CI, and production push notifications are not implemented in this repository.

## Highest-value improvement opportunities

These are proposals, not completed work:

1. Introduce a `QueueRepository` interface in front of `FakeData` before Retrofit integration.
2. Split `FakeData` (about 1,250 lines) into fixtures, stores, and asynchronous repository behavior.
3. Split the largest activities/fragments (`CreateQueueActivity`, `ActiveTicketActivity`, `LiveConsoleActivity`, `QueueDetailActivity`, `BrowseFragment`) by feature responsibility after current behavior is covered.
4. Remove the duplicate Material dependency declaration after confirming the version-catalog version is the intended one.
5. Add focused instrumentation coverage for launch routing, navigation, and the join/called flow.
6. Replace template example tests with project-specific smoke tests.
7. Add CI for unit tests, resource processing, lint, and debug assembly.
8. Resolve the generated backup-rule TODO based on the actual SharedPreferences retention policy.
9. Add a single configurable backend base URL when the API implementation begins.
10. Verify the full Civic Paper UI on Pixel and Xiaomi, recording OEM-specific differences separately.
