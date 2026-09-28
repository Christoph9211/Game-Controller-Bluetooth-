package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BondedHost(val address: String, val name: String)
data class ConnectionState(
    val active: Boolean = false, val ready: Boolean = false, val connected: Boolean = false,
    val stopping: Boolean = false, val sessionOpen: Boolean = false, val hostAddress: String? = null, val connectingAddress: String? = null,
    val status: String = "Stopped", val hosts: List<BondedHost> = emptyList(),
    val pcMode: Boolean = false, val diagnostics: String = "No sender session."
)

/** Application binding survives Activity recreation; the UI alone supplies watchdog pulses. */
@SuppressLint("MissingPermission")
class BluetoothConnection(private val context: Context) {
    private val prefs = context.getSharedPreferences("controller", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(ConnectionState(pcMode=prefs.getBoolean("pc_bridge",false)))
    val state = mutableState.asStateFlow()
    var service: HidService? = null
        private set
    private var bound = false
    private var stopping = false
    private val intent get() = Intent(context, HidService::class.java)
    val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val binding = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as HidService.LocalBinder).service()
            service?.release()
            if (stopping) finishStop() else refresh()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            completeStop()
            error("Bluetooth service disconnected. Stop/Start to retry.")
        }
        override fun onBindingDied(name: ComponentName) { completeStop() }
        override fun onNullBinding(name: ComponentName) { completeStop() }
    }
    fun hasPermissions(): Boolean = Build.VERSION.SDK_INT < 31 || permissions.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
    fun error(message: String) {
        mutableState.value = mutableState.value.copy(active=false, ready=false, connected=false,
            hostAddress=null, connectingAddress=null, status=message)
    }
    fun start() {
        if (stopping) return
        if (!hasPermissions()) { error("Nearby devices permission required"); return }
        try {
            if (adapter?.isEnabled != true) { error("Bluetooth unavailable or off"); return }
            context.startForegroundService(intent)
            if (!bound) bound = context.bindService(intent,binding,Context.BIND_AUTO_CREATE)
            if (!bound) { context.stopService(intent); error("Unable to bind controller service"); return }
            mutableState.value = mutableState.value.copy(active=true,sessionOpen=true,status="Starting Bluetooth…")
        } catch (e: RuntimeException) { error("Cannot start Bluetooth: ${e.javaClass.simpleName}") }
    }
    fun stop() {
        if (stopping) return
        stopping=true
        mutableState.value=mutableState.value.copy(stopping=true,ready=false,connected=false,status="Stopping Bluetooth…")
        if (service != null) finishStop() else {
            // A pending bind must complete before changing the descriptor preference.
            if (!bound) completeStop()
        }
    }
    private fun finishStop() {
        service?.stopSession { completeStop() }
    }
    private fun completeStop() {
        if (bound) context.unbindService(binding)
        bound=false; service=null
        context.stopService(intent)
        stopping=false
        mutableState.value=ConnectionState(pcMode=prefs.getBoolean("pc_bridge",false))
        refresh()
    }
    fun setMode(pc: Boolean) {
        if (bound || stopping || state.value.active) return
        prefs.edit().putBoolean("pc_bridge",pc).apply()
        mutableState.value=state.value.copy(pcMode=pc)
    }
    fun connect(address: String) {
        if (!hasPermissions()) { error("Nearby devices permission required"); return }
        try {
            val host=adapter?.bondedDevices?.firstOrNull { it.address==address } ?: return
            service?.connect(host)
        } catch (e: RuntimeException) { error("Cannot connect: ${e.javaClass.simpleName}") }
    }
    fun refresh() {
        if (stopping) return
        if (!hasPermissions()) {
            service?.release()
            error("Nearby devices permission required")
            mutableState.value=state.value.copy(hosts=emptyList())
            return
        }
        try {
            val hosts=adapter?.bondedDevices.orEmpty().map { BondedHost(it.address,it.name ?: "Paired device") }.sortedBy { it.name }
            val s=service
            mutableState.value=state.value.copy(hosts=hosts,
                active=s?.active() ?: state.value.active, ready=s?.ready() ?: false,
                connected=s?.connected() ?: false, hostAddress=s?.hostAddress(), connectingAddress=s?.connectingAddress(),
                status=s?.status() ?: state.value.status, diagnostics=s?.diagnostics() ?: "No sender session.")
        } catch (e: RuntimeException) { error("Bluetooth unavailable: ${e.javaClass.simpleName}") }
    }
    companion object {
        // These constant permission names are requested only on Android 12+.
        @SuppressLint("InlinedApi")
        val permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE,Manifest.permission.BLUETOOTH_SCAN)
    }
}
