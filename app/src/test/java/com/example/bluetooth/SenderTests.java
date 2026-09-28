package com.example.bluetooth;
import java.util.*;
import java.util.concurrent.*;
public final class SenderTests {
    static int checks;
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static byte[] state(int b,float x){return GamepadReport.encode(b,8,x,0,0,0,0,0);}
    static final class Fake implements InputSender.Clock,InputSender.Scheduler,InputSender.Transport {
        long now,due,duration;Runnable task;boolean accept=true,disconnected,throwOnSend;
        final List<byte[]> sent=new ArrayList<>();final List<Long> times=new ArrayList<>();
        final ReportQueue queue=new ReportQueue();final InputSender sender=new InputSender(this,this,this,queue);
        public long millis(){return now;}public long nanos(){return now*1000000;}
        public void at(Runnable r,long at){check(task==null,"at most one pending callback");task=r;due=at;}
        public void cancel(Runnable r){if(task==r)task=null;}
        public boolean send(byte[] b){if(throwOnSend)throw new IllegalStateException("transport failure");sent.add(b);times.add(now);now+=duration;return accept;}
        public void failed(){disconnected=true;}
        void step(){Runnable r=task;check(r!=null,"callback exists");task=null;now=Math.max(now,due);r.run();}
        void start(){sender.start();step();sent.clear();times.clear();}
    }
    public static void main(String[] args)throws Exception {
        for(int period:new int[]{4,8,16}){
            Fake f=new Fake();f.sender.period(period);f.start();f.now=1;f.sender.submit(state(0,.2f),1L);check(f.due==1,"initial stick immediate");f.step();
            f.now=2;f.sender.submit(state(0,.3f),2L);check(f.due==1+period,"absolute analog deadline");
            f.now=3;f.sender.submit(state(0,-.9f),3L);f.step();check(f.sent.get(1)[3]<0,"fresh reversal");check(f.times.get(1)-f.times.get(0)==period,"analog spacing");
            f.sender.submit(state(0,0),f.now);check(f.due==f.now,"return neutral immediate");f.step();
        }
        Fake f=new Fake();f.start();f.now=1;f.sender.submit(state(0,.5f),1L);f.step();f.now=2;f.sender.submit(state(0,.6f),2L);check(f.due==9,"analog queued");
        f.sender.submit(state(1,.6f),2L);check(f.due==2,"button preempts analog");f.sender.submit(state(0,-.7f),2L);f.step();f.step();
        check(f.sent.get(1)[0]==1&&f.sent.get(2)[0]==0,"short tap retained");check(f.sent.get(1)[3]<0,"digital edge uses newest axes");
        f=new Fake();f.start();f.duration=3;f.now=1;f.sender.submit(state(0,.2f),null);f.step();f.sender.submit(state(0,.3f),null);check(f.due==9,"processing time not added to period");
        f=new Fake();f.start();check(f.due==250,"idle heartbeat");f.step();check(f.times.get(0)==250,"heartbeat at 250");
        f=new Fake();f.start();f.accept=false;f.sender.submit(state(1,0),null);for(int i=0;i<5;i++){f.step();if(i<4)check(f.due==f.now+8,"retries paced");}
        check(f.disconnected&&f.task==null,"five failures disconnect");check(Arrays.equals(f.sent.get(1),GamepadReport.neutral()),"retry neutral");
        f=new Fake();f.start();f.sender.submit(state(1,0),null);f.now=81;f.step();check(f.sent.get(0)[0]==0&&f.queue.expired()==1,"expiry fails neutral");
        f=new Fake();f.start();for(int i=0;i<33;i++)f.sender.submit(state(i%2==0?1:0,0),null);f.step();check(f.sent.get(0)[0]==0&&f.queue.overflow()==1,"overflow neutral");
        f=new Fake();f.start();f.sender.submit(state(1,0),null);f.step();f.now=751;f.sender.checkPulse(0);f.step();check(f.sent.get(1)[0]==0,"UI stall releases");
        f=new Fake();f.start();Runnable obsolete=f.task;f.sender.stop();f.sender.start();obsolete.run();check(f.sent.isEmpty(),"old connection callback ignored");f.step();
        f.sender.stop();check(f.task==null,"shutdown cancels callback");
        f=new Fake();f.start();f.now=1;f.sender.submit(state(0,.2f),null);f.step();f.now=2;f.sender.submit(state(0,.4f),null);f.sender.period(4);check(f.due==5,"changing interval reschedules pending analog");
        f=new Fake();f.start();f.sender.submit(GamepadReport.encode(0,2,0,0,0,0,1,0),null);f.sender.submit(GamepadReport.neutral(),null);f.step();f.step();check(f.sent.get(0)[2]==2&&(f.sent.get(0)[7]&255)==255&&f.sent.get(1)[2]==8,"D-pad and trigger edges retained");
        f=new Fake();f.start();f.sender.submit(state(1,0),null);f.step();f.now=750;f.sender.checkPulse(0);check(f.queue.size()==0,"UI pulse exact boundary");f.sender.checkPulse(750);check(f.queue.size()==0,"fresh UI pulse preserves held input");
        ReportQueue q=new ReportQueue();for(int i=0;i<81;i++)q.offer(state(0,(i+1)/100f),i);check(q.poll(81).data[3]==0,"fresh analog cannot conceal oldest pending age");
        TimingStats stats=new TimingStats();for(int i=0;i<3000;i++)stats.add(i);check(stats.summary().contains("window 2048"),"diagnostics bounded");stats.reset();check(stats.summary().equals("count 0"),"diagnostics reset");
        f=new Fake();f.start();f.throwOnSend=true;f.sender.submit(state(1,0),null);f.step();check(f.disconnected&&f.task==null,"transport exception stops without escaping worker");
        overdueCallbacks();concurrency();benchmark();
        System.out.println("PASS sender: "+checks+" checks");
    }
    static void overdueCallbacks(){
        Fake f=new Fake();f.start();
        f.sender.submit(state(0,.2f),null);f.step();
        f.now=1;f.sender.submit(state(0,.3f),null);
        Runnable original=f.task;check(f.due==8,"analog deadline before worker stall");
        f.now=10;f.sender.submit(state(0,.3f),null);
        check(f.task==original&&f.due==8,"duplicate input preserves overdue callback");
        f.now=11;f.sender.submit(state(0,-.7f),null);
        check(f.task==original&&f.due==8,"coalescing preserves overdue callback");
        f.now=12;f.sender.submit(state(1,-.7f),null);
        check(f.task==original&&f.due==8,"digital edge preserves earlier overdue callback");
        f.sender.lateness.reset();f.step();
        check(f.sent.get(1)[0]==1&&f.sent.get(1)[3]<0,"preserved callback sends newest axes and edge");
        check(f.sender.lateness.summary().contains("max 4.000 ms"),"lateness retains original deadline");
        f=new Fake();f.start();f.sender.submit(state(0,.2f),null);f.step();
        f.now=1;f.sender.submit(state(0,.3f),null);f.now=10;f.sender.period(16);
        check(f.due==16,"explicit interval increase still enforces new throttle");
        f.now=11;f.sender.release();check(f.due==11,"safety release advances future callback");f.step();
        check(Arrays.equals(f.sent.get(1),GamepadReport.neutral()),"advanced safety release sends neutral");
    }
    static void concurrency()throws Exception {
        Fake clock=new Fake();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);List<byte[]> sent=new ArrayList<>();
        InputSender sender=new InputSender(clock,clock,new InputSender.Transport(){public boolean send(byte[] b){sent.add(b);if(sent.size()==2){entered.countDown();try{check(release.await(5,TimeUnit.SECONDS),"release send");}catch(InterruptedException e){throw new RuntimeException(e);}}return true;}public void failed(){}},new ReportQueue());
        sender.start();clock.step();sender.submit(state(1,0),null);
        Thread thread=new Thread(clock::step);thread.start();check(entered.await(5,TimeUnit.SECONDS),"send entered");
        sender.submit(state(0,0),null);release.countDown();thread.join(5000);check(!thread.isAlive(),"worker exits");clock.step();check(sent.get(2)[0]==0,"submission during send not lost");sender.stop();
    }
    static void benchmark(){
        // Original loop: one report, simulated 2 ms API cost, then postDelayed(8).
        List<Long> arrivals=new ArrayList<>();for(long t=1;t<17001;t+=17)arrivals.add(t);
        List<Long> oldWait=new ArrayList<>(),newWait=new ArrayList<>();long tick=10;
        for(long arrival:arrivals){while(tick<arrival)tick+=8;oldWait.add(tick-arrival);tick+=10;}
        Fake f=new Fake();f.start();f.duration=2;int button=0;
        for(long arrival:arrivals){while(f.due<arrival)f.step();f.now=arrival;button^=1;f.sender.submit(state(button,0),arrival);f.step();newWait.add(f.times.get(f.times.size()-1)-arrival);}
        Collections.sort(oldWait);Collections.sort(newWait);
        System.out.println("Synthetic identical 1000-edge trace, 2 ms API cost; scheduling wait only (ms):");
        System.out.println("Baseline post-send delay: "+percentiles(oldWait));System.out.println("Input driven: "+percentiles(newWait));
        check(newWait.get(990)<=oldWait.get(990),"synthetic tail does not regress");
    }
    static String percentiles(List<Long> v){return "p50="+v.get(499)+" p95="+v.get(949)+" p99="+v.get(989)+" max="+v.get(999);}
}
