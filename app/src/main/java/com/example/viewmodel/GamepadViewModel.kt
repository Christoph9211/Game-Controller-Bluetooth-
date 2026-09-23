package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.GamepadDatabase
import com.example.data.local.LayoutConfigEntity
import com.example.data.local.PairedDeviceEntity
import com.example.data.model.ControllerElementId
import com.example.data.model.ControllerLayoutProfile
import com.example.data.model.DeviceTarget
import com.example.data.model.DeviceType
import com.example.data.model.ElementLayoutConfig
import com.example.data.model.GamepadScreen
import com.example.data.model.QuickActionsSettings
import com.example.data.model.StickStylePreset
import com.example.data.model.TelemetryData
import com.example.util.HapticFeedbackManager
import com.example.util.HapticMotorChannel
import com.example.util.HapticTelemetryEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    val hapticManager = HapticFeedbackManager(application)
    val lastHapticEvent: StateFlow<HapticTelemetryEvent?> = hapticManager.lastHapticEvent

    private var leftStickPrevDist = 0f
    private var rightStickPrevDist = 0f

    private val db = Room.databaseBuilder(
        application,
        GamepadDatabase::class.java,
        "gamepad_db"
    ).build()

    // Navigation state
    private val _currentScreen = MutableStateFlow(GamepadScreen.CONTROLLER)
    val currentScreen: StateFlow<GamepadScreen> = _currentScreen.asStateFlow()

    private val _isQuickDrawerOpen = MutableStateFlow(false)
    val isQuickDrawerOpen: StateFlow<Boolean> = _isQuickDrawerOpen.asStateFlow()

    // Layout configuration state
    private val _activeLayout = MutableStateFlow(ControllerLayoutProfile())
    val activeLayout: StateFlow<ControllerLayoutProfile> = _activeLayout.asStateFlow()

    // Working copy for Customize Layout Editor
    private val _editingLayout = MutableStateFlow(ControllerLayoutProfile())
    val editingLayout: StateFlow<ControllerLayoutProfile> = _editingLayout.asStateFlow()

    private val _selectedElementId = MutableStateFlow(ControllerElementId.RIGHT_STICK)
    val selectedElementId: StateFlow<ControllerElementId> = _selectedElementId.asStateFlow()

    private val _editorActiveTab = MutableStateFlow("layout") // "layout", "mapping", "sticks"
    val editorActiveTab: StateFlow<String> = _editorActiveTab.asStateFlow()

    // Telemetry state
    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    // Quick Settings
    private val _quickSettings = MutableStateFlow(QuickActionsSettings())
    val quickSettings: StateFlow<QuickActionsSettings> = _quickSettings.asStateFlow()

    // Device Discovery Scanner state
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceTarget>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceTarget>> = _discoveredDevices.asStateFlow()

    private val _pairedHosts = MutableStateFlow<List<DeviceTarget>>(emptyList())
    val pairedHosts: StateFlow<List<DeviceTarget>> = _pairedHosts.asStateFlow()

    // Toast message for layout saved or status
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Live controller input states (for HUD & diagnostics)
    private val _leftStickPos = MutableStateFlow(Pair(0f, 0f))
    val leftStickPos: StateFlow<Pair<Float, Float>> = _leftStickPos.asStateFlow()

    private val _rightStickPos = MutableStateFlow(Pair(0f, 0f))
    val rightStickPos: StateFlow<Pair<Float, Float>> = _rightStickPos.asStateFlow()

    private val _ltPressure = MutableStateFlow(0f)
    val ltPressure: StateFlow<Float> = _ltPressure.asStateFlow()

    private val _rtPressure = MutableStateFlow(0f)
    val rtPressure: StateFlow<Float> = _rtPressure.asStateFlow()

    private val _lastPressedButton = MutableStateFlow<String?>(null)
    val lastPressedButton: StateFlow<String?> = _lastPressedButton.asStateFlow()

    init {
        initInitialData()
        startTelemetryLoop()
    }

    private fun initInitialData() {
        val initialPaired = listOf(
            DeviceTarget(
                id = "host_1",
                name = "Custom Rig (RTX 4090)",
                subtitle = "Windows Bridge • 125 Hz Stream",
                type = DeviceType.PC,
                connectionType = "Windows Bridge · Xbox",
                rssiDbm = -42,
                isPaired = true,
                isConnected = true,
                streamRate = "125 Hz Stream",
                lastSeenOrConnected = "Active Now (Current Session)"
            ),
            DeviceTarget(
                id = "host_2",
                name = "Living Room Media PC",
                subtitle = "Direct Android HID",
                type = DeviceType.PC,
                connectionType = "Direct Android HID",
                rssiDbm = -58,
                isPaired = true,
                isConnected = false,
                streamRate = "Ready",
                lastSeenOrConnected = "Yesterday at 11:24 PM"
            ),
            DeviceTarget(
                id = "host_3",
                name = "Alienware M16 Laptop",
                subtitle = "Direct Bluetooth HID • Ready",
                type = DeviceType.LAPTOP,
                connectionType = "Direct Bluetooth HID",
                rssiDbm = -62,
                isPaired = true,
                isConnected = false,
                streamRate = "Ready",
                lastSeenOrConnected = "Oct 24, 2024 · 4:15 PM"
            ),
            DeviceTarget(
                id = "host_4",
                name = "Steam Deck OLED (Docked)",
                subtitle = "Steam Input Target • -74 dBm",
                type = DeviceType.STEAM_DECK,
                connectionType = "Direct Bluetooth HID",
                rssiDbm = -74,
                isPaired = true,
                isConnected = false,
                streamRate = "Standby",
                lastSeenOrConnected = "Oct 18, 2024 · 8:30 PM"
            )
        )
        _pairedHosts.value = initialPaired
        _discoveredDevices.value = initialPaired

        // Load saved layout from Room if available
        viewModelScope.launch {
            val saved = db.layoutDao().getAllConfigsSync()
            if (saved.isNotEmpty()) {
                val map = saved.associate { entity ->
                    val id = ControllerElementId.valueOf(entity.elementIdString)
                    val preset = try {
                        StickStylePreset.valueOf(entity.stylePresetString)
                    } catch (e: Exception) {
                        StickStylePreset.HALO
                    }
                    id to ElementLayoutConfig(
                        elementId = id,
                        xPercent = entity.xPercent,
                        yPercent = entity.yPercent,
                        scale = entity.scale,
                        stylePreset = preset
                    )
                }
                val loadedProfile = ControllerLayoutProfile(
                    id = "custom_profile",
                    name = "Custom Layout",
                    isCustom = true,
                    elements = map
                )
                _activeLayout.value = loadedProfile
                _editingLayout.value = loadedProfile
            }
        }
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (true) {
                delay(2000)
                // Ping jitter simulation
                val jitter = (3.4f + Random.nextFloat() * 0.8f)
                val roundedPing = (jitter * 10).toInt() / 10f
                _telemetry.update { old ->
                    old.copy(
                        roundtripMs = roundedPing,
                        jitterMs = (Random.nextFloat() * 0.4f),
                        totalPackets = old.totalPackets + Random.nextInt(240, 260)
                    )
                }
            }
        }
    }

    // Navigation functions
    fun navigateTo(screen: GamepadScreen) {
        if (screen == GamepadScreen.CUSTOMIZE_LAYOUT) {
            // Sync editing layout with active layout when opening editor
            _editingLayout.value = _activeLayout.value
        }
        _currentScreen.value = screen
        _isQuickDrawerOpen.value = false
    }

    fun openQuickDrawer() {
        _isQuickDrawerOpen.value = true
    }

    fun closeQuickDrawer() {
        _isQuickDrawerOpen.value = false
    }

    // Layout Editor interactions
    fun selectElement(id: ControllerElementId) {
        _selectedElementId.value = id
    }

    fun setEditorTab(tab: String) {
        _editorActiveTab.value = tab
    }

    fun updateElementPosition(id: ControllerElementId, xPercent: Float, yPercent: Float) {
        val clampedX = xPercent.coerceIn(2f, 94f)
        val clampedY = yPercent.coerceIn(2f, 92f)
        _editingLayout.update { current ->
            val updatedMap = current.elements.toMutableMap()
            val existing = updatedMap[id] ?: ElementLayoutConfig(id, clampedX, clampedY)
            updatedMap[id] = existing.copy(xPercent = clampedX, yPercent = clampedY)
            current.copy(elements = updatedMap)
        }
    }

    fun updateElementScale(id: ControllerElementId, scale: Float) {
        val clampedScale = scale.coerceIn(0.8f, 1.4f)
        _editingLayout.update { current ->
            val updatedMap = current.elements.toMutableMap()
            val existing = updatedMap[id] ?: ElementLayoutConfig(id, 50f, 50f)
            updatedMap[id] = existing.copy(scale = clampedScale)
            current.copy(elements = updatedMap)
        }
    }

    fun updateElementStyle(id: ControllerElementId, style: StickStylePreset) {
        _editingLayout.update { current ->
            val updatedMap = current.elements.toMutableMap()
            val existing = updatedMap[id] ?: ElementLayoutConfig(id, 50f, 50f)
            updatedMap[id] = existing.copy(stylePreset = style)
            current.copy(elements = updatedMap)
        }
    }

    fun resetLayoutToDefault() {
        val defaultProfile = ControllerLayoutProfile()
        _editingLayout.value = defaultProfile
        showToast("Controls restored to default ergonomics")
    }

    fun saveLayout() {
        val currentEdit = _editingLayout.value
        _activeLayout.value = currentEdit.copy(isCustom = true, name = "Custom Layout")
        showToast("Controller preset saved to device memory")

        // Persist to Room
        viewModelScope.launch {
            val entities = currentEdit.elements.values.map {
                LayoutConfigEntity(
                    elementIdString = it.elementId.name,
                    xPercent = it.xPercent,
                    yPercent = it.yPercent,
                    scale = it.scale,
                    stylePresetString = it.stylePreset.name
                )
            }
            db.layoutDao().insertConfigs(entities)
        }
    }

    fun cancelLayoutEdit() {
        _editingLayout.value = _activeLayout.value
        showToast("Changes discarded")
        _currentScreen.value = GamepadScreen.CONTROLLER
    }

    // Input handlers
    fun onLeftStickMoved(x: Float, y: Float) {
        _leftStickPos.value = Pair(x, y)
        val dist = sqrt(x * x + y * y)
        val settings = _quickSettings.value
        if (settings.stickPerimeterHaptic && dist >= 0.94f && leftStickPrevDist < 0.94f) {
            hapticManager.performStickPerimeterBump(settings.hapticsEnabled, settings.hapticStrength)
        } else if (leftStickPrevDist > 0.18f && dist <= 0.05f) {
            hapticManager.performStickCenterSnap(settings.hapticsEnabled, settings.hapticStrength)
        }
        leftStickPrevDist = dist
    }

    fun onRightStickMoved(x: Float, y: Float) {
        _rightStickPos.value = Pair(x, y)
        val dist = sqrt(x * x + y * y)
        val settings = _quickSettings.value
        if (settings.stickPerimeterHaptic && dist >= 0.94f && rightStickPrevDist < 0.94f) {
            hapticManager.performStickPerimeterBump(settings.hapticsEnabled, settings.hapticStrength)
        } else if (rightStickPrevDist > 0.18f && dist <= 0.05f) {
            hapticManager.performStickCenterSnap(settings.hapticsEnabled, settings.hapticStrength)
        }
        rightStickPrevDist = dist
    }

    fun onTriggerChanged(isLeft: Boolean, pressure: Float) {
        val prevPressure = if (isLeft) _ltPressure.value else _rtPressure.value
        val triggerName = if (isLeft) "LT" else "RT"
        val settings = _quickSettings.value

        if (isLeft) {
            _ltPressure.value = pressure
            _telemetry.update { it.copy(ltPressure = (pressure * 255).toInt()) }
        } else {
            _rtPressure.value = pressure
            _telemetry.update { it.copy(rtPressure = (pressure * 255).toInt()) }
        }

        if (pressure >= 0.95f && prevPressure < 0.95f) {
            hapticManager.performTriggerBottomOut(settings.hapticsEnabled, settings.hapticStrength, triggerName)
        } else if (settings.triggerResistanceHaptic && (
            (prevPressure < 0.33f && pressure >= 0.33f) ||
            (prevPressure < 0.66f && pressure >= 0.66f)
        )) {
            hapticManager.performTriggerStep(settings.hapticsEnabled, settings.hapticStrength, triggerName)
        }
    }

    fun onButtonPressed(buttonName: String) {
        _lastPressedButton.value = buttonName
        val settings = _quickSettings.value
        if (buttonName.startsWith("LB") || buttonName.startsWith("RB")) {
            hapticManager.performBumperPress(settings.hapticsEnabled, settings.hapticStrength, buttonName)
        } else if (buttonName == "L3" || buttonName == "R3") {
            hapticManager.performStickClick(settings.hapticsEnabled, settings.hapticStrength, buttonName)
        } else {
            hapticManager.performButtonPress(settings.hapticsEnabled, settings.hapticStrength, buttonName)
        }
        if (settings.turboEnabled) {
            hapticManager.performTurboPulse(settings.hapticsEnabled, settings.hapticStrength)
        }
    }

    fun testHapticMotor(channel: HapticMotorChannel) {
        hapticManager.performMotorRumble(
            enabled = true,
            channel = channel,
            strength = _quickSettings.value.hapticStrength
        )
    }

    fun testHapticEffect(effectType: String) {
        val settings = _quickSettings.value
        when (effectType) {
            "button" -> hapticManager.performButtonPress(true, settings.hapticStrength, "Test Button")
            "bumper" -> hapticManager.performBumperPress(true, settings.hapticStrength, "Test Bumper")
            "trigger_step" -> hapticManager.performTriggerStep(true, settings.hapticStrength, "Test LT/RT")
            "trigger_bottom" -> hapticManager.performTriggerBottomOut(true, settings.hapticStrength, "Test LT/RT")
            "stick_click" -> hapticManager.performStickClick(true, settings.hapticStrength, "Test L3/R3")
            "stick_perimeter" -> hapticManager.performStickPerimeterBump(true, settings.hapticStrength)
            "stick_deadzone" -> hapticManager.performStickCenterSnap(true, settings.hapticStrength)
            "turbo" -> hapticManager.performTurboPulse(true, settings.hapticStrength)
        }
    }

    fun setHapticProfile(profile: String) {
        _quickSettings.update { it.copy(hapticProfile = profile) }
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
        showToast("Haptic Profile: $profile")
    }

    fun toggleTriggerResistanceHaptic() {
        _quickSettings.update { it.copy(triggerResistanceHaptic = !it.triggerResistanceHaptic) }
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    fun toggleStickPerimeterHaptic() {
        _quickSettings.update { it.copy(stickPerimeterHaptic = !it.stickPerimeterHaptic) }
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    // Device Discovery Scanner functions
    fun toggleDiscoveryScan() {
        _isScanning.value = !_isScanning.value
        if (_isScanning.value) {
            showToast("Searching for discoverable Bluetooth targets...")
        } else {
            showToast("Discovery scanning paused")
        }
    }

    fun connectToDevice(targetId: String) {
        _pairedHosts.update { list ->
            list.map { item ->
                item.copy(isConnected = item.id == targetId)
            }
        }
        val target = _pairedHosts.value.find { it.id == targetId }
        if (target != null) {
            _telemetry.update {
                it.copy(
                    hostName = target.name,
                    linkState = "HID Connected",
                    rfDbm = target.rssiDbm
                )
            }
            showToast("Connected to ${target.name}")
        }
    }

    fun disconnectActiveDevice() {
        _pairedHosts.update { list ->
            list.map { it.copy(isConnected = false) }
        }
        _telemetry.update { it.copy(linkState = "Disconnected") }
        showToast("Disconnected from host")
    }

    // Quick Settings
    fun updateProfile(profileName: String) {
        _quickSettings.update { it.copy(currentProfile = profileName) }
        _telemetry.update { it.copy(profileName = profileName) }
        showToast("Profile switched to $profileName")
    }

    fun toggleHaptics() {
        _quickSettings.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
        showToast("Haptics: ${if (_quickSettings.value.hapticsEnabled) "Enabled" else "Disabled"}")
    }

    fun setHapticStrength(strength: Float) {
        _quickSettings.update { it.copy(hapticStrength = strength) }
    }

    fun toggleGyro() {
        _quickSettings.update { it.copy(gyroAimEnabled = !it.gyroAimEnabled) }
        showToast("Gyro Aim: ${if (_quickSettings.value.gyroAimEnabled) "Enabled" else "Disabled"}")
    }

    fun toggleTurbo() {
        _quickSettings.update { it.copy(turboEnabled = !it.turboEnabled) }
        showToast("Turbo Mode: ${if (_quickSettings.value.turboEnabled) "Active" else "Off"}")
    }

    fun setDeadzone(pct: Int) {
        _quickSettings.update { it.copy(deadzonePct = pct) }
    }

    fun setPollingRate(hz: Int) {
        _quickSettings.update { it.copy(pollRateHz = hz) }
        _telemetry.update { it.copy(samplingHz = hz) }
        showToast("Polling rate set to ${hz}Hz")
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(2200)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
