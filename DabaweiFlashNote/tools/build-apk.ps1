$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\Dev\Android\sdk" }
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\Dev\Java\jdk-17" }
$androidUserHome = if ($env:ANDROID_USER_HOME) { $env:ANDROID_USER_HOME } else { "D:\Dev\Android\.android" }

$platform = Join-Path $androidHome "platforms\android-35\android.jar"
$buildTools = Join-Path $androidHome "build-tools\35.0.0"
$aapt2 = Join-Path $buildTools "aapt2.exe"
$d8 = Join-Path $buildTools "d8.bat"
$zipalign = Join-Path $buildTools "zipalign.exe"
$apksigner = Join-Path $buildTools "apksigner.bat"
$javac = Join-Path $javaHome "bin\javac.exe"
$jar = Join-Path $javaHome "bin\jar.exe"
$keytool = Join-Path $javaHome "bin\keytool.exe"

foreach ($tool in @($platform, $aapt2, $d8, $zipalign, $apksigner, $javac, $jar, $keytool)) {
  if (-not (Test-Path $tool)) {
    throw "Missing required tool: $tool"
  }
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
$javaBin = Join-Path $javaHome "bin"
if (($env:Path -split ";") -notcontains $javaBin) {
  $env:Path = $javaBin + ";" + $env:Path
}

$versionCode = "52"
$versionName = "0.52-feishu-card-silent-reminder"

$backupScript = Join-Path $projectRoot "tools\backup-core-code.py"
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
  $python = Get-Command py -ErrorAction SilentlyContinue
}
if (-not $python) {
  throw "Python is required for core code backup."
}
if ($python.Name -ieq "py.exe" -or $python.Name -ieq "py") {
  & $python.Source -3 $backupScript --version-code $versionCode --version-name $versionName --quiet
} else {
  & $python.Source $backupScript --version-code $versionCode --version-name $versionName --quiet
}
if ($LASTEXITCODE -ne 0) { throw "core code backup failed" }

$buildDir = Join-Path $projectRoot "build"
$compiledDir = Join-Path $buildDir "compiled-res"
$generatedDir = Join-Path $buildDir "generated"
$classesDir = Join-Path $buildDir "classes"
$dexDir = Join-Path $buildDir "dex"
$intermediatesDir = Join-Path $buildDir "intermediates"
$outputsDir = Join-Path $buildDir "outputs"

if (Test-Path $buildDir) {
  Remove-Item -LiteralPath $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $compiledDir, $generatedDir, $classesDir, $dexDir, $intermediatesDir, $outputsDir | Out-Null

$manifest = Join-Path $projectRoot "app\src\main\AndroidManifest.xml"
$resDir = Join-Path $projectRoot "app\src\main\res"
$sourceDir = Join-Path $projectRoot "app\src\main\java"
$buildInfoPath = Join-Path $generatedDir "com\dabawei\flashnote\BuildInfo.java"
$unsignedApk = Join-Path $intermediatesDir "DabaweiFlashNote-unsigned.apk"
$dexedApk = Join-Path $intermediatesDir "DabaweiFlashNote-dexed.apk"
$alignedApk = Join-Path $intermediatesDir "DabaweiFlashNote-aligned.apk"
$signedApk = Join-Path $outputsDir "DabaweiFlashNote-debug.apk"

$buildDate = Get-Date -Format "yyyy-MM-dd HH:mm"
$feishuDefaultPath = Join-Path $projectRoot "..\Codex-Temp\feishu-webhook-default.txt"
$feishuDefaultWebhook = ""
if (Test-Path -LiteralPath $feishuDefaultPath) {
  $feishuDefaultWebhook = (Get-Content -LiteralPath $feishuDefaultPath -Raw -Encoding UTF8).Trim()
}
if (-not $feishuDefaultWebhook -and $env:DABAWEI_FEISHU_WEBHOOK) {
  $feishuDefaultWebhook = $env:DABAWEI_FEISHU_WEBHOOK.Trim()
}
if ($feishuDefaultWebhook -and $feishuDefaultWebhook -notmatch '^https://') {
  throw "Default Feishu webhook must use HTTPS."
}
$feishuJavaValue = $feishuDefaultWebhook.Replace('\', '\\').Replace('"', '\"')
$buildInfoContent = @"
package com.dabawei.flashnote;

public final class BuildInfo {
    public static final String BUILD_DATE = "$buildDate";
    public static final String DEFAULT_FEISHU_WEBHOOK_URL = "$feishuJavaValue";

    private BuildInfo() {
    }
}
"@
$buildInfoParent = Split-Path -Parent $buildInfoPath
New-Item -ItemType Directory -Force -Path $buildInfoParent | Out-Null
[System.IO.File]::WriteAllText($buildInfoPath, $buildInfoContent, [System.Text.UTF8Encoding]::new($false))

& $aapt2 compile --dir $resDir -o $compiledDir
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

$compiledResources = Get-ChildItem -LiteralPath $compiledDir -Filter "*.flat" -Recurse
if (-not $compiledResources) { throw "No compiled resources were generated" }

$linkArgs = @(
  "link",
  "-o", $unsignedApk,
  "-I", $platform,
  "--manifest", $manifest,
  "--java", $generatedDir,
  "--auto-add-overlay",
  "--min-sdk-version", "23",
  "--target-sdk-version", "35",
  "--version-code", $versionCode,
  "--version-name", $versionName
)
foreach ($resource in $compiledResources) {
  $linkArgs += @("-R", $resource.FullName)
}
& $aapt2 @linkArgs
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

$javaFiles = @()
$javaFiles += Get-ChildItem -LiteralPath $sourceDir -Filter "*.java" -Recurse |
  Where-Object { $_.Name -ne "BuildInfo.java" } |
  ForEach-Object { $_.FullName }
$javaFiles += Get-ChildItem -LiteralPath $generatedDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
if (-not $javaFiles) { throw "No Java files found" }

& $javac -encoding UTF-8 -source 8 -target 8 -bootclasspath $platform -d $classesDir @javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

$classFiles = Get-ChildItem -LiteralPath $classesDir -Filter "*.class" -Recurse | ForEach-Object { $_.FullName }
if (-not $classFiles) { throw "No class files generated" }

$d8ArgsFile = Join-Path $intermediatesDir "d8-classes.txt"
[System.IO.File]::WriteAllLines($d8ArgsFile, $classFiles, [System.Text.UTF8Encoding]::new($false))
& $d8 --min-api 23 --lib $platform --output $dexDir "@$d8ArgsFile"
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Copy-Item -LiteralPath $unsignedApk -Destination $dexedApk -Force
& $jar uf $dexedApk -C $dexDir "classes.dex"
if ($LASTEXITCODE -ne 0) { throw "jar update failed" }

& $zipalign -f -p 4 $dexedApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

New-Item -ItemType Directory -Force -Path $androidUserHome | Out-Null
$debugKeystore = Join-Path $androidUserHome "debug.keystore"
if (-not (Test-Path $debugKeystore)) {
  & $keytool -genkeypair -v `
    -keystore $debugKeystore `
    -storepass android `
    -alias androiddebugkey `
    -keypass android `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -dname "CN=Android Debug,O=Android,C=US"
  if ($LASTEXITCODE -ne 0) { throw "debug keystore generation failed" }
}

& $apksigner sign `
  --ks $debugKeystore `
  --ks-pass pass:android `
  --key-pass pass:android `
  --out $signedApk `
  $alignedApk
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

& $apksigner verify --verbose $signedApk
if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed" }

Write-Output "Built APK: $signedApk"
