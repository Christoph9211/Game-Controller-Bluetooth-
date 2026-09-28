using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;
namespace PhoneGamepad;

internal interface IControllerOutput:IDisposable { void Send(PadState state); }
internal sealed record BridgeSnapshot(bool Running,string Status,PadState State,long Reports);
internal sealed class XboxOutput:IControllerOutput
{
    private readonly ViGEmClient client;
    private readonly IXbox360Controller pad;
    public XboxOutput(){client=new ViGEmClient();try{pad=client.CreateXbox360Controller();pad.AutoSubmitReport=false;pad.Connect();}catch{client.Dispose();throw;}}
    public void Send(PadState s){
        pad.SetButtonsFull(s.Buttons);
        pad.SetAxisValue(Xbox360Axis.LeftThumbX,s.LeftX);pad.SetAxisValue(Xbox360Axis.LeftThumbY,s.LeftY);
        pad.SetAxisValue(Xbox360Axis.RightThumbX,s.RightX);pad.SetAxisValue(Xbox360Axis.RightThumbY,s.RightY);
        pad.SetSliderValue(Xbox360Slider.LeftTrigger,s.LeftTrigger);pad.SetSliderValue(Xbox360Slider.RightTrigger,s.RightTrigger);pad.SubmitReport();
    }
    public void Dispose(){try{pad.Disconnect();}finally{client.Dispose();}}
}
internal sealed class BridgeEngine:IDisposable
{
    private readonly object sync=new();
    private readonly Func<IControllerOutput> outputFactory;
    private readonly Func<long> clock;
    private readonly InputDiagnostics diagnostics;
    private readonly SafetyGate gate=new();
    private IControllerOutput? pad;
    private PadState current;
    private bool disposed;
    private long reports;
    private string status="Install controller support once, then Start bridge.";
    public BridgeEngine(Func<IControllerOutput> outputFactory,Func<long> clock,InputDiagnostics diagnostics){this.outputFactory=outputFactory;this.clock=clock;this.diagnostics=diagnostics;}
    public BridgeSnapshot Snapshot(){lock(sync)return new(pad!=null,status,current,reports);}
    public bool Start()
    {
        lock(sync)
        {
            if(pad!=null)return true;
            try{
                pad=outputFactory();gate.Reset();Send(default);status="Ready. Connect the phone in PC mode and release all controls.";return true;
            }
            catch(Exception e){StopCore();status="Cannot create Xbox controller: "+e.GetType().Name+". Use Install controller support, or restart Windows if requested.";return false;}
        }
    }
    public void Accept(byte[] report)
    {
        lock(sync)
        {
            if(pad==null||disposed)return;
            long decodeStart=System.Diagnostics.Stopwatch.GetTimestamp();
            bool valid=Protocol.TryDecode(report,out var state);diagnostics.Decode.Add(System.Diagnostics.Stopwatch.GetElapsedTime(decodeStart).TotalMilliseconds);
            if(!valid){diagnostics.Rejected();ResetCore("Rejected incompatible report; release controls to resume.");return;}
            try{
                Send(gate.Accept(state,clock()));reports++;
                status=gate.Armed?"Xbox controller active. Keep this window open or minimized.":"Release ALL controls on the phone to arm the bridge.";
            }
            catch(Exception e){StopCore();status="Xbox output stopped: "+e.GetType().Name+". Start bridge to retry.";}
        }
    }
    public void Reset(string reason){lock(sync)ResetCore(reason);}
    private void ResetCore(string reason)
    {
        diagnostics.SafetyReset();gate.Reset();try{if(pad!=null)Send(default);}catch{StopCore();}status=reason;
    }
    public void Watchdog()
    {
        lock(sync){if(disposed||pad==null)return;if(gate.Expire(clock()))ResetCore("Phone input timed out; controls released. Reconnect and release all controls.");}
    }
    private void Send(PadState s)
    {
        if(pad==null)return;
        long began=System.Diagnostics.Stopwatch.GetTimestamp();
        try{pad.Send(s);current=s;}finally{diagnostics.Submit.Add(System.Diagnostics.Stopwatch.GetElapsedTime(began).TotalMilliseconds);}
    }
    private void StopCore()
    {
        if(pad!=null){try{Send(default);}catch{}try{pad.Dispose();}catch{}pad=null;}
        current=default;gate.Reset();
    }
    public void Stop(){lock(sync){StopCore();status="Stopped. The virtual controller is disconnected.";}}
    public void Dispose(){lock(sync){disposed=true;StopCore();}}
    public static int ProbeBackend()
    {
        try{using var c=new ViGEmClient();return 0;}
        catch(Exception e){return e.GetType().Name.Contains("BusNotFound",StringComparison.OrdinalIgnoreCase)?0:1;}
    }
}
