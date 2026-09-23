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

  @Test
  fun `test canvas layout builder drag drop and customize elements`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val viewModel = com.example.viewmodel.GamepadViewModel(context)

    // Verify initial layout has 7 standard elements
    val initialLayout = viewModel.editingLayout.value
    assertEquals(7, initialLayout.elements.size)

    // Add a new Turbo Button via drag and drop
    viewModel.addElement(
      id = com.example.data.model.ControllerElementId.TURBO_BTN,
      xPercent = 50f,
      yPercent = 65f,
      scale = 1.1f
    )
    val afterAdd = viewModel.editingLayout.value
    assertEquals(8, afterAdd.elements.size)
    org.junit.Assert.assertTrue(afterAdd.elements.containsKey(com.example.data.model.ControllerElementId.TURBO_BTN))

    // Reposition element
    viewModel.updateElementPosition(
      id = com.example.data.model.ControllerElementId.TURBO_BTN,
      xPercent = 48f,
      yPercent = 70f
    )
    val turboConfig = viewModel.editingLayout.value.elements[com.example.data.model.ControllerElementId.TURBO_BTN]
    assertEquals(48f, turboConfig?.xPercent)
    assertEquals(70f, turboConfig?.yPercent)

    // Scale element
    viewModel.updateElementScale(
      id = com.example.data.model.ControllerElementId.TURBO_BTN,
      scale = 1.25f
    )
    assertEquals(1.25f, viewModel.editingLayout.value.elements[com.example.data.model.ControllerElementId.TURBO_BTN]?.scale)

    // Snap grid modes
    viewModel.setSnapGridMode("16DP")
    assertEquals("16DP", viewModel.snapGridMode.value)
    viewModel.setSnapGridMode("OFF")
    assertEquals("OFF", viewModel.snapGridMode.value)
    viewModel.setSnapGridMode("8DP")
    assertEquals("8DP", viewModel.snapGridMode.value)

    // Test mode toggle
    assertEquals(false, viewModel.isTestMode.value)
    viewModel.toggleTestMode()
    assertEquals(true, viewModel.isTestMode.value)
    viewModel.toggleTestMode()
    assertEquals(false, viewModel.isTestMode.value)

    // Apply layout presets
    viewModel.applyLayoutPreset("FPS")
    val fpsElements = viewModel.editingLayout.value.elements
    org.junit.Assert.assertTrue(fpsElements.containsKey(com.example.data.model.ControllerElementId.PADDLE_P1))
    org.junit.Assert.assertTrue(fpsElements.containsKey(com.example.data.model.ControllerElementId.PADDLE_P2))

    // Remove element
    viewModel.removeElement(com.example.data.model.ControllerElementId.PADDLE_P1)
    org.junit.Assert.assertFalse(viewModel.editingLayout.value.elements.containsKey(com.example.data.model.ControllerElementId.PADDLE_P1))

    // Reset layout
    viewModel.resetLayoutToDefault()
    assertEquals(7, viewModel.editingLayout.value.elements.size)
  }
}
