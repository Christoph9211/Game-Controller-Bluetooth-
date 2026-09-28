package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TriggerBumperGroup(
    isLeft: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    onBumperPress: (Boolean) -> Unit = {},
    onTriggerChange: (pressure: Float) -> Unit = {}
) {
    val triggerName = if (isLeft) "LT" else "RT"
    val bumperName = if (isLeft) "LB" else "RB"

    var triggerPressure by remember { mutableFloatStateOf(0f) }
    var isBumperPressed by remember { mutableStateOf(false) }

    val animatedPressure by animateFloatAsState(
        targetValue = triggerPressure,
        label = "triggerPressure"
    )

    val widthDp: Dp = (84 * scale).dp
    val triggerHeightDp: Dp = (42 * scale).dp.coerceAtLeast(48.dp)
    val bumperHeightDp: Dp = (36 * scale).dp.coerceAtLeast(48.dp)

    Column(
        modifier = modifier.testTag(if (isLeft) "group_lt_lb" else "group_rt_rb"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End
    ) {
        // Trigger Capsule (Analog Squeeze)
        Box(
            modifier = Modifier
                .width(widthDp)
                .height(triggerHeightDp)
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceControl)
                .border(
                    width = if (animatedPressure > 0.05f) 1.5.dp else 1.dp,
                    color = if (animatedPressure > 0.05f) ControlBorderGlow else ControlBorderSubtle.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                )
                .testTag(if (isLeft) "trigger_lt" else "trigger_rt")
                .controllerGesture(triggerName) { position, size ->
                    triggerPressure=position?.let { (it.x / size.width).coerceIn(0f,1f) } ?: 0f
                    onTriggerChange(triggerPressure)
                }
            ,
            contentAlignment = Alignment.Center
        ) {
            // Analog fill level bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPressure)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ActiveControlFill.copy(alpha = 0.6f),
                                    PrimaryContainerBlue.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = triggerName,
                    color = if (animatedPressure > 0.05f) TextPrimary else TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(animatedPressure * 255).toInt()}",
                    color = if (animatedPressure > 0.05f) ControlBorderGlow else TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bumper Capsule (Digital Tap)
        Box(
            modifier = Modifier
                .width(widthDp)
                .height(bumperHeightDp)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(if (isBumperPressed) ActiveControlFill else SurfaceControl)
                .border(
                    width = 1.dp,
                    color = if (isBumperPressed) ControlBorderGlow else ControlBorderSubtle.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp)
                )
                .testTag(if (isLeft) "bumper_lb" else "bumper_rb")
                .controllerButton(bumperName) { down ->
                    if (isBumperPressed != down) { isBumperPressed=down; onBumperPress(down) }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bumperName,
                color = if (isBumperPressed) ControlBorderGlow else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
