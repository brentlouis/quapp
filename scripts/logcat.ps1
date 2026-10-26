[CmdletBinding()]
param(
    [switch]$Clear
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\lib\Quapp-Environment.ps1"
$environment = Initialize-QuappEnvironment

if ($Clear) {
    & $environment.Adb logcat -c
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$appPid = (& $environment.Adb shell pidof -s com.example.quapp).Trim()
if (-not $appPid) {
    throw 'Quapp is not running. Install or launch the app, then rerun this script.'
}

Write-Host "Streaming logcat for Quapp process $appPid. Press Ctrl+C to stop." -ForegroundColor Cyan
& $environment.Adb logcat "--pid=$appPid"
