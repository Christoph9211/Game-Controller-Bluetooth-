package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.TextSecondary

@Composable
fun DPadView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 116.dp,
    onHatChange: (Int) -> Unit = {}
) {
    val armWidthDp = sizeDp * 0.36f
    val cornerRadius = 14.dp

    var pressedHat by remember { mutableStateOf(8) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("dpad_view")
            .controllerGesture("D-pad") { position, size ->
                val hat=position?.let { com.example.bluetooth.GamepadReport.hat(
                    (it.x-size.width/2f)/(size.width/2f), (it.y-size.height/2f)/(size.height/2f)) } ?: 8
                if (hat != pressedHat) { pressedHat=hat; onHatChange(hat) }
            },
        contentAlignment = Alignment.Center
    ) {
        // Vertical Arm
        Box(
            modifier = Modifier
                .size(width = armWidthDp, height = sizeDp)
                .shadow(6.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(SurfaceControl)
                .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
        ) {
            // Up button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .align(Alignment.TopCenter)
                    .background(if (pressedHat in listOf(7,0,1)) ActiveControlFill else Color.Transparent)
                    .testTag("dpad_up")
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropUp,
                    contentDescription = "D-Pad Up",
                    tint = if (pressedHat in listOf(7,0,1)) ControlBorderGlow else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Down button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .align(Alignment.BottomCenter)
                    .background(if (pressedHat in listOf(3,4,5)) ActiveControlFill else Color.Transparent)
                    .testTag("dpad_down")
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "D-Pad Down",
                    tint = if (pressedHat in listOf(3,4,5)) ControlBorderGlow else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Horizontal Arm
        Box(
            modifier = Modifier
                .size(width = sizeDp, height = armWidthDp)
                .shadow(6.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(SurfaceControl)
                .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
        ) {
            // Left button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterStart)
                    .background(if (pressedHat in listOf(5,6,7)) ActiveControlFill else Color.Transparent)
                    .testTag("dpad_left")
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = "D-Pad Left",
                    tint = if (pressedHat in listOf(5,6,7)) ControlBorderGlow else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Right button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterEnd)
                    .background(if (pressedHat in listOf(1,2,3)) ActiveControlFill else Color.Transparent)
                    .testTag("dpad_right")
                    ,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowRight,
                    contentDescription = "D-Pad Right",
                    tint = if (pressedHat in listOf(1,2,3)) ControlBorderGlow else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Central pivot deboss
        Box(
            modifier = Modifier
                .size(armWidthDp * 0.72f)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, SurfaceControlRaised, CircleShape)
        )
    }
}
