package com.example.bluetooth

import android.app.Application
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.ServiceConnection
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35])
class BluetoothConnectionTest {
    @Test fun onlyHidCallbacksMarkAHostConnected() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(*BluetoothConnection.permissions)
        val connection=BluetoothConnection(app)
        val host=connection.adapter!!.getRemoteDevice("00:11:22:33:44:55")
        shadowOf(connection.adapter).setBondedDevices(setOf(host))
        connection.refresh()
        assertEquals(host.address,connection.state.value.hosts.single().address)
        connection.connect(host.address)
        assertFalse(connection.state.value.connected)
        val serviceController=Robolectric.buildService(HidService::class.java).create()
        val service=serviceController.get()
        fun field(name: String)=HidService::class.java.getDeclaredField(name).apply { isAccessible=true }
        // Simulate the framework's service binding and HID callbacks, not optimistic UI updates.
        val bindingField=BluetoothConnection::class.java.getDeclaredField("binding").apply { isAccessible=true }
        (bindingField.get(connection) as ServiceConnection).onServiceConnected(
            ComponentName(app,HidService::class.java),service.onBind(null))
        field("active").setBoolean(service,true)
        val callback=field("callback").get(service) as BluetoothHidDevice.Callback
        try {
            callback.onAppStatusChanged(null,true)
            connection.refresh()
            assertTrue(connection.state.value.ready)
            assertFalse(connection.state.value.connected)
            callback.onConnectionStateChanged(host,BluetoothProfile.STATE_CONNECTED)
            connection.refresh()
            assertTrue(connection.state.value.connected)
            assertEquals(host.address,connection.state.value.hostAddress)
            callback.onConnectionStateChanged(host,BluetoothProfile.STATE_DISCONNECTED)
            connection.refresh()
            assertFalse(connection.state.value.connected)
            assertNull(connection.state.value.hostAddress)
        } finally { serviceController.destroy() }
    }
}
