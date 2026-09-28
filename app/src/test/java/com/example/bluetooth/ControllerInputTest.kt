package com.example.bluetooth

import org.junit.Assert.*
import org.junit.Test

class ControllerInputTest {
    @Test fun combinedControlsAndNeutral() {
        val input=ControllerInput()
        input.button("A",true); input.button("RB",true)
        input.lx=1f; input.ry=-1f; input.lt=0.5f; input.rt=1f; input.hat=1
        val expected=GamepadReport.encode(GamepadReport.A or GamepadReport.R1,1,1f,0f,0f,-1f,0.5f,1f)
        assertArrayEquals(expected,input.report(4))
        input.button("A",false)
        assertEquals(0,GamepadReport.digital(input.report(4)) and GamepadReport.A)
        assertNotEquals(0,GamepadReport.digital(input.report(4)) and GamepadReport.R1)
        assertNotEquals(0,GamepadReport.digital(input.report(4)) and GamepadReport.L2)
        input.lt=0.49f
        assertEquals(0,GamepadReport.digital(input.report(4)) and GamepadReport.L2)
        input.reset()
        assertArrayEquals(GamepadReport.neutral(),input.report(4))
    }
    @Test fun radialDeadzoneAndInvalidAxes() {
        val input=ControllerInput()
        input.lx=0.04f
        assertArrayEquals(GamepadReport.neutral(),input.report(4))
        input.lx=0.52f
        assertEquals(64,input.report(4)[3].toInt())
        input.lx=Float.NaN; input.ly=1f
        assertEquals(0,input.report(4)[3].toInt()); assertEquals(0,input.report(4)[4].toInt())
        assertEquals(1,GamepadReport.hat(1f,-1f))
        assertEquals(7,GamepadReport.hat(-1f,-1f))
        assertEquals(8,GamepadReport.hat(0f,0f))
    }
}
