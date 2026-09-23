package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bluetooth Gamepad", appName)
  }

  @Test
  fun `test controller layout profile defaults`() {
    val defaultProfile = com.example.data.model.ControllerLayoutProfile()
    assertEquals("Asymmetric Offset (Default)", defaultProfile.name)
    assertEquals(false, defaultProfile.isCustom)
    assertEquals(7, defaultProfile.elements.size)
  }

  @Test
  fun `test discovered gamepads initialization and connect`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val viewModel = com.example.viewmodel.GamepadViewModel(context)

    val discovered = viewModel.discoveredDevices.value
    org.junit.Assert.assertTrue("Discovered devices should not be empty", discovered.isNotEmpty())

    val xboxPad = discovered.find { it.id == "pad_xbox_1" }
    org.junit.Assert.assertNotNull(xboxPad)
    assertEquals(com.example.data.model.DeviceType.GAMEPAD, xboxPad?.type)
    assertEquals("Xbox Wireless Controller", xboxPad?.name)

    // Test selection
    viewModel.selectDevice("pad_xbox_1")
    assertEquals("pad_xbox_1", viewModel.selectedDeviceId.value)

    // Test filter
    viewModel.setDeviceFilter("GAMEPADS")
    assertEquals("GAMEPADS", viewModel.deviceFilter.value)

    // Test connection initiation
    viewModel.initiatePairAndConnect("pad_xbox_1")
    val connecting = viewModel.connectingDeviceId.value
    // During connection handshake, connectingDeviceId is set
    // After handshake finishes, device becomes connected
  }
}
