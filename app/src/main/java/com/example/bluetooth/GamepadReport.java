package com.example.bluetooth;

/** Wire format: buttons LE16, hat+padding, X/Y/Z/Rz int8, brake/gas uint8.
 * Report ID belongs in sendReport's id argument, NOT in the nine-byte payload. */
public final class GamepadReport {
    private GamepadReport() {}
    public static final int ID = 1, LENGTH = 9;
    // Linux HID gamepad usages map to Android Generic.kl; C and Z occupy gaps.
    public static final int A=1, B=2, X=1<<3, Y=1<<4, L1=1<<6, R1=1<<7,
            L2=1<<8, R2=1<<9, SELECT=1<<10, START=1<<11, L3=1<<13, R3=1<<14;
    public static final byte[] DESCRIPTOR = hex(
        "05 01 09 05 A1 01 85 01 " +       // Generic Desktop / Gamepad / Application
        "05 09 19 01 29 10 15 00 25 01 75 01 95 10 81 02 " + // 16 buttons
        "05 01 09 39 15 00 25 07 35 00 46 3B 01 65 14 75 04 95 01 81 42 " + // hat, null=8
        "65 00 75 04 95 01 81 03 " +      // constant padding; reset units
        "09 30 09 31 09 32 09 35 15 81 25 7F 35 81 45 7F 75 08 95 04 81 02 " +
        "05 02 09 C5 09 C4 15 00 26 FF 00 35 00 46 FF 00 75 08 95 02 81 02 C0");
    public static byte[] encode(int buttons, int hat, float lx, float ly, float rx, float ry, float lt, float rt) {
        int l=trigger(lt), r=trigger(rt);
        buttons &= 0xFFFF;
        if (l >= 128) buttons |= L2;
        if (r >= 128) buttons |= R2;
        return new byte[]{(byte)buttons,(byte)(buttons>>>8),(byte)(hat>=0 && hat<8?hat:8),
                axis(lx),axis(ly),axis(rx),axis(ry),(byte)l,(byte)r};
    }
    public static byte[] neutral() { return encode(0,8,0,0,0,0,0,0); }
    public static int digital(byte[] b) { return (b[0]&255)|((b[1]&255)<<8)|((b[2]&15)<<16); }
    public static byte axis(float f) { return (byte)Math.round(clamp(f,-1,1)*127); }
    public static int trigger(float f) { return Math.round(clamp(f,0,1)*255); }
    private static float clamp(float f,float lo,float hi) { return Float.isNaN(f)?0:Math.max(lo,Math.min(hi,f)); }
    public static float[] stick(float x,float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) return new float[]{0,0};
        double length=Math.hypot(x,y);
        if (length<=0.04) return new float[]{0,0};
        double scale=(Math.min(1,length)-0.04)/0.96/length;
        return new float[]{(float)(x*scale),(float)(y*scale)};
    }
    public static int hat(float x,float y) {
        if (Math.hypot(x,y)<0.23) return 8;
        return ((int)Math.round(Math.atan2(x,-y)*4/Math.PI)+8)%8;
    }
    private static byte[] hex(String text) {
        String[] parts=text.trim().split(" +"); byte[] b=new byte[parts.length];
        for (int i=0;i<b.length;i++) b[i]=(byte)Integer.parseInt(parts[i],16);
        return b;
    }
}
