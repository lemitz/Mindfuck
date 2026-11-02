# Builds what the release workflow builds, into dist/:
#   Mindfuck-<version>-android.apk, SHA256SUMS.txt
# Usage: ./scripts/build.ps1 [-SkipTests] [-Offline]
#   -Offline passes --offline to Gradle (needs a warm Gradle cache, like the dev machine).
param([switch]$SkipTests, [switch]$Offline)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

& "$PSScriptRoot/check-release.ps1"
$version = (Get-Content VERSION -Raw).Trim()
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null

function Run($what, [scriptblock]$cmd) {
    Write-Host "==> $what"
    & $cmd
    if ($LASTEXITCODE -ne 0) { throw "$what failed ($LASTEXITCODE)" }
}

$tasks = @(':app:assembleRelease')
if (-not $SkipTests) { $tasks = @(':app:testDebugUnitTest') + $tasks }
$gradleArgs = $tasks + @('--no-daemon')
if ($Offline) { $gradleArgs += '--offline' }
Run 'gradle' { & .\gradlew.bat @gradleArgs }

Copy-Item (Join-Path $root 'app/build/outputs/apk/release/app-release.apk') (Join-Path $dist "Mindfuck-$version-android.apk") -Force

$hashes = Get-ChildItem $dist -File | Where-Object { $_.Name -like 'Mindfuck-*' } | Sort-Object Name | ForEach-Object {
    "$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant())  $($_.Name)"
}
[IO.File]::WriteAllText((Join-Path $dist 'SHA256SUMS.txt'), ($hashes -join "`n") + "`n", [Text.Encoding]::ASCII)
Write-Host "`nDone: v$version"
Get-ChildItem $dist | Select-Object Name, Length | Format-Table -AutoSize
