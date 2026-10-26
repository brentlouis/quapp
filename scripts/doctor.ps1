[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\lib\Quapp-Environment.ps1"

Write-Host 'Quapp environment check' -ForegroundColor Cyan

try {
    $environment = Initialize-QuappEnvironment
} catch {
    Write-Error $_.Exception.Message
    exit 1
}

$requiredFiles = @(
    'settings.gradle',
    'app\build.gradle',
    'app\src\main\AndroidManifest.xml',
    'DESIGN.md',
    'DECISIONS.md',
    'MODELS.md',
    'PROGRESS.md'
)

$missing = @()
foreach ($relativePath in $requiredFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $environment.ProjectRoot $relativePath))) {
        $missing += $relativePath
    }
}

$javaProcess = [Diagnostics.Process]::new()
$javaProcess.StartInfo = [Diagnostics.ProcessStartInfo]::new()
$javaProcess.StartInfo.FileName = Join-Path $environment.JavaHome 'bin\java.exe'
$javaProcess.StartInfo.Arguments = '--version'
$javaProcess.StartInfo.UseShellExecute = $false
$javaProcess.StartInfo.RedirectStandardOutput = $true
$javaProcess.StartInfo.RedirectStandardError = $true
$null = $javaProcess.Start()
$javaOutput = $javaProcess.StandardOutput.ReadToEnd()
$javaError = $javaProcess.StandardError.ReadToEnd()
$javaProcess.WaitForExit()
if ($javaProcess.ExitCode -ne 0) { throw "Java version check failed: $javaError" }
$javaVersion = (($javaOutput + $javaError) -split "`r?`n" | Where-Object { $_ } | Select-Object -First 1)
$adbReady = Test-Path -LiteralPath $environment.Adb

[pscustomobject]@{
    Project = $environment.ProjectRoot
    Java = $javaVersion
    JavaHome = $environment.JavaHome
    AndroidSdk = $environment.AndroidSdk
    GradleUserHome = $environment.GradleUserHome
    AdbAvailable = $adbReady
    RequiredFiles = if ($missing.Count -eq 0) { 'OK' } else { 'Missing: ' + ($missing -join ', ') }
} | Format-List

if ($missing.Count -gt 0 -or -not $adbReady) {
    if (-not $adbReady) { Write-Error "ADB was not found at $($environment.Adb). Install Android SDK Platform-Tools." }
    exit 1
}

Write-Host 'Environment is ready for Quapp development.' -ForegroundColor Green
