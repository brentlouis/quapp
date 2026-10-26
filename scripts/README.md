# Developer scripts

Run these scripts from any PowerShell location; each resolves the repository root itself.

| Script | Purpose |
|---|---|
| `doctor.ps1` | Verify Java, Android SDK, ADB, and required project files. |
| `check.ps1` | Run unit tests and resource processing. Add `-Full` for lint and debug assembly. |
| `install-debug.ps1` | Build, install, and launch on exactly one connected emulator/device. |
| `logcat.ps1` | Stream logs only for the running Quapp process. Add `-Clear` to clear old logs first. |
| `lib/Quapp-Environment.ps1` | Shared Java and Android SDK discovery. |

Examples:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/doctor.ps1
powershell -ExecutionPolicy Bypass -File scripts/check.ps1 -Full
powershell -ExecutionPolicy Bypass -File scripts/install-debug.ps1
powershell -ExecutionPolicy Bypass -File scripts/logcat.ps1 -Clear
```
