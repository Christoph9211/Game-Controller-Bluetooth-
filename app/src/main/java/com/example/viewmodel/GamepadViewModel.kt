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

    private val _snapGridMode = MutableStateFlow("8DP") // "8DP", "16DP", "OFF"
    val snapGridMode: StateFlow<String> = _snapGridMode.asStateFlow()

    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    // Telemetry state
    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    // Quick Settings
    private val _quickSettings = MutableStateFlow(QuickActionsSettings())
    val quickSettings: StateFlow<QuickActionsSettings> = _quickSettings.asStateFlow()

    // Device Discovery Scanner state
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectingDeviceId = MutableStateFlow<String?>(null)
    val connectingDeviceId: StateFlow<String?> = _connectingDeviceId.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>("pad_xbox_1")
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _deviceFilter = MutableStateFlow("ALL") // "ALL", "GAMEPADS", "HOSTS"
    val deviceFilter: StateFlow<String> = _deviceFilter.asStateFlow()

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
        val initialDiscovered = listOf(
            DeviceTarget(
                id = "pad_xbox_1",
                name = "Xbox Wireless Controller",
                subtitle = "Bluetooth LE HID • Carbon Black",
                type = DeviceType.GAMEPAD,
                connectionType = "BLE HID / XInput",
                rssiDbm = -46,
                isPaired = false,
                isConnected = false,
                streamRate = "Ready to Pair",
                lastSeenOrConnected = "Active Beacon (Signal Strong)",
                batteryPct = 88,
                macAddress = "4C:0B:BE:91:2E:7A",
                protocol = "BLE HID / XInput"
            ),
            DeviceTarget(
                id = "pad_dualsense_1",
                name = "DualSense Wireless Controller",
                subtitle = "PlayStation HID • Haptics & Gyro Supported",
                type = DeviceType.GAMEPAD,
                connectionType = "Direct Bluetooth HID",
                rssiDbm = -52,
                isPaired = false,
                isConnected = false,
                streamRate = "Ready to Pair",
                lastSeenOrConnected = "Active Beacon",
                batteryPct = 74,
                macAddress = "00:1B:DC:04:78:E2",
                protocol = "PlayStation Bluetooth HID"
            ),
            DeviceTarget(
                id = "pad_8bitdo_1",
                name = "8BitDo Ultimate Bluetooth",
                subtitle = "Hall Effect Sticks • 1000Hz Polling",
                type = DeviceType.GAMEPAD,
                connectionType = "Bluetooth 5.0 Low Latency",
                rssiDbm = -48,
                isPaired = false,
                isConnected = false,
                streamRate = "Ready to Pair",
                lastSeenOrConnected = "Discovered Just Now",
                batteryPct = 84,
                macAddress = "E4:5F:01:8A:2C:4D",
                protocol = "Bluetooth 5.0 LL"
            ),
            DeviceTarget(
                id = "pad_switch_1",
                name = "Nintendo Switch Pro Controller",
                subtitle = "Switch HID • Motion Sensors & HD Rumble",
                type = DeviceType.GAMEPAD,
                connectionType = "Switch Bluetooth HID",
                rssiDbm = -64,
                isPaired = false,
                isConnected = false,
                streamRate = "Ready to Pair",
                lastSeenOrConnected = "Discovered 1m ago",
                batteryPct = 95,
                macAddress = "98:B6:E9:12:F4:30",
                protocol = "Nintendo HID Protocol"
            ),
            DeviceTarget(
                id = "pad_razer_1",
                name = "Razer Kishi V2 Pro",
                subtitle = "Mobile Gamepad • Microswitches & Analog Hall",
                type = DeviceType.GAMEPAD,
                connectionType = "Direct BLE Gamepad",
                rssiDbm = -39,
                isPaired = false,
                isConnected = false,
                streamRate = "Ready to Pair",
                lastSeenOrConnected = "Discovered Just Now",
                batteryPct = 100,
                macAddress = "2C:F0:5D:87:11:AB",
                protocol = "Direct BLE Gamepad"
            ),
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
                lastSeenOrConnected = "Active Now (Current Session)",
                batteryPct = null,
                macAddress = "94:E6:F7:2B:90:1C",
                protocol = "Windows Bridge HID"
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
                lastSeenOrConnected = "Oct 18, 2024 · 8:30 PM",
                batteryPct = 68,
                macAddress = "B0:D5:9D:6C:3E:91",
                protocol = "Steam Remote HID"
            )
        )
        _discoveredDevices.value = initialDiscovered
        _pairedHosts.value = initialDiscovered.filter { it.isPaired }

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

    fun setSnapGridMode(mode: String) {
        _snapGridMode.value = mode
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    fun toggleTestMode() {
        _isTestMode.value = !_isTestMode.value
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
        showToast(if (_isTestMode.value) "Live Test Mode: test controls directly!" else "Blueprint Editor Mode")
    }

    fun addElement(id: ControllerElementId, xPercent: Float, yPercent: Float, scale: Float = 1.0f) {
        val clampedX = xPercent.coerceIn(2f, 94f)
        val clampedY = yPercent.coerceIn(2f, 92f)
        _editingLayout.update { current ->
            val updatedMap = current.elements.toMutableMap()
            updatedMap[id] = ElementLayoutConfig(
                elementId = id,
                xPercent = clampedX,
                yPercent = clampedY,
                scale = scale
            )
            current.copy(elements = updatedMap)
        }
        _selectedElementId.value = id
        hapticManager.performButtonPress(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength, id.displayName)
        showToast("Added ${id.displayName} to layout")
    }

    fun removeElement(id: ControllerElementId) {
        _editingLayout.update { current ->
            val updatedMap = current.elements.toMutableMap()
            updatedMap.remove(id)
            current.copy(elements = updatedMap)
        }
        val remaining = _editingLayout.value.elements.keys.firstOrNull()
        if (remaining != null) {
            _selectedElementId.value = remaining
        }
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
        showToast("Removed ${id.displayName}")
    }

    fun applyLayoutPreset(presetKey: String) {
        val newElements = when (presetKey) {
            "FPS" -> ControllerLayoutProfile.fpsLayoutElements()
            "FIGHTING" -> ControllerLayoutProfile.arcadeFightingLayoutElements()
            "SOUTHPAW" -> ControllerLayoutProfile.southpawLayoutElements()
            else -> ControllerLayoutProfile.defaultLayoutElements()
        }
        val presetName = when (presetKey) {
            "FPS" -> "FPS Pro Layout"
            "FIGHTING" -> "Arcade Fighter Layout"
            "SOUTHPAW" -> "Southpaw Layout"
            else -> "Default Asymmetric"
        }
        _editingLayout.value = ControllerLayoutProfile(
            id = "preset_$presetKey",
            name = presetName,
            isCustom = true,
            elements = newElements
        )
        hapticManager.performButtonPress(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength, presetName)
        showToast("Applied $presetName preset")
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

    fun selectDevice(targetId: String?) {
        _selectedDeviceId.value = targetId
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    fun setDeviceFilter(filter: String) {
        _deviceFilter.value = filter
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    fun initiatePairAndConnect(targetId: String) {
        val target = _discoveredDevices.value.find { it.id == targetId } ?: return
        if (target.isConnected) {
            disconnectActiveDevice()
            return
        }

        viewModelScope.launch {
            _connectingDeviceId.value = targetId
            hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
            showToast("Pairing with ${target.name}...")

            // Realistic connection handshake delay
            kotlinx.coroutines.delay(650)

            _discoveredDevices.update { list ->
                list.map { item ->
                    if (item.id == targetId) {
                        item.copy(isPaired = true, isConnected = true, streamRate = "Active HID Link")
                    } else {
                        item.copy(isConnected = false)
                    }
                }
            }

            _pairedHosts.update { list ->
                val existing = list.any { it.id == targetId }
                val updatedTarget = _discoveredDevices.value.first { it.id == targetId }
                if (existing) {
                    list.map { if (it.id == targetId) updatedTarget else it.copy(isConnected = false) }
                } else {
                    list.map { it.copy(isConnected = false) } + updatedTarget
                }
            }

            _telemetry.update {
                it.copy(
                    hostName = target.name,
                    linkState = "HID Active",
                    rfDbm = target.rssiDbm
                )
            }

            // Persist to Room
            db.deviceDao().insertDevice(
                com.example.data.local.PairedDeviceEntity(
                    id = target.id,
                    name = target.name,
                    subtitle = target.subtitle,
                    type = target.type.name,
                    connectionType = target.connectionType,
                    rssiDbm = target.rssiDbm,
                    isPaired = true,
                    isConnected = true,
                    streamRate = target.streamRate,
                    lastSeenOrConnected = "Active Now"
                )
            )

            _connectingDeviceId.value = null
            hapticManager.performMotorRumble(
                enabled = _quickSettings.value.hapticsEnabled,
                channel = com.example.util.HapticMotorChannel.DUAL_STEREO,
                strength = _quickSettings.value.hapticStrength,
                durationMs = 80L
            )
            showToast("Connected: ${target.name} paired successfully!")
        }
    }

    fun connectToDevice(targetId: String) {
        initiatePairAndConnect(targetId)
    }

    fun disconnectActiveDevice() {
        _discoveredDevices.update { list ->
            list.map { it.copy(isConnected = false) }
        }
        _pairedHosts.update { list ->
            list.map { it.copy(isConnected = false) }
        }
        _telemetry.update { it.copy(linkState = "Disconnected") }
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
        showToast("Disconnected from device")
        viewModelScope.launch {
            db.deviceDao().disconnectAll()
        }
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
