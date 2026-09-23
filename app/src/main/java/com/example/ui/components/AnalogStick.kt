package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StickStylePreset
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextTertiary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 112.dp,
    label: String = "Left Stick",
    stylePreset: StickStylePreset = StickStylePreset.HALO,
    onMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    onStickClick: () -> Unit = {}
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val sizePx = with(density) { sizeDp.toPx() }
    val maxRadiusPx = sizePx / 2.3f
    val puckSizeDp = sizeDp * 0.52f

    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }

    // Smooth return to center when released
    val animatedX by animateFloatAsState(
        targetValue = if (isTouching) dragOffsetPx.x else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 800f),
        label = "stickX"
    )
    val animatedY by animateFloatAsState(
        targetValue = if (isTouching) dragOffsetPx.y else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 800f),
        label = "stickY"
    )

    val currentOffsetX = if (isTouching) dragOffsetPx.x else animatedX
    val currentOffsetY = if (isTouching) dragOffsetPx.y else animatedY

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag(if (label.contains("Left", ignoreCase = true)) "analog_stick_left" else "analog_stick_right")
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.5.dp, if (isTouching) ControlBorderGlow.copy(alpha = 0.6f) else SurfaceControlRaised, CircleShape)
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                // Draw concentric guide circles
                drawCircle(
                    color = SurfaceControl.copy(alpha = 0.4f),
                    radius = size.width * 0.42f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = ControlBorderSubtle.copy(alpha = 0.3f),
                    radius = size.width * 0.25f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // When touching, draw displacement vector line
                if (isTouching && (currentOffsetX != 0f || currentOffsetY != 0f)) {
                    drawLine(
                        color = ControlBorderGlow.copy(alpha = 0.7f),
                        start = center,
                        end = Offset(center.x + currentOffsetX, center.y + currentOffsetY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isTouching = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val center = Offset(sizePx / 2f, sizePx / 2f)
                        val delta = offset - center
                        val distance = sqrt(delta.x * delta.x + delta.y * delta.y)
                        val clampedDist = distance.coerceAtMost(maxRadiusPx)
                        val angle = atan2(delta.y, delta.x)
                        val x = cos(angle) * clampedDist
                        val y = sin(angle) * clampedDist
                        dragOffsetPx = Offset(x, y)
                        onMove(x / maxRadiusPx, y / maxRadiusPx)
                    },
                    onDragEnd = {
                        isTouching = false
                        dragOffsetPx = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        isTouching = false
                        dragOffsetPx = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffsetPx + dragAmount
                        val distance = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        if (distance <= maxRadiusPx) {
                            dragOffsetPx = newOffset
                        } else {
                            val angle = atan2(newOffset.y, newOffset.x)
                            dragOffsetPx = Offset(cos(angle) * maxRadiusPx, sin(angle) * maxRadiusPx)
                        }
                        val normalizedX = (dragOffsetPx.x / maxRadiusPx).coerceIn(-1f, 1f)
                        val normalizedY = (dragOffsetPx.y / maxRadiusPx).coerceIn(-1f, 1f)
                        onMove(normalizedX, normalizedY)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating puck (thumb cap)
        Box(
            modifier = Modifier
                .offset {
                    val xDp = with(density) { currentOffsetX.toDp() }
                    val yDp = with(density) { currentOffsetY.toDp() }
                    IntOffset(
                        (currentOffsetX).roundToInt(),
                        (currentOffsetY).roundToInt()
                    )
                }
                .size(puckSizeDp)
                .shadow(
                    elevation = if (isTouching) 12.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = if (isTouching) ControlBorderGlow else Color.Black,
                    spotColor = if (isTouching) ControlBorderGlow else Color.Black
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SurfaceControlRaised,
                            SurfaceCard,
                            SurfaceDefault
                        )
                    )
                )
                .border(
                    width = if (isTouching) 2.dp else 1.2.dp,
                    color = if (isTouching) ControlBorderGlow else ControlBorderSubtle,
                    shape = CircleShape
                )
                .pointerInput(label) {
                    detectTapGestures(
                        onDoubleTap = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onStickClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Style-specific inner cap
            when (stylePreset) {
                StickStylePreset.HALO -> {
                    // Halo style: glowing cyan center disk
                    Box(
                        modifier = Modifier
                            .size(puckSizeDp * 0.48f)
                            .clip(CircleShape)
                            .background(if (isTouching) PrimaryContainerBlue else SurfaceControl)
                            .border(1.dp, if (isTouching) ControlBorderGlow else SurfaceControlRaised, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(puckSizeDp * 0.22f)
                                .clip(CircleShape)
                                .background(if (isTouching) Color.White else ControlBorderGlow)
                        )
                    }
                }
                StickStylePreset.TARGET -> {
                    // Target style: concentric rings
                    Canvas(modifier = Modifier.size(puckSizeDp * 0.6f)) {
                        drawCircle(
                            color = if (isTouching) ControlBorderGlow else ControlBorderSubtle,
                            radius = size.width / 2f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = if (isTouching) ControlBorderGlow else ActiveControlFill,
                            radius = size.width / 4f,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = if (isTouching) Color.White else SurfaceControlRaised,
                            radius = size.width / 8f
                        )
                    }
                }
                StickStylePreset.MINIMAL -> {
                    // Minimal style: debossed concave dish
                    Box(
                        modifier = Modifier
                            .size(puckSizeDp * 0.44f)
                            .clip(CircleShape)
                            .background(SurfaceContainerLowest)
                            .border(1.dp, SurfaceControlRaised, CircleShape)
                    )
                }
            }
        }
    }
}
