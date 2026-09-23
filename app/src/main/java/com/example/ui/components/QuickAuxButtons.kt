package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun QuickAuxButtons(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    onSelectPress: () -> Unit = {},
    onStartPress: () -> Unit = {},
    onGuidePress: () -> Unit = {}
) {
    val buttonSize = (38 * scale).dp

    Row(
        modifier = modifier.testTag("aux_buttons_row"),
        horizontalArrangement = Arrangement.spacedBy((18 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Select button
        AuxRoundButton(
            icon = Icons.Default.Menu,
            label = "Select",
            testTag = "btn_select",
            sizeDp = buttonSize,
            onPress = onSelectPress
        )

        // Guide / Home Nexus button
        AuxRoundButton(
            icon = Icons.Default.SportsEsports,
            label = "Guide",
            testTag = "btn_guide",
            sizeDp = (buttonSize.value * 1.15f).dp,
            isJewel = true,
            onPress = onGuidePress
        )

        // Start button
        AuxRoundButton(
            icon = Icons.Default.PlayArrow,
            label = "Start",
            testTag = "btn_start",
            sizeDp = buttonSize,
            onPress = onStartPress
        )
    }
}

@Composable
private fun AuxRoundButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    sizeDp: Dp,
    isJewel: Boolean = false,
    onPress: () -> Unit = {}
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .shadow(
                    elevation = if (isJewel) 8.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = if (isJewel) ControlBorderGlow else SurfaceControlRaised
                )
                .clip(CircleShape)
                .background(if (isPressed) ActiveControlFill else SurfaceCard)
                .border(
                    width = if (isJewel) 1.5.dp else 1.dp,
                    color = if (isPressed || isJewel) ControlBorderGlow.copy(alpha = 0.8f) else ControlBorderSubtle.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .testTag(testTag)
                .pointerInput(testTag) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onPress()
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isJewel) ControlBorderGlow else (if (isPressed) ControlBorderGlow else TextSecondary),
                modifier = Modifier.size(sizeDp * 0.52f)
            )
        }

        Text(
            text = label,
            color = TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
