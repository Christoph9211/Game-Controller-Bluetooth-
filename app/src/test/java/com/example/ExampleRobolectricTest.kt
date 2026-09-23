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
}
