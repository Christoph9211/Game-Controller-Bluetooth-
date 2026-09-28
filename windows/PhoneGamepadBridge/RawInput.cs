using System.ComponentModel;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
namespace PhoneGamepad;

internal sealed record InputDevice(nint Handle,string Path)
{
    public override string ToString()=>"Phone bridge " + Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(Path)))[..8];
}
internal sealed record InputBatch(nint Device,uint AgeMs,List<byte[]> Reports);

/// <summary>Register only the dedicated vendor collection. Never intercept keyboards/mice.</summary>
internal static class RawInput
{
    public const int InputMessage=0xff,DeviceChangeMessage=0xfe;
    const uint RidInput=0x10000003,RidiName=0x20000007,RidiInfo=0x2000000b;
    [StructLayout(LayoutKind.Sequential)] struct Registration {public ushort Page,Usage;public uint Flags;public nint Target;}
    [StructLayout(LayoutKind.Sequential)] struct DeviceList {public nint Handle;public uint Type;}
    [StructLayout(LayoutKind.Sequential)] struct Header {public uint Type,Size;public nint Device,WParam;}
    [StructLayout(LayoutKind.Explicit,Size=32)] struct Info
    {
        [FieldOffset(0)] public uint Size;[FieldOffset(4)]public uint Type;
        [FieldOffset(20)]public ushort Page;[FieldOffset(22)]public ushort Usage;
    }
    [DllImport("user32.dll",SetLastError=true)] [return:MarshalAs(UnmanagedType.Bool)]
    static extern bool RegisterRawInputDevices(Registration[] devices,uint count,uint size);
    [DllImport("user32.dll",SetLastError=true)] static extern uint GetRawInputDeviceList(nint devices,ref uint count,uint size);
    [DllImport("user32.dll",EntryPoint="GetRawInputDeviceInfoW",SetLastError=true)] static extern uint DeviceInfo(nint device,uint command,nint data,ref uint size);
    [DllImport("user32.dll",SetLastError=true)] static extern uint GetRawInputData(nint input,uint command,nint data,ref uint size,uint headerSize);
    [DllImport("user32.dll")]static extern int GetMessageTime();
    public static void Register(nint window)
    {
        Registration[] r=[new(){Page=Protocol.UsagePage,Usage=Protocol.Usage,Flags=0x100|0x2000,Target=window}];
        if(!RegisterRawInputDevices(r,1,(uint)Marshal.SizeOf<Registration>()))throw new Win32Exception();
    }
    public static void Unregister()
    {
        Registration[] r=[new(){Page=Protocol.UsagePage,Usage=Protocol.Usage,Flags=1,Target=0}];
        RegisterRawInputDevices(r,1,(uint)Marshal.SizeOf<Registration>());
    }
    public static List<InputDevice> Devices()
    {
        uint count=0,size=(uint)Marshal.SizeOf<DeviceList>();
        if(GetRawInputDeviceList(0,ref count,size)==uint.MaxValue)throw new Win32Exception();
        if(count>4096)throw new InvalidDataException("Unexpected input-device count");
        List<InputDevice> result=[];if(count==0)return result;
        nint mem=Marshal.AllocHGlobal(checked((int)(count*size)));
        try
        {
            uint found=GetRawInputDeviceList(mem,ref count,size);if(found==uint.MaxValue)throw new Win32Exception();
            for(int i=0;i<found;i++)
            {
                var d=Marshal.PtrToStructure<DeviceList>(mem+i*(int)size);if(d.Type!=2)continue;
                nint infoMem=Marshal.AllocHGlobal(32);
                try
                {
                    Marshal.StructureToPtr(new Info{Size=32},infoMem,false);uint infoSize=32;
                    if(DeviceInfo(d.Handle,RidiInfo,infoMem,ref infoSize)==uint.MaxValue)continue;
                    var info=Marshal.PtrToStructure<Info>(infoMem);
                    if(info.Type!=2||info.Page!=Protocol.UsagePage||info.Usage!=Protocol.Usage)continue;
                    uint chars=0;if(DeviceInfo(d.Handle,RidiName,0,ref chars)==uint.MaxValue||chars==0||chars>8192)continue;
                    nint name=Marshal.AllocHGlobal(checked((int)(chars+1)*2));
                    try {if(DeviceInfo(d.Handle,RidiName,name,ref chars)!=uint.MaxValue)result.Add(new(d.Handle,Marshal.PtrToStringUni(name)??""));}
                    finally{Marshal.FreeHGlobal(name);}
                }
                finally{Marshal.FreeHGlobal(infoMem);}
            }
        }
        finally{Marshal.FreeHGlobal(mem);}
        return result;
    }
    public static InputBatch? Read(nint input)
    {
        uint age=unchecked((uint)Environment.TickCount-(uint)GetMessageTime());
        uint size=0,headerSize=(uint)Marshal.SizeOf<Header>();
        if(GetRawInputData(input,RidInput,0,ref size,headerSize)==uint.MaxValue)return null;
        if(size<headerSize+8||size>65536)return null;
        nint mem=Marshal.AllocHGlobal((int)size);
        try
        {
            uint read=GetRawInputData(input,RidInput,mem,ref size,headerSize);
            if(read==uint.MaxValue||read<headerSize+8)return null;
            var header=Marshal.PtrToStructure<Header>(mem);if(header.Type!=2)return null;
            uint width=unchecked((uint)Marshal.ReadInt32(mem,(int)headerSize));
            uint count=unchecked((uint)Marshal.ReadInt32(mem,(int)headerSize+4));
            List<byte[]> reports=[];
            // An invalid/oversized batch from the selected device becomes a safety reset.
            if(width!=Protocol.ReportLength||count==0||count>64||headerSize+8+(ulong)width*count>read)
                return new(header.Device,age,reports);
            for(int i=0;i<count;i++){
                var r=new byte[Protocol.ReportLength];Marshal.Copy(mem+(int)headerSize+8+i*r.Length,r,0,r.Length);reports.Add(r);
            }
            return new(header.Device,age,reports);
        }
        finally{Marshal.FreeHGlobal(mem);}
    }
}
