package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceTarget
import com.example.data.model.DeviceType
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
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxBlueX
import com.example.ui.theme.XboxGreenA
import com.example.ui.theme.XboxYellowY

/**
 * Composable UI that displays discovered Bluetooth devices (gamepads, consoles, and host rigs)
 * and provides a prominent 'Connect' button to initiate pairing with selected gamepads.
 */
@Composable
fun DiscoveredBluetoothDevicesView(
    devices: List<DeviceTarget>,
    isScanning: Boolean,
    connectingDeviceId: String?,
    selectedDeviceId: String?,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    onSelectDevice: (String) -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: () -> Unit,
    onToggleScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredDevices = remember(devices, activeFilter) {
        when (activeFilter) {
            "GAMEPADS" -> devices.filter { it.type == DeviceType.GAMEPAD }
            "HOSTS" -> devices.filter { it.type != DeviceType.GAMEPAD }
            else -> devices
        }
    }

    val selectedDevice = remember(devices, selectedDeviceId) {
        devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()
    }

    var showPairingHelp by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("discovered_bluetooth_devices_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Scanner Status Header with Radar Pulse
        DiscoveryRadarHeader(
            isScanning = isScanning,
            deviceCount = filteredDevices.size,
            onToggleScan = onToggleScan,
            onToggleHelp = { showPairingHelp = !showPairingHelp }
        )

        // 2. Filter Category Pills
        FilterCategoryRow(
            allCount = devices.size,
            gamepadCount = devices.count { it.type == DeviceType.GAMEPAD },
            hostCount = devices.count { it.type != DeviceType.GAMEPAD },
            selectedFilter = activeFilter,
            onSelectFilter = onFilterChange
        )

        // 3. Pairing Help Card (Collapsible)
        AnimatedVisibility(visible = showPairingHelp, enter = fadeIn(), exit = fadeOut()) {
            GamepadPairingGuideCard(onDismiss = { showPairingHelp = false })
        }

        // 4. Discovered Device Cards List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredDevices.isEmpty()) {
                EmptyDiscoveredDevicesCard(isScanning = isScanning, onStartScan = onToggleScan)
            } else {
                filteredDevices.forEach { device ->
                    DiscoveredDeviceItemCard(
                        device = device,
                        isSelected = device.id == selectedDeviceId,
                        isConnecting = connectingDeviceId == device.id,
                        onCardClick = { onSelectDevice(device.id) },
                        onConnectClick = { onConnectDevice(device.id) },
                        onDisconnectClick = onDisconnectDevice
                    )
                }
            }
        }

        // 5. Selected Device Quick Action Bottom Dock (if a device is selected)
        if (selectedDevice != null) {
            SelectedDeviceActionDock(
                device = selectedDevice,
                isConnecting = connectingDeviceId == selectedDevice.id,
                onConnect = { onConnectDevice(selectedDevice.id) },
                onDisconnect = onDisconnectDevice
            )
        }
    }
}

/**
 * Radar Pulse & Discovery Header
 */
