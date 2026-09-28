using PhoneGamepad;
int checks=0;
void Check(bool ok,string what){checks++;if(!ok)throw new Exception(what);}
byte[] Packet()=>[0x66,0x50,0x47,1,0,0,8,0,0,0,0,0,0];
PadState Decode(byte[] r){Check(Protocol.TryDecode(r,out var p),"valid packet");return p;}
Check(Decode(Packet())==default,"neutral");
int[] bits={0,1,3,4,6,7,10,11,13,14};
ushort[] masks={0x1000,0x2000,0x4000,0x8000,0x0100,0x0200,0x0020,0x0010,0x0040,0x0080};
for(int i=0;i<bits.Length;i++){var r=Packet();int b=1<<bits[i];r[4]=(byte)b;r[5]=(byte)(b>>8);Check(Decode(r).Buttons==masks[i],"button "+bits[i]);}
ushort[] hats={1,9,8,10,2,6,4,5,0};
for(byte h=0;h<=8;h++){var r=Packet();r[6]=h;Check(Decode(r).Buttons==hats[h],"hat");}
for(int axis=0;axis<4;axis++)for(int v=-127;v<=127;v++){
    var r=Packet();r[7+axis]=unchecked((byte)(sbyte)v);var p=Decode(r);
    short[] actual={p.LeftX,p.LeftY,p.RightX,p.RightY};
    int signed=axis%2==1?-v:v;
    int expected=(int)Math.Round(signed*(signed<0?32768d:32767d)/127d,MidpointRounding.AwayFromZero);
    Check(actual[axis]==expected,"axis sign/scale");
    Check(actual.Where((_,index)=>index!=axis).All(x=>x==0),"other axes untouched");
}
for(int i=0;i<=255;i++){var r=Packet();r[11]=(byte)i;r[12]=(byte)(255-i);var p=Decode(r);Check(p.LeftTrigger==i&&p.RightTrigger==255-i,"trigger");}
var combo=Decode([0x66,0x50,0x47,1,9,1,2,129,127,127,129,255,0]);
Check(combo==new PadState(0x5008,255,0,-32768,-32768,32767,32767),"Android shared simultaneous fixture");
for(int len=0;len<30;len++)if(len!=13)Check(!Protocol.TryDecode(new byte[len],out _),"length rejected");
for(int i=0;i<4;i++){var r=Packet();r[i]^=0xff;Check(!Protocol.TryDecode(r,out _),"wrong ID/magic/version rejected");}
for(int h=9;h<256;h++){var r=Packet();r[6]=(byte)h;Check(!Protocol.TryDecode(r,out _),"invalid hat/padding");}
for(int i=7;i<=10;i++){var r=Packet();r[i]=128;Check(!Protocol.TryDecode(r,out _),"out of descriptor range");}
var g=new SafetyGate();var held=new PadState(0x1000,0,0,0,0,0,0);
Check(g.Accept(held,0)==default&&!g.Armed,"initial held ignored");
Check(g.Accept(default,1)==default&&g.Armed,"neutral arms");
Check(g.Accept(held,2)==held,"press");Check(g.Accept(default,3)==default,"release edge");
Check(!g.Expire(1003),"exact timeout boundary");Check(g.Expire(1004)&&!g.Armed,"timeout disarms");
Check(g.Accept(held,1005)==default,"stale hold cannot resume");g.Accept(default,1006);Check(g.Accept(held,1007)==held,"neutral enables recovery");
g.Reset();Check(!g.Armed&&g.Accept(held,1008)==default,"disconnect/suspend disarms");
var random=new Random(6601);
for(int i=0;i<20000;i++){
    var r=Packet();for(int a=7;a<=10;a++)r[a]=unchecked((byte)(sbyte)random.Next(-127,128));
    var p=Decode(r);Check(Math.Sign(p.LeftX)==Math.Sign((sbyte)r[7])&&Math.Sign(p.LeftY)==-Math.Sign((sbyte)r[8]),"random left directions");
    Check(Math.Sign(p.RightX)==Math.Sign((sbyte)r[9])&&Math.Sign(p.RightY)==-Math.Sign((sbyte)r[10]),"random right directions");
}
Console.WriteLine($"PASS {checks} checks: wire validation, fixed mappings, all stick values, hats, triggers, lifecycle gate, 20,000 random reports. No Bluetooth or driver hardware claim.");
