$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Push-Location $PSScriptRoot
try {
    & "$PSScriptRoot/Prepare-Driver.ps1"
    dotnet run --project Bridge.Tests -c Release
    if ($LASTEXITCODE -ne 0) { throw 'Protocol/safety tests failed' }
    dotnet run --project Worker.Tests -c Release
    if ($LASTEXITCODE -ne 0) { throw 'Input worker tests failed' }
    dotnet publish PhoneGamepadBridge -c Release -r win-x64 --self-contained true -o dist
    if ($LASTEXITCODE -ne 0) { throw 'Windows publish failed' }
    Copy-Item README.md dist/README.txt
    Copy-Item ../docs/LOW_LATENCY.md dist/LOW_LATENCY.md
    Copy-Item THIRD-PARTY-NOTICES.md dist/
    Copy-Item vendor/licenses dist/ -Recurse -Force
    Copy-Item vendor/driver-provenance.json dist/
    $exe = Join-Path $PSScriptRoot 'dist/PhoneGamepadBridge.exe'
    foreach ($arg in @('--verify-bundle','--probe-backend')) {
        $process = Start-Process $exe -ArgumentList $arg -WindowStyle Hidden -Wait -PassThru
        if ($process.ExitCode -ne 0) { throw "Packaged executable check failed: $arg ($($process.ExitCode))" }
        Write-Output "PASS packaged executable $arg (no driver installation performed)"
    }
    Get-ChildItem dist -File | Where-Object Name -ne 'SHA256SUMS.txt' | ForEach-Object {
        '{0}  {1}' -f (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(), $_.Name
    } | Set-Content dist/SHA256SUMS.txt
} finally { Pop-Location }
