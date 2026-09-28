package com.example.bluetooth;

import android.Manifest;
import com.example.MainActivity;
import com.example.R;
import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;

/** All Bluetooth operations and callbacks are serialized off the UI thread. */
@SuppressLint("MissingPermission")
public final class HidService extends Service {
    private static final String CHANNEL="gamepad", STOP="com.example.bluetooth.STOP";
    private static final int NOTIFICATION=1;
    private final LocalBinder binder=new LocalBinder();
    private final ReportQueue queue=new ReportQueue();
    private HandlerThread thread;
    private Handler worker;
    private BluetoothAdapter adapter;
    private BluetoothHidDevice hid;
    private volatile BluetoothDevice host,connecting;
    public String hostAddress() { BluetoothDevice d=host;return connected&&d!=null?d.getAddress():null; }
    public String connectingAddress() { BluetoothDevice d=connecting;return d==null?null:d.getAddress(); }
    private volatile boolean active,ready,connected;
    // Frozen for this service instance. Mode changes are allowed only after stopping it.
    private boolean pcMode;
    private int reportId(){return pcMode?PcProtocol.ID:GamepadReport.ID;}
    private int reportLength(){return pcMode?PcProtocol.LENGTH:GamepadReport.LENGTH;}
    private byte[] wire(byte[] state){return pcMode?PcProtocol.wrap(state):state;}
    private volatile String status="Stopped";
    private volatile long lastUiPulse;
    private volatile int periodMs=8;
    private boolean proxyPending;
    private InputSender inputSender;
    public final class LocalBinder extends Binder { public HidService service() { return HidService.this; } }
    @Override public IBinder onBind(Intent intent) { return binder; }
    public String status() { return status; }
    public boolean active() { return active; }
    public boolean ready() { return ready; }
    public boolean connected() { return connected; }
    public void pulse() { lastUiPulse=SystemClock.uptimeMillis(); }
    public void setPeriod(int millis) { periodMs=millis==4?4:millis==16?16:8;inputSender.period(periodMs); }
    public String metrics() { return (pcMode?"PC BRIDGE | ":"GENERIC HID | ")+periodMs+" ms analog interval | "+inputSender.status(); }
    public String diagnostics(){return (pcMode?"PC BRIDGE\n":"GENERIC HID\n")+inputSender.diagnostics();}
    public void resetDiagnostics(){inputSender.resetDiagnostics();}
    public void submit(byte[] report,Long eventTime) { if(active&&connected)inputSender.submit(report,eventTime); }
    public void release() { if(inputSender!=null)inputSender.release(); }
    @Override public void onCreate() {
        super.onCreate();
        pcMode=getSharedPreferences("controller",MODE_PRIVATE).getBoolean("pc_bridge",false);
        thread=new HandlerThread("hid-sender",android.os.Process.THREAD_PRIORITY_DISPLAY);thread.start();worker=new Handler(thread.getLooper());
        inputSender=new InputSender(new InputSender.Clock(){public long millis(){return SystemClock.uptimeMillis();}public long nanos(){return System.nanoTime();}},
            new InputSender.Scheduler(){public void at(Runnable task,long uptime){worker.postAtTime(task,uptime);}public void cancel(Runnable task){worker.removeCallbacks(task);}},
            new InputSender.Transport(){
                public boolean send(byte[] state){
                    if(!active||!connected||hid==null||host==null)return false;
                    try{return hid.sendReport(host,reportId(),wire(state));}
                    catch(RuntimeException e){problem(e);throw e;}
                }
                public void failed(){
                    try{if(hid!=null&&host!=null)hid.disconnect(host);}catch(RuntimeException e){problem(e);}
                    host=null;connecting=null;connected=false;status="Input transport failed. Reconnect.";
                }
            },queue);

        BluetoothManager manager=getSystemService(BluetoothManager.class);adapter=manager==null?null:manager.getAdapter();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Controller connection",NotificationManager.IMPORTANCE_LOW));
        IntentFilter f=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);f.addAction(Intent.ACTION_SCREEN_OFF);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else registerReceiver(receiver,f);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && STOP.equals(intent.getAction())) { stopSession();return START_NOT_STICKY; }
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            status="Nearby devices permission required";stopSelf();return START_NOT_STICKY;
        }
        try { startForeground(NOTIFICATION,notification()); }
        catch(RuntimeException e) { status="Cannot start controller: "+e.getClass().getSimpleName();stopSelf();return START_NOT_STICKY; }
        if(!active) { active=true;pulse();worker.post(this::openProfile);worker.post(safetyWatchdog); }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,HidService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_gamepad).setContentTitle("Bluetooth Gamepad active")
                .setContentText("Return to controller, or Stop to release Bluetooth HID.").setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build();
    }
    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {
            if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())) { release();return; }
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(i.getAction())) {
                int state=i.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                worker.post(()->{
                    if(state==BluetoothAdapter.STATE_OFF) { closeProfile();status="Bluetooth off. Turn it on, then reconnect."; }
                    else if(state==BluetoothAdapter.STATE_ON && active) openProfile();
                });
            }
        }
    };
    private void openProfile() {
        if(!active || hid!=null || proxyPending) return;
        try {
            if(adapter==null || !adapter.isEnabled()) { status="Bluetooth is unavailable or off";return; }
            status="Opening Bluetooth HID profile...";proxyPending=true;
            if(!adapter.getProfileProxy(this,profileListener,BluetoothProfile.HID_DEVICE)) { proxyPending=false;status="HID Device profile unavailable on this phone"; }
            worker.postDelayed(()->{ if(active && !ready && !connected) status="HID not ready. Stop/retry; close other HID apps. This phone may not support HID Device."; },10000);
        } catch(RuntimeException e) { proxyPending=false;problem(e); }
    }
    private final BluetoothProfile.ServiceListener profileListener=new BluetoothProfile.ServiceListener() {
        @Override public void onServiceConnected(int profile,BluetoothProfile proxy) {
            worker.post(()->{
                proxyPending=false;
                if(!active || hid!=null) { adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,proxy);return; }
                hid=(BluetoothHidDevice)proxy;
                try {
                    status="Registering "+(pcMode?"PC bridge data channel...":"gamepad...");
                    BluetoothHidDeviceAppSdpSettings sdp=new BluetoothHidDeviceAppSdpSettings(
                        pcMode?"Phone Gamepad PC Bridge":"Android Bluetooth Gamepad",
                        pcMode?"PhoneGamepadBridge v1 data":"Two-stick touch gamepad","Christopher",
                        pcMode?(byte)0:BluetoothHidDevice.SUBCLASS2_GAMEPAD,pcMode?PcProtocol.DESCRIPTOR:GamepadReport.DESCRIPTOR);
                    if(!hid.registerApp(sdp,null,null,r->worker.post(r),callback)) status="HID registration rejected. Stop other HID apps, then Stop/Start.";
                } catch(RuntimeException e) { problem(e); }
            });
        }
        @Override public void onServiceDisconnected(int profile) {
            worker.post(()->{ hid=null;proxyPending=false;ready=false;connected=false;host=null;connecting=null;inputSender.stop();release();status="Bluetooth HID service disconnected. Stop/Start to retry."; });
        }
    };
    private final BluetoothHidDevice.Callback callback=new BluetoothHidDevice.Callback() {
        @Override public void onAppStatusChanged(BluetoothDevice plugged,boolean registered) {
            if(!active) return;
            ready=registered;
            if(!registered) { connected=false;host=null;connecting=null;inputSender.stop();release();status="HID unregistered. Return to controller and Stop/Start."; }
            else {
                status=pcMode?"PC bridge ready. Pair from Windows, Connect, then Start bridge on the PC.":"Ready. Make discoverable, pair from the game phone, then Connect.";
                try { if(plugged!=null && hid!=null && hid.getConnectionState(plugged)==BluetoothProfile.STATE_CONNECTED) onConnectionStateChanged(plugged,BluetoothProfile.STATE_CONNECTED); }
                catch(RuntimeException e) { problem(e); }
            }
        }
        @Override public void onConnectionStateChanged(BluetoothDevice device,int state) {
            if(!active) return;
            if(state==BluetoothProfile.STATE_CONNECTED) {
                if(host!=null && !host.equals(device)) { try { hid.disconnect(device); } catch(RuntimeException e) { problem(e); } return; }
                host=device;connecting=null;connected=true;inputSender.start();
                status=(pcMode?"PC bridge connected to ":"Connected to ")+name(device);
            } else if(state==BluetoothProfile.STATE_CONNECTING) {
                connecting=device;status="Connecting to "+name(device)+"...";
            } else if(state==BluetoothProfile.STATE_DISCONNECTED) {
                if(host==null || host.equals(device)) { host=null;connecting=null;connected=false;inputSender.stop();release();status="Disconnected. Select the paired host to reconnect."; }
            }
        }
        @Override public void onGetReport(BluetoothDevice device,byte type,byte id,int bufferSize) {
            if(hid==null || !active) return;
            try {
                if(type!=BluetoothHidDevice.REPORT_TYPE_INPUT || (id&255)!=reportId()) { hid.reportError(device,BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);return; }
                if(bufferSize!=0 && bufferSize<reportLength()) { hid.reportError(device,BluetoothHidDevice.ERROR_RSP_INVALID_PARAM);return; }
                hid.replyReport(device,type,id,wire(queue.current()));
            } catch(RuntimeException e) { problem(e); }
        }
        @Override public void onSetReport(BluetoothDevice device,byte type,byte id,byte[] data) {
            if(hid!=null && active) try { hid.reportError(device,BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ); } catch(RuntimeException e) { problem(e); }
        }
        @Override public void onSetProtocol(BluetoothDevice device,byte protocol) {
            if(protocol==BluetoothHidDevice.PROTOCOL_BOOT_MODE) {
                release();status="Host requested unsupported boot protocol; reconnect.";
                try { if(hid!=null) hid.disconnect(device); } catch(RuntimeException e) { problem(e); }
            }
        }
        @Override public void onVirtualCableUnplug(BluetoothDevice device) {
            connected=false;host=null;connecting=null;inputSender.stop();release();status="Host unplugged controller. Pair/connect again.";
        }
    };
    public void connect(BluetoothDevice device) {
        worker.post(()->{
            if(!active || !ready || hid==null) { status="Start controller and wait for Ready first";return; }
            if(host!=null || connecting!=null) { status="Already connected or connecting; disconnect first";return; }
            try {
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED) { status="Pair this host in Bluetooth settings first";return; }
                adapter.cancelDiscovery();release();connecting=device;
                if(!hid.connect(device)) { connecting=null;status="Connection request rejected. Retry from the host's Bluetooth settings.";return; }
                status="Connecting to "+name(device)+"...";
                worker.postDelayed(()->{
                    if(device.equals(connecting) && !connected && hid!=null) {
                        try { hid.disconnect(device); } catch(RuntimeException e) { problem(e); }
                        connecting=null;status="Connection timed out. Enable Input device if shown, or forget/re-pair these two devices.";
                    }
                },12000);
            } catch(RuntimeException e) { connecting=null;problem(e); }
        });
    }
    public void disconnect() {
        release();worker.post(()->{
            try {
                BluetoothDevice target=host!=null?host:connecting;
                if(hid!=null && target!=null) { hid.sendReport(target,reportId(),wire(GamepadReport.neutral()));hid.disconnect(target); }
                host=null;connecting=null;connected=false;inputSender.stop();status="Disconnected";
            } catch(RuntimeException e) { problem(e); }
        });
    }
    private final Runnable safetyWatchdog=new Runnable(){
        @Override public void run(){
            if(!active)return;
            if(connected)inputSender.checkPulse(lastUiPulse);
            worker.postDelayed(this,connected?8:250);
        }
    };
    private String name(BluetoothDevice d) {
        try { String n=d.getName();return n==null?"paired device":n; } catch(SecurityException e) { return "paired device"; }
    }
    private void problem(RuntimeException e) { status="Bluetooth error: "+e.getClass().getSimpleName()+". Check Nearby devices permission, then Stop/Start."; }
    public void stopSession() {
        stopSession(null);
    }
    public void stopSession(Runnable finished) {
        active=false;inputSender.stop();release();status="Stopped";
        worker.post(()->{ worker.removeCallbacks(safetyWatchdog);closeProfile();
            if(finished!=null)new Handler(Looper.getMainLooper()).post(finished); });
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    private void closeProfile() {
        inputSender.stop();
        ready=false;connected=false;proxyPending=false;
        BluetoothHidDevice old=hid;hid=null;
        if(old!=null) {
            try { if(host!=null) { old.sendReport(host,reportId(),wire(GamepadReport.neutral()));old.disconnect(host); } old.unregisterApp(); }
            catch(RuntimeException ignored) { }
            try { adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,old); } catch(RuntimeException ignored) { }
        }
        host=null;connecting=null;release();
    }
    @Override public void onTaskRemoved(Intent rootIntent) { stopSession(); }
    @Override public void onDestroy() {
        active=false;unregisterReceiver(receiver);
        worker.post(()->{ worker.removeCallbacksAndMessages(null);closeProfile();thread.quitSafely(); });
        super.onDestroy();
    }
}
