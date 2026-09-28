$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$vendor = Join-Path $PSScriptRoot 'vendor'
$licenses = Join-Path $vendor 'licenses'
New-Item -ItemType Directory -Force $licenses | Out-Null
$name = 'ViGEmBus_1.22.0_x64_x86_arm64.exe'
$url = "https://github.com/nefarius/ViGEmBus/releases/download/v1.22.0/$name"
$path = Join-Path $vendor $name
Invoke-WebRequest -Uri $url -OutFile $path
# Pin the exact upstream bytes independently of their signature.
# First observed and Authenticode-verified in Actions run 35449044111.
$expectedHash = '89220a7865076b342892f98865f3499fb7c4cfd673159e89d352c360fd014c6a'
if ((Get-Item $path).Length -ne 6278576) { throw 'Unexpected upstream installer size; review release before updating the pin.' }
$hash = (Get-FileHash -Algorithm SHA256 $path).Hash.ToLowerInvariant()
if ($hash -ne $expectedHash) { throw 'Upstream driver SHA-256 differs from the reviewed artifact. Nothing will be packaged.' }
$signature = Get-AuthenticodeSignature -FilePath $path
if ($signature.Status -ne 'Valid' -or $null -eq $signature.SignerCertificate -or
    $signature.SignerCertificate.Subject -notmatch 'Nefarius Software Solutions') {
    throw "Refusing to package an untrusted driver installer: $($signature.Status)"
}
[IO.File]::WriteAllText((Join-Path $vendor 'driver.sha256'), $hash)
[ordered]@{
    source = $url; release = '1.22.0'; bytes = (Get-Item $path).Length; sha256 = $hash
    signatureStatus = [string]$signature.Status; publisher = $signature.SignerCertificate.Subject
    signerThumbprint = $signature.SignerCertificate.Thumbprint
    note = 'Downloaded and signature-verified only. The build NEVER installs this driver.'
} | ConvertTo-Json | Set-Content (Join-Path $vendor 'driver-provenance.json')
$notices = @{
    'ViGEmBus-BSD-3-Clause.txt' = 'https://raw.githubusercontent.com/nefarius/ViGEmBus/master/LICENSE'
    'ViGEm.NET-MIT.txt' = 'https://raw.githubusercontent.com/nefarius/ViGEm.NET/master/LICENSE'
    'ViGEmClient-MIT.txt' = 'https://raw.githubusercontent.com/nefarius/ViGEmClient/master/LICENSE'
    'DotNet-LICENSE.txt' = 'https://raw.githubusercontent.com/dotnet/runtime/v10.0.0/LICENSE.TXT'
    'DotNet-THIRD-PARTY-NOTICES.txt' = 'https://raw.githubusercontent.com/dotnet/runtime/v10.0.0/THIRD-PARTY-NOTICES.TXT'
}
foreach ($item in $notices.GetEnumerator()) {
    Invoke-WebRequest -Uri $item.Value -OutFile (Join-Path $licenses $item.Key)
}
Write-Output "VERIFIED upstream driver SHA256 $hash; publisher $($signature.SignerCertificate.Subject)"
