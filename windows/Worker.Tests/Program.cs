using PhoneGamepad;
using System.Collections.Concurrent;

internal static class Tests
{
    private static int checks;
    private static void Check(bool ok,string reason){checks++;if(!ok)throw new Exception(reason);}
    private static async Task Done(Task task)=>await task.WaitAsync(TimeSpan.FromSeconds(10));
    private static async Task Until(Func<bool> condition){using var timeout=new CancellationTokenSource(TimeSpan.FromSeconds(10));while(!condition())await Task.Delay(10,timeout.Token);}
    private static byte[] Packet(byte button=0)=>[0x66,0x50,0x47,1,button,0,8,0,0,0,0,0,0];
    private static InputBatch Batch(byte button=0,uint age=0,nint device=1)=>new(device,age,[Packet(button)]);
    [STAThread] public static int Main(){
        ApplicationConfiguration.Initialize();
        using var form=new Form{Text="Worker regression tests",ShowInTaskbar=false,Opacity=0};
        Exception? failure=null;
        form.Shown+=async(_,_)=>{try{await Task.Run(()=>Run(form));}catch(Exception e){failure=e;}finally{form.Close();}};
        Application.Run(form);
        if(failure!=null){Console.Error.WriteLine(failure);return 1;}
        Console.WriteLine($"PASS Windows worker: {checks} checks; fake controller, real message loops; no driver installed.");return 0;
    }
    private static async Task Run(Form ui){
        var source=new FakeSource();var output=new FakeOutput();long now=0;
        var worker=new InputWorker(source,()=>{output.Owner=Environment.CurrentManagedThreadId;return output;},()=>Interlocked.Read(ref now));
        using var uiBlocked=new ManualResetEventSlim();using var uiRelease=new ManualResetEventSlim();
        try{
            await Done(worker.Ready);await Until(()=>worker.Snapshot.Selected==1);await Done(worker.StartAsync());
            await Done(worker.InjectAsync(Batch(1)));Check(worker.Snapshot.Bridge.State==default,"held state cannot initially arm");
            await Done(worker.InjectAsync(Batch()));await Done(worker.InjectAsync(Batch(1)));Check(worker.Snapshot.Bridge.State.Buttons==0x1000,"neutral then press");
            Check(source.RegisterThread==output.Owner,"registration and output on owner thread");
            source.Block=true;await Done(worker.RefreshAsync());Check(source.Entered.Wait(10000),"enumeration blocked");
            ui.BeginInvoke(()=>{uiBlocked.Set();if(!uiRelease.Wait(10000))throw new TimeoutException("test UI release");});
            Check(uiBlocked.Wait(10000),"real UI message loop blocked");
            long before=worker.Snapshot.Bridge.Reports;
            await Done(worker.InjectAsync(Batch()));await Done(worker.InjectAsync(Batch(1)));
            Check(worker.Snapshot.Bridge.Reports==before+2&&!source.Release.IsSet&&!uiRelease.IsSet,"input progresses while UI and enumeration remain blocked");
            await Done(worker.DeviceChangedAsync(2,1));Check(worker.Snapshot.Selected==0&&worker.Snapshot.Bridge.State==default,"removal neutralizes before scan completes");
            source.Found=[new(2,"replacement")];source.Block=false;source.Release.Set();uiRelease.Set();
            await Until(()=>worker.Snapshot.Selected==2);Check(source.MaxConcurrent==1,"single enumeration at a time");Check(source.EnumerationThread!=output.Owner,"enumeration outside input thread");
            await Done(worker.InjectAsync(Batch(1,device:1)));Check(worker.Snapshot.Bridge.State==default,"removed device ignored");
            await Done(worker.InjectAsync(Batch(1,device:2)));Check(worker.Snapshot.Bridge.State==default,"replacement requires neutral");
            await Done(worker.InjectAsync(Batch(device:2)));await Done(worker.InjectAsync(Batch(1,device:2)));
            await Done(worker.InjectAsync(Batch(1,101,2)));Check(worker.Snapshot.Bridge.State==default,"stale batch resets");
            await Done(worker.InjectAsync(Batch(1,device:2)));Check(worker.Snapshot.Bridge.State==default,"stale rearm requires neutral");
            await Done(worker.InjectAsync(Batch(device:2)));await Done(worker.InjectAsync(Batch(1,device:2)));
            await Done(worker.InjectAsync(new(2,0,[new byte[2]])));Check(worker.Snapshot.Bridge.State==default,"malformed resets");
            await Done(worker.InjectAsync(Batch(device:2)));await Done(worker.InjectAsync(Batch(1,device:2)));
            Interlocked.Exchange(ref now,1001);await Done(worker.WatchdogAsync());Check(worker.Snapshot.Bridge.State==default,"timeout neutral");
            await Done(worker.InjectAsync(Batch(device:2)));await Done(worker.InjectAsync(Batch(1,device:2)));
            await Done(worker.ResetAsync("Suspend"));Check(worker.Snapshot.Bridge.State==default,"suspend neutral");
            source.Found=[new(2,"replacement"),new(3,"other")];await Done(worker.RefreshAsync());await Until(()=>worker.Snapshot.Devices.Length==2);
            Check(worker.Snapshot.Selected==2,"path selection preserved");await Done(worker.SelectAsync(3));Check(worker.Snapshot.Selected==3&&worker.Snapshot.Bridge.State==default,"selection switch resets");
            await Done(worker.InjectAsync(new(3,0,[Packet(),Packet(1),Packet()])));Check(worker.Snapshot.Bridge.State==default,"batch tap completes released");
            source.Found=[new(3,"handle-reused")];await Done(worker.RefreshAsync());await Until(()=>worker.Snapshot.Devices.Length==1&&worker.Snapshot.Devices[0].Path=="handle-reused");
            await Done(worker.InjectAsync(Batch(1,device:3)));Check(worker.Snapshot.Bridge.State==default,"reused handle with different path requires rearm");await Done(worker.InjectAsync(Batch(device:3)));
            output.Block=true;var blockedSend=worker.InjectAsync(Batch(1,device:3));Check(output.Entered.Wait(10000),"output blocked");
            var read=Task.Run(()=>worker.Snapshot);await Done(read);Check(!blockedSend.IsCompleted,"snapshot does not wait for controller lock");output.Block=false;output.Release.Set();await Done(blockedSend);
            await Done(worker.StopAsync());Check(output.Disposed&&!worker.Snapshot.Bridge.Running,"stop completes after disposal");
            Check(output.Threads.All(t=>t==output.Owner),"all submissions and disposal on input thread");
            await Done(worker.ResetDiagnosticsAsync());Check(worker.Diagnostics.Summary().Contains("count 0"),"diagnostics reset");
        }finally{uiRelease.Set();source.Release.Set();output.Release.Set();await Done(worker.ShutdownAsync());}
        Check(source.UnregisterThread==source.RegisterThread,"unregister on input thread");
        await Done(worker.ShutdownAsync());Check(true,"shutdown idempotent");
        var failed=new InputWorker(new FakeSource{FailRegistration=true},()=>new FakeOutput());
        try{await Done(failed.Ready);throw new Exception("registration should fail");}catch(InvalidOperationException){}
        await Done(failed.ShutdownAsync());Check(true,"startup failure cleans up");
    }
    private sealed class FakeSource:IInputSource
    {
        public volatile bool Block;
        public bool FailRegistration;
        public InputDevice[] Found=[new(1,"phone")];
        public readonly ManualResetEventSlim Entered=new(),Release=new();
        public int RegisterThread,UnregisterThread,EnumerationThread,MaxConcurrent;private int concurrent;
        public void Register(nint window){RegisterThread=Environment.CurrentManagedThreadId;if(FailRegistration)throw new InvalidOperationException("fake registration failure");}
        public void Unregister(){UnregisterThread=Environment.CurrentManagedThreadId;}
        public List<InputDevice> Devices(){EnumerationThread=Environment.CurrentManagedThreadId;int active=Interlocked.Increment(ref concurrent);MaxConcurrent=Math.Max(MaxConcurrent,active);var result=Found.ToList();try{if(Block){Entered.Set();if(!Release.Wait(10000))throw new TimeoutException("enumeration release");}return result;}finally{Interlocked.Decrement(ref concurrent);}}
        public InputBatch? Read(nint input)=>null;
    }
    private sealed class FakeOutput:IControllerOutput
    {
        public int Owner;public bool Disposed;public volatile bool Block;
        public readonly ConcurrentBag<int> Threads=new();
        public readonly ManualResetEventSlim Entered=new(),Release=new();
        public void Send(PadState state){Threads.Add(Environment.CurrentManagedThreadId);if(Block){Entered.Set();if(!Release.Wait(10000))throw new TimeoutException("output release");}}
        public void Dispose(){Threads.Add(Environment.CurrentManagedThreadId);Disposed=true;}
    }
}
