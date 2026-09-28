namespace PhoneGamepad;

public readonly record struct PadState(ushort Buttons, byte LeftTrigger, byte RightTrigger,
    short LeftX, short LeftY, short RightX, short RightY);

/// <summary>Exact protocol, not heuristic DirectInput button/axis discovery.</summary>
public static class Protocol
{
    public const ushort UsagePage=0xff66, Usage=1;
    public const int ReportLength=13;
    public static bool TryDecode(ReadOnlySpan<byte> r, out PadState state)
    {
        state=default;
        if(r.Length!=ReportLength || r[0]!=0x66 || r[1]!=0x50 || r[2]!=0x47 || r[3]!=1) return false;
        var b=r[4]|r[5]<<8;
        if(r[6]>8 || r[7]==128 || r[8]==128 || r[9]==128 || r[10]==128) return false;
        ushort buttons=0;
        // Android generic HID bit index -> XINPUT_GAMEPAD button mask.
        int[] source={0,1,3,4,6,7,10,11,13,14};
        ushort[] target={0x1000,0x2000,0x4000,0x8000,0x0100,0x0200,0x0020,0x0010,0x0040,0x0080};
        for(var i=0;i<source.Length;i++)if((b&(1<<source[i]))!=0)buttons|=target[i];
        ReadOnlySpan<ushort> hats=[1,9,8,10,2,6,4,5,0];buttons|=hats[r[6]];
        // Touch/Android Y increases downward; XInput Y increases UPWARD. Invert exactly once.
        state=new PadState(buttons,r[11],r[12],Scale((sbyte)r[7]),Scale(-(sbyte)r[8]),
            Scale((sbyte)r[9]),Scale(-(sbyte)r[10]));
        return true;
    }
    private static short Scale(int value)=>(short)Math.Round(value*(value<0?32768d:32767d)/127d,MidpointRounding.AwayFromZero);
}

/// <summary>Never resume held controls after a disconnect, stale input or suspend.</summary>
public sealed class SafetyGate
{
    public const long TimeoutMs=1000;
    public bool Armed {get;private set;}
    public long LastSeen {get;private set;}
    public void Reset(){Armed=false;LastSeen=0;}
    public PadState Accept(PadState state,long now)
    {
        LastSeen=now;
        if(!Armed && state==default)Armed=true;
        return Armed?state:default;
    }
    public bool Expire(long now)
    {
        if(!Armed || now-LastSeen<=TimeoutMs)return false;
        Reset();return true;
    }
}
