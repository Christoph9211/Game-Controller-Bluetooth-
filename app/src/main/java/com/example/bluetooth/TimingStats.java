package com.example.bluetooth;
import java.util.Arrays;
import java.util.Locale;
/** Bounded capture. Sorting is performed by the caller, never by the sender. */
public final class TimingStats {
    private final long[] values=new long[2048];
    private int next,size;
    private long count;
    public synchronized void add(long micros){values[next]=Math.max(0,micros);next=(next+1)%values.length;size=Math.min(size+1,values.length);count++;}
    public synchronized void reset(){next=size=0;count=0;}
    public String summary(){long[] copy;long total;synchronized(this){copy=Arrays.copyOf(values,size);total=count;}Arrays.sort(copy);
        if(copy.length==0)return "count 0";
        return String.format(Locale.US,"count %d, window %d, p50 %.3f / p95 %.3f / p99 %.3f / max %.3f ms",total,copy.length,at(copy,.50)/1000d,at(copy,.95)/1000d,at(copy,.99)/1000d,copy[copy.length-1]/1000d);}
    private static long at(long[] a,double p){return a[Math.max(0,(int)Math.ceil(a.length*p)-1)];}
}
