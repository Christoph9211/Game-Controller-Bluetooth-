package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: GamepadViewModel = viewModel()
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
                            DeviceDiscoveryScreen(viewModel = viewModel)
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
