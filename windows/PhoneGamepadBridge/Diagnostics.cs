using System.Diagnostics;
namespace PhoneGamepad;

internal sealed class TimingSeries
{
    private readonly object sync=new();
    private readonly double[] values=new double[2048];
    private int next,size;
    private long count;
    public void Add(double milliseconds){lock(sync){values[next]=Math.Max(0,milliseconds);next=(next+1)%values.Length;size=Math.Min(size+1,values.Length);count++;}}
    public void Reset(){lock(sync){next=size=0;count=0;}}
    public string Summary(){double[] copy;long total;lock(sync){copy=values[..size];total=count;}Array.Sort(copy);
        if(copy.Length==0)return "count 0";
        double P(double p)=>copy[Math.Max(0,(int)Math.Ceiling(copy.Length*p)-1)];
        return FormattableString.Invariant($"count {total}, window {copy.Length}, p50 {P(.5):F3} / p95 {P(.95):F3} / p99 {P(.99):F3} / max {copy[^1]:F3} ms");}
}
internal sealed class InputDiagnostics
{
    public readonly TimingSeries MessageAge=new(),Decode=new(),Submit=new(),Handler=new();
    private long started=Stopwatch.GetTimestamp(),rejected,resets;
    public void Rejected()=>Interlocked.Increment(ref rejected);
    public void SafetyReset()=>Interlocked.Increment(ref resets);
    public void Reset(){MessageAge.Reset();Decode.Reset();Submit.Reset();Handler.Reset();Interlocked.Exchange(ref rejected,0);Interlocked.Exchange(ref resets,0);Interlocked.Exchange(ref started,Stopwatch.GetTimestamp());}
    public string Summary()=>FormattableString.Invariant($"Experimental Windows local timings; not Bluetooth or game latency\nCapture {Stopwatch.GetElapsedTime(Interlocked.Read(ref started)).TotalSeconds:F1} s; UI 100 ms; scan fallback 30 s; rejected {Interlocked.Read(ref rejected)}; safety resets {Interlocked.Read(ref resets)}\nMessage age (coarse): {MessageAge.Summary()}\nDecode: {Decode.Summary()}\nVirtual submit: {Submit.Summary()}\nInput handler: {Handler.Summary()}");
}
