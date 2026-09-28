using System.Diagnostics;
using System.Reflection;
using System.Security.Cryptography;
namespace PhoneGamepad;

internal static class DriverSetup
{
    public const string FileName="ViGEmBus_1.22.0_x64_x86_arm64.exe";
    // CI verifies upstream Authenticode publisher before embedding. This hash is compiled
    // into the application alongside that verified payload, not fetched at run time.
    public static async Task<int> InstallAsync()
    {
        var assembly=Assembly.GetExecutingAssembly();
        using var checksum=assembly.GetManifestResourceStream("Bridge.Driver.sha256")??throw new IOException("Bundled driver checksum missing");
        using var reader=new StreamReader(checksum);string expected=(await reader.ReadToEndAsync()).Trim();
        if(expected.Length!=64||expected.Any(c=>!Uri.IsHexDigit(c)))throw new IOException("Invalid driver checksum");
        string folder=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"PhoneGamepadBridge","DriverCache",expected);
        Directory.CreateDirectory(folder);string path=Path.Combine(folder,FileName);
        using(var source=assembly.GetManifestResourceStream("Bridge.Driver.exe")??throw new IOException("Bundled driver missing"))
        using(var target=new FileStream(path,FileMode.Create,FileAccess.Write,FileShare.None))await source.CopyToAsync(target);
        // Hold a read-only sharing lock across verification AND elevation/installer execution.
        // No remotely supplied path, URL or arbitrary elevated command is accepted.
        using var locked=new FileStream(path,FileMode.Open,FileAccess.Read,FileShare.Read);
        var actual=Convert.ToHexString(await SHA256.HashDataAsync(locked));
        if(!actual.Equals(expected,StringComparison.OrdinalIgnoreCase))throw new IOException("Driver integrity check failed; nothing was installed");
        using var process=Process.Start(new ProcessStartInfo(path){UseShellExecute=true,Verb="runas"})??throw new IOException("Installer did not start");
        await process.WaitForExitAsync();return process.ExitCode;
    }
    public static bool VerifyEmbedded()
    {
        var assembly=Assembly.GetExecutingAssembly();
        using var data=assembly.GetManifestResourceStream("Bridge.Driver.exe");
        using var hash=assembly.GetManifestResourceStream("Bridge.Driver.sha256");
        if(data==null||hash==null)return false;
        using var r=new StreamReader(hash);
        return Convert.ToHexString(SHA256.HashData(data)).Equals(r.ReadToEnd().Trim(),StringComparison.OrdinalIgnoreCase);
    }
}
