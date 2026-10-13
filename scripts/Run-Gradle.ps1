param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Tasks = @(':core:test')
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkDirectories = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools\jdk') -Directory)
if ($jdkDirectories.Count -ne 1) {
    throw 'Expected one portable JDK under .tools/jdk. Complete toolchain setup first.'
}
$env:JAVA_HOME = $jdkDirectories[0].FullName
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-cache'
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools\android-user'
New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force | Out-Null
$gradleExecutable = Join-Path $projectRoot '.tools\gradle\gradle-8.13\bin\gradle.bat'
if (-not (Test-Path -LiteralPath $gradleExecutable)) {
    $gradleExecutable = Join-Path $projectRoot 'gradlew.bat'
}
Push-Location $projectRoot
try {
    & $gradleExecutable @Tasks --console=plain --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
