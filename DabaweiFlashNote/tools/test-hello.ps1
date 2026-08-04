$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$stringsPath = Join-Path $projectRoot "app\src\main\res\values\strings.xml"
$widgetInfoPath = Join-Path $projectRoot "app\src\main\res\xml\flash_note_widget_info.xml"
$mainLayoutPath = Join-Path $projectRoot "app\src\main\res\layout\activity_main.xml"
$mainActivityPath = Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\MainActivity.java"

if (-not (Test-Path $stringsPath)) {
  throw "Missing strings.xml at $stringsPath"
}

[xml]$strings = Get-Content -LiteralPath $stringsPath -Encoding UTF8
$appName = $strings.resources.string | Where-Object { $_.name -eq "app_name" } | Select-Object -First 1
$save = $strings.resources.string | Where-Object { $_.name -eq "save_note" } | Select-Object -First 1
$saveTodo = $strings.resources.string | Where-Object { $_.name -eq "save_todo" } | Select-Object -First 1
$uploadImage = $strings.resources.string | Where-Object { $_.name -eq "upload_image" } | Select-Object -First 1
$pasteClipboard = $strings.resources.string | Where-Object { $_.name -eq "paste_clipboard" } | Select-Object -First 1
$todoBadge = $strings.resources.string | Where-Object { $_.name -eq "todo_badge" } | Select-Object -First 1
$search = $strings.resources.string | Where-Object { $_.name -eq "search_hint" } | Select-Object -First 1
$export = $strings.resources.string | Where-Object { $_.name -eq "export_markdown" } | Select-Object -First 1
$theme = $strings.resources.string | Where-Object { $_.name -eq "theme_switch" } | Select-Object -First 1
$themeSelect = $strings.resources.string | Where-Object { $_.name -eq "theme_select" } | Select-Object -First 1
$widgetTitle = $strings.resources.string | Where-Object { $_.name -eq "widget_title" } | Select-Object -First 1
$widgetAction = $strings.resources.string | Where-Object { $_.name -eq "widget_action" } | Select-Object -First 1
$syncSettings = $strings.resources.string | Where-Object { $_.name -eq "sync_settings" } | Select-Object -First 1
$syncPendingNow = $strings.resources.string | Where-Object { $_.name -eq "sync_pending_now" } | Select-Object -First 1
$recordAction = $strings.resources.string | Where-Object { $_.name -eq "record_action" } | Select-Object -First 1
$navHome = $strings.resources.string | Where-Object { $_.name -eq "nav_home" } | Select-Object -First 1
$navTags = $strings.resources.string | Where-Object { $_.name -eq "nav_tags" } | Select-Object -First 1
$navStats = $strings.resources.string | Where-Object { $_.name -eq "nav_stats" } | Select-Object -First 1
$navMine = $strings.resources.string | Where-Object { $_.name -eq "nav_mine" } | Select-Object -First 1
$settingsTitle = $strings.resources.string | Where-Object { $_.name -eq "settings_title" } | Select-Object -First 1
$settingsGear = $strings.resources.string | Where-Object { $_.name -eq "settings_gear" } | Select-Object -First 1
$syncedBadge = $strings.resources.string | Where-Object { $_.name -eq "synced_badge" } | Select-Object -First 1
$deleteNote = $strings.resources.string | Where-Object { $_.name -eq "delete_note" } | Select-Object -First 1

if (-not $appName) {
  throw "Missing string resource app_name"
}

if (-not $save) {
  throw "Missing string resource save_note"
}

if (-not $saveTodo) {
  throw "Missing string resource save_todo"
}

if (-not $uploadImage) {
  throw "Missing string resource upload_image"
}

if (-not $pasteClipboard) {
  throw "Missing string resource paste_clipboard"
}

if (-not $todoBadge) {
  throw "Missing string resource todo_badge"
}

if (-not $search) {
  throw "Missing string resource search_hint"
}

if (-not $export) {
  throw "Missing string resource export_markdown"
}

if (-not $theme) {
  throw "Missing string resource theme_switch"
}

if (-not $themeSelect) {
  throw "Missing string resource theme_select"
}

if (-not $widgetTitle) {
  throw "Missing string resource widget_title"
}

if (-not $widgetAction) {
  throw "Missing string resource widget_action"
}

if (-not $syncSettings) {
  throw "Missing string resource sync_settings"
}

if (-not $syncPendingNow) {
  throw "Missing string resource sync_pending_now"
}

if (-not $recordAction) {
  throw "Missing string resource record_action"
}

foreach ($nav in @($navHome, $navTags, $navStats, $navMine)) {
  if (-not $nav) {
    throw "Missing bottom navigation string resource"
  }
}

if (-not $settingsTitle) {
  throw "Missing string resource settings_title"
}

if (-not $settingsGear) {
  throw "Missing string resource settings_gear"
}

if (-not $syncedBadge) {
  throw "Missing string resource synced_badge"
}

if (-not $deleteNote) {
  throw "Missing string resource delete_note"
}

if (-not (Test-Path $widgetInfoPath)) {
  throw "Missing app widget provider XML: $widgetInfoPath"
}

if (-not (Test-Path $mainLayoutPath)) {
  throw "Missing activity_main.xml at $mainLayoutPath"
}

if (-not (Test-Path $mainActivityPath)) {
  throw "Missing MainActivity.java at $mainActivityPath"
}

$mainLayoutRaw = Get-Content -LiteralPath $mainLayoutPath -Encoding UTF8 -Raw
foreach ($buttonId in @("recordButton", "saveButton", "saveTodoButton")) {
  $pattern = '(?s)<Button[^>]*android:id="@\+id/' + $buttonId + '"[^>]*>'
  $match = [regex]::Match($mainLayoutRaw, $pattern)
  if (-not $match.Success) {
    throw "Missing main action button: $buttonId"
  }
  if ($match.Value -match "backgroundTint") {
    throw "Main action button must not use fixed backgroundTint: $buttonId"
  }
}

