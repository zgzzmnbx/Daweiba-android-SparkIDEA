$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$resRoot = Join-Path $projectRoot "app\src\main\res"
$javaRoot = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote"
$failures = New-Object System.Collections.Generic.List[string]

function Fail([string]$message) {
  $failures.Add($message)
  Write-Output "RED: $message"
}

function Read-Required([string]$path) {
  if (-not (Test-Path -LiteralPath $path)) {
    Fail "missing file: $path"
    return ""
  }
  return Get-Content -LiteralPath $path -Raw -Encoding UTF8
}

function Get-ResourceMap([string]$path) {
  $map = @{}
  if (-not (Test-Path -LiteralPath $path)) {
    return $map
  }
  try {
    [xml]$document = Get-Content -LiteralPath $path -Raw -Encoding UTF8
    foreach ($node in @($document.resources.color)) {
      if ($node.name -and $node.InnerText -match '^#[0-9A-Fa-f]{6}$') {
        $map[$node.name] = $node.InnerText
      }
    }
  } catch {
    Fail "invalid color XML: $path ($($_.Exception.Message))"
  }
  return $map
}

function Get-Luminance([string]$hex) {
  $rgb = @(1, 3, 5 | ForEach-Object { [Convert]::ToInt32($hex.Substring($_, 2), 16) / 255.0 })
  $linear = $rgb | ForEach-Object {
    if ($_ -le 0.04045) { $_ / 12.92 } else { [math]::Pow(($_ + 0.055) / 1.055, 2.4) }
  }
  return 0.2126 * $linear[0] + 0.7152 * $linear[1] + 0.0722 * $linear[2]
}

function Get-Contrast([string]$first, [string]$second) {
  $a = Get-Luminance $first
  $b = Get-Luminance $second
  return ([math]::Max($a, $b) + 0.05) / ([math]::Min($a, $b) + 0.05)
}

$lightPath = Join-Path $resRoot "values\colors.xml"
$darkPath = Join-Path $resRoot "values-night\colors.xml"
$lightColors = Get-ResourceMap $lightPath
$darkColors = Get-ResourceMap $darkPath
$requiredTokens = @(
  "background", "surface", "card", "foreground", "muted_foreground", "border", "input",
  "primary", "primary_foreground", "accent", "accent_foreground", "destructive",
  "destructive_foreground", "success", "success_foreground", "warning", "warning_foreground", "ring"
)
foreach ($token in $requiredTokens) {
  if (-not $lightColors.ContainsKey($token)) { Fail "light semantic token missing: $token" }
  if (-not $darkColors.ContainsKey($token)) { Fail "dark semantic token missing: $token" }
}

foreach ($pair in @(
  @("foreground", "background"), @("muted_foreground", "background"),
  @("primary_foreground", "primary"), @("accent_foreground", "accent"),
  @("destructive_foreground", "destructive"), @("success_foreground", "success"),
  @("warning_foreground", "warning")
)) {
  foreach ($mode in @(@("light", $lightColors), @("dark", $darkColors))) {
    $colors = $mode[1]
    if ($colors.ContainsKey($pair[0]) -and $colors.ContainsKey($pair[1])) {
      $contrast = Get-Contrast $colors[$pair[0]] $colors[$pair[1]]
      if ($contrast -lt 4.5) {
        Fail "$($mode[0]) contrast $($pair[0])/$($pair[1]) is $([math]::Round($contrast, 2))"
      }
    }
  }
}

$themePath = Join-Path $javaRoot "ThemePalette.java"
$themeRaw = Read-Required $themePath
foreach ($legacyKey in @("paper", "ink", "forest", "apple", "linear", "notion", "raycast", "obsidian")) {
  if ($themeRaw -notmatch [regex]::Escape($legacyKey)) { Fail "legacy theme key missing: $legacyKey" }
}
foreach ($newKey in @("system", "light", "dark")) {
  if ($themeRaw -notmatch ('"' + $newKey + '"')) { Fail "theme preference missing: $newKey" }
}
if ($themeRaw -notmatch "migratePreference|resolve") { Fail "ThemePalette has no explicit legacy migration/resolution" }

