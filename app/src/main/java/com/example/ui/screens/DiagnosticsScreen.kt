package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GamepadScreen
import com.example.ui.theme.*
import com.example.viewmodel.GamepadViewModel

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.util.HapticMotorChannel

@Composable
fun DiagnosticsScreen(viewModel: GamepadViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.connection.collectAsState()
    val settings by viewModel.quickSettings.collectAsState()
    val haptic by viewModel.lastHapticEvent.collectAsState()
    val clipboard=LocalClipboardManager.current
    Column(modifier.fillMaxSize().background(SurfaceCanvas).verticalScroll(rememberScrollState())
        .padding(16.dp).testTag("diagnostics_screen"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={ viewModel.navigateTo(GamepadScreen.CONTROLLER) }) { Text("Back to controller") }
        Text("Diagnostics",style=MaterialTheme.typography.headlineSmall,color=TextPrimary)
        StatusCard("Bluetooth connection") {
            Text(state.status,color=ControlBorderGlow)
            Text("""RSSI: Unavailable
Round-trip latency: Unavailable
Receiver packet loss: Unavailable""",color=TextSecondary)
        }
        StatusCard("Local sender measurements") {
            Text("${settings.sendIntervalMs} ms analog scheduling interval. This is not a guaranteed radio rate or game latency.",color=TextSecondary)
            SelectionContainer { Text(state.diagnostics,color=TextPrimary) }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={ clipboard.setText(AnnotatedString(state.diagnostics)) }) { Text("Copy diagnostics") }
                OutlinedButton(onClick={ viewModel.bluetooth.service?.resetDiagnostics(); viewModel.bluetooth.refresh() }) { Text("Reset") }
            }
        }
        StatusCard("Phone haptics") {
            Text("Tests this phone’s vibrator only. No remote rumble or independent physical motor channels are reported.",color=TextSecondary)
            Button(onClick={ viewModel.testHapticMotor(HapticMotorChannel.DUAL_STEREO) }) { Text("Test phone vibration") }
            if (haptic != null) Text("Last local haptic: ${haptic!!.eventName}",color=TextSecondary)
        }
    }
}

@Composable
internal fun StatusCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceCard,RoundedCornerShape(16.dp))
        .border(1.dp,ControlBorderSubtle,RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium,color=TextPrimary)
        content()
    }
}
