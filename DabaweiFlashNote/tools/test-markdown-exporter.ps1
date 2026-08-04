$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\Dev\Java\jdk-17" }
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"

foreach ($tool in @($javac, $java)) {
  if (-not (Test-Path $tool)) {
    throw "Missing required tool: $tool"
  }
}

$testBuildDir = Join-Path $projectRoot "build\test-classes"
if (Test-Path $testBuildDir) {
  Remove-Item -LiteralPath $testBuildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $testBuildDir | Out-Null

$sourceFiles = @(
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\FlashNote.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\MarkdownExporter.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\MarkdownAnchorInserter.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\ObsidianImageAsset.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\PullGestureAction.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\SyncPathDefaults.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\ThemePalette.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\TodoSyncItem.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\TodoSyncParser.java"),
  (Join-Path $projectRoot "app\src\main\java\com\dabawei\flashnote\WebDavUrlBuilder.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\MarkdownAnchorInserterTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\ThemePaletteTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\WebDavUrlBuilderTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\ObsidianImageAssetTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\PullGestureActionTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\SyncPathDefaultsTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\TodoSyncParserTest.java"),
  (Join-Path $projectRoot "app\src\test\java\com\dabawei\flashnote\MarkdownExporterTest.java")
)

& $javac -encoding UTF-8 -source 8 -target 8 -d $testBuildDir @sourceFiles
if ($LASTEXITCODE -ne 0) { throw "MarkdownExporter test compilation failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.MarkdownExporterTest
if ($LASTEXITCODE -ne 0) { throw "MarkdownExporter tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.MarkdownAnchorInserterTest
if ($LASTEXITCODE -ne 0) { throw "MarkdownAnchorInserter tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.ThemePaletteTest
if ($LASTEXITCODE -ne 0) { throw "ThemePalette tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.WebDavUrlBuilderTest
if ($LASTEXITCODE -ne 0) { throw "WebDavUrlBuilder tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.ObsidianImageAssetTest
if ($LASTEXITCODE -ne 0) { throw "ObsidianImageAsset tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.SyncPathDefaultsTest
if ($LASTEXITCODE -ne 0) { throw "SyncPathDefaults tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.PullGestureActionTest
if ($LASTEXITCODE -ne 0) { throw "PullGestureAction tests failed" }

& $java -cp $testBuildDir com.dabawei.flashnote.TodoSyncParserTest
if ($LASTEXITCODE -ne 0) { throw "TodoSyncParser tests failed" }
