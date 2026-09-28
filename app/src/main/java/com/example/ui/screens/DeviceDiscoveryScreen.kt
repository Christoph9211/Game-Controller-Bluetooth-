package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.data.model.GamepadScreen
import com.example.ui.theme.*
import com.example.viewmodel.GamepadViewModel

@Composable
fun DeviceDiscoveryScreen(viewModel: GamepadViewModel, modifier: Modifier = Modifier,
    onStartBluetooth: () -> Unit = {}, onPairDevice: () -> Unit = {}) {
    val state by viewModel.connection.collectAsState()
    Column(modifier.fillMaxSize().background(SurfaceCanvas).verticalScroll(rememberScrollState())
        .padding(16.dp).testTag("device_discovery_screen"), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={ viewModel.navigateTo(GamepadScreen.CONTROLLER) }) { Text("Back to controller") }
        Text("Bluetooth connection",style=MaterialTheme.typography.headlineSmall,color=TextPrimary)
        StatusCard("Controller status") {
            Text(state.status,color=ControlBorderGlow)
            Text("Paired and HID connected are separate states. Receiver gameplay is not confirmed by a connection.",color=TextSecondary)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=onStartBluetooth,enabled=!state.sessionOpen && !state.stopping) { Text("Start Bluetooth") }
                OutlinedButton(onClick={ viewModel.stopBluetooth() },enabled=state.sessionOpen && !state.stopping) { Text("Stop") }
            }
            if (state.connected) OutlinedButton(onClick={ viewModel.disconnectActiveDevice() }) { Text("Disconnect host") }
        }
        StatusCard("Host mode") {
            Text(if (state.pcMode) "Windows bridge (experimental)" else "Android / generic HID",color=TextPrimary)
            Switch(checked=state.pcMode,onCheckedChange={ viewModel.setHostMode(it) },
                modifier=Modifier.semantics { contentDescription="Windows bridge mode (experimental)" },
                enabled=!state.sessionOpen && !state.stopping)
            Text("Stop Bluetooth before switching. Forget the pairing on both devices, then pair again after changing modes.",color=TextSecondary)
            if (state.pcMode) Text("Requires the existing PhoneGamepadBridge.exe companion on Windows. Start its bridge after connecting. The companion and virtual-controller driver are separate, experimental software.",color=TextSecondary)
        }
        StatusCard("Pair a receiving device") {
            Text("""1. Start Bluetooth and wait for HID registration.
2. Make this phone discoverable.
3. On the receiving phone or PC, open Bluetooth settings and pair with this phone.
4. Refresh below and connect the paired host.""",color=TextSecondary)
            Button(onClick=onPairDevice,enabled=state.ready && !state.connected && !state.stopping) { Text("Make phone discoverable") }
            TextButton(onClick={ viewModel.bluetooth.refresh() }) { Text("Refresh paired hosts") }
        }
        Text("Paired hosts",style=MaterialTheme.typography.titleLarge,color=TextPrimary)
        if (state.hosts.isEmpty()) Text("No paired hosts available. Grant Nearby devices permission and pair from the receiver.",color=TextSecondary)
        state.hosts.forEach { host ->
            StatusCard(host.name) {
                Text(host.address,color=TextSecondary)
                val connected=state.connected && state.hostAddress==host.address
                val connecting=state.connectingAddress==host.address
                Text(if (connected) "HID connected" else if (connecting) "Connecting…" else "Paired · Not connected",color=ControlBorderGlow)
                Button(onClick={ viewModel.connectToDevice(host.address) },
                    enabled=state.ready && !state.connected && state.connectingAddress==null && !state.stopping) { Text("Connect") }
            }
        }
    }
}
