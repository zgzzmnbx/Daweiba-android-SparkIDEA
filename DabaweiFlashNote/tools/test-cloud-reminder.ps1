$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$pythonCommand = Get-Command python.exe -ErrorAction SilentlyContinue
if ($null -eq $pythonCommand) {
  throw "Missing required tool: python.exe"
}

$testPath = Join-Path $projectRoot "cloud\test_cloud_reminder_service.py"
$previousBytecodeSetting = $env:PYTHONDONTWRITEBYTECODE
$env:PYTHONDONTWRITEBYTECODE = "1"
try {
  & $pythonCommand.Source $testPath
} finally {
  if ($null -eq $previousBytecodeSetting) {
    Remove-Item Env:PYTHONDONTWRITEBYTECODE -ErrorAction SilentlyContinue
  } else {
    $env:PYTHONDONTWRITEBYTECODE = $previousBytecodeSetting
  }
}
if ($LASTEXITCODE -ne 0) {
  throw "Cloud reminder tests failed"
}

Write-Output "Cloud reminder delivery tests passed."