@Composable
private fun DiscoveryRadarHeader(
    isScanning: Boolean,
    deviceCount: Int,
    onToggleScan: () -> Unit,
    onToggleHelp: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSpin"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Animated Radar Beacon
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = pulseAlpha))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isScanning) ActiveControlFill else SurfaceControl)
                        .border(1.dp, if (isScanning) ControlBorderGlow else ControlBorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth Radar",
                        tint = if (isScanning) PrimaryBlue else TextTertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (isScanning) Modifier.rotate(spinAngle * 0.1f) else Modifier)
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isScanning) "SCANNING FOR GAMEPADS..." else "DISCOVERY PAUSED",
                        color = if (isScanning) PrimaryBlue else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) XboxGreenA else TextTertiary)
                    )
                }
                Text(
                    text = "$deviceCount Bluetooth targets in range • 2.4 GHz BLE",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }

        // Action Buttons: Rescan & Pairing Guide
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onToggleHelp,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceControl)
                    .testTag("toggle_pairing_guide_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Pairing Guide",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Button(
                onClick = onToggleScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) SurfaceControl else PrimaryContainerBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("toggle_discovery_scan_button")
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    tint = if (isScanning) TextPrimary else Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .then(if (isScanning) Modifier.rotate(spinAngle) else Modifier)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "Pause Scan" else "Scan Now",
                    color = if (isScanning) TextPrimary else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Filter Category Row
 */
@Composable
private fun FilterCategoryRow(
    allCount: Int,
    gamepadCount: Int,
    hostCount: Int,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterPill(
            label = "All Targets ($allCount)",
            isSelected = selectedFilter == "ALL",
            onClick = { onSelectFilter("ALL") },
            modifier = Modifier.testTag("filter_all_button")
        )
        FilterPill(
            label = "Gamepads ($gamepadCount)",
            isSelected = selectedFilter == "GAMEPADS",
            onClick = { onSelectFilter("GAMEPADS") },
            icon = Icons.Default.SportsEsports,
            modifier = Modifier.testTag("filter_gamepads_button")
        )
        FilterPill(
            label = "Host PCs ($hostCount)",
            isSelected = selectedFilter == "HOSTS",
            onClick = { onSelectFilter("HOSTS") },
            icon = Icons.Default.DesktopWindows,
            modifier = Modifier.testTag("filter_hosts_button")
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val bg = if (isSelected) PrimaryContainerBlue else SurfaceControl
    val textColor = if (isSelected) Color.White else TextSecondary
    val borderCol = if (isSelected) ControlBorderGlow else Color.Transparent

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Individual Discovered Device Card with 'Connect' Button
 */
@Composable
private fun DiscoveredDeviceItemCard(
    device: DeviceTarget,
    isSelected: Boolean,
    isConnecting: Boolean,
    onCardClick: () -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            device.isConnected -> StatusSuccess
            isSelected -> ControlBorderGlow
            else -> ControlBorderSubtle.copy(alpha = 0.4f)
        },
        label = "cardBorderColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) SurfaceContainerHigh else SurfaceCard)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onCardClick() }
            .padding(14.dp)
            .testTag("device_card_${device.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: Brand Icon, Name, Type, Battery, Signal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Device Avatar / Controller Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (device.type) {
                                DeviceType.GAMEPAD -> ActiveControlFill
                                DeviceType.STEAM_DECK -> SurfaceControlRaised
                                else -> SurfaceControl
                            }
                        )
                        .border(
                            1.dp,
                            if (device.isConnected) StatusSuccess else ControlBorderSubtle.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (device.type) {
                            DeviceType.GAMEPAD -> Icons.Default.SportsEsports
                            DeviceType.STEAM_DECK -> Icons.Default.VideogameAsset
                            DeviceType.LAPTOP -> Icons.Default.LaptopChromebook
                            else -> Icons.Default.DesktopWindows
                        },
                        contentDescription = null,
                        tint = when (device.type) {
                            DeviceType.GAMEPAD -> XboxGreenA
                            DeviceType.STEAM_DECK -> XboxYellowY
                            DeviceType.LAPTOP -> XboxBlueX
                            else -> PrimaryBlue
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title, Subtitle & MAC
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Connected indicator dot
                        if (device.isConnected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StatusSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = StatusSuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = device.subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    // Hardware MAC & Protocol Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.macAddress,
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "•",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = device.protocol,
                            color = PrimaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Right Badges: RSSI Signal & Battery
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Signal dBm Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceControl)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sigColor = when {
                        device.rssiDbm >= -55 -> XboxGreenA
                        device.rssiDbm >= -70 -> XboxBlueX
                        else -> XboxYellowY
                    }
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = "Signal RSSI",
                        tint = sigColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${device.rssiDbm} dBm",
                        color = sigColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Battery badge if available
                if (device.batteryPct != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceControl)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (device.batteryPct >= 80) Icons.Default.BatteryFull else Icons.Default.BatteryStd,
                            contentDescription = "Battery Level",
                            tint = if (device.batteryPct > 20) TextSecondary else StatusError,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${device.batteryPct}%",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom Row: Status info & The 'Connect' Button
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
                        .background(
                            when {
                                device.isConnected -> StatusSuccess
                                device.isPaired -> PrimaryBlue
                                else -> XboxYellowY
                            }
                        )
                )
                Text(
                    text = when {
                        device.isConnected -> "Linked • 1000Hz HID Stream Active"
                        device.isPaired -> "Paired in Device Memory"
                        else -> "Discovered • Ready to Pair"
                    },
                    color = when {
                        device.isConnected -> StatusSuccess
                        device.isPaired -> PrimaryBlue
                        else -> TextTertiary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Dedicated 'Connect' Button (Initiates pairing or disconnects)
            if (device.isConnected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("disconnect_button_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = "Disconnect",
                        tint = StatusError,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disconnect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    enabled = !isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isPaired) PrimaryContainerBlue else XboxGreenA
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(10.dp),
                            spotColor = if (device.isPaired) PrimaryContainerBlue else XboxGreenA
                        )
                        .testTag("connect_button_${device.id}")
                        .testTag("connect_gamepad_button")
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pairing...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = if (device.isPaired) Icons.Default.BluetoothConnected else Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (device.isPaired) "Connect" else "Pair & Connect",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Action Dock for Selected Gamepad / Host
 */
@Composable
private fun SelectedDeviceActionDock(
    device: DeviceTarget,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(SurfaceControlRaised, SurfaceDefault)
                )
            )
            .border(1.dp, ControlBorderGlow.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("selected_gamepad_action_dock"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "TARGET: ${device.name}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${device.connectionType} • RSSI: ${device.rssiDbm} dBm • Latency < 4ms",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Primary Connect Button in Action Dock
        if (device.isConnected) {
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("primary_disconnect_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Disconnect", color = StatusError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                colors = ButtonDefaults.buttonColors(containerColor = XboxGreenA),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .shadow(10.dp, RoundedCornerShape(10.dp), spotColor = XboxGreenA)
                    .testTag("primary_connect_action_button")
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pairing Handshake...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.BluetoothConnected,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connect to Gamepad",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Gamepad Bluetooth Pairing Guide Card
 */
@Composable
private fun GamepadPairingGuideCard(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh)
            .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "BLUETOOTH GAMEPAD PAIRING GUIDE",
                    color = PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Close",
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onDismiss() }
            )
        }

        Text(
            text = "• Xbox Controller: Turn on with Xbox button, then press and hold the small Pair button on top for 3 seconds until the Xbox button flashes rapidly.\n" +
                    "• PlayStation DualSense: Press and hold PS Button + Create/Share Button together until the light bar blinks in bursts.\n" +
                    "• Nintendo Switch Pro: Press and hold the small Sync button on the top edge near the USB-C port until bottom LEDs cycle.\n" +
                    "• 8BitDo: Turn switch to 'B' (Bluetooth) and hold the Pair button on the top edge for 3 seconds.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * Empty Discovered Devices Card
 */
@Composable
private fun EmptyDiscoveredDevicesCard(
    isScanning: Boolean,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothSearching,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = if (isScanning) "Searching for nearby gamepads..." else "No devices discovered yet",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Make sure your controller is in pairing mode and within 10 meters.",
            color = TextSecondary,
            fontSize = 12.sp
        )
        if (!isScanning) {
            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Start Discovery Scan", fontSize = 12.sp)
            }
        }
    }
}
