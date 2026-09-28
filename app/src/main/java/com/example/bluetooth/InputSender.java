package com.example.bluetooth;

/** Clock/scheduler/transport seams keep policy deterministic and independent of Android. */
public final class InputSender {
    public interface Clock { long millis(); long nanos(); }
    public interface Scheduler { void at(Runnable task,long uptime); void cancel(Runnable task); }
    public interface Transport { boolean send(byte[] report); void failed(); }
    private final Clock clock;
    private final Scheduler scheduler;
    private final Transport transport;
    private final ReportQueue queue;
    public final TimingStats touchAge=new TimingStats(),queueWait=new TimingStats(),oldestAge=new TimingStats(),lateness=new TimingStats(),callTime=new TimingStats();
    private boolean running,inFlight;
    private int period=8,failures;
    private long dispatch,generation,lastStart,lastSend,retryAt,scheduledAt=Long.MAX_VALUE,captureStart;
    private long accepted,rejected,safetyResets,lastQueueAge;
    private Runnable pending;
    public InputSender(Clock clock,Scheduler scheduler,Transport transport,ReportQueue queue){this.clock=clock;this.scheduler=scheduler;this.transport=transport;this.queue=queue;captureStart=clock.millis();}
    public synchronized void start(){stop();running=true;lastStart=clock.millis()-period;lastSend=clock.millis();retryAt=0;failures=0;queue.reset(clock.millis());schedule();}
    public synchronized void stop(){running=false;generation++;if(pending!=null)scheduler.cancel(pending);pending=null;scheduledAt=Long.MAX_VALUE;}
    public synchronized void period(int ms){period=ms==4?4:ms==16?16:8;schedule();}
    public synchronized void submit(byte[] data,Long eventTime){if(!running)return;long now=clock.millis();if(eventTime!=null)touchAge.add((now-eventTime)*1000);queue.offer(data,now);schedule();}
    public synchronized void checkPulse(long lastPulse){if(running&&clock.millis()-lastPulse>750&&!java.util.Arrays.equals(queue.current(),GamepadReport.neutral()))release();}
    public synchronized void release(){queue.reset(clock.millis());safetyResets++;schedule();}
    private void schedule(){
        if(!running||inFlight)return;
        long now=clock.millis();queue.expire(now);
        long due=queue.size()>0?(queue.urgent()?now:Math.max(now,lastStart+period)):Math.max(now,lastSend+250);
        due=Math.max(due,retryAt);
        // Coalescing must not move an already eligible callback behind other worker messages.
        // A future deadline can still be required by an explicit interval change or retry.
        if(pending!=null&&(scheduledAt==due||(scheduledAt<=now&&due<=now)))return;
        if(pending!=null)scheduler.cancel(pending);
        long token=generation,deadline=due,ticket=++dispatch;
        pending=()->pump(token,deadline,ticket);scheduledAt=due;scheduler.at(pending,due);
    }
    private void pump(long token,long deadline,long ticket){
        byte[] data;long began;
        synchronized(this){
            if(!running||token!=generation||ticket!=dispatch||deadline!=scheduledAt)return;
            pending=null;scheduledAt=Long.MAX_VALUE;began=clock.millis();
            ReportQueue.Sample sample=queue.poll(began);
            data=sample==null?queue.current():sample.data;
            lastQueueAge=sample==null?0:Math.max(0,began-sample.created);
            if(sample!=null){queueWait.add((began-sample.latestCreated)*1000);oldestAge.add((began-sample.created)*1000);}
            lateness.add((began-deadline)*1000);lastStart=began;inFlight=true;
        }
        long t=clock.nanos();boolean ok=false,fatal=false;
        try{ok=transport.send(data);}catch(RuntimeException e){fatal=true;}finally{
            callTime.add((clock.nanos()-t)/1000);
            boolean disconnect=false;
            synchronized(this){
                inFlight=false;
                if(running&&token==generation){
                    lastSend=clock.millis();
                    if(ok){accepted++;failures=0;retryAt=0;}
                    else{rejected++;failures++;safetyResets++;queue.reset(lastSend);retryAt=lastSend+period;if(fatal||failures>=5){stop();disconnect=true;}}
                }
                schedule();
            }
            if(disconnect)transport.failed();
        }
    }
    public synchronized String status(){return "accepted "+accepted+" | rejected "+rejected+" | queue "+queue.size()+" | last queue age "+lastQueueAge+" ms | resets "+queue.resets();}
    public synchronized void resetDiagnostics(){touchAge.reset();queueWait.reset();oldestAge.reset();lateness.reset();callTime.reset();queue.clearCounters();accepted=rejected=safetyResets=lastQueueAge=0;captureStart=clock.millis();}
    public String diagnostics(){String header;synchronized(this){header="Experimental local timings; not Bluetooth or game latency\nAnalog interval "+period+" ms; capture "+(clock.millis()-captureStart)+" ms; accepted "+accepted+"; rejected "+rejected+"; safety resets "+(safetyResets+queue.resets())+"; coalesced "+queue.coalesced()+"; expired "+queue.expired()+"; overflow "+queue.overflow();}
        return header+"\nTouch event age: "+touchAge.summary()+"\nLatest queue wait: "+queueWait.summary()+"\nOldest pending age: "+oldestAge.summary()+"\nScheduling lateness: "+lateness.summary()+"\nBluetooth API duration: "+callTime.summary();}
}
