using System.Collections.Concurrent;
using System.Diagnostics;
using System.Runtime.InteropServices;
namespace PhoneGamepad;

internal interface IInputSource
{
    void Register(nint window);
    void Unregister();
    List<InputDevice> Devices();
    InputBatch? Read(nint input);
}
internal sealed class WindowsInputSource:IInputSource
{
    public void Register(nint window)=>RawInput.Register(window);
    public void Unregister()=>RawInput.Unregister();
    public List<InputDevice> Devices()=>RawInput.Devices();
    public InputBatch? Read(nint input)=>RawInput.Read(input);
}
internal sealed record WorkerSnapshot(BridgeSnapshot Bridge,InputDevice[] Devices,nint Selected);

/// <summary>Owns Raw Input, controller lifetime, and commands. Enumeration never runs on this thread.</summary>
internal sealed class InputWorker
{
    private readonly IInputSource source;
    private readonly Func<IControllerOutput> outputFactory;
    private readonly Func<long> clock;
    private readonly ConcurrentQueue<Action> commands=new();
    private readonly object posting=new();
    private readonly TaskCompletionSource ready=new(TaskCreationOptions.RunContinuationsAsynchronously);
    private readonly TaskCompletionSource stopped=new(TaskCreationOptions.RunContinuationsAsynchronously);
    private readonly Thread thread;
    private InputWindow? window;
    private BridgeEngine bridge=null!;
    private ApplicationContext context=null!;
    private System.Windows.Forms.Timer? watchdog,fallback;
    private WorkerSnapshot snapshot=new(new(false,"Starting input worker...",default,0),[],0);
    private InputDevice[] devices=[];
    private nint selected;
    private string? selectedPath;
    private long deviceGeneration;
    private bool scanning,rescan,closing;
    private bool accepting=true;
    public InputDiagnostics Diagnostics {get;}=new();
    public Task Ready=>ready.Task;
    public WorkerSnapshot Snapshot=>Volatile.Read(ref snapshot);
    [DllImport("user32.dll",SetLastError=true)] private static extern bool PostMessage(nint window,uint message,nint wParam,nint lParam);
    private const int CommandMessage=0x8001;
    public InputWorker(IInputSource? source=null,Func<IControllerOutput>? outputFactory=null,Func<long>? clock=null){
        this.source=source??new WindowsInputSource();this.outputFactory=outputFactory??(()=>new XboxOutput());this.clock=clock??(()=>Environment.TickCount64);
        thread=new Thread(Run){Name="gamepad-input",IsBackground=true};thread.SetApartmentState(ApartmentState.STA);thread.Start();
    }
    private void Run(){
        try{
            context=new ApplicationContext();bridge=new(outputFactory,clock,Diagnostics);
            window=new InputWindow(this);source.Register(window.Handle);
            watchdog=new(){Interval=100};watchdog.Tick+=(_,_)=>{bridge.Watchdog();Publish();};watchdog.Start();
            fallback=new(){Interval=30000};fallback.Tick+=(_,_)=>Scan();fallback.Start();
            Publish();ready.TrySetResult();Scan();Application.Run(context);
        }catch(Exception e){ready.TrySetException(e);Volatile.Write(ref snapshot,new(new(false,"Input worker stopped: "+e.Message,default,0),[],0));}
        finally{
            lock(posting){accepting=false;}
            watchdog?.Dispose();fallback?.Dispose();
            try{source.Unregister();}catch{}
            bridge?.Dispose();window?.Close();context?.Dispose();
            // Complete queued command tasks with their closing guard rather than leave awaiters hanging.
            closing=true;while(commands.TryDequeue(out var action))action();
            stopped.TrySetResult();
        }
    }
    private void Publish()=>Volatile.Write(ref snapshot,new(bridge.Snapshot(),devices,selected));
    private async Task Command(Action action){
        await ready.Task.ConfigureAwait(false);
        var done=new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        lock(posting){
            if(!accepting)throw new ObjectDisposedException(nameof(InputWorker));
            commands.Enqueue(()=>{if(done.Task.IsCompleted)return;try{if(closing)throw new ObjectDisposedException(nameof(InputWorker));action();Publish();done.TrySetResult();}catch(Exception e){done.TrySetException(e);}});
            if(!PostMessage(window!.Handle,CommandMessage,0,0))done.TrySetException(new System.ComponentModel.Win32Exception());
        }
        await done.Task.ConfigureAwait(false);
    }
    public Task StartAsync()=>Command(()=>bridge.Start());
    public Task StopAsync()=>Command(()=>bridge.Stop());
    public Task ResetAsync(string reason)=>Command(()=>bridge.Reset(reason));
    public Task ResetDiagnosticsAsync()=>Command(Diagnostics.Reset);
    public Task RefreshAsync()=>Command(Scan);
    public Task SelectAsync(nint handle)=>Command(()=>Select(devices.FirstOrDefault(d=>d.Handle==handle)));
    private void Select(InputDevice? device){
        nint next=device?.Handle??0;
        if(next!=selected||device?.Path!=selectedPath){selected=next;selectedPath=device?.Path;bridge.Reset(next==0?"No PC-mode phone selected.":"Phone selected. Release all controls to arm.");}
    }
    private void Scan(){
        if(closing)return;
        if(scanning){rescan=true;return;}
        scanning=true;long generation=deviceGeneration;
        _=Task.Run(()=>source.Devices()).ContinueWith(async task=>{
            try{await Command(()=>{
                scanning=false;
                if(generation==deviceGeneration){
                    if(task.IsCompletedSuccessfully){
                        var found=task.Result.OrderBy(d=>d.Path,StringComparer.Ordinal).ThenBy(d=>d.Handle).ToArray();
                        if(!devices.SequenceEqual(found))devices=found;
                        var match=devices.FirstOrDefault(d=>d.Path==selectedPath);
                        Select(match??(devices.Length==1?devices[0]:null));
                    }else if(task.Exception?.GetBaseException() is not System.ComponentModel.Win32Exception){bridge.Reset("Input scan failed: "+task.Exception?.GetBaseException().GetType().Name);}
                }else rescan=true;
                if(rescan){rescan=false;Scan();}
            }).ConfigureAwait(false);}catch(ObjectDisposedException){}catch{ /* Worker startup/shutdown failure is already in its status. */ }
        },TaskScheduler.Default).Unwrap();
    }
    private void DeviceChanged(nint kind,nint handle){
        deviceGeneration++;
        if(kind==2&&handle==selected){selected=0;bridge.Reset("Phone disconnected; Xbox controls released.");}
        Scan();Publish();
    }
    private void ProcessBatch(InputBatch? batch){
        if(batch==null||selected==0||batch.Device!=selected)return;
        Diagnostics.MessageAge.Add(batch.AgeMs);
        if(batch.AgeMs>100||batch.Reports.Count==0){Diagnostics.Rejected();bridge.Reset("Stale/invalid input discarded. Release all controls to resume.");}
        else foreach(var report in batch.Reports)bridge.Accept(report);
        Publish();
    }
    private void HandleInput(Func<InputBatch?> read){long began=Stopwatch.GetTimestamp();try{ProcessBatch(read());}catch(Exception e){bridge.Reset("Input processing stopped: "+e.GetType().Name);Publish();}finally{Diagnostics.Handler.Add(Stopwatch.GetElapsedTime(began).TotalMilliseconds);}}
    // Test adapter follows the same processing path as WM_INPUT, without forging OS handles.
    internal Task InjectAsync(InputBatch batch)=>Command(()=>HandleInput(()=>batch));
    internal Task DeviceChangedAsync(nint kind,nint handle)=>Command(()=>DeviceChanged(kind,handle));
    internal Task WatchdogAsync()=>Command(()=>bridge.Watchdog());
    public async Task ShutdownAsync(){
        try{await Command(()=>{
            closing=true;lock(posting){accepting=false;}
            context.ExitThread();
        }).ConfigureAwait(false);}catch(ObjectDisposedException){}catch{ /* Always await resource cleanup after startup failure. */ }
        await stopped.Task.ConfigureAwait(false);
        await Task.Run(()=>thread.Join()).ConfigureAwait(false);
    }
    private sealed class InputWindow:NativeWindow
    {
        private readonly InputWorker owner;
        public InputWindow(InputWorker owner){this.owner=owner;CreateHandle(new CreateParams{Caption="PhoneGamepadInput",Parent=new nint(-3)});}
        public void Close()=>DestroyHandle();
        protected override void WndProc(ref Message m){
            try{
                if(m.Msg==CommandMessage){if(owner.commands.TryDequeue(out var action))action();}
                else if(!owner.closing&&m.Msg==RawInput.InputMessage){nint input=m.LParam;owner.HandleInput(()=>owner.source.Read(input));}
                else if(!owner.closing&&m.Msg==RawInput.DeviceChangeMessage)owner.DeviceChanged(m.WParam,m.LParam);
            }finally{base.WndProc(ref m);}
        }
    }
}
