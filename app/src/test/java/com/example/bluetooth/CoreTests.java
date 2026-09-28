package com.example.bluetooth;
import java.util.*;
public final class CoreTests {
    private static int assertions;
    private static void check(boolean b,String m) { assertions++;if (!b) throw new AssertionError(m); }
    public static void main(String[] args) {
        byte[] n=GamepadReport.neutral();
        check(Arrays.equals(n,new byte[]{0,0,8,0,0,0,0,0,0}),"neutral format");
        check(GamepadReport.X==8 && GamepadReport.Y==16,"Android C/Z gaps");
        check(GamepadReport.L3==8192 && GamepadReport.R3==16384,"thumb clicks");
        for (int i=0;i<16;i++) {
            byte[] b=GamepadReport.encode(1<<i,8,0,0,0,0,0,0);
            check(((b[0]&255)|((b[1]&255)<<8))==(1<<i),"button LE packing");
        }
        byte[] ends=GamepadReport.encode(0,7,-2,2,-1,1,1,1);
        check(ends[3]==-127 && ends[4]==127 && ends[5]==-127 && ends[6]==127,"axis extrema");
        check((ends[7]&255)==255 && (ends[8]&255)==255,"triggers unsigned");
        check((GamepadReport.digital(ends)&(GamepadReport.L2|GamepadReport.R2))==(GamepadReport.L2|GamepadReport.R2),"digital triggers");
        check(GamepadReport.axis(Float.NaN)==0,"NaN neutral");
        check(GamepadReport.hat(0,0)==8,"hat neutral");
        float[][] dirs={{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
        for(int i=0;i<8;i++) check(GamepadReport.hat(dirs[i][0],dirs[i][1])==i,"hat direction");
        check(Arrays.equals(GamepadReport.stick(.02f,.03f),new float[]{0,0}),"radial deadzone");
        check(GamepadReport.stick(.05f,0)[0]>0,"stick responds above 4% deadzone");
        Random r=new Random(66);
        for(int i=0;i<10000;i++) {
            float[] v=GamepadReport.stick(r.nextFloat()*6-3,r.nextFloat()*6-3);
            check(Math.hypot(v[0],v[1])<=1.000001,"radial clamp");
            check(Math.abs(GamepadReport.axis(v[0]))<=127,"wire clamp");
        }
        // Parse HID short items independently; count declared input bits and report ID.
        int bits=0,size=0,count=0,id=0,depth=0;
        byte[] d=GamepadReport.DESCRIPTOR;
        for(int p=0;p<d.length;) {
            int tag=d[p++]&255,len=tag&3;if(len==3)len=4;
            int val=0;for(int k=0;k<len;k++) val|=(d[p++]&255)<<(8*k);
            switch(tag&252) {
                case 0x74:size=val;break;case 0x94:count=val;break;
                case 0x80:bits+=size*count;break;case 0x84:id=val;break;
                case 0xA0:depth++;break;case 0xC0:depth--;break;
                default:break;
            }
            check(depth>=0,"descriptor collection balance");
        }
        check(bits==72 && id==1 && depth==0,"descriptor exactly nine bytes, report 1");
        ReportQueue q=new ReportQueue();
        byte[] down=GamepadReport.encode(GamepadReport.A,8,0,0,0,0,0,0);
        q.offer(down,0);q.offer(n,1);
        check(q.size()==2,"short tap retained");
        check((q.poll(2).data[0]&1)==1 && q.poll(2).data[0]==0,"press then release");
        for(int i=1;i<100;i++) q.offer(GamepadReport.encode(0,8,i/100f,0,0,0,0,0),i);
        check(q.size()==1,"analog coalesced");
        check(q.poll(100).data[3]==0,"stale analog neutralized");
        check(q.resets()==1,"expiry counted");
        q=new ReportQueue();q.offer(down,0);q.offer(GamepadReport.encode(GamepadReport.A,8,.8f,0,0,0,0,0),1);q.offer(n,2);
        check(q.size()==2 && q.poll(3).data[3]==0 && q.poll(3).data[0]==0,"freshest axes on ordered digital edges");
        q=new ReportQueue();
        for(int i=0;i<33;i++) q.offer(i%2==0?down:n,0);
        check(q.size()==1 && Arrays.equals(q.poll(0).data,n) && q.resets()==1,"overflow fails neutral");
        q.offer(down,1);down[0]=0;check(q.poll(2).data[0]==1,"defensive copy");
        q.reset(3);check(Arrays.equals(q.current(),n),"lifecycle neutral");
        boolean rejected=false;try{q.offer(new byte[10],4);}catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"reject report ID accidentally prepended");
        System.out.println("PASS: "+assertions+" assertions (codec, descriptor, radial input, edge queue, expiry and overflow).");
    }
}
