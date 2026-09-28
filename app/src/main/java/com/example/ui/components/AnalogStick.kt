package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.StickStylePreset
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControlRaised

/** Pure stick geometry, kept separate from Compose so touch-down neutrality is regression-testable. */
class FloatingStickState(private val movementRadius: Float, private val deadzone: Float) {
    var owner: PointerId? = null
        private set
    var origin: Offset = Offset.Zero
        private set
    var finger: Offset = Offset.Zero
        private set

    fun acquire(
        id: PointerId,
        point: Offset,
        home: Offset,
        activationRadius: Float,
        recenter: Boolean = true
    ): Boolean {
        if (owner != null || (point - home).getDistance() > activationRadius) return false
        owner = id
        origin = if (recenter) point else home
        finger = point
        return true
    }

    fun move(id: PointerId, point: Offset): Offset {
        if (owner != id) return Offset.Zero
        finger = point
        val delta = point - origin
        val distance = delta.getDistance()
        if (distance == 0f || distance / movementRadius <= deadzone) return Offset.Zero
        val scale = movementRadius / distance.coerceAtLeast(movementRadius)
        return Offset(delta.x * scale / movementRadius, delta.y * scale / movementRadius)
    }

    fun release(id: PointerId): Boolean {
        if (owner != id) return false
        reset()
        return true
    }

    fun reset() {
        owner = null
        origin = Offset.Zero
        finger = Offset.Zero
    }
}

@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 112.dp,
    label: String = "Left Stick",
    stylePreset: StickStylePreset = StickStylePreset.HALO,
    recenterOnTouch: Boolean = true,
    extraActivationReachDp: Dp = 24.dp,
    deadzonePct: Int = 4,
    onMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    onStickClick: (Boolean) -> Unit = {}
) {
    val density = LocalDensity.current
    val currentOnMove by rememberUpdatedState(onMove)
    val sizePx = with(density) { sizeDp.toPx() }
    val reachPx = with(density) { if (recenterOnTouch) extraActivationReachDp.toPx() else 0f }
    val canvasDp = sizeDp + (if (recenterOnTouch) extraActivationReachDp * 2 else 0.dp)
    val movementRadius = sizePx / 2.3f
    val baseRadius = sizePx / 2f
    val home = Offset(baseRadius + reachPx, baseRadius + reachPx)
    val activationRadius = if (recenterOnTouch) baseRadius + reachPx else baseRadius
    val state = remember(movementRadius, deadzonePct) {
        FloatingStickState(movementRadius, deadzonePct.coerceIn(0, 25) / 100f)
    }
    var active by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf(Offset.Zero) }
    var activeCenter by remember { mutableStateOf(home) }

    DisposableEffect(state) {
        onDispose {
            state.reset()
            currentOnMove(0f, 0f)
        }
    }

    Box(modifier.size(canvasDp)) {
        Canvas(
            modifier = Modifier
                .size(canvasDp)
                .testTag(if (label.contains("Left", true)) "analog_stick_left" else "analog_stick_right")
                .semantics { contentDescription = label }
                .pointerInput(state, recenterOnTouch, reachPx) {
                    try {
                        awaitPointerEventScope { while (true) {
                            val event = awaitPointerEvent()
                            // Stable pointer IDs are used; array indices are never retained.
                            if (state.owner == null) {
                                val down = event.changes.firstOrNull { it.pressed && !it.previousPressed && !it.isConsumed }
                                if (down != null && state.acquire(down.id, down.position, home, activationRadius, recenterOnTouch)) {
                                    down.consume()
                                    active = true
                                    activeCenter = if (recenterOnTouch) down.position else home
                                    output = if (recenterOnTouch) Offset.Zero else state.move(down.id, down.position)
                                    currentOnMove(output.x, output.y) // neutral is submitted before any drag report
                                }
                            } else {
                                val owner = state.owner
                                val change = event.changes.firstOrNull { it.id == owner }
                                if (change == null || change.changedToUpIgnoreConsumed() || !change.pressed || change.isConsumed) {
                                    if (owner != null) state.release(owner)
                                    active = false
                                    activeCenter = home
                                    output = Offset.Zero
                                    currentOnMove(0f, 0f)
                                } else {
                                    change.consume()
                                    output = state.move(change.id, change.position)
                                    currentOnMove(output.x, output.y)
                                }
                            }
                        } }
                    } finally {
                        if (state.owner != null) {
                            state.reset()
                            active = false
                            activeCenter = home
                            output = Offset.Zero
                            currentOnMove(0f, 0f)
                        }
                    }
                }
        ) {
            val center = if (active) activeCenter else home
            drawCircle(SurfaceContainerLowest, baseRadius, center)
            drawCircle(if (active) ControlBorderGlow.copy(alpha = .6f) else SurfaceControlRaised,
                baseRadius, center, style = Stroke(1.5.dp.toPx()))
            drawCircle(ControlBorderSubtle.copy(alpha = .3f), sizePx * .25f, center,
                style = Stroke(1.dp.toPx()))
            if (active && output != Offset.Zero) {
                drawLine(ControlBorderGlow.copy(alpha = .7f), center,
                    center + output * movementRadius, 2.dp.toPx())
            }
            val puckCenter = center + output * movementRadius
            val puckRadius = sizePx * .26f
            drawCircle(Brush.radialGradient(listOf(SurfaceControlRaised, SurfaceCard), center = puckCenter,
                radius = puckRadius), puckRadius, puckCenter)
            drawCircle(if (active) ControlBorderGlow else ControlBorderSubtle, puckRadius, puckCenter,
                style = Stroke(if (active) 2.dp.toPx() else 1.2.dp.toPx()))
            when (stylePreset) {
                StickStylePreset.HALO -> drawCircle(if (active) Color.White else ControlBorderGlow,
                    puckRadius * .22f, puckCenter)
                StickStylePreset.TARGET -> {
                    drawCircle(ControlBorderGlow, puckRadius * .55f, puckCenter, style = Stroke(1.5.dp.toPx()))
                    drawCircle(if (active) Color.White else SurfaceControlRaised, puckRadius * .15f, puckCenter)
                }
                StickStylePreset.MINIMAL -> drawCircle(SurfaceContainerLowest, puckRadius * .44f, puckCenter)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)
            .offset(x = if (recenterOnTouch) -extraActivationReachDp else 0.dp,
                y = if (recenterOnTouch) -extraActivationReachDp else 0.dp)
            .size(48.dp).clip(CircleShape).background(SurfaceControlRaised)
            .controllerButton(if (label.contains("Left", true)) "L3" else "R3", onStickClick),
            contentAlignment = Alignment.Center) {
            Text(if (label.contains("Left", true)) "L3" else "R3", color = ControlBorderGlow)
        }
    }
}
