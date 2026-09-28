package com.example.bluetooth;
import org.junit.Test;
import static org.junit.Assert.*;
public class SenderPolicyTest {
    @Test public void freshestAxesDoNotEraseTap(){
        ReportQueue q=new ReportQueue();q.offer(GamepadReport.encode(GamepadReport.A,8,1,0,0,0,0,0),1);q.offer(GamepadReport.neutral(),2);
        ReportQueue.Sample down=q.poll(3);assertEquals(1,down.data[0]);assertEquals(0,down.data[3]);assertEquals(0,q.poll(3).data[0]);
    }
    @Test public void boundedTimingWindow(){TimingStats s=new TimingStats();for(int i=0;i<3000;i++)s.add(i);assertTrue(s.summary().contains("window 2048"));s.reset();assertEquals("count 0",s.summary());}
}
