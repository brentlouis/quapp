[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\lib\Quapp-Environment.ps1"
$environment = Initialize-QuappEnvironment
$gradle = Join-Path $environment.ProjectRoot 'gradlew.bat'
$apk = Join-Path $environment.ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'

Push-Location $environment.ProjectRoot
try {
    & $gradle '--no-configuration-cache' ':app:assembleDebug'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

$devices = & $environment.Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\sdevice$' }
if ($devices.Count -ne 1) {
    throw "Expected exactly one connected emulator or device; found $($devices.Count). Check 'adb devices'."
}

& $environment.Adb install -r $apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $environment.Adb shell am start -n 'com.example.quapp/.SplashActivity'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host 'Quapp debug APK installed and launched.' -ForegroundColor Green
