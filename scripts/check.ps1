[CmdletBinding()]
param(
    [switch]$Full
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\lib\Quapp-Environment.ps1"
$environment = Initialize-QuappEnvironment
$gradle = Join-Path $environment.ProjectRoot 'gradlew.bat'

$tasks = @(':app:testDebugUnitTest', ':app:processDebugResources')
if ($Full) {
    $tasks += ':app:lintDebug'
    $tasks += ':app:assembleDebug'
}

Write-Host ('Running Quapp checks: ' + ($tasks -join ', ')) -ForegroundColor Cyan
Push-Location $environment.ProjectRoot
try {
    & $gradle '--no-configuration-cache' @tasks
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

Write-Host 'Quapp checks passed.' -ForegroundColor Green
