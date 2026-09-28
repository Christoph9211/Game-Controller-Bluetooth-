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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxBlueX
import com.example.util.HapticTelemetryEvent
import com.example.viewmodel.GamepadViewModel

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

    var previousSize by remember { mutableStateOf(IntSize.Zero) }
    DisposableEffect(Unit) { onDispose { viewModel.releaseControls() } }
    val generation by viewModel.inputGeneration.collectAsState()
    val connection by viewModel.connection.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .testTag("controller_landscape_screen")
            .onSizeChanged { size ->
                if (previousSize != IntSize.Zero && previousSize != size) viewModel.releaseControls()
                previousSize=size
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Status & Global Toolbar
            ControllerTopBar(
                hostName = telemetry.hostName,
                connected = connection.connected,
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
            key(generation) { BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val canvasWidth = maxWidth
                val canvasHeight = maxHeight
                val portrait = canvasWidth < 600.dp
                val largestScale=activeLayout.elements.values.maxOfOrNull { it.scale } ?: 1f
                val fits = if (portrait) canvasWidth >= 320.dp && canvasHeight >= 540.dp
                    else canvasHeight >= (230 * largestScale + 40).dp && canvasWidth >= (480 * largestScale + 24).dp
                if (!fits) {
                    Text("Enlarge or rotate the window to use the controller.", color=TextPrimary,
                        modifier=Modifier.align(Alignment.Center).padding(24.dp))
                    return@BoxWithConstraints
                }
                val portraitPositions = mapOf(
                    ControllerElementId.LT_LB to (0f to 0f), ControllerElementId.RT_RB to (100f to 0f),
                    ControllerElementId.AUX_BUTTONS to (50f to 23f),
                    ControllerElementId.LEFT_STICK to (0f to 43f), ControllerElementId.RIGHT_STICK to (100f to 43f),
                    ControllerElementId.DPAD to (0f to 88f), ControllerElementId.ABXY to (100f to 88f))
                val lowerRow=listOf(ControllerElementId.LEFT_STICK,ControllerElementId.RIGHT_STICK,
                    ControllerElementId.DPAD,ControllerElementId.ABXY)
                    .filter { it in activeLayout.elements }.sortedBy { activeLayout.elements.getValue(it).xPercent }
                val elements = if (!activeLayout.isCustom) activeLayout.elements.mapValues { (id,config) ->
                    if (portrait) {
                        val pos=portraitPositions[id]
                        if (pos==null) config else config.copy(
                            xPercent=if (id==ControllerElementId.AUX_BUTTONS) 50f else if (config.xPercent<50f) 0f else 100f,
                            yPercent=pos.second)
                    } else if (id in lowerRow) {
                        config.copy(xPercent=100f*lowerRow.indexOf(id)/(lowerRow.size-1).coerceAtLeast(1),yPercent=100f)
                    } else config.copy(yPercent=0f, xPercent=if (id==ControllerElementId.AUX_BUTTONS) 50f else config.xPercent)
                } else activeLayout.elements

                // Subtle blueprint alignment crosshairs in background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCanvas)
                )

                // 1. LT / LB Group
                val ltConfig = elements[ControllerElementId.LT_LB]
                if (ltConfig != null) {
                    val xPos = ((canvasWidth - (84 * ltConfig.scale).dp).coerceAtLeast(0.dp) * (ltConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (110 * ltConfig.scale).dp).coerceAtLeast(0.dp) * (ltConfig.yPercent / 100f))
                    TriggerBumperGroup(
                        isLeft = true,
                        scale = ltConfig.scale,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onBumperPress = { viewModel.onButtonChanged("LB", it) },
                        onTriggerChange = { viewModel.onTriggerChanged(true, it) }
                    )
                }

                // 2. RT / RB Group
                val rtConfig = elements[ControllerElementId.RT_RB]
                if (rtConfig != null) {
                    val xPos = ((canvasWidth - (84 * rtConfig.scale).dp).coerceAtLeast(0.dp) * (rtConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (110 * rtConfig.scale).dp).coerceAtLeast(0.dp) * (rtConfig.yPercent / 100f))
                    TriggerBumperGroup(
                        isLeft = false,
                        scale = rtConfig.scale,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onBumperPress = { viewModel.onButtonChanged("RB", it) },
                        onTriggerChange = { viewModel.onTriggerChanged(false, it) }
                    )
                }

                // 3. Left Analog Stick (Offset ergonomic)
                val leftStickConfig = elements[ControllerElementId.LEFT_STICK]
                if (leftStickConfig != null) {
                    val xPos = ((canvasWidth - (116 * leftStickConfig.scale).dp).coerceAtLeast(0.dp) * (leftStickConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (116 * leftStickConfig.scale).dp).coerceAtLeast(0.dp) * (leftStickConfig.yPercent / 100f))
                    AnalogStick(
                        sizeDp = (116 * leftStickConfig.scale).dp,
                        label = "Left Stick",
                        stylePreset = leftStickConfig.stylePreset,
                        recenterOnTouch = quickSettings.recenterSticksOnTouch,
                        extraActivationReachDp = quickSettings.extraActivationReachDp.dp,
                        deadzonePct = quickSettings.deadzonePct,
                        modifier = Modifier.offset {
                            val reach = if (quickSettings.recenterSticksOnTouch) quickSettings.extraActivationReachDp.dp.roundToPx() else 0
                            IntOffset(xPos.roundToPx() - reach, yPos.roundToPx() - reach)
                        },
                        onMove = { x, y -> viewModel.onLeftStickMoved(x, y) },
                        onStickClick = { viewModel.onButtonChanged("L3", it) }
                    )
                }

                // 4. Directional Pad
                val dpadConfig = elements[ControllerElementId.DPAD]
                if (dpadConfig != null) {
                    val xPos = ((canvasWidth - (116 * dpadConfig.scale).dp).coerceAtLeast(0.dp) * (dpadConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (116 * dpadConfig.scale).dp).coerceAtLeast(0.dp) * (dpadConfig.yPercent / 100f))
                    DPadView(
                        sizeDp = (116 * dpadConfig.scale).dp,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onHatChange = { viewModel.onHatChanged(it) }
                    )
                }

                // 5. Central Aux Buttons (Select, Guide, Start)
                val auxConfig = elements[ControllerElementId.AUX_BUTTONS]
                if (auxConfig != null) {
                    val xPos = ((canvasWidth - (210 * auxConfig.scale).dp).coerceAtLeast(0.dp) * (auxConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (70 * auxConfig.scale).dp).coerceAtLeast(0.dp) * (auxConfig.yPercent / 100f))
                    QuickAuxButtons(
                        scale = auxConfig.scale,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onSelectPress = { viewModel.onButtonChanged("SELECT", it) },
                        onStartPress = { viewModel.onButtonChanged("START", it) },
                        onGuidePress = { viewModel.openQuickDrawer() }
                    )
                }

                // 6. Right Analog Stick (Asymmetric offset)
                val rightStickConfig = elements[ControllerElementId.RIGHT_STICK]
                if (rightStickConfig != null) {
                    val xPos = ((canvasWidth - (116 * rightStickConfig.scale).dp).coerceAtLeast(0.dp) * (rightStickConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (116 * rightStickConfig.scale).dp).coerceAtLeast(0.dp) * (rightStickConfig.yPercent / 100f))
                    AnalogStick(
                        sizeDp = (116 * rightStickConfig.scale).dp,
                        label = "Right Stick",
                        stylePreset = rightStickConfig.stylePreset,
                        recenterOnTouch = quickSettings.recenterSticksOnTouch,
                        extraActivationReachDp = quickSettings.extraActivationReachDp.dp,
                        deadzonePct = quickSettings.deadzonePct,
                        modifier = Modifier.offset {
                            val reach = if (quickSettings.recenterSticksOnTouch) quickSettings.extraActivationReachDp.dp.roundToPx() else 0
                            IntOffset(xPos.roundToPx() - reach, yPos.roundToPx() - reach)
                        },
                        onMove = { x, y -> viewModel.onRightStickMoved(x, y) },
                        onStickClick = { viewModel.onButtonChanged("R3", it) }
                    )
                }

                // 7. ABXY Cluster (Action diamond)
                val abxyConfig = elements[ControllerElementId.ABXY]
                if (abxyConfig != null) {
                    val xPos = ((canvasWidth - (120 * abxyConfig.scale).dp).coerceAtLeast(0.dp) * (abxyConfig.xPercent / 100f))
                    val yPos = ((canvasHeight - 32.dp - (120 * abxyConfig.scale).dp).coerceAtLeast(0.dp) * (abxyConfig.yPercent / 100f))
                    ABXYCluster(
                        sizeDp = (120 * abxyConfig.scale).dp,
                        modifier = Modifier.offset {
                            IntOffset(xPos.roundToPx(), yPos.roundToPx())
                        },
                        onButtonChange = { name, down -> viewModel.onButtonChanged(name, down) }
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
    hostName: String, connected: Boolean, profileName: String, hapticsEnabled: Boolean,
    lastHapticEvent: HapticTelemetryEvent?, onOpenDrawer: () -> Unit,
    onNavigateCustomize: () -> Unit, onNavigateDiscovery: () -> Unit,
    onNavigateDiagnostics: () -> Unit, onOpenSavedLayouts: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(SurfaceDefault).padding(horizontal=8.dp)) {
        Text(if (connected) "HID connected: $hostName" else "Not connected · Local controls",
            color=if (connected) ControlBorderGlow else TextSecondary, maxLines=1,
            modifier=Modifier.padding(4.dp), fontSize=12.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onNavigateDiscovery) { Text("Connect") }
            TextButton(onClick=onNavigateCustomize) { Text("Customize") }
            TextButton(onClick=onNavigateDiagnostics) { Text("Diagnostics") }
            TextButton(onClick=onOpenSavedLayouts) { Text("Layouts") }
            IconButton(onClick=onOpenDrawer) {
                Icon(Icons.Default.Tune,contentDescription="Quick actions",tint=ControlBorderGlow)
            }
        }
    }
}
