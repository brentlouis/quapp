# Quapp

Quapp is a native Android queue-management app for community services. It uses Java, XML views, Material Components, and min SDK 24. The current app runs on in-memory `FakeData`; the planned FastAPI/Retrofit backend is not part of this repository yet.

## Start here

1. Open this folder in Android Studio.
2. Run `powershell -ExecutionPolicy Bypass -File scripts/doctor.ps1` to check Java, Android SDK, ADB, and required project files.
3. Run `powershell -ExecutionPolicy Bypass -File scripts/check.ps1` for unit tests and Android resource processing.
4. Start the `app` configuration on a Pixel emulator or the physical Xiaomi test device.
5. Use the Profile screen's debug slow-response controls to exercise loading, empty, and error states.

For a full local build and lint pass:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check.ps1 -Full
```

For device installation and launch:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/install-debug.ps1
```

For live app logs:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/logcat.ps1 -Clear
```

## Project guide

- [Repository map](docs/repo-map.md) — architecture, entry points, data flows, tests, and improvement opportunities.
- [Debugging](docs/debugging.md) — symptom-based troubleshooting, breakpoints, state simulation, and device notes.
- [Development workflow](docs/development.md) — safe change sequence and verification levels.
- [Documentation index](docs/README.md) — source-of-truth documents and supporting material.
- [Where files belong](docs/folder-guide.md) — where to edit, save, and find project files.
- [Design specification](DESIGN.md) — current Civic Paper visual system.
- [Decisions](DECISIONS.md) — rationale and superseded choices.
- [Data contract](MODELS.md) — models and planned API schema.
- [Progress](PROGRESS.md) — implementation and verification status.

## Folder map

| Folder | Purpose |
|---|---|
| `app/` | Android application source, resources, and tests. Keep its standard Gradle structure intact. |
| `docs/` | Fast navigation, architecture, development, and debugging guides. |
| `scripts/` | Environment checks, verification, installation, and log collection. |
| `tools/` | Logo and grain generators, plus the HTML gallery rebuild script. |
| `artifacts/` | Downloadable Civic Paper HTML gallery, editable artboards, preview, and ZIP. |
| `design-experiment/` | Alternative design experiment; reference only. |
| `diagnostics/` | Local crash evidence and troubleshooting notes. |
| `licenses/` | Third-party font and icon licenses. |
| `obsidian-starter/` | Second-brain starter content and pending vault records. |

## Important boundaries

- Keep Java and XML as the app stack; do not introduce Kotlin or Compose.
- Treat `DESIGN.md`, `DECISIONS.md`, and `MODELS.md` as contracts before structural changes.
- Do not mark build or device verification complete without running it.
- The exported HTML gallery is a visual reference, not the Android runtime and not a backend.
- Project changes must be recorded in the Obsidian workflow described by `AGENTS.md`.
