$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$manifestPath = Join-Path $projectRoot "app\src\main\AndroidManifest.xml"
$stringsPath = Join-Path $projectRoot "app\src\main\res\values\strings.xml"
$databasePath = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\FlashNoteDatabase.java"
$schedulerPath = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\ReminderScheduler.java"
$receiverPath = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\ReminderReceiver.java"
$reconciliationPath = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\ReminderReconciliation.java"
$buildPath = Join-Path $projectRoot "tools\build-apk.ps1"

foreach ($path in @($manifestPath, $stringsPath, $databasePath, $schedulerPath, $receiverPath, $reconciliationPath, $buildPath)) {
  if (-not (Test-Path -LiteralPath $path)) {
    throw "Missing reminder P0 file: $path"
  }
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8
$strings = Get-Content -LiteralPath $stringsPath -Raw -Encoding UTF8
$database = Get-Content -LiteralPath $databasePath -Raw -Encoding UTF8
$scheduler = Get-Content -LiteralPath $schedulerPath -Raw -Encoding UTF8
$receiver = Get-Content -LiteralPath $receiverPath -Raw -Encoding UTF8
$reconciliation = Get-Content -LiteralPath $reconciliationPath -Raw -Encoding UTF8
$build = Get-Content -LiteralPath $buildPath -Raw -Encoding UTF8

foreach ($permission in @(
  "android.permission.POST_NOTIFICATIONS",
  "android.permission.SCHEDULE_EXACT_ALARM",
  "android.permission.RECEIVE_BOOT_COMPLETED",
  "android.permission.WAKE_LOCK"
)) {
  if ($manifest -notmatch [regex]::Escape($permission)) {
    throw "Missing manifest permission: $permission"
  }
}

foreach ($marker in @(
  ".ReminderReceiver",
  "android.intent.action.BOOT_COMPLETED",
  "android.intent.action.TIME_SET",
  "android.intent.action.TIMEZONE_CHANGED",
  "REMINDER_FIRE",
  "REMINDER_SNOOZE"
)) {
  if ($manifest -notmatch [regex]::Escape($marker)) {
    throw "Missing reminder manifest marker: $marker"
  }
}

foreach ($resource in @(
  "reminder_add",
  "reminder_edit",
  "reminder_cancel",
  "reminder_notification_permission_explain",
  "reminder_exact_permission_explain",
  "reminder_system_delay",
  "reminder_bell"
)) {
  if ($strings -notmatch ('name="' + [regex]::Escape($resource) + '"')) {
    throw "Missing reminder string resource: $resource"
  }
}

foreach ($marker in @(
  "CREATE TABLE IF",
  "TABLE_REMINDERS",
  "getReminderByTaskId",
  "getRemoteReminders",
  "getSchedulableReminders"
)) {
  if ($database -notmatch [regex]::Escape($marker)) {
    throw "Missing reminder database marker: $marker"
  }
}

foreach ($marker in @(
  "AlarmManager",
  "setExactAndAllowWhileIdle",
  "canScheduleExactAlarms",
  "todo_reminders"
)) {
  if ($scheduler -notmatch [regex]::Escape($marker)) {
    throw "Missing reminder scheduler marker: $marker"
  }
}

foreach ($marker in @(
  "snoozeTen",
  "snoozeHour",
  "viewPendingIntent",
  "EXTRA_OPEN_TODO",
  "STATUS_FIRED"
)) {
  if ($receiver -notmatch [regex]::Escape($marker)) {
    throw "Missing reminder notification marker: $marker"
  }
}

foreach ($marker in @(
  "STATUS_OVERDUE",
  "STATUS_CANCELLED",
  "getRemoteRemindAtText"
)) {
  if ($reconciliation -notmatch [regex]::Escape($marker)) {
    throw "Missing reminder reconciliation marker: $marker"
  }
}

if ($build -notmatch '\$versionCode\s*=\s*"51"' -or $build -notmatch '\$versionName\s*=\s*"0\.51-feishu-reminder-push"') {
  throw "Build version is not 51 / 0.51-feishu-reminder-push"
}

Write-Output "Reminder P0 static test passed."
