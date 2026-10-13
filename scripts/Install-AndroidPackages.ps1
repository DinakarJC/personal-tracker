param(
    [switch]$AcceptLicense
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkDirectories = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\jdk') -Directory)
if ($jdkDirectories.Count -ne 1) {
    throw 'Expected one portable JDK under .tools/jdk.'
}
$env:JAVA_HOME = $jdkDirectories[0].FullName
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools\android-user'
New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force | Out-Null
$sdkDirectory = Join-Path $projectRoot '.tools\android-sdk'
$sdkManager = Join-Path $sdkDirectory 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path -LiteralPath $sdkManager)) {
    throw 'Verified Android command-line tools have not been extracted yet.'
}
if ($AcceptLicense) {
    1..100 | ForEach-Object { 'y' } | & $sdkManager "--sdk_root=$sdkDirectory" --licenses
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK license acceptance failed.' }
} else {
    & $sdkManager "--sdk_root=$sdkDirectory" --licenses
    if ($LASTEXITCODE -ne 0) { throw 'Review and accept the required SDK licenses before installation.' }
}
& $sdkManager "--sdk_root=$sdkDirectory" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK package installation failed.' }
Write-Output "Android SDK ready at $sdkDirectory"
