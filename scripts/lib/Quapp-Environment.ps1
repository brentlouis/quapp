Set-StrictMode -Version Latest

function Get-QuappRoot {
    return (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
}

function Resolve-QuappJavaHome {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $candidates += 'C:\Program Files\Android\Android Studio\jbr'
    $candidates += 'C:\Program Files\Android\Android Studio\jre'
    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr')
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe'))) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw 'Java was not found. Install Android Studio or set JAVA_HOME to a JDK compatible with the Android Gradle Plugin.'
}

function Resolve-QuappAndroidSdk {
    $candidates = @()
    if ($env:ANDROID_HOME) { $candidates += $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { $candidates += $env:ANDROID_SDK_ROOT }

    $localProperties = Join-Path (Get-QuappRoot) 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) {
            $sdkValue = ($sdkLine -replace '^sdk\.dir=', '') -replace '\\:', ':' -replace '\\\\', '\'
            $candidates += $sdkValue
        }
    }

    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw 'Android SDK was not found. Open the project in Android Studio or configure sdk.dir in local.properties.'
}

function Initialize-QuappEnvironment {
    $javaHome = Resolve-QuappJavaHome
    $androidSdk = Resolve-QuappAndroidSdk
    $userProfile = [Environment]::GetFolderPath('UserProfile')
    if ([string]::IsNullOrWhiteSpace($userProfile)) { $userProfile = $env:USERPROFILE }
    if ([string]::IsNullOrWhiteSpace($userProfile) -and $env:LOCALAPPDATA) {
        $userProfile = Split-Path -Parent (Split-Path -Parent $env:LOCALAPPDATA)
    }
    if ([string]::IsNullOrWhiteSpace($userProfile)) {
        throw 'The Windows user profile path could not be resolved for the Gradle cache.'
    }
    $gradleUserHome = if ($env:GRADLE_USER_HOME) {
        $env:GRADLE_USER_HOME
    } else {
        Join-Path $userProfile '.gradle'
    }

    $env:JAVA_HOME = $javaHome
    $env:ANDROID_HOME = $androidSdk
    $env:ANDROID_SDK_ROOT = $androidSdk
    $env:GRADLE_USER_HOME = $gradleUserHome

    return [pscustomobject]@{
        ProjectRoot = Get-QuappRoot
        JavaHome = $javaHome
        AndroidSdk = $androidSdk
        GradleUserHome = $gradleUserHome
        Adb = Join-Path $androidSdk 'platform-tools\adb.exe'
    }
}
