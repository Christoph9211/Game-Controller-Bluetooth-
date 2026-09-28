package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
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

  @Test
  fun `connection starts without fabricated telemetry and denied permission cannot connect`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val viewModel = com.example.viewmodel.GamepadViewModel(context)
    org.junit.Assert.assertFalse(viewModel.connection.value.connected)
    org.junit.Assert.assertNull(viewModel.connection.value.hostAddress)
    assertEquals("Not connected",viewModel.telemetry.value.hostName)
    viewModel.connectToDevice("00:11:22:33:44:55")
    org.junit.Assert.assertFalse(viewModel.connection.value.connected)
    if (android.os.Build.VERSION.SDK_INT >= 31) {
      viewModel.bluetooth.start()
      org.junit.Assert.assertTrue(viewModel.connection.value.status.contains("permission"))
      org.junit.Assert.assertFalse(viewModel.connection.value.active)
    }
  }

  @Test
  fun `focus loss and navigation clear input and block stale touches`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val viewModel = com.example.viewmodel.GamepadViewModel(context)
    viewModel.setForeground(true)
    viewModel.onLeftStickMoved(1f,0f)
    viewModel.onTriggerChanged(true,1f)
    viewModel.onButtonChanged("A",true)
    assertEquals(1f,viewModel.leftStickPos.value.first)
    viewModel.setForeground(false)
    viewModel.onLeftStickMoved(1f,0f)
    assertEquals(0f,viewModel.leftStickPos.value.first)
    assertEquals(0f,viewModel.ltPressure.value)
    org.junit.Assert.assertNull(viewModel.lastPressedButton.value)
    viewModel.setForeground(true)
    viewModel.onButtonChanged("B",true)
    viewModel.openQuickDrawer()
    viewModel.onButtonChanged("A",true)
    org.junit.Assert.assertNull(viewModel.lastPressedButton.value)
    viewModel.navigateTo(com.example.data.model.GamepadScreen.CUSTOMIZE_LAYOUT)
    viewModel.onTriggerChanged(false,1f)
    assertEquals(0f,viewModel.rtPressure.value)
  }

  @Test
  fun `test room database custom layout configurations persistence and management`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val viewModel = com.example.viewmodel.GamepadViewModel(context)

    // 1. Verify dialog open/close state
    org.junit.Assert.assertFalse(viewModel.isSavedLayoutsManagerOpen.value)
    viewModel.openSavedLayoutsManager()
    org.junit.Assert.assertTrue(viewModel.isSavedLayoutsManagerOpen.value)
    viewModel.closeSavedLayoutsManager()
    org.junit.Assert.assertFalse(viewModel.isSavedLayoutsManagerOpen.value)

    // 2. Direct Repository test ensuring Room persistence
    val repository = viewModel.customLayoutRepository
    repository.seedInitialPresetsIfEmpty()
    val initialPresets = repository.getAllLayoutsSync()
    org.junit.Assert.assertTrue("Initial presets should be seeded in Room", initialPresets.isNotEmpty())

    // 3. Save a custom layout to Room
    val customId = "layout_custom_test"
    val testLayout = com.example.data.local.CustomLayoutEntity(
      id = customId,
      name = "Tournament Claw Layout",
      description = "4-finger claw grip optimization",
      isPreset = false,
      elementCount = 7,
      elementsJson = com.example.data.local.CustomLayoutSerializer.serialize(
        com.example.data.model.ControllerLayoutProfile.defaultLayoutElements()
      )
    )
    repository.saveLayout(testLayout)

    val fetched = repository.getLayoutById(customId)
    org.junit.Assert.assertNotNull("Layout should be fetched from Room", fetched)
    assertEquals("Tournament Claw Layout", fetched?.name)
    assertEquals("4-finger claw grip optimization", fetched?.description)

    val allAfterInsert = repository.getAllLayoutsSync()
    org.junit.Assert.assertTrue(allAfterInsert.any { it.id == customId })

    // 4. Update the layout
    val updated = fetched!!.copy(name = "Claw Layout V2")
    repository.updateLayout(updated)
    val fetchedUpdated = repository.getLayoutById(customId)
    assertEquals("Claw Layout V2", fetchedUpdated?.name)

    // 5. Delete the layout from Room
    repository.deleteLayoutById(customId)
    val fetchedDeleted = repository.getLayoutById(customId)
    org.junit.Assert.assertNull("Layout should be deleted from Room", fetchedDeleted)
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context,com.example.data.local.GamepadDatabase::class.java).build()
    try {
      val a=com.example.data.local.LayoutConfigEntity("ABXY",50f,50f,1f,"HALO")
      val b=a.copy(elementIdString="DPAD")
      db.layoutDao().replaceConfigs(listOf(a,b))
      db.layoutDao().replaceConfigs(listOf(a))
      assertEquals(listOf(a),db.layoutDao().getAllConfigsSync())
    } finally { db.close() }
  }
}
