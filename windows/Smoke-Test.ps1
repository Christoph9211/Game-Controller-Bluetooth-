$ErrorActionPreference = 'Stop'
Add-Type @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public static class BridgeSmokeWindow {
    public delegate bool Visitor(IntPtr window, IntPtr data);
    [DllImport("user32.dll")] public static extern bool EnumWindows(Visitor callback, IntPtr data);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr window, out uint process);
    [DllImport("user32.dll",CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr window, StringBuilder text, int max);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);
    public static IntPtr Find(uint process) {
        IntPtr found=IntPtr.Zero;
        EnumWindows((window,data)=>{
            uint owner;GetWindowThreadProcessId(window,out owner);
            if(owner==process){var title=new StringBuilder(256);GetWindowText(window,title,256);if(title.ToString().StartsWith("Phone Gamepad Bridge")){found=window;return false;}}
            return true;
        },IntPtr.Zero);
        return found;
    }
}
'@
$exe = Join-Path $PSScriptRoot 'dist/PhoneGamepadBridge.exe'
$process = Start-Process $exe -WindowStyle Hidden -PassThru
try {
    $window = [IntPtr]::Zero
    for ($attempt=0; $attempt -lt 40; $attempt++) {
        Start-Sleep -Milliseconds 250
        $process.Refresh()
        if ($process.HasExited) { throw 'Bridge exited before creating its window' }
        $window = [BridgeSmokeWindow]::Find([uint32]$process.Id)
        if ($window -ne [IntPtr]::Zero) { break }
    }
    if ($window -eq [IntPtr]::Zero) { throw 'Bridge did not create its window' }
    Write-Output 'PASS hidden main window created'
    if (-not [BridgeSmokeWindow]::PostMessage($window,0x10,[IntPtr]::Zero,[IntPtr]::Zero)) { throw 'Close request failed' }
    if (-not $process.WaitForExit(10000)) { throw 'Bridge did not exit cleanly' }
    if ($process.ExitCode -ne 0) { throw "Bridge exited with $($process.ExitCode)" }
    Write-Output 'PASS clean shutdown. Bluetooth, gameplay and virtual output hardware are unverified.'
} finally { if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force } }
