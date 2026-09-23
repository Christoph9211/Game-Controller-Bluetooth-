package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.XboxBlueX
import com.example.ui.theme.XboxGreenA
import com.example.ui.theme.XboxRedB
import com.example.ui.theme.XboxYellowY

@Composable
fun ABXYCluster(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 120.dp,
    onButtonPress: (String) -> Unit = {}
) {
    val buttonSize = sizeDp * 0.35f

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("abxy_cluster"),
        contentAlignment = Alignment.Center
    ) {
        // Y button (Top - Amber / Yellow)
        ActionPodButton(
            letter = "Y",
            accentColor = XboxYellowY,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("abxy_button_y"),
            sizeDp = buttonSize,
            onPress = { onButtonPress("Y") }
        )

        // X button (Left - Electric Blue)
        ActionPodButton(
            letter = "X",
            accentColor = XboxBlueX,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag("abxy_button_x"),
            sizeDp = buttonSize,
            onPress = { onButtonPress("X") }
        )

        // B button (Right - Scarlet Red)
        ActionPodButton(
            letter = "B",
            accentColor = XboxRedB,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("abxy_button_b"),
            sizeDp = buttonSize,
            onPress = { onButtonPress("B") }
        )

        // A button (Bottom - Emerald Green)
        ActionPodButton(
            letter = "A",
            accentColor = XboxGreenA,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("abxy_button_a"),
            sizeDp = buttonSize,
            onPress = { onButtonPress("A") }
        )
    }
}

@Composable
private fun ActionPodButton(
    letter: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 42.dp,
    onPress: () -> Unit = {}
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) 0.92f else 1.0f

    Box(
        modifier = modifier
            .size(sizeDp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = CircleShape,
                ambientColor = accentColor.copy(alpha = 0.6f),
                spotColor = accentColor.copy(alpha = 0.8f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(SurfaceControl, SurfaceControlRaised)
                    } else {
                        listOf(SurfaceControlRaised, SurfaceCard)
                    }
                )
            )
            .border(
                width = 1.2.dp,
                color = if (isPressed) accentColor else ControlBorderSubtle.copy(alpha = 0.6f),
                shape = CircleShape
            )
            .pointerInput(letter) {
                detectTapGestures(
                    onPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = accentColor,
            fontSize = (sizeDp.value * 0.44f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
    }
}
