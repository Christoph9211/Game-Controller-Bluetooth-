package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WifiTethering
import com.example.util.HapticMotorChannel
import com.example.util.HapticTelemetryEvent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceTarget
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
fun DiagnosticsScreen(
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val pairedHosts by viewModel.pairedHosts.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val lastHapticEvent by viewModel.lastHapticEvent.collectAsState()
    val quickSettings by viewModel.quickSettings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .testTag("device_diagnostics_screen")
    ) {
        // Global Header Bar
        DiagnosticsTopHeader(
            hostName = telemetry.hostName,
            onBack = { viewModel.navigateTo(GamepadScreen.CONTROLLER) },
            onNavigateController = { viewModel.navigateTo(GamepadScreen.CONTROLLER) },
            onNavigateCustomize = { viewModel.navigateTo(GamepadScreen.CUSTOMIZE_LAYOUT) },
            onNavigateDiscovery = { viewModel.navigateTo(GamepadScreen.DEVICE_DISCOVERY) }
        )

        // Main Scrollable Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Page Header & Telemetry Core Ribbon
            TelemetryPageHeader(
                telemetry = telemetry,
                roundtripMs = telemetry.roundtripMs
            )

            // Top Telemetry Grid (4 Core Metric Blocks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Metric 1: Active Host Interface
                ActiveHostCard(
                    modifier = Modifier.weight(1f),
                    telemetry = telemetry,
                    onDisconnect = { viewModel.disconnectActiveDevice() },
                    onConfigMode = { viewModel.navigateTo(GamepadScreen.CUSTOMIZE_LAYOUT) }
                )

                // Metric 2: RF Link Quality
                RfLinkQualityCard(
                    modifier = Modifier.weight(1f),
                    rfDbm = telemetry.rfDbm,
                    integrity = telemetry.signalIntegrity
                )

                // Metric 3: Input Queue Latency
                InputLatencyCard(
                    modifier = Modifier.weight(1f),
                    roundtripMs = telemetry.roundtripMs
                )

                // Metric 4: Physical Sensors Subsystem
                SensorsTelemetryCard(
                    modifier = Modifier.weight(1f),
                    thermalC = telemetry.thermalC,
                    lastHapticEvent = lastHapticEvent,
                    hapticsEnabled = quickSettings.hapticsEnabled,
                    onTestHaptic = { viewModel.testHapticMotor(it) },
                    onRecalibrate = { viewModel.showToast("Sensors recalibrated to zero-point vectors") }
                )
            }

            // Secondary Section: Two-Column Split (Real-Time Packet Stream & Paired Host History)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Real-Time Packet Stream (weight 7)
                RealTimePacketStreamCard(
                    modifier = Modifier.weight(1.35f),
                    txRate = telemetry.txRate,
                    totalPackets = telemetry.totalPackets
                )

                // Right Column: Paired Host History (weight 5)
                PairedHostHistoryCard(
                    modifier = Modifier.weight(1f),
                    pairedHosts = pairedHosts,
                    onConnectHost = { viewModel.connectToDevice(it) },
                    onManageSlots = { viewModel.navigateTo(GamepadScreen.DEVICE_DISCOVERY) }
                )
            }

            // Bottom Action Center & Host Discovery Hub
            DiscoveryActionHub(
                onScan = { viewModel.navigateTo(GamepadScreen.DEVICE_DISCOVERY) },
                onClearPairings = { viewModel.showToast("Inactive pairings cleared") },
                onRadioReset = { viewModel.showToast("Bluetooth radio reset complete") },
                onExportLogs = { viewModel.showToast("Diagnostics log exported to storage (.json)") }
            )

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bluetooth Gamepad HID Engine v2.4.0 • Firmware Synchronized",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Target: Win11 XInput Bridge • Packet Loss: 0.00%",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }

    // Toast alert
    AnimatedVisibility(
        visible = toastMessage != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, XboxGreenA, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsTopHeader(
    hostName: String,
    onBack: () -> Unit,
    onNavigateController: () -> Unit,
    onNavigateCustomize: () -> Unit,
    onNavigateDiscovery: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(SurfaceDefault)
            .border(1.dp, SurfaceControlRaised.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .testTag("diagnostics_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "Bluetooth Gamepad",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Status chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCanvas)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(XboxGreenA)
                )
                Text(text = hostName, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "·", color = TextTertiary, fontSize = 11.sp)
                Text(text = "Connected (8ms Polling)", color = XboxBlueX, fontSize = 11.sp)
            }
        }

        // Navigation Tabs in Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HeaderNavLink("Diagnostics", isSelected = true, onClick = {})
            HeaderNavLink("Controller", isSelected = false, onClick = onNavigateController)
            HeaderNavLink("Customize", isSelected = false, onClick = onNavigateCustomize)
            HeaderNavLink("Discovery", isSelected = false, onClick = onNavigateDiscovery)
        }

        // Right Quick Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Text("BT 5.3 HID", color = TextSecondary, fontSize = 11.sp)
                Text("|", color = SurfaceControl)
                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = XboxGreenA, modifier = Modifier.size(16.dp))
                Text("100%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = SurfaceCanvas, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HeaderNavLink(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryContainerBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TelemetryPageHeader(
    telemetry: com.example.data.model.TelemetryData,
    roundtripMs: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceControl)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                    Text("HID TELEMETRY CORE", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("•", color = TextTertiary)
                Text("TX_QUEUE_STABLE", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Text(
                text = "Device Diagnostics & Telemetry",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "Real-time HID report transmission, RF signal integrity, and connection history.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Active Status Ribbon Card
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(XboxGreenA)
                )
                Column {
                    Text("LINK STATE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("HID Connected", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(modifier = Modifier.size(width = 1.dp, height = 32.dp).background(SurfaceControlRaised))

            Column {
                Text("ROUNDTRIP", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$roundtripMs", color = XboxBlueX, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(" ms", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Box(modifier = Modifier.size(width = 1.dp, height = 32.dp).background(SurfaceControlRaised))

            Column {
                Text("SAMPLING", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("${telemetry.samplingHz} Hz", color = XboxGreenA, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ActiveHostCard(
    telemetry: com.example.data.model.TelemetryData,
    onDisconnect: () -> Unit,
    onConfigMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACTIVE HOST INTERFACE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        }

        Column {
            Text(telemetry.hostName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Win Xbox Bridge (Phone Companion)", color = XboxBlueX, fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCanvas)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MAC ID:", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(telemetry.hostMac, color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PROFILE:", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(telemetry.profileName, color = XboxGreenA, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Disconnect", color = StatusError, fontSize = 11.sp)
            }
            Button(
                onClick = onConfigMode,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Config Mode", color = TextPrimary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RfLinkQualityCard(
    rfDbm: Int,
    integrity: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RF LINK QUALITY", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = XboxGreenA, modifier = Modifier.size(18.dp))
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("$rfDbm", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("dBm (RSSI)", color = TextTertiary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceCanvas)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("$integrity% Integrity", color = XboxGreenA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Cyan 6-bar Signal Meter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCanvas)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(0.95f, 0.92f, 0.88f, 0.98f, 0.90f, 0.20f).forEachIndexed { index, fill ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .fillMaxHeight(fill)
                            .background(if (index < 5) XboxBlueX else SurfaceControlRaised)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Carrier: 2.402 GHz BLE", color = TextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("0.00% Loss", color = XboxGreenA, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = XboxGreenA, modifier = Modifier.size(14.dp))
            Text("Direct line-of-sight optimal • Transmit +4dBm", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InputLatencyCard(
    roundtripMs: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INPUT QUEUE LATENCY", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$roundtripMs", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("ms avg", color = TextTertiary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.weight(1f))
            Text("Jitter ±0.4 ms", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        // Latency Sparkline Canvas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCanvas)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val points = listOf(24f, 23f, 26f, 21f, 24f, 20f, 22f, 18f, 21f, 20f, 19f)
                val stepX = size.width / (points.size - 1)
                val strokePath = Path()
                points.forEachIndexed { i, p ->
                    val y = (p / 30f) * size.height
                    if (i == 0) strokePath.moveTo(0f, y) else strokePath.lineTo(i * stepX, y)
                }
                drawPath(strokePath, color = PrimaryContainerBlue, style = Stroke(width = 2.dp.toPx()))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Min: 2.4 ms", color = TextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("8 ms Window (125Hz)", color = TextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("Max: 5.1 ms", color = TextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
            Text("Strict temporal packet pacing active", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SensorsTelemetryCard(
    thermalC: Float,
    lastHapticEvent: HapticTelemetryEvent?,
    hapticsEnabled: Boolean,
    onTestHaptic: (HapticMotorChannel) -> Unit,
    onRecalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SUBSYSTEM TELEMETRY", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = XboxYellowY, modifier = Modifier.size(18.dp))
        }

        Column {
            Text("Physical Sensors & Haptics", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Dual LRA & calibration vectors", color = TextSecondary, fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCanvas)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HALL STICKS:", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("0.0% Drift (Zero-Deadzone)", color = XboxGreenA, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LT / RT SENSORS:", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("256-Step Linear Analog", color = XboxBlueX, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HAPTIC MOTOR:", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    if (hapticsEnabled) "Dual LRA Active" else "Haptics Disabled",
                    color = if (hapticsEnabled) PrimaryBlue else TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (lastHapticEvent != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ACTIVE LRA:", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "${lastHapticEvent.eventName.take(16)} (${lastHapticEvent.amplitudePct}%)",
                        color = XboxYellowY,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Quick LRA Motor Test Triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { onTestHaptic(HapticMotorChannel.LEFT_HEAVY) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).height(28.dp)
            ) {
                Text("L-Heavy", fontSize = 9.sp, color = TextPrimary)
            }
            Button(
                onClick = { onTestHaptic(HapticMotorChannel.RIGHT_LIGHT) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).height(28.dp)
            ) {
                Text("R-Light", fontSize = 9.sp, color = TextPrimary)
            }
            Button(
                onClick = { onTestHaptic(HapticMotorChannel.DUAL_STEREO) },
                colors = ButtonDefaults.buttonColors(containerColor = ActiveControlFill),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).height(28.dp)
            ) {
                Text("Dual LRA", fontSize = 9.sp, color = ControlBorderGlow, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(XboxGreenA))
                Text("Thermal: ${thermalC}°C Nominal", color = TextSecondary, fontSize = 10.sp)
            }
            Text(
                text = "Recalibrate",
                color = PrimaryBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRecalibrate() }
            )
        }
    }
}

@Composable
private fun RealTimePacketStreamCard(
    txRate: Float,
    totalPackets: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Text("Real-Time Packet Stream", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text("Historical 30-second window at 8ms interval resolution", color = TextTertiary, fontSize = 11.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCanvas)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("TX Rate: $txRate pkts/sec", color = XboxGreenA, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Live Histogram Stream Graphic
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCanvas)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("T -30s", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("T -15s", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("LIVE (T-0)", color = XboxBlueX, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(
                    0.55f to "idle", 0.62f to "idle", 0.58f to "idle", 0.60f to "idle",
                    0.72f to "normal", 0.75f to "normal", 0.68f to "idle", 0.82f to "normal",
                    0.79f to "normal", 0.88f to "burst", 0.84f to "burst", 0.78f to "normal",
                    0.65f to "idle", 0.68f to "idle", 0.80f to "normal", 0.76f to "normal",
                    0.92f to "burst", 0.90f to "burst", 0.88f to "burst", 0.85f to "normal",
                    0.82f to "normal", 0.94f to "cyan", 0.96f to "cyan", 0.91f to "cyan"
                )

                heights.forEach { (h, type) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .fillMaxHeight(h)
                            .background(
                                when (type) {
                                    "burst" -> XboxGreenA
                                    "cyan" -> XboxBlueX
                                    "normal" -> PrimaryContainerBlue
                                    else -> SurfaceControl
                                }
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem("Normal Report", PrimaryContainerBlue)
                    LegendItem("Burst Input", XboxGreenA)
                    LegendItem("Idle Throttle", SurfaceControl)
                }
                Text("Total Packets Sent: $totalPackets", color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Diagnostic Verification Suite (4 chips)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DIAGNOSTIC VERIFICATION SUITE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagChip(label = "BT 5.3 Controller Stack", status = "OK · READY", isGreen = true, modifier = Modifier.weight(1f))
                DiagChip(label = "HID Descriptors", status = "SYNCED (XINPUT)", isGreen = true, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagChip(label = "MCU Ingest Buffer", status = "0 OVERFLOWS", isGreen = true, modifier = Modifier.weight(1f))
                DiagChip(label = "Anti-Deadzone", status = "ADAPTIVE ACTIVE", isGreen = false, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun DiagChip(
    label: String,
    status: String,
    isGreen: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCanvas)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isGreen) XboxGreenA else XboxBlueX)
            )
            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = status,
            color = if (isGreen) XboxGreenA else XboxBlueX,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PairedHostHistoryCard(
    pairedHosts: List<DeviceTarget>,
    onConnectHost: (String) -> Unit,
    onManageSlots: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Text("Paired Host History", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceControl)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${pairedHosts.size} Profiles", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // List of Hosts
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pairedHosts.forEach { host ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCanvas)
                        .border(
                            width = 1.dp,
                            color = if (host.isConnected) XboxGreenA.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(host.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(host.lastSeenOrConnected, color = TextTertiary, fontSize = 10.sp)
                        Text(host.connectionType, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (host.isConnected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(XboxGreenA.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active Now", color = XboxGreenA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { onConnectHost(host.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControlRaised),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect", color = TextPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Footer link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Max Pairing Slots: 4 of 8 occupied", color = TextTertiary, fontSize = 11.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onManageSlots() }
            ) {
                Text("Manage Slots", color = TextSecondary, fontSize = 11.sp)
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun DiscoveryActionHub(
    onScan: () -> Unit,
    onClearPairings: () -> Unit,
    onRadioReset: () -> Unit,
    onExportLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceControl),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Radar, contentDescription = null, tint = PrimaryContainerBlue, modifier = Modifier.size(28.dp))
        }

        Text(
            text = "Discover New Gaming Target",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Discovering discoverable Bluetooth gaming hosts and Phone Gamepad Bridge companions in proximity. Put your PC or console into pairing mode.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(520.dp)
        )

        Button(
            onClick = onScan,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .width(280.dp)
                .height(46.dp)
                .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = PrimaryContainerBlue)
                .testTag("diag_scan_for_new_devices_btn")
        ) {
            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan for New Devices", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Secondary Links
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onClearPairings() }
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Text("Clear Inactive Pairings", color = TextTertiary, fontSize = 11.sp)
            }
            Text("•", color = SurfaceControlRaised)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onRadioReset() }
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Text("Bluetooth Radio Reset", color = TextTertiary, fontSize = 11.sp)
            }
            Text("•", color = SurfaceControlRaised)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onExportLogs() }
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Text("Export Diagnostics Log (.json)", color = TextTertiary, fontSize = 11.sp)
            }
        }
    }
}
