using Microsoft.Win32;
using System.ComponentModel;
using System.Diagnostics;
namespace PhoneGamepad;

internal static class Program
{
    [STAThread] static int Main(string[] args)
    {
        if(args.Contains("--verify-bundle"))return DriverSetup.VerifyEmbedded()?0:1;
        if(args.Contains("--probe-backend"))return BridgeEngine.ProbeBackend();
        using var mutex=new Mutex(true,"Local\\PhoneGamepadBridge-FF66-v1",out bool first);
        if(!first){MessageBox.Show("Phone Gamepad Bridge is already running. Restore its existing window.");return 0;}
        ApplicationConfiguration.Initialize();Application.Run(new MainWindow());return 0;
    }
}

internal sealed class MainWindow:Form
{
    private readonly InputWorker bridge=new();
    private readonly ComboBox devices=new(){DropDownStyle=ComboBoxStyle.DropDownList,Width=380};
    private readonly Label status=new(){AutoSize=true,MaximumSize=new Size(680,0)};
    private readonly Label values=new(){AutoSize=true};
    private readonly Button start=new(){Text="Start bridge",AutoSize=true};
    private readonly Button install=new(){Text="Install controller support (one time)",AutoSize=true};
    private readonly System.Windows.Forms.Timer timer=new(){Interval=100};
    private bool changing,closing,installing;
    private InputDevice[] displayed=[];
    private bool shutdownComplete;
    public MainWindow()
    {
        Text="Phone Gamepad Bridge 0.2.0 - Windows Xbox mode";ClientSize=new Size(740,450);MinimumSize=new Size(660,440);
        AutoScaleMode=AutoScaleMode.Dpi;
        var layout=new FlowLayoutPanel{Dock=DockStyle.Fill,FlowDirection=FlowDirection.TopDown,WrapContents=false,AutoScroll=true,Padding=new Padding(20)};
        layout.Controls.Add(new Label{Text="PHONE GAMEPAD  /  PC BRIDGE",Font=new Font(SystemFonts.DefaultFont.FontFamily,18,FontStyle.Bold),AutoSize=true});
        layout.Controls.Add(new Label{AutoSize=true,MaximumSize=new Size(680,0),Text="1. Install controller support once below (Windows approval required).\n2. On the phone: Stop > Options > Windows Xbox bridge. Start, pair and connect.\n3. Start bridge. Release controls, then open your game normally. No Steam or x360ce.\nAfter changing phone mode, forget and re-pair these two devices to refresh the HID descriptor."});
        layout.Controls.Add(install);install.Click+=async(_,_)=>await InstallDriver();
        var row=new FlowLayoutPanel{AutoSize=true,WrapContents=false};row.Controls.Add(devices);
        var refresh=new Button{Text="Refresh",AutoSize=true};refresh.Click+=async(_,_)=>await RunCommand(bridge.RefreshAsync);row.Controls.Add(refresh);layout.Controls.Add(row);
        devices.SelectedIndexChanged+=async(_,_)=>{if(!changing)await RunCommand(()=>bridge.SelectAsync((devices.SelectedItem as InputDevice)?.Handle??0));};
        var controls=new FlowLayoutPanel{AutoSize=true};controls.Controls.Add(start);
        start.Click+=async(_,_)=>{start.Enabled=false;try{await RunCommand(bridge.Snapshot.Bridge.Running?bridge.StopAsync:bridge.StartAsync);}finally{start.Enabled=true;}UpdateStatus();};
        var bluetooth=new Button{Text="Bluetooth settings",AutoSize=true};bluetooth.Click+=(_,_)=>OpenSettings();controls.Controls.Add(bluetooth);
        var test=new Button{Text="Windows controller test",AutoSize=true};test.Click+=(_,_)=>{try{Process.Start(new ProcessStartInfo("control.exe","joy.cpl"){UseShellExecute=true});}catch(Exception e){MessageBox.Show(e.Message);}};controls.Controls.Add(test);layout.Controls.Add(controls);
        var copy=new Button{Text="Copy diagnostics",AutoSize=true};copy.Click+=(_,_)=>{try{Clipboard.SetText(bridge.Diagnostics.Summary());}catch(System.Runtime.InteropServices.ExternalException){MessageBox.Show("Clipboard is busy. Try Copy diagnostics again.");}};controls.Controls.Add(copy);
        var reset=new Button{Text="Reset diagnostics",AutoSize=true};reset.Click+=async(_,_)=>await RunCommand(bridge.ResetDiagnosticsAsync);controls.Controls.Add(reset);
        layout.Controls.Add(status);layout.Controls.Add(values);
        layout.Controls.Add(new Label{AutoSize=true,MaximumSize=new Size(680,0),Text="PC mode uses a dedicated HID data channel, so there is no second physical gamepad to hide.\nNo remapping wizard, HidHide, game-folder DLLs, accounts or network server.\nCompatibility build: ViGEmBus is retired. Bundled upstream installer retains its signature.\nThis bridge is an unsigned test build; do not disable Windows security to run it."});
        Controls.Add(layout);
        Shown+=async(_,_)=>{await RunCommand(()=>bridge.Ready);UpdateStatus();timer.Start();};
        timer.Tick+=(_,_)=>UpdateStatus();
        SystemEvents.PowerModeChanged+=PowerChanged;
        FormClosing+=async(_,e)=>{
            if(shutdownComplete)return;
            e.Cancel=true;
            if(installing){MessageBox.Show("Finish or cancel the driver installer before closing.");return;}
            if(closing)return;
            closing=true;timer.Stop();Enabled=false;SystemEvents.PowerModeChanged-=PowerChanged;
            await bridge.ShutdownAsync();shutdownComplete=true;Close();
        };
    }
    private async Task RunCommand(Func<Task> action){try{await action();}catch(Exception e){if(!closing)MessageBox.Show("Input worker: "+e.Message);}}
    private void PowerChanged(object? sender,PowerModeChangedEventArgs e){_ = ResetForPower();}
    private async Task ResetForPower(){try{await bridge.ResetAsync("Power-state change: controls released. Release phone controls to resume.");}catch{}}
    private void UpdateStatus()
    {
        var snapshot=bridge.Snapshot;var s=snapshot.Bridge;
        if(!ReferenceEquals(displayed,snapshot.Devices)){
            changing=true;devices.Items.Clear();devices.Items.AddRange(snapshot.Devices.Cast<object>().ToArray());displayed=snapshot.Devices;changing=false;
        }
        int index=Array.FindIndex(displayed,d=>d.Handle==snapshot.Selected);
        if(devices.SelectedIndex!=index){changing=true;devices.SelectedIndex=index;changing=false;}start.Text=s.Running?"Stop bridge":"Start bridge";status.Text=s.Status;
        values.Text=$"Phones found: {devices.Items.Count} | reports accepted: {s.Reports}\n"+
            $"Xbox buttons: 0x{s.State.Buttons:X4} | LT {s.State.LeftTrigger} RT {s.State.RightTrigger}\n"+
            $"Left X {s.State.LeftX} Y {s.State.LeftY} | Right X {s.State.RightX} Y {s.State.RightY}\n"+
            "Xbox directions: up/right positive. Input counts are not latency measurements.";
    }
    private async Task InstallDriver()
    {
        var yes=MessageBox.Show("This installs the bundled, upstream-signed ViGEmBus 1.22.0 system driver. It is retired and no longer receives updates.\n\nWindows administrator approval and the official installer prompts are required. No security settings will be changed. Other software may share this driver.\n\nContinue?","Install Xbox controller support",MessageBoxButtons.YesNo,MessageBoxIcon.Warning);
        if(yes!=DialogResult.Yes)return;
        installing=true;install.Enabled=false;start.Enabled=false;
        try{
            await bridge.StopAsync();
            int code=await DriverSetup.InstallAsync();
            if(code==0){await bridge.StartAsync();MessageBox.Show("Installer finished. The bridge checked whether the driver is usable; see its status. Restart Windows first if the installer requested it.");}
            else if(code==3010||code==1641)MessageBox.Show("Windows must restart before controller support is used. Save your work and restart manually.");
            else MessageBox.Show($"Installer ended with code {code}. Nothing will be forced. Complete/cancel any existing installation before retrying.");
        }
        catch(Win32Exception e)when(e.NativeErrorCode==1223){MessageBox.Show("Windows administrator approval was cancelled. No fallback installation was attempted.");}
        catch(Exception e){MessageBox.Show("Driver setup stopped: "+e.Message);}
        finally{installing=false;install.Enabled=true;start.Enabled=true;UpdateStatus();}
    }
    private static void OpenSettings(){try{Process.Start(new ProcessStartInfo("ms-settings:bluetooth"){UseShellExecute=true});}catch(Exception e){MessageBox.Show(e.Message);}}
}
