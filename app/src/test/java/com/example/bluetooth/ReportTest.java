package com.example.bluetooth;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReportTest {
    @Test public void neutral() { assertArrayEquals(new byte[]{0,0,8,0,0,0,0,0,0},GamepadReport.neutral()); }
    @Test public void shortTapSurvives() {
        ReportQueue q=new ReportQueue();q.offer(GamepadReport.encode(GamepadReport.A,8,0,0,0,0,0,0),0);q.offer(GamepadReport.neutral(),1);
        assertEquals(1,q.poll(2).data[0]);assertEquals(0,q.poll(2).data[0]);
    }
    @Test public void expiryIsNeutral() {
        ReportQueue q=new ReportQueue();q.offer(GamepadReport.encode(GamepadReport.X,8,1,0,0,0,0,0),0);
        assertArrayEquals(GamepadReport.neutral(),q.poll(81).data);
    }
}
