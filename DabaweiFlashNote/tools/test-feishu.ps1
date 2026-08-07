$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\Dev\Java\jdk-17" }
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"

foreach ($tool in @($javac, $java)) {
  if (-not (Test-Path -LiteralPath $tool)) {
    throw "Missing required tool: $tool"
  }
}

$sourceRoot = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote"
$testRoot = Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote"
$testBuildDir = Join-Path $projectRoot "build\test-classes-feishu"
if (Test-Path -LiteralPath $testBuildDir) {
  Remove-Item -LiteralPath $testBuildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $testBuildDir | Out-Null

$sourceFiles = @(
  (Join-Path $sourceRoot "FeishuWebhookClient.java"),
  (Join-Path $testRoot "FeishuWebhookTest.java")
)

& $javac -encoding UTF-8 -source 8 -target 8 -d $testBuildDir @sourceFiles
if ($LASTEXITCODE -ne 0) { throw "Feishu webhook pure-Java test compilation failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.FeishuWebhookTest
if ($LASTEXITCODE -ne 0) { throw "Feishu webhook tests failed" }

$manifestPath = Join-Path $projectRoot "app\src\main\AndroidManifest.xml"
$stringsPath = Join-Path $projectRoot "app\src\main\res\values\strings.xml"
$settingsPath = Join-Path $sourceRoot "SyncSettingsActivity.java"
$settingsModelPath = Join-Path $sourceRoot "FeishuSettings.java"
$receiverPath = Join-Path $sourceRoot "ReminderReceiver.java"
$clientPath = Join-Path $sourceRoot "FeishuWebhookClient.java"
$buildInfoPath = Join-Path $sourceRoot "BuildInfo.java"
$buildPath = Join-Path $projectRoot "tools\build-apk.ps1"

$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8
$strings = Get-Content -LiteralPath $stringsPath -Raw -Encoding UTF8
$settings = Get-Content -LiteralPath $settingsPath -Raw -Encoding UTF8
$settingsModel = Get-Content -LiteralPath $settingsModelPath -Raw -Encoding UTF8
$receiver = Get-Content -LiteralPath $receiverPath -Raw -Encoding UTF8
$client = Get-Content -LiteralPath $clientPath -Raw -Encoding UTF8
$buildInfo = Get-Content -LiteralPath $buildInfoPath -Raw -Encoding UTF8
$build = Get-Content -LiteralPath $buildPath -Raw -Encoding UTF8

foreach ($marker in @("android.permission.INTERNET")) {
  if ($manifest -notmatch [regex]::Escape($marker)) { throw "Missing Feishu manifest marker: $marker" }
}
foreach ($marker in @(
  "feishu_push_title",
  "feishu_push_enabled",
  "feishu_webhook_hint",
  "feishu_webhook_help",
  "feishu_webhook_invalid"
)) {
  if ($strings -notmatch ('name="' + [regex]::Escape($marker) + '"')) {
    throw "Missing Feishu string resource: $marker"
  }
}
foreach ($marker in @("FeishuSettings.load", "FeishuSettings.save", "feishuPushEnabled", "feishuWebhookUrl")) {
  if ($settings -notmatch [regex]::Escape($marker)) { throw "Missing Feishu settings marker: $marker" }
}
foreach ($marker in @("DEFAULT_FEISHU_WEBHOOK_URL", "isReady", "isValidWebhookUrl")) {
  if ($settingsModel -notmatch [regex]::Escape($marker)) { throw "Missing Feishu model marker: $marker" }
}
foreach ($marker in @("sendFeishuReminder", "FeishuWebhookClient.send", "goAsync")) {
  if ($receiver -notmatch [regex]::Escape($marker)) { throw "Missing Feishu receiver marker: $marker" }
}
foreach ($marker in @("buildTextPayload", "HttpURLConnection", "Content-Type")) {
  if ($client -notmatch [regex]::Escape($marker)) { throw "Missing Feishu client marker: $marker" }
}
if ($buildInfo -notmatch "DEFAULT_FEISHU_WEBHOOK_URL") {
  throw "BuildInfo must expose the generated Feishu default setting."
}
if ($build -notmatch '\$versionCode\s*=\s*"51"' -or $build -notmatch '\$versionName\s*=\s*"0\.51-feishu-reminder-push"') {
  throw "Build version is not 51 / 0.51-feishu-reminder-push"
}
foreach ($marker in @("feishu-webhook-default.txt", "DABAWEI_FEISHU_WEBHOOK", "DEFAULT_FEISHU_WEBHOOK_URL")) {
  if ($build -notmatch [regex]::Escape($marker)) { throw "Build missing Feishu default marker: $marker" }
}

Write-Output "Feishu webhook pure-Java and static tests passed."
