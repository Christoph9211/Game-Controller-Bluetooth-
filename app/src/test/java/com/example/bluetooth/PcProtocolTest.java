package com.example.bluetooth;
import org.junit.Test;
import static org.junit.Assert.*;
public final class PcProtocolTest {
    @Test public void neutralEnvelopeHasSeparateReportId() {
        assertArrayEquals(new byte[]{0x50,0x47,1,0,0,8,0,0,0,0,0,0},PcProtocol.wrap(GamepadReport.neutral()));
        assertEquals(0x66,PcProtocol.ID);assertEquals(12,PcProtocol.LENGTH);
    }
    @Test public void directionsAndButtonsSurviveWithoutChangingAndroidPacket() {
        byte[] raw=GamepadReport.encode(GamepadReport.A|GamepadReport.X,2,-1,1,1,-1,1,0);
        byte[] before=raw.clone(),out=PcProtocol.wrap(raw);
        assertArrayEquals(before,raw);
        assertArrayEquals(new byte[]{0x50,0x47,1,9,1,2,-127,127,127,-127,-1,0},out);
        out[3]=0;assertArrayEquals(before,raw);
    }
    @Test public void vendorCollectionIsNotAnAdditionalGamepad() {
        byte[] d=PcProtocol.DESCRIPTOR;
        assertEquals(0x06,d[0]&255);assertEquals(0x66,d[1]&255);assertEquals(0xff,d[2]&255);
        assertEquals(0x95,d[16]&255);assertEquals(PcProtocol.LENGTH,d[17]&255);
        assertEquals(0xc0,d[d.length-1]&255);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongLength(){PcProtocol.wrap(new byte[10]);}
}