foreach ($imageButtonId in @("uploadImageButton", "clipboardButton")) {
  $pattern = '(?s)<ImageButton[^>]*android:id="@\+id/' + $imageButtonId + '"[^>]*>'
  $match = [regex]::Match($mainLayoutRaw, $pattern)
  if (-not $match.Success) {
    throw "Missing input assist image button: $imageButtonId"
  }
}

if ($mainLayoutRaw -match "syncCurrentButton|settingsButton") {
  throw "Main title bar must not contain old syncCurrentButton/settingsButton"
}

$mainActivityRaw = Get-Content -LiteralPath $mainActivityPath -Encoding UTF8 -Raw
if ($mainActivityRaw -notmatch "setBackgroundTintList\(null\)") {
  throw "MainActivity styleButton must clear backgroundTint before applying theme background"
}

$expectedAppName = [char]0x5927 + [char]0x5c3e + [char]0x5df4 + [char]0x95ea + [char]0x5ff5
if ($appName.InnerText -ne $expectedAppName) {
  throw "Unexpected app_name: '$($appName.InnerText)'"
}

$expectedSave = [char]0x4fdd + [char]0x5b58 + [char]0x5355 + [char]0x6761 + [char]0x7b14 + [char]0x8bb0
$expectedSaveTodo = [char]0x4fdd + [char]0x5b58 + [char]0x4e3a + [char]0x5f85 + [char]0x529e
$expectedUploadImage = [char]0x4e0a + [char]0x4f20 + [char]0x56fe + [char]0x7247
$expectedPasteClipboard = [char]0x526a + [char]0x5207 + [char]0x677f
$expectedTodoBadge = [char]0x5f85 + [char]0x529e
$expectedSearch = [char]0x641c + [char]0x7d22 + [char]0x95ea + [char]0x5ff5
$expectedExport = [char]0x5bfc + [char]0x51fa
$expectedTheme = [char]0x4e3b + [char]0x9898
$expectedThemeSelect = [char]0x4e3b + [char]0x9898 + [char]0x9009 + [char]0x62e9
$expectedWidgetTitle = $expectedAppName
$expectedWidgetAction = [char]0x5199 + [char]0x4e00 + [char]0x6761 + [char]0x95ea + [char]0x5ff5
$expectedSyncSettings = [char]0x540c + [char]0x6b65
$expectedSyncPendingNow = [char]0x7acb + [char]0x5373 + [char]0x540c + [char]0x6b65 + [char]0x5f85 + [char]0x540c + [char]0x6b65
$expectedSettingsTitle = [char]0x8bbe + [char]0x7f6e
$expectedSyncedBadge = [char]0x5df2 + [char]0x540c + [char]0x6b65
$expectedDeleteNote = [char]0x5220 + [char]0x9664

if ($save.InnerText -ne $expectedSave) {
  throw "Unexpected save_note: '$($save.InnerText)'"
}

if ($saveTodo.InnerText -ne $expectedSaveTodo) {
  throw "Unexpected save_todo: '$($saveTodo.InnerText)'"
}

if ($uploadImage.InnerText -ne $expectedUploadImage) {
  throw "Unexpected upload_image: '$($uploadImage.InnerText)'"
}

if ($pasteClipboard.InnerText -ne $expectedPasteClipboard) {
  throw "Unexpected paste_clipboard: '$($pasteClipboard.InnerText)'"
}

if ($todoBadge.InnerText -ne $expectedTodoBadge) {
  throw "Unexpected todo_badge: '$($todoBadge.InnerText)'"
}

if ($search.InnerText -ne $expectedSearch) {
  throw "Unexpected search_hint: '$($search.InnerText)'"
}

if ($export.InnerText -ne $expectedExport) {
  throw "Unexpected export_markdown: '$($export.InnerText)'"
}

if ($theme.InnerText -ne $expectedTheme) {
  throw "Unexpected theme_switch: '$($theme.InnerText)'"
}

if ($themeSelect.InnerText -ne $expectedThemeSelect) {
  throw "Unexpected theme_select: '$($themeSelect.InnerText)'"
}

if ($widgetTitle.InnerText -ne $expectedWidgetTitle) {
  throw "Unexpected widget_title: '$($widgetTitle.InnerText)'"
}

if ($widgetAction.InnerText -ne $expectedWidgetAction) {
  throw "Unexpected widget_action: '$($widgetAction.InnerText)'"
}

if ($syncSettings.InnerText -ne $expectedSyncSettings) {
  throw "Unexpected sync_settings: '$($syncSettings.InnerText)'"
}

if ($syncPendingNow.InnerText -ne $expectedSyncPendingNow) {
  throw "Unexpected sync_pending_now: '$($syncPendingNow.InnerText)'"
}

if ($settingsTitle.InnerText -ne $expectedSettingsTitle) {
  throw "Unexpected settings_title: '$($settingsTitle.InnerText)'"
}

if ($settingsGear.InnerText -ne [char]0x2699) {
  throw "Unexpected settings_gear: '$($settingsGear.InnerText)'"
}

if ($syncedBadge.InnerText -ne $expectedSyncedBadge) {
  throw "Unexpected synced_badge: '$($syncedBadge.InnerText)'"
}

if ($deleteNote.InnerText -ne $expectedDeleteNote) {
  throw "Unexpected delete_note: '$($deleteNote.InnerText)'"
}

Write-Output "FlashNote resource test passed."
