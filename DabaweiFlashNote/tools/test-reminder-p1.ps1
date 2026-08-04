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
$testBuildDir = Join-Path $projectRoot "build\test-classes-p1"
if (Test-Path -LiteralPath $testBuildDir) {
  Remove-Item -LiteralPath $testBuildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $testBuildDir | Out-Null

$sourceFiles = @(
  (Join-Path $sourceRoot "ReminderIds.java"),
  (Join-Path $sourceRoot "ReminderOccurrence.java"),
  (Join-Path $sourceRoot "ReminderRecord.java"),
  (Join-Path $sourceRoot "ReminderReconciliation.java"),
  (Join-Path $sourceRoot "ReminderTimeCalculator.java"),
  (Join-Path $sourceRoot "NaturalLanguageReminderParser.java"),
  (Join-Path $sourceRoot "TodoDateTime.java"),
  (Join-Path $sourceRoot "TodoSyncItem.java"),
  (Join-Path $testRoot "ReminderP0Test.java"),
  (Join-Path $testRoot "ReminderP1Test.java")
)

& $javac -encoding UTF-8 -source 8 -target 8 -d $testBuildDir @sourceFiles
if ($LASTEXITCODE -ne 0) { throw "Reminder P1 pure-Java test compilation failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.ReminderP0Test
if ($LASTEXITCODE -ne 0) { throw "Reminder P0 regression tests failed" }
& $java -cp $testBuildDir com.dabawei.flashnote.ReminderP1Test
if ($LASTEXITCODE -ne 0) { throw "Reminder P1 tests failed" }

$manifestPath = Join-Path $projectRoot "app\src\main\AndroidManifest.xml"
$stringsPath = Join-Path $projectRoot "app\src\main\res\values\strings.xml"
$databasePath = Join-Path $sourceRoot "FlashNoteDatabase.java"
$schedulerPath = Join-Path $sourceRoot "ReminderScheduler.java"
$receiverPath = Join-Path $sourceRoot "ReminderReceiver.java"
$mainActivityPath = Join-Path $sourceRoot "MainActivity.java"
$settingsActivityPath = Join-Path $sourceRoot "SyncSettingsActivity.java"
$buildPath = Join-Path $projectRoot "tools\build-apk.ps1"

foreach ($path in @(
  $manifestPath, $stringsPath, $databasePath, $schedulerPath, $receiverPath,
  $mainActivityPath, $settingsActivityPath, $buildPath
)) {
  if (-not (Test-Path -LiteralPath $path)) {
    throw "Missing reminder P1 file: $path"
  }
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8
$strings = Get-Content -LiteralPath $stringsPath -Raw -Encoding UTF8
$database = Get-Content -LiteralPath $databasePath -Raw -Encoding UTF8
$scheduler = Get-Content -LiteralPath $schedulerPath -Raw -Encoding UTF8
$receiver = Get-Content -LiteralPath $receiverPath -Raw -Encoding UTF8
$mainActivity = Get-Content -LiteralPath $mainActivityPath -Raw -Encoding UTF8
$settingsActivity = Get-Content -LiteralPath $settingsActivityPath -Raw -Encoding UTF8
$build = Get-Content -LiteralPath $buildPath -Raw -Encoding UTF8

foreach ($marker in @(
  "DAILY_OVERVIEW",
  "BACKGROUND_SYNC",
  ".TodoSyncJobService",
  "android.permission.BIND_JOB_SERVICE"
)) {
  if ($manifest -notmatch [regex]::Escape($marker)) {
    throw "Missing P1 manifest marker: $marker"
  }
}

foreach ($resource in @(
  "p1_daily_overview",
  "p1_daily_overview_time",
  "p1_background_sync",
  "p1_lock_screen_private",
  "p1_reminder_diagnostics",
  "p1_natural_time_title",
  "p1_multi_reminder",
  "p1_day_before",
  "p1_hour_before"
)) {
  if ($strings -notmatch ('name="' + [regex]::Escape($resource) + '"')) {
    throw "Missing P1 string resource: $resource"
  }
}

foreach ($marker in @(
  "TABLE_TODO_ITEMS",
  "TABLE_OCCURRENCES",
  "replaceRemoteTodos",
  "getOverviewTodos",
  "getScheduledReminderCount",
  "onUpgrade"
)) {
  if ($database -notmatch [regex]::Escape($marker)) {
    throw "Missing P1 database marker: $marker"
  }
}

foreach ($marker in @(
  "setInexactRepeating",
  "DAILY_OVERVIEW_CHANNEL_ID",
  "rescheduleDailyOverview",
  "getSchedulableOccurrences",
  "ReminderSettings.isLockScreenPrivate"
)) {
  if ($scheduler -notmatch [regex]::Escape($marker)) {
    throw "Missing P1 scheduler marker: $marker"
  }
}

foreach ($marker in @(
  "handleDailyOverview",
  "handleBackgroundSync",
  "getOccurrenceId",
  "ReminderSettings.isLockScreenPrivate"
)) {
  if ($receiver -notmatch [regex]::Escape($marker)) {
    throw "Missing P1 receiver marker: $marker"
  }
}

foreach ($marker in @(
  "NaturalLanguageReminderParser.parse",
  "showPreAlertPicker",
  "savePreAlerts",
  "PRE_ALERTS"
)) {
  if ($mainActivity -notmatch [regex]::Escape($marker)) {
    throw "Missing P1 MainActivity marker: $marker"
  }
}

foreach ($marker in @(
  "refreshReminderDiagnostics",
  "BackgroundSyncScheduler.ensureScheduled",
  "dailyOverviewTimeButton"
)) {
  if ($settingsActivity -notmatch [regex]::Escape($marker)) {
    throw "Missing P1 settings marker: $marker"
  }
}

if ($build -notmatch '\$versionCode\s*=\s*"49"' -or $build -notmatch '\$versionName\s*=\s*"0\.49-p1-todo-reminders"') {
  throw "Build version is not 49 / 0.49-p1-todo-reminders"
}

Write-Output "Reminder P1 pure-Java and static tests passed."
