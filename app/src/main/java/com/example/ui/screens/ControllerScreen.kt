package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ControllerElementId
import com.example.data.model.GamepadScreen
import com.example.ui.components.ABXYCluster
import com.example.ui.components.AnalogStick
import com.example.ui.components.DPadView
import com.example.ui.components.QuickAuxButtons
import com.example.ui.components.SavedLayoutsManagerDialog
import com.example.ui.components.TriggerBumperGroup
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxBlueX
import com.example.util.HapticTelemetryEvent
import com.example.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

@Composable
fun ControllerScreen(
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    val activeLayout by viewModel.activeLayout.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()
    val leftStickPos by viewModel.leftStickPos.collectAsState()
    val rightStickPos by viewModel.rightStickPos.collectAsState()
    val ltPressure by viewModel.ltPressure.collectAsState()
    val rtPressure by viewModel.rtPressure.collectAsState()
    val lastPressedButton by viewModel.lastPressedButton.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val lastHapticEvent by viewModel.lastHapticEvent.collectAsState()
    val isSavedLayoutsManagerOpen by viewModel.isSavedLayoutsManagerOpen.collectAsState()

    val elements = activeLayout.elements

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .testTag("controller_landscape_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Status & Global Toolbar
            ControllerTopBar(
                hostName = telemetry.hostName,
                roundtripMs = telemetry.roundtripMs,
                profileName = activeLayout.name,
                hapticsEnabled = quickSettings.hapticsEnabled,
                lastHapticEvent = lastHapticEvent,
                onOpenDrawer = { viewModel.openQuickDrawer() },
                onNavigateCustomize = { viewModel.navigateTo(GamepadScreen.CUSTOMIZE_LAYOUT) },
                onNavigateDiscovery = { viewModel.navigateTo(GamepadScreen.DEVICE_DISCOVERY) },
                onNavigateDiagnostics = { viewModel.navigateTo(GamepadScreen.DIAGNOSTICS) },
                onOpenSavedLayouts = { viewModel.openSavedLayoutsManager() }
            )

            // Dynamic Controller Surface
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val canvasWidth = maxWidth
                val canvasHeight = maxHeight

                // Subtle blueprint alignment crosshairs in background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCanvas)
                )

                // 1. LT / LB Group
                val ltConfig = elements[ControllerElementId.LT_LB]
                if (ltConfig != null) {
                    val xPos = (canvasWidth * (ltConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (ltConfig.yPercent / 100f))
                    TriggerBumperGroup(
                        isLeft = true,
                        scale = ltConfig.scale,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onBumperPress = { viewModel.onButtonPressed("LB") },
                        onTriggerChange = { viewModel.onTriggerChanged(true, it) }
                    )
                }

                // 2. RT / RB Group
                val rtConfig = elements[ControllerElementId.RT_RB]
                if (rtConfig != null) {
                    val xPos = (canvasWidth * (rtConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (rtConfig.yPercent / 100f))
                    TriggerBumperGroup(
                        isLeft = false,
                        scale = rtConfig.scale,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onBumperPress = { viewModel.onButtonPressed("RB") },
                        onTriggerChange = { viewModel.onTriggerChanged(false, it) }
                    )
                }

                // 3. Left Analog Stick (Offset ergonomic)
                val leftStickConfig = elements[ControllerElementId.LEFT_STICK]
                if (leftStickConfig != null) {
                    val xPos = (canvasWidth * (leftStickConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (leftStickConfig.yPercent / 100f))
                    AnalogStick(
                        sizeDp = (116 * leftStickConfig.scale).dp,
                        label = "Left Stick",
                        stylePreset = leftStickConfig.stylePreset,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onMove = { x, y -> viewModel.onLeftStickMoved(x, y) },
                        onStickClick = { viewModel.onButtonPressed("L3") }
                    )
                }

                // 4. Directional Pad
                val dpadConfig = elements[ControllerElementId.DPAD]
                if (dpadConfig != null) {
                    val xPos = (canvasWidth * (dpadConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (dpadConfig.yPercent / 100f))
                    DPadView(
                        sizeDp = (116 * dpadConfig.scale).dp,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onDirectionPress = { viewModel.onButtonPressed("D-Pad $it") }
                    )
                }

                // 5. Central Aux Buttons (Select, Guide, Start)
                val auxConfig = elements[ControllerElementId.AUX_BUTTONS]
                if (auxConfig != null) {
                    val xPos = (canvasWidth * (auxConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (auxConfig.yPercent / 100f))
                    QuickAuxButtons(
                        scale = auxConfig.scale,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onSelectPress = { viewModel.onButtonPressed("SELECT") },
                        onStartPress = { viewModel.onButtonPressed("START") },
                        onGuidePress = { viewModel.openQuickDrawer() }
                    )
                }

                // 6. Right Analog Stick (Asymmetric offset)
                val rightStickConfig = elements[ControllerElementId.RIGHT_STICK]
                if (rightStickConfig != null) {
                    val xPos = (canvasWidth * (rightStickConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (rightStickConfig.yPercent / 100f))
                    AnalogStick(
                        sizeDp = (116 * rightStickConfig.scale).dp,
                        label = "Right Stick",
                        stylePreset = rightStickConfig.stylePreset,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onMove = { x, y -> viewModel.onRightStickMoved(x, y) },
                        onStickClick = { viewModel.onButtonPressed("R3") }
                    )
                }

                // 7. ABXY Cluster (Action diamond)
                val abxyConfig = elements[ControllerElementId.ABXY]
                if (abxyConfig != null) {
                    val xPos = (canvasWidth * (abxyConfig.xPercent / 100f))
                    val yPos = (canvasHeight * (abxyConfig.yPercent / 100f))
                    ABXYCluster(
                        sizeDp = (120 * abxyConfig.scale).dp,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onButtonPress = { viewModel.onButtonPressed(it) }
                    )
                }

                // Bottom Live Telemetry Stream Strip
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard.copy(alpha = 0.85f))
                        .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LS: (${(leftStickPos.first * 100).toInt()}%, ${(leftStickPos.second * 100).toInt()}%)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "RS: (${(rightStickPos.first * 100).toInt()}%, ${(rightStickPos.second * 100).toInt()}%)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "LT: ${(ltPressure * 255).toInt()}  RT: ${(rtPressure * 255).toInt()}",
                        color = ControlBorderGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (lastPressedButton != null) {
                        Text(
                            text = "PRESSED: $lastPressedButton",
                            color = XboxBlueX,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Animated Toast Banner
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, ControlBorderGlow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Room Database Saved Layout Profiles Manager Dialog
        if (isSavedLayoutsManagerOpen) {
            SavedLayoutsManagerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeSavedLayoutsManager() }
            )
        }
    }
}

@Composable
private fun ControllerTopBar(
    hostName: String,
    roundtripMs: Float,
    profileName: String,
    hapticsEnabled: Boolean,
    lastHapticEvent: HapticTelemetryEvent?,
    onOpenDrawer: () -> Unit,
    onNavigateCustomize: () -> Unit,
    onNavigateDiscovery: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    onOpenSavedLayouts: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(SurfaceDefault.copy(alpha = 0.95f))
            .border(width = 1.dp, color = SurfaceControlRaised.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Connected Host Badge & Profile & Haptic Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live status pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCanvas)
                    .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusSuccess)
                        .shadow(6.dp, CircleShape, spotColor = StatusSuccess)
                )
                Text(
                    text = hostName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "•",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${roundtripMs}ms",
                    color = ControlBorderGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Profile Chip (Clickable to switch layout configurations)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceControl)
                    .clickable { onOpenSavedLayouts() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = PrimaryContainerBlue,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = profileName,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Haptic Status Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hapticsEnabled) ActiveControlFill else SurfaceControl)
                    .border(1.dp, if (hapticsEnabled) ControlBorderGlow.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onOpenDrawer() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = "Haptics Status",
                    tint = if (hapticsEnabled) XboxBlueX else TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (lastHapticEvent != null) lastHapticEvent.eventName.take(12) else if (hapticsEnabled) "Dual LRA" else "Off",
                    color = if (hapticsEnabled) TextPrimary else TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center / Right: Action Navigation Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Saved Layouts Manager Button (Room Database)
            IconButton(
                onClick = onOpenSavedLayouts,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .testTag("nav_saved_layouts_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Saved Layouts",
                    tint = PrimaryContainerBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Discovery Button
            IconButton(
                onClick = onNavigateDiscovery,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .testTag("nav_discovery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = "Device Discovery",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Diagnostics Button
            IconButton(
                onClick = onNavigateDiagnostics,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .testTag("nav_diagnostics_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Diagnostics",
                    tint = ControlBorderGlow,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Customize Layout Button
            IconButton(
                onClick = onNavigateCustomize,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .testTag("nav_customize_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Customize Layout",
                    tint = PrimaryContainerBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Quick Actions Drawer Toggle
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceControlRaised)
                    .testTag("quick_drawer_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Quick Actions Drawer",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
