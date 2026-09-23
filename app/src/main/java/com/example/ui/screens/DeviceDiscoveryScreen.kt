package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceTarget
import com.example.data.model.DeviceType
import com.example.data.model.GamepadScreen
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxBlueX
import com.example.ui.theme.XboxGreenA
import com.example.ui.theme.XboxYellowY
import com.example.viewmodel.GamepadViewModel

@Composable
fun DeviceDiscoveryScreen(
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .testTag("device_discovery_screen")
    ) {
        // Discovery Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDefault)
                .border(1.dp, SurfaceControlRaised.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(GamepadScreen.CONTROLLER) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .testTag("discovery_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Device Discovery",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SurfaceCanvas,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.navigateTo(GamepadScreen.CONTROLLER) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceControl)
                        .testTag("close_discovery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Chip & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    )
                    Text(
                        text = "HID BROADCAST MODE",
                        color = PrimaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Channel 7 • 2.4 GHz",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Central Scanning Radar Module
            CentralScanningRadar(isScanning = isScanning)

            // Discovery Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusSuccess)
                    )
                    Text(
                        text = "NEARBY TARGETS (${discoveredDevices.size} DISCOVERED)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = if (isScanning) "Auto-refreshing" else "Paused",
                    color = PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Target Devices List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                discoveredDevices.forEach { device ->
                    DeviceTargetCard(
                        device = device,
                        onConnect = { viewModel.connectToDevice(device.id) },
                        onOpenDiagnostics = { viewModel.navigateTo(GamepadScreen.DIAGNOSTICS) }
                    )
                }
            }

            // Bottom Action Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.toggleDiscoveryScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("toggle_discovery_scan_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        tint = if (isScanning) StatusError else XboxGreenA,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Stop Discovery" else "Restart Discovery",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "PC not appearing? Ensure Phone Bridge server is running",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CentralScanningRadar(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonarPulse")
    val sonarScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarScale"
    )
    val sonarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Concentric Sonar Circles
        Box(
            modifier = Modifier
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                // Expanding ping wave
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(sonarScale)
                        .clip(CircleShape)
                        .background(PrimaryContainerBlue.copy(alpha = sonarAlpha))
                )
            }

            // Outer ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(SurfaceControl.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(SurfaceControl),
                    contentAlignment = Alignment.Center
                ) {
                    // Central Transmitter Hub
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(20.dp, CircleShape, spotColor = PrimaryContainerBlue)
                            .clip(CircleShape)
                            .background(PrimaryContainerBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Orbiting ping dots
            Box(
                modifier = Modifier
                    .offset(x = 55.dp, y = (-40).dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(XboxBlueX)
            )
            Box(
                modifier = Modifier
                    .offset(x = (-45).dp, y = 45.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(XboxGreenA)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scanning Callouts
        Text(
            text = "Searching for Gaming PC...",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Broadcasting low-latency HID telemetry via Gamepad Bridge daemon.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Signal Stats Ticker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("POLL RATE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("250 Hz", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LATENCY", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("< 2.8 ms", color = XboxGreenA, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PROFILE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("XInput", color = XboxBlueX, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DeviceTargetCard(
    device: DeviceTarget,
    onConnect: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val isMain = device.id == "host_1"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(
                1.dp,
                if (device.isConnected) ControlBorderGlow else ControlBorderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceControl),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (device.type) {
                            DeviceType.PC -> Icons.Default.DesktopWindows
                            DeviceType.LAPTOP -> Icons.Default.LaptopChromebook
                            DeviceType.STEAM_DECK -> Icons.Default.VideogameAsset
                            else -> Icons.Default.SportsEsports
                        },
                        contentDescription = null,
                        tint = when (device.type) {
                            DeviceType.PC -> PrimaryBlue
                            DeviceType.LAPTOP -> XboxBlueX
                            DeviceType.STEAM_DECK -> XboxYellowY
                            else -> PrimaryBlue
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (device.isConnected) XboxGreenA else StatusWarning)
                        )
                    }
                    Text(
                        text = device.subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // RSSI Signal Meter Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceControl)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = XboxGreenA,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${device.rssiDbm} dBm",
                    color = XboxGreenA,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons
        if (isMain) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shadow(12.dp, RoundedCornerShape(10.dp), spotColor = PrimaryContainerBlue)
                        .testTag("pair_link_stream_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (device.isConnected) "Linked & Streaming" else "Pair & Link Stream",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onOpenDiagnostics,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceControl)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Config Mode",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (device.type == DeviceType.LAPTOP) Icons.Default.Bolt else Icons.Default.AddLink,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (device.isPaired) "Connect" else "Pair",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