$allXml = Get-ChildItem -LiteralPath $resRoot -Filter "*.xml" -Recurse
foreach ($xmlPath in $allXml) {
  try {
    [void][xml](Get-Content -LiteralPath $xmlPath.FullName -Raw -Encoding UTF8)
  } catch {
    Fail "invalid resource XML: $($xmlPath.FullName)"
  }
}

$layoutFiles = Get-ChildItem -LiteralPath (Join-Path $resRoot "layout") -Filter "*.xml"
$layoutText = ($layoutFiles | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8 }) -join "`n"
$requiredIds = @(
  "pullRoot", "rootLayout", "searchPanel", "noteInputPanel", "saveActions", "historyHeader", "todoPage", "bottomNav",
  "appTitle", "syncStatus", "noteInput", "searchInput", "saveButton", "saveTodoButton", "uploadImageButton", "clipboardButton",
  "recordButton", "noteList", "todoList", "navHome", "navTags", "navStats", "navMine", "navHomeIcon", "navTagsIcon", "navStatsIcon", "navMineIcon",
  "themeSpinner", "claudeFontStyle", "exportButton", "reminderNotificationSettingsButton", "reminderExactSettingsButton",
  "dailyOverviewEnabled", "dailyOverviewTimeButton", "backgroundSyncEnabled", "cloudReminderEnabled", "cloudReminderHelp",
  "lockscreenPrivate", "feishuPushEnabled", "feishuWebhookUrl",
  "reminderDiagnostics", "refreshReminderDiagnosticsButton", "syncEnabled", "webdavBaseUrl", "webdavUsername", "webdavPassword", "webdavRemotePath", "webdavAnchor",
  "saveSyncSettingsButton", "versionInfo", "quickNoteInput", "quickSaveButton", "widgetTitle", "widgetRecent", "widgetAction", "noteCard", "deleteButton"
)
foreach ($id in $requiredIds) {
  if ($layoutText -notmatch ('@\+id/' + [regex]::Escape($id) + '\b')) { Fail "required view id missing: $id" }
}

$interactiveIds = @(
  "recordButton", "saveButton", "saveTodoButton", "uploadImageButton", "clipboardButton", "quickSaveButton", "exportButton",
  "reminderNotificationSettingsButton", "reminderExactSettingsButton", "dailyOverviewEnabled", "dailyOverviewTimeButton", "backgroundSyncEnabled",
  "cloudReminderEnabled", "lockscreenPrivate", "feishuPushEnabled", "syncEnabled", "refreshReminderDiagnosticsButton", "saveSyncSettingsButton", "themeSpinner", "deleteButton",
  "navHome", "navTags", "navStats", "navMine", "widgetAction"
)
foreach ($id in $interactiveIds) {
  $match = [regex]::Match($layoutText, '(?s)<[^>]+(?:android:id="@\+id/' + [regex]::Escape($id) + '"|android:id="@id/' + [regex]::Escape($id) + '")[^>]*>')
  if (-not $match.Success) {
    Fail "interactive view missing: $id"
    continue
  }
  if ($match.Value -notmatch 'android:(?:minHeight|layout_height)="(?:4[8-9]|[5-9][0-9]|[1-9][0-9]{2,})dp"' -and $match.Value -notmatch 'android:layout_height="match_parent"') {
    Fail "interactive target under 48dp: $id"
  }
}

$settingsLayout = Read-Required (Join-Path $resRoot "layout\activity_sync_settings.xml")
foreach ($secretId in @("webdavPassword", "feishuWebhookUrl")) {
  $secretMatch = [regex]::Match($settingsLayout, '(?s)<EditText[^>]*android:id="@\+id/' + $secretId + '"[^>]*>')
  if (-not $secretMatch.Success -or $secretMatch.Value -notmatch 'textPassword') {
    Fail "sensitive field is not masked by default: $secretId"
  }
}

