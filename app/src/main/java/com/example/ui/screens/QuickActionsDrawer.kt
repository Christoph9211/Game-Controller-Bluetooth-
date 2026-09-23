package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GamepadScreen
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxBlueX
import com.example.util.HapticMotorChannel
import com.example.viewmodel.GamepadViewModel

@Composable
fun QuickActionsDrawer(
    viewModel: GamepadViewModel,
    isOpen: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickSettings by viewModel.quickSettings.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val lastHapticEvent by viewModel.lastHapticEvent.collectAsState()

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onClose() }
                .testTag("quick_actions_drawer_scrim")
        ) {
            // Slide-out panel from the right
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(SurfaceDefault)
                    .border(1.dp, SurfaceControlRaised, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .testTag("quick_actions_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = PrimaryContainerBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Quick Actions",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceControl)
                                .testTag("close_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Connected Device Quick Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = telemetry.hostName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ping: ${telemetry.roundtripMs}ms • Sampling: ${telemetry.samplingHz}Hz",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.disconnectActiveDevice() },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("drawer_disconnect_btn")
                        ) {
                            Text("Disconnect", color = StatusError, fontSize = 11.sp)
                        }
                    }

                    // Profile Preset Switcher
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRESET PROFILE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        val profiles = listOf(
                            "P1: Low Latency FPS",
                            "P2: Racing Sim",
                            "P3: Retro Arcade"
                        )

                        profiles.forEach { prof ->
                            val isSelected = quickSettings.currentProfile == prof
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ActiveControlFill.copy(alpha = 0.4f) else SurfaceCard)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ControlBorderGlow else ControlBorderSubtle.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.updateProfile(prof) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prof,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusSuccess)
                                    )
                                }
                            }
                        }
                    }

                    // Toggles (Haptics, Gyro, Turbo)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "HARDWARE SUBSYSTEMS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        // Dual LRA Haptic Feedback Toggle & Configuration
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceCard)
                                .border(1.dp, if (quickSettings.hapticsEnabled) ControlBorderGlow.copy(alpha = 0.4f) else ControlBorderSubtle.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = if (quickSettings.hapticsEnabled) XboxBlueX else TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text("Dual LRA Haptics", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("True tactile motor vibration", color = TextTertiary, fontSize = 11.sp)
                                    }
                                }
                                Switch(
                                    checked = quickSettings.hapticsEnabled,
                                    onCheckedChange = { viewModel.toggleHaptics() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryContainerBlue),
                                    modifier = Modifier.testTag("drawer_haptics_switch")
                                )
                            }

                            if (quickSettings.hapticsEnabled) {
                                // Intensity Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Vibration Force", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("${(quickSettings.hapticStrength * 100).toInt()}%", color = XboxBlueX, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = quickSettings.hapticStrength,
                                        onValueChange = { viewModel.setHapticStrength(it) },
                                        valueRange = 0.2f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = PrimaryContainerBlue,
                                            activeTrackColor = ControlBorderGlow,
                                            inactiveTrackColor = SurfaceControl
                                        ),
                                        modifier = Modifier.height(24.dp).testTag("drawer_haptic_strength_slider")
                                    )
                                }

                                // Haptic Profiles (Crisp, Heavy, Subtle)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Profile Preset", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Crisp Mechanical", "Heavy Dual LRA", "Subtle Micro-Tick").forEach { prof ->
                                            val isSel = quickSettings.hapticProfile == prof
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) ActiveControlFill else SurfaceControl)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSel) ControlBorderGlow else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { viewModel.setHapticProfile(prof) }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = prof.split(" ")[0],
                                                    color = if (isSel) TextPrimary else TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive Motor & Tactile Test Bench
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Test Haptic Actuators", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.testHapticMotor(HapticMotorChannel.LEFT_HEAVY) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("test_haptic_left_heavy")
                                        ) {
                                            Text("Heavy LRA", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Button(
                                            onClick = { viewModel.testHapticMotor(HapticMotorChannel.RIGHT_LIGHT) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("test_haptic_right_light")
                                        ) {
                                            Text("Light LRA", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Button(
                                            onClick = { viewModel.testHapticMotor(HapticMotorChannel.DUAL_STEREO) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ActiveControlFill),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("test_haptic_stereo")
                                        ) {
                                            Text("Stereo LRA", fontSize = 10.sp, color = ControlBorderGlow, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Live Haptic Telemetry Badge
                                if (lastHapticEvent != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceControlRaised)
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚡ ${lastHapticEvent?.eventName}",
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${lastHapticEvent?.amplitudePct}% • ${lastHapticEvent?.durationMs}ms",
                                            color = XboxBlueX,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Gyro Aim Assist Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Gyro Aim Assist", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tilt device for precision adjustments", color = TextTertiary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = quickSettings.gyroAimEnabled,
                                onCheckedChange = { viewModel.toggleGyro() },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryContainerBlue)
                            )
                        }

                        // Turbo Rapid Fire Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Turbo Rapid Fire", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("15 clicks/sec auto-fire repeat", color = TextTertiary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = quickSettings.turboEnabled,
                                onCheckedChange = { viewModel.toggleTurbo() },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryContainerBlue)
                            )
                        }
                    }

                    // Polling Rate Selector (125Hz / 250Hz / 500Hz)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "POLLING RATE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(125, 250, 500).forEach { hz ->
                                val isSelected = quickSettings.pollRateHz == hz
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryContainerBlue else SurfaceCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) ControlBorderGlow else ControlBorderSubtle.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setPollingRate(hz) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${hz}Hz",
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Deadzone Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("STICK DEADZONE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${quickSettings.deadzonePct}%", color = XboxBlueX, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = quickSettings.deadzonePct.toFloat(),
                            onValueChange = { viewModel.setDeadzone(it.toInt()) },
                            valueRange = 0f..20f,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryContainerBlue,
                                activeTrackColor = PrimaryContainerBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Deep Links to other screens
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.navigateTo(GamepadScreen.CUSTOMIZE_LAYOUT)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Layout Customizer", color = TextPrimary, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.navigateTo(GamepadScreen.DIAGNOSTICS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Device Diagnostics", color = TextPrimary, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.navigateTo(GamepadScreen.DEVICE_DISCOVERY)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Discover New Devices", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
