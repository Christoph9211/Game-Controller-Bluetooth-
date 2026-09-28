package com.example.bluetooth;

/** Opt-in Windows bridge transport. NOT a second generic gamepad collection.
 * Payload: 'P','G',version=1, followed by the unchanged nine-byte Android pad state.
 * Android sendReport/replyReport take the ID separately. Windows Raw Input includes it.
 */
public final class PcProtocol {
    private PcProtocol() {}
    public static final int ID=0x66, LENGTH=12;
    // Vendor-defined page FF66, usage 1; one opaque 12-byte input report.
    public static final byte[] DESCRIPTOR=new byte[]{
        0x06,0x66,(byte)0xff,0x09,0x01,(byte)0xa1,0x01,(byte)0x85,0x66,
        0x15,0x00,0x26,(byte)0xff,0x00,0x75,0x08,(byte)0x95,0x0c,0x09,0x02,
        (byte)0x81,0x02,(byte)0xc0};
    public static byte[] wrap(byte[] state) {
        if(state==null || state.length!=GamepadReport.LENGTH)throw new IllegalArgumentException("Expected nine-byte pad state");
        byte[] out=new byte[LENGTH];out[0]=0x50;out[1]=0x47;out[2]=1;
        System.arraycopy(state,0,out,3,state.length);return out;
    }
}
