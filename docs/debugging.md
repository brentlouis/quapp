# Debugging Quapp

## Fast path

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/doctor.ps1
powershell -ExecutionPolicy Bypass -File scripts/check.ps1
powershell -ExecutionPolicy Bypass -File scripts/install-debug.ps1
powershell -ExecutionPolicy Bypass -File scripts/logcat.ps1 -Clear
```

`doctor.ps1` finds Android Studio's bundled Java when `JAVA_HOME` is missing and reads the SDK path from environment variables or `local.properties`.

## Useful breakpoints

| Problem | Start here |
|---|---|
| Wrong screen after launch | `SplashActivity.onCreate`, then `Session.homeIntent`. |
| Login or role not remembered | `Session` read/write/clear methods. |
| Queue list wrong or stale | `FakeData` callback, `QueueFilter`, then the fragment's render method. |
| Loading never completes | `LoadingStateHelper.show*`, callback completion, and fragment/activity lifecycle guards. |
| Join rejected | `JoinQueueActivity`, `ProximityCheck`, `TicketRules`, and cooldown state. |
| Ticket status wrong | `ActiveTicketStore`, then `ActiveTicketActivity` rendering. |
| Called notification missing | `CalledNotifier`, notification permission, then `HereReceiver`. |
| Owner console state wrong | `LiveConsoleActivity`, `ConsoleSheets`, and `FakeData` live-queue state. |
| Visual mismatch | Layout resource, `themes.xml`, `styles.xml`, `type.xml`, `dimens.xml`, then custom rendering helpers. |

## Exercise loading, empty, and error states

Open Profile and enable the debug slow-response controls. `FakeData` supports simulated latency plus one-time error and empty-result injection. Check all four states:

1. Loading text and progress are visible.
2. Content replaces loading after the callback.
3. Empty state explains the result and offers the intended action.
4. Error state offers Retry and recovers.

Avoid adding `Thread.sleep()` to application code; it blocks the UI thread and does not represent network behavior.

## Logcat

`scripts/logcat.ps1` filters logs to the running `com.example.quapp` process. If no process is found, install and launch the app first. Android crashes appear under `AndroidRuntime` with the first `Caused by` line usually closest to the defect.

For an app that closes immediately:

1. Run `scripts/logcat.ps1 -Clear`.
2. Reproduce once.
3. Find `FATAL EXCEPTION` and the first stack frame under `com.example.quapp`.
4. Inspect that line and the data entering it before changing code.

## Build failures

### Java not found

Run `scripts/doctor.ps1`. The scripts prefer `JAVA_HOME`, then Android Studio's bundled JBR. The project uses Java 11 source compatibility, while current Android Gradle Plugin releases normally run on a newer bundled JDK.

### Android SDK not found

Open the project once in Android Studio or set `sdk.dir` in `local.properties`. This file is local-only and must not be committed.

### Resource linking error

Run `scripts/check.ps1`, then read the first `AAPT` error. Typical causes are an invalid resource name, a missing `@string`/`@dimen`, or a malformed XML attribute. Fix the first resource error before later cascade errors.

### Configuration-cache issue

The helper scripts pass `--no-configuration-cache` for predictable debugging. If Android Studio retains a stale error, stop Gradle from the IDE and rerun the check. Do not delete global caches as a first response.

## Device differences

- Test both a Pixel emulator and the physical Xiaomi device.
- HyperOS may draw an opaque navigation-bar band on the splash even when the app requests transparency. Confirm on Pixel before changing app code.
- Verify paper grain and contrast on the Xiaomi; lower-end displays may render the texture darker.
- Confirm touch targets, keyboard insets, long text, and system bars on both devices.

## Debug outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Unit-test reports: `app/build/reports/tests/testDebugUnitTest/`
- Lint report: `app/build/reports/lint-results-debug.html`
- Lint triage snapshot: `docs/lint-status.md`
- Gradle reports: `build/reports/`
- JVM crash dumps: `diagnostics/jvm-crashes/`

Generated output is evidence for debugging, not source code. Do not edit files under `build/` or `app/build/`.
