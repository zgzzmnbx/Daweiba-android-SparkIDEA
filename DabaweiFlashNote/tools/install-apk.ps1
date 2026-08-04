$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\Dev\Android\sdk" }
$adb = Join-Path $androidHome "platform-tools\adb.exe"
$apk = Join-Path $projectRoot "build\outputs\DabaweiFlashNote-debug.apk"

if (-not (Test-Path $adb)) {
  throw "Missing adb: $adb"
}

if (-not (Test-Path $apk)) {
  throw "Missing APK. Build first: powershell -ExecutionPolicy Bypass -File tools\build-apk.ps1"
}

$devices = & $adb devices
$readyDevices = $devices | Where-Object { $_ -match "\sdevice$" }

if (-not $readyDevices) {
  Write-Output $devices
  throw "No authorized Android device found. Enable USB debugging and accept the authorization prompt on the phone."
}

& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed" }

Write-Output "Installed APK: $apk"
