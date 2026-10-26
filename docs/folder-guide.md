# Where files belong

Start with [the project README](../README.md). For a bug, use [the debugging guide](debugging.md); for a class or data flow, use [the repository map](repo-map.md).

| Work | Location |
|---|---|
| Android screens and behavior | `app/src/main/java/com/example/quapp/` |
| Screen layouts | `app/src/main/res/layout/` |
| Colors, dimensions, styles, and strings | `app/src/main/res/values/` |
| Drawable resources | `app/src/main/res/drawable/` |
| Launcher and Activity registration | `app/src/main/AndroidManifest.xml` |
| Local unit tests | `app/src/test/` |
| Device tests | `app/src/androidTest/` |
| Current design and confirmed choices | Root `DESIGN.md` and `DECISIONS.md` |
| Work status | Root `PROGRESS.md` and the Obsidian project logs |
| Setup and debugging commands | `scripts/` |
| Asset and gallery generators | `tools/` |
| Editable visual reference and downloads | `artifacts/` |
| Alternative design reference | `design-experiment/` and root `DESIGN-experiment.md` |
| Third-party notices | `licenses/` |
| Vault starter and unsaved records | `obsidian-starter/` |
| Local diagnostic evidence | `diagnostics/` |

Keep new supporting documentation in `docs/` and link it from [the index](README.md). Keep Android files in their standard source/resource folders so Gradle and Android Studio can find them.

## Generated and local files

`build/`, `app/build/`, `.gradle/`, and `.gradle-test/` contain generated output or caches. `.idea/` contains IDE metadata. `local.properties` records the machine's Android SDK location. These are not places to edit app behavior or save project notes.

Root Gradle files, the wrapper, instruction files, and established design contracts stay at the root. Their existing paths are used by tooling and project references.

## Remaining limitations

Two older `hs_err_pid*.log` crash dumps remain at the root after Windows denied moving them. They are ignored. Do not delete evidence just to make the folder look tidy.

Obsidian is a separate folder; the starter files do not sync automatically. Pending records are only saved in the vault after their destination notes are read back and verified.
