package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class FloatingStickStateTest {
    private val home = Offset(100f, 100f)
    @Test fun `touch down is neutral and movement is relative`() {
        val state = FloatingStickState(50f, .05f)
        assertTrue(state.acquire(PointerId(7), Offset(120f, 85f), home, 75f))
        assertEquals(Offset.Zero, state.move(PointerId(7), Offset(120f, 85f)))
        assertEquals(Offset(.4f, 0f), state.move(PointerId(7), Offset(140f, 85f)))
        assertEquals(Offset.Zero, state.move(PointerId(7), Offset(120f, 85f)))
    }
    @Test fun `boundary is inclusive and rejected down cannot acquire by moving`() {
        val state = FloatingStickState(50f, 0f)
        assertFalse(state.acquire(PointerId(1), Offset(176f, 100f), home, 75f))
        assertEquals(Offset.Zero, state.move(PointerId(1), home))
        assertTrue(state.acquire(PointerId(2), Offset(100f, 175f), home, 75f))
    }
    @Test fun `deadzone diagonal and saturation use radial math`() {
        val state = FloatingStickState(50f, .10f)
        state.acquire(PointerId(3), home, home, 50f)
        assertEquals(Offset.Zero, state.move(PointerId(3), Offset(104f, 103f)))
        assertEquals(Offset(.5f, .5f), state.move(PointerId(3), Offset(125f, 125f)))
        val saturated = state.move(PointerId(3), Offset(200f, 200f))
        assertEquals(1f, sqrt(saturated.x * saturated.x + saturated.y * saturated.y), .0001f)
    }
    @Test fun `unrelated pointer cannot steal move or release ownership`() {
        val state = FloatingStickState(50f, 0f)
        assertTrue(state.acquire(PointerId(10), home, home, 50f))
        assertFalse(state.acquire(PointerId(11), home, home, 50f))
        assertFalse(state.release(PointerId(11)))
        assertEquals(Offset(.8f, 0f), state.move(PointerId(10), Offset(140f, 100f)))
        assertTrue(state.release(PointerId(10)))
        assertTrue(state.acquire(PointerId(11), Offset(120f, 100f), home, 50f))
        assertEquals(Offset.Zero, state.move(PointerId(11), Offset(120f, 100f)))
    }
    @Test fun `disabled mode retains fixed home behavior`() {
        val state = FloatingStickState(50f, 0f)
        state.acquire(PointerId(5), Offset(125f, 100f), home, 50f, recenter = false)
        assertEquals(Offset(.5f, 0f), state.move(PointerId(5), Offset(125f, 100f)))
        state.reset()
        assertNull(state.owner)
    }
}
