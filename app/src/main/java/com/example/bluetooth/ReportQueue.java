package com.example.bluetooth;
import java.util.ArrayDeque;
import java.util.Arrays;

/** Digital edges are ordered; axes always describe the newest input. */
public final class ReportQueue {
    private static final int CAPACITY=32;
    private static final long MAX_AGE_MS=80;
    public static final class Sample {
        public final byte[] data;
        public final long created, latestCreated;
        Sample(byte[] data,long created,long latestCreated) { this.data=data.clone();this.created=created;this.latestCreated=latestCreated; }
    }
    private final ArrayDeque<Sample> edges=new ArrayDeque<>();
    private byte[] latest=GamepadReport.neutral();
    private boolean analogPending,urgent,neutralPending;
    private long oldestAnalog,latestAt,resets,coalesced,expired,overflow;
    public synchronized void offer(byte[] data,long now) {
        if(data.length!=GamepadReport.LENGTH)throw new IllegalArgumentException("Invalid HID report length");
        if(Arrays.equals(latest,data))return;
        // A queued safety neutral must be delivered before accepting resumed controls.
        if(neutralPending)return;
        boolean digital=GamepadReport.digital(latest)!=GamepadReport.digital(data) || latest[7]!=data[7] || latest[8]!=data[8];
        boolean axes=false;
        for(int i=3;i<=6;i++)axes|=latest[i]!=data[i];
        if(digital) {
            if(edges.size()>=CAPACITY){reset(now);resets++;overflow++;return;}
            edges.addLast(new Sample(data,now,now));
        }
        if(axes){
            if(analogPending)coalesced++;else oldestAnalog=now;
            analogPending=true;
            urgent|=stickZero(latest,3)!=stickZero(data,3)||stickZero(latest,5)!=stickZero(data,5);
        }
        latest=data.clone();latestAt=now;
    }
    private static boolean stickZero(byte[] data,int i){return data[i]==0&&data[i+1]==0;}
    private long oldest(){return Math.min(edges.isEmpty()?Long.MAX_VALUE:edges.peekFirst().created,analogPending?oldestAnalog:Long.MAX_VALUE);}
    public synchronized void expire(long now){if(size()>0&&!neutralPending&&now-oldest()>MAX_AGE_MS){reset(now);resets++;expired++;}}
    public synchronized boolean urgent(){return neutralPending||urgent||!edges.isEmpty();}
    public synchronized Sample poll(long now) {
        expire(now);
        if(neutralPending){neutralPending=false;return new Sample(GamepadReport.neutral(),latestAt,latestAt);}
        if(size()==0)return null;
        long age=oldest();
        Sample edge=edges.pollFirst();
        byte[] data=edge==null?latest.clone():edge.data.clone();
        System.arraycopy(latest,3,data,3,4);
        analogPending=false;urgent=false;
        return new Sample(data,age,latestAt);
    }
    public synchronized void reset(long now){edges.clear();latest=GamepadReport.neutral();analogPending=false;urgent=false;neutralPending=true;latestAt=now;}
    public synchronized byte[] current(){return latest.clone();}
    public synchronized int size(){return neutralPending?1:edges.size()+(analogPending&&edges.isEmpty()?1:0);}
    public synchronized long resets(){return resets;}
    public synchronized long coalesced(){return coalesced;}
    public synchronized long expired(){return expired;}
    public synchronized long overflow(){return overflow;}
    public synchronized void clearCounters(){resets=coalesced=expired=overflow=0;}
}
