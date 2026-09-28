package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ControllerGesturesTest {
    @get:Rule val compose=createComposeRule()
    @Test fun simultaneousPressReleaseCancelAndDisposal() {
        val held=mutableSetOf<String>()
        var visible by mutableStateOf(true)
        compose.setContent {
            if (visible) Row(Modifier.testTag("controls")) {
                listOf("A","B").forEach { name ->
                    Box(Modifier.size(80.dp).controllerButton(name) { if (it) held.add(name) else held.remove(name) })
                }
            }
        }
        compose.onNodeWithTag("controls").performTouchInput {
            down(0,Offset(width*0.25f,height/2f)); down(1,Offset(width*0.75f,height/2f))
        }
        compose.runOnIdle { assertEquals(setOf("A","B"),held) }
        compose.onNodeWithTag("controls").performTouchInput { up(0) }
        compose.runOnIdle { assertEquals(setOf("B"),held) }
        compose.onNodeWithTag("controls").performTouchInput { cancel() }
        compose.runOnIdle { assertTrue(held.isEmpty()) }
        compose.onNodeWithTag("controls").performTouchInput { down(0,Offset(width*0.25f,height/2f)) }
        compose.runOnIdle { assertEquals(setOf("A"),held); visible=false }
        compose.runOnIdle { assertTrue(held.isEmpty()) }
    }
    @Test fun floatingStickAndHeldClickResetIndependently() {
        var position = Offset.Zero
        var clicked = false
        var generation by mutableStateOf(0)
        compose.setContent {
            key(generation) {
                AnalogStick(onMove = { x, y -> position = Offset(x, y) }, onStickClick = { clicked = it })
            }
        }
        val stick = compose.onNodeWithTag("analog_stick_left")
        stick.performTouchInput { down(0, Offset(width * .35f, height * .4f)) }
        compose.runOnIdle { assertEquals(Offset.Zero, position) }
        stick.performTouchInput { moveBy(0, Offset(20f, 0f)) }
        compose.runOnIdle { assertTrue(position.x > 0f) }
        compose.onNodeWithContentDescription("L3").performTouchInput { down(1, center) }
        compose.runOnIdle { assertTrue(clicked); assertTrue(position.x > 0f) }
        compose.onNodeWithContentDescription("L3").performTouchInput { up(1) }
        compose.runOnIdle { assertFalse(clicked); assertTrue(position.x > 0f) }
        compose.runOnIdle { generation++ }
        compose.runOnIdle { assertEquals(Offset.Zero, position); assertFalse(clicked) }
        stick.performTouchInput { moveBy(0, Offset(5f, 0f)); up(0) }
        compose.runOnIdle { assertEquals(Offset.Zero, position) }
        stick.performTouchInput { down(center); moveBy(Offset(-20f, 0f)) }
        compose.runOnIdle { assertTrue(position.x < 0f) }
        stick.performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(Offset.Zero, position) }
    }

    @Test fun dpadDiagonalReturnsNeutral() {
        var hat=8
        compose.setContent { DPadView(onHatChange={ hat=it }) }
        compose.onNodeWithTag("dpad_view").performTouchInput { down(Offset(width*0.8f,height*0.2f)) }
        compose.runOnIdle { assertEquals(1,hat) }
        compose.onNodeWithTag("dpad_view").performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(8,hat) }
    }
    @Test fun safetyResetRequiresFreshTouch() {
        var generation by mutableStateOf(0)
        var held=false
        compose.setContent {
            key(generation) {
                Box(Modifier.size(80.dp).testTag("button").controllerButton("A") { held=it })
            }
        }
        compose.onNodeWithTag("button").performTouchInput { down(center) }
        compose.runOnIdle { assertTrue(held); generation++ }
        compose.runOnIdle { assertFalse(held) }
        compose.onNodeWithTag("button").performTouchInput { moveBy(Offset(5f,0f)); up() }
        compose.runOnIdle { assertFalse(held) }
        compose.onNodeWithTag("button").performTouchInput { down(center) }
        compose.runOnIdle { assertTrue(held) }
        compose.onNodeWithTag("button").performTouchInput { up() }
    }

    @Test
    @Config(qualifiers="w900dp-h410dp-land")
    fun defaultLandscapeControlsDoNotOverlap() {
        val vm=com.example.viewmodel.GamepadViewModel(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        compose.setContent { com.example.ui.theme.MyApplicationTheme { com.example.ui.screens.ControllerScreen(vm) } }
        val tags=listOf("group_lt_lb","group_rt_rb","analog_stick_left","analog_stick_right","dpad_view","abxy_cluster","aux_buttons_row")
        val bounds=tags.associateWith { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        for (i in tags.indices) for (j in i+1 until tags.size) {
            assertFalse("${tags[i]} overlaps ${tags[j]}",bounds.getValue(tags[i]).overlaps(bounds.getValue(tags[j])))
        }
    }

    @Test
    @Config(qualifiers="w400dp-h850dp-port")
    fun savedProfileCardsFitNarrowWindows() {
        val vm=com.example.viewmodel.GamepadViewModel(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        kotlinx.coroutines.runBlocking { vm.customLayoutRepository.seedInitialPresetsIfEmpty() }
        compose.setContent { com.example.ui.theme.MyApplicationTheme { SavedLayoutsManagerDialog(vm) {} } }
        compose.waitUntil(5000) { vm.savedLayouts.value.isNotEmpty() }
        val first=vm.savedLayouts.value.first().id
        val card=compose.onNodeWithTag("layout_card_$first").fetchSemanticsNode().boundsInRoot
        val dialog=compose.onNodeWithTag("saved_layouts_dialog").fetchSemanticsNode().boundsInRoot
        assertTrue("Profile should not consume the whole dialog height",card.height < dialog.height/2)
        compose.onNodeWithTag("load_layout_$first").assertIsDisplayed()
    }
}