$cloudSettingsRaw = Read-Required (Join-Path $javaRoot "CloudReminderSettings.java")
if ($cloudSettingsRaw -notmatch 'getBoolean\(KEY_ENABLED, true\)') {
  Fail "cloud reminder switch is not enabled by default"
}
if ($cloudSettingsRaw -notmatch 'isReady\(\)') {
  Fail "cloud reminder settings have no readiness gate"
}

$legacyIconFiles = @(
  "ic_action_save.xml", "ic_action_todo.xml", "ic_input_clipboard.xml", "ic_input_image.xml",
  "ic_nav_history.xml", "ic_nav_home.xml", "ic_nav_mine.xml", "ic_nav_settings.xml",
  "ic_nav_stats.xml", "ic_nav_tags.xml", "ic_status_synced.xml", "ic_timeline_delete.xml",
  "ic_top_sync.xml", "ic_trash.xml"
)
$repoRoot = Split-Path -Parent $projectRoot
$legacyIconPaths = $legacyIconFiles | ForEach-Object { "DabaweiFlashNote/app/src/main/res/drawable/$_" }
$legacyIconMismatches = New-Object System.Collections.Generic.List[string]
foreach ($legacyIconPath in $legacyIconPaths) {
  $baselineBlob = (& git -C $repoRoot rev-parse "436027b:$legacyIconPath").Trim()
  $currentBlob = (& git -C $repoRoot hash-object -- $legacyIconPath).Trim()
  if ($baselineBlob -ne $currentBlob) {
    $legacyIconMismatches.Add($legacyIconPath)
  }
}
if ($legacyIconMismatches.Count -gt 0) {
  Fail "legacy icon assets diverged from the pre-redesign baseline: $($legacyIconMismatches -join ', ')"
}

$uiFiles = @(
  (Join-Path $resRoot "layout"), (Join-Path $resRoot "drawable"),
  (Join-Path $javaRoot "MainActivity.java"), (Join-Path $javaRoot "SyncSettingsActivity.java"),
  (Join-Path $javaRoot "QuickCaptureActivity.java"), (Join-Path $javaRoot "FlashNoteWidgetProvider.java")
)
foreach ($path in $uiFiles) {
  $items = if (Test-Path -LiteralPath $path -PathType Container) { Get-ChildItem -LiteralPath $path -Filter "*.xml" } else { Get-Item -LiteralPath $path }
  foreach ($item in $items) {
    $raw = Get-Content -LiteralPath $item.FullName -Raw -Encoding UTF8
    if ($raw -match '#[0-9A-Fa-f]{6}' -and $legacyIconFiles -notcontains $item.Name) {
      Fail "raw UI color outside semantic token files: $($item.FullName)"
    }
  }
}

$mainRaw = Read-Required (Join-Path $javaRoot "MainActivity.java")
if ($mainRaw -match "\\uD83D\\uDD14|\\u26A0|\\u2315|\\u2699") { Fail "operation/status UI still uses emoji or glyph icons" }
if ($mainRaw.Contains('Color.parseColor("#')) { Fail "MainActivity contains scattered UI hex color" }

$buildRaw = Read-Required (Join-Path $projectRoot "tools\build-apk.ps1")
if (-not $buildRaw.Contains('$versionCode = "52"') -or -not $buildRaw.Contains('$versionName = "0.6.0-shadcn-rhea-ui"')) {
  Fail "build script is not v52 / 0.6.0-shadcn-rhea-ui"
}

if ($failures.Count -gt 0) {
  Write-Output "UI redesign contract is RED ($($failures.Count) failures)."
  exit 1
}

Write-Output "UI redesign contract is GREEN (tokens, contrast, migration, touch targets, masking, XML and IDs)."
