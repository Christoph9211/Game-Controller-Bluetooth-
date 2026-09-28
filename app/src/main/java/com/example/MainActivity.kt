package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.LaunchedEffect
import com.example.bluetooth.BluetoothConnection
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GamepadScreen
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.CustomizeLayoutScreen
import com.example.ui.screens.DeviceDiscoveryScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.QuickActionsDrawer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceCanvas
import com.example.viewmodel.GamepadViewModel

class MainActivity : ComponentActivity() {
    private val controller by lazy { ViewModelProvider(this)[GamepadViewModel::class.java] }
    private var resumed=false
    private val nearbyRequest=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (controller.bluetooth.hasPermissions()) startBluetooth()
        else controller.bluetooth.error("Nearby devices permission denied. Allow it in app settings to connect.")
    }
    private val enableRequest=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode==RESULT_OK) startBluetooth()
        else controller.bluetooth.error("Bluetooth enable request cancelled")
    }
    private val visibilityRequest=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        controller.bluetooth.refresh()
    }
    private val notificationRequest=registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private fun startBluetooth() {
        controller.releaseControls()
        if (!controller.bluetooth.hasPermissions()) { nearbyRequest.launch(BluetoothConnection.permissions); return }
        try {
            val adapter=controller.bluetooth.adapter
            if (adapter==null) { controller.bluetooth.error("This device has no Bluetooth adapter"); return }
            if (!adapter.isEnabled) { enableRequest.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return }
            controller.bluetooth.start()
            if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (e: RuntimeException) { controller.bluetooth.error("Cannot start Bluetooth: ${e.javaClass.simpleName}") }
    }
    private fun pairDevice() {
        controller.releaseControls()
        if (!controller.connection.value.ready || !controller.bluetooth.hasPermissions()) return
        try {
            visibilityRequest.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120))
        } catch (e: RuntimeException) { controller.bluetooth.error("Cannot request visibility: ${e.javaClass.simpleName}") }
    }
    override fun onResume() { super.onResume(); resumed=true; controller.setForeground(hasWindowFocus()) }
    override fun onPause() { resumed=false; controller.setForeground(false); super.onPause() }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        controller.setForeground(resumed && hasFocus)
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        controller.releaseControls(); super.onConfigurationChanged(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            MyApplicationTheme {
                val viewModel = controller
                val toast by viewModel.toastMessage.collectAsState()
                LaunchedEffect(toast) { toast?.let { Toast.makeText(this@MainActivity,it,Toast.LENGTH_SHORT).show() } }
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isDrawerOpen by viewModel.isQuickDrawerOpen.collectAsState()

                // Handle system back navigation
                BackHandler(enabled = isDrawerOpen || currentScreen != GamepadScreen.CONTROLLER) {
                    if (isDrawerOpen) {
                        viewModel.closeQuickDrawer()
                    } else if (currentScreen != GamepadScreen.CONTROLLER) {
                        viewModel.navigateTo(GamepadScreen.CONTROLLER)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCanvas)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    when (currentScreen) {
                        GamepadScreen.CONTROLLER -> {
                            ControllerScreen(viewModel = viewModel)
                        }
                        GamepadScreen.CUSTOMIZE_LAYOUT -> {
                            CustomizeLayoutScreen(viewModel = viewModel)
                        }
                        GamepadScreen.DEVICE_DISCOVERY -> {
                            DeviceDiscoveryScreen(viewModel = viewModel, onStartBluetooth=::startBluetooth, onPairDevice=::pairDevice)
                        }
                        GamepadScreen.DIAGNOSTICS -> {
                            DiagnosticsScreen(viewModel = viewModel)
                        }
                    }

                    // Quick Actions Drawer Overlay
                    QuickActionsDrawer(
                        viewModel = viewModel,
                        isOpen = isDrawerOpen,
                        onClose = { viewModel.closeQuickDrawer() }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
