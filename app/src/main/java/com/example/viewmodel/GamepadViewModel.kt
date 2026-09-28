package com.example.viewmodel

import android.app.Application
import com.example.bluetooth.BluetoothConnection
import com.example.bluetooth.ControllerInput
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.CustomLayoutEntity
import com.example.data.local.CustomLayoutRepository
import com.example.data.local.CustomLayoutSerializer
import com.example.data.local.GamepadDatabase
import com.example.data.local.LayoutConfigEntity
import com.example.data.model.ControllerElementId
import com.example.data.model.ControllerLayoutProfile
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt

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

    // Room Repository for Saved Layout Configurations
    val customLayoutRepository = CustomLayoutRepository(db.customLayoutDao())

    val savedLayouts: StateFlow<List<CustomLayoutEntity>> = customLayoutRepository.allLayouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSavedLayoutsManagerOpen = MutableStateFlow(false)
    val isSavedLayoutsManagerOpen: StateFlow<Boolean> = _isSavedLayoutsManagerOpen.asStateFlow()

    private val _activeSavedLayoutId = MutableStateFlow<String?>("preset_default")
    val activeSavedLayoutId: StateFlow<String?> = _activeSavedLayoutId.asStateFlow()

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

    val bluetooth = BluetoothConnection(application)
    val connection = bluetooth.state
    private val input = ControllerInput()
    private val _inputGeneration = MutableStateFlow(0)
    val inputGeneration = _inputGeneration.asStateFlow()
    private var foreground = false
    private var lastPulse = android.os.SystemClock.uptimeMillis()

    init {
        initInitialData()
        viewModelScope.launch {
            var previousHost: String? = null
            while (true) {
                val now=android.os.SystemClock.uptimeMillis()
                if (foreground && now-lastPulse>750) releaseControls()
                lastPulse=now
                if (foreground) bluetooth.service?.pulse()
                bluetooth.refresh()
                val state = connection.value
                if (previousHost != state.hostAddress) releaseControls()
                previousHost = state.hostAddress
                bluetooth.service?.setPeriod(_quickSettings.value.sendIntervalMs)
                _telemetry.update { it.copy(linkState=state.status,
                    hostName=state.hosts.firstOrNull { host -> host.address==state.hostAddress }?.name ?: "Not connected") }
                delay(100)
            }
        }
    }

    fun setForeground(active: Boolean) {
        foreground=active
        lastPulse=android.os.SystemClock.uptimeMillis()
        if (!active) releaseControls()
        bluetooth.refresh()
    }
    fun releaseControls() {
        input.reset()
        _leftStickPos.value=0f to 0f; _rightStickPos.value=0f to 0f
        _ltPressure.value=0f; _rtPressure.value=0f; _lastPressedButton.value=null
        leftStickPrevDist=0f; rightStickPrevDist=0f
        _telemetry.update { it.copy(ltPressure=0,rtPressure=0) }
        _inputGeneration.value++
        bluetooth.service?.release()
    }
    private fun acceptsInput(): Boolean {
        if (!foreground || _currentScreen.value!=GamepadScreen.CONTROLLER ||
            _isQuickDrawerOpen.value || _isSavedLayoutsManagerOpen.value) return false
        if (android.os.SystemClock.uptimeMillis()-lastPulse>750 ||
            bluetooth.service?.hostAddress()!=connection.value.hostAddress) {
            releaseControls()
            return false
        }
        return true
    }
    private fun submitInput() {
        bluetooth.service?.submit(input.report(_quickSettings.value.deadzonePct), null)
    }
    fun onButtonChanged(name: String, down: Boolean) {
        if (!acceptsInput()) return
        input.button(name,down)
        if (down) onButtonPressed(name) else if (_lastPressedButton.value==name) _lastPressedButton.value=null
        submitInput()
    }
    fun onHatChanged(hat: Int) {
        if (!acceptsInput()) return
        if (hat != 8 && hat != input.hat) onButtonPressed("D-pad")
        input.hat=if (hat in 0..7) hat else 8
        submitInput()
    }
    fun stopBluetooth() { releaseControls(); bluetooth.stop() }
    fun connectToDevice(address: String) { releaseControls(); bluetooth.connect(address) }
    fun disconnectActiveDevice() { releaseControls(); bluetooth.service?.disconnect() }
    fun setHostMode(pc: Boolean) { releaseControls(); bluetooth.setMode(pc) }
    override fun onCleared() { releaseControls(); bluetooth.stop(); db.close(); super.onCleared() }

    private fun initInitialData() {
        // Seed initial Room layouts if database is empty
        viewModelScope.launch {
            customLayoutRepository.seedInitialPresetsIfEmpty()
        }

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
                releaseControls()
                _activeLayout.value = loadedProfile
                _editingLayout.value = loadedProfile
            }
        }
    }

    // Navigation functions
    fun navigateTo(screen: GamepadScreen) {
        releaseControls()
        if (screen == GamepadScreen.CUSTOMIZE_LAYOUT) {
            // Sync editing layout with active layout when opening editor
            _editingLayout.value = _activeLayout.value
        }
        _currentScreen.value = screen
        _isQuickDrawerOpen.value = false
    }

    fun openQuickDrawer() {
        releaseControls()
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
        showToast(if (_isTestMode.value) "Local layout preview: no Bluetooth input is sent" else "Blueprint Editor Mode")
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
        releaseControls()
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
        releaseControls()
        val defaultProfile = ControllerLayoutProfile()
        _editingLayout.value = defaultProfile
        showToast("Controls restored to default ergonomics")
    }

    fun openSavedLayoutsManager() {
        releaseControls()
        _isSavedLayoutsManagerOpen.value = true
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    fun closeSavedLayoutsManager() {
        _isSavedLayoutsManagerOpen.value = false
    }

    fun saveCurrentLayoutAs(name: String, description: String = ""): kotlinx.coroutines.Job {
        val currentEdit = _editingLayout.value
        val trimmedName = name.trim().ifBlank { "Custom Layout (${System.currentTimeMillis() % 10000})" }
        val id = "layout_" + UUID.randomUUID().toString().take(8)
        val json = CustomLayoutSerializer.serialize(currentEdit.elements)
        val entity = CustomLayoutEntity(
            id = id,
            name = trimmedName,
            description = description.trim(),
            isPreset = false,
            elementCount = currentEdit.elements.size,
            elementsJson = json,
            lastModified = System.currentTimeMillis()
        )

        return viewModelScope.launch {
            customLayoutRepository.saveLayout(entity)
            _activeSavedLayoutId.value = id
            val newProfile = currentEdit.copy(id = id, name = trimmedName, isCustom = true)
            releaseControls()
            _activeLayout.value = newProfile
            _editingLayout.value = newProfile

            // Also persist to legacy configs table for backwards compatibility
            val entities = currentEdit.elements.values.map {
                LayoutConfigEntity(
                    elementIdString = it.elementId.name,
                    xPercent = it.xPercent,
                    yPercent = it.yPercent,
                    scale = it.scale,
                    stylePresetString = it.stylePreset.name
                )
            }
            db.layoutDao().replaceConfigs(entities)

            showToast("Saved \"$trimmedName\" to Room database")
            hapticManager.performButtonPress(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength, "Save")
        }
    }

    fun loadSavedLayout(layoutId: String): kotlinx.coroutines.Job = viewModelScope.launch {
        val entity = customLayoutRepository.getLayoutById(layoutId)
        releaseControls()
        if (entity != null) {
            val elements = CustomLayoutSerializer.deserialize(entity.elementsJson)
            val profile = ControllerLayoutProfile(
                id = entity.id,
                name = entity.name,
                isCustom = !entity.isPreset,
                elements = elements
            )
            _activeLayout.value = profile
            _editingLayout.value = profile
            _activeSavedLayoutId.value = entity.id

            // Also update legacy configs table
            val entities = elements.values.map {
                LayoutConfigEntity(
                    elementIdString = it.elementId.name,
                    xPercent = it.xPercent,
                    yPercent = it.yPercent,
                    scale = it.scale,
                    stylePresetString = it.stylePreset.name
                )
            }
            db.layoutDao().replaceConfigs(entities)

            showToast("Loaded \"${entity.name}\" layout")
            hapticManager.performButtonPress(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength, "Load")
        } else {
            showToast("Failed to load layout")
        }
    }

    fun deleteSavedLayout(layoutId: String): kotlinx.coroutines.Job = viewModelScope.launch {
        customLayoutRepository.deleteLayoutById(layoutId)
        if (_activeSavedLayoutId.value == layoutId) {
            _activeSavedLayoutId.value = "preset_default"
        }
        showToast("Deleted layout configuration")
        hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
    }

    fun duplicateSavedLayout(layoutId: String): kotlinx.coroutines.Job = viewModelScope.launch {
        val existing = customLayoutRepository.getLayoutById(layoutId)
        if (existing != null) {
            val newId = "layout_" + UUID.randomUUID().toString().take(8)
            val copy = existing.copy(
                id = newId,
                name = "${existing.name} (Copy)",
                isPreset = false,
                lastModified = System.currentTimeMillis()
            )
            customLayoutRepository.saveLayout(copy)
            showToast("Duplicated \"${existing.name}\"")
            hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
        }
    }

    fun updateSavedLayoutDetails(layoutId: String, newName: String, newDescription: String): kotlinx.coroutines.Job = viewModelScope.launch {
        val existing = customLayoutRepository.getLayoutById(layoutId)
        if (existing != null) {
            val updated = existing.copy(
                name = newName.trim().ifBlank { existing.name },
                description = newDescription.trim(),
                lastModified = System.currentTimeMillis()
            )
            customLayoutRepository.saveLayout(updated)
            if (_activeSavedLayoutId.value == layoutId) {
                _activeLayout.update { it.copy(name = updated.name) }
                _editingLayout.update { it.copy(name = updated.name) }
            }
            showToast("Updated \"${updated.name}\"")
            hapticManager.performUiTick(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength)
        }
    }

    fun saveLayout(): kotlinx.coroutines.Job {
        val currentEdit = _editingLayout.value
        val activeId = _activeSavedLayoutId.value ?: "preset_default"

        return viewModelScope.launch {
            val existing = customLayoutRepository.getLayoutById(activeId)
            val isCustomActive = existing != null && !existing.isPreset
            val layoutId = if (isCustomActive) activeId else "layout_" + UUID.randomUUID().toString().take(8)
            val layoutName = if (isCustomActive) existing!!.name else "Custom Layout (${System.currentTimeMillis() % 10000})"
            val json = CustomLayoutSerializer.serialize(currentEdit.elements)

            val entity = CustomLayoutEntity(
                id = layoutId,
                name = layoutName,
                description = existing?.description ?: "User customized gamepad arrangement",
                isPreset = false,
                elementCount = currentEdit.elements.size,
                elementsJson = json,
                lastModified = System.currentTimeMillis()
            )
            customLayoutRepository.saveLayout(entity)
            _activeSavedLayoutId.value = layoutId

            val profile = currentEdit.copy(id = layoutId, name = layoutName, isCustom = true)
            releaseControls()
            _activeLayout.value = profile
            _editingLayout.value = profile

            // Persist to legacy Room table as well
            val entities = currentEdit.elements.values.map {
                LayoutConfigEntity(
                    elementIdString = it.elementId.name,
                    xPercent = it.xPercent,
                    yPercent = it.yPercent,
                    scale = it.scale,
                    stylePresetString = it.stylePreset.name
                )
            }
            db.layoutDao().replaceConfigs(entities)

            showToast("Saved \"$layoutName\" to Room database")
            hapticManager.performButtonPress(_quickSettings.value.hapticsEnabled, _quickSettings.value.hapticStrength, "SaveLayout")
        }
    }

    fun cancelLayoutEdit() {
        _editingLayout.value = _activeLayout.value
        showToast("Changes discarded")
        _currentScreen.value = GamepadScreen.CONTROLLER
    }

    // Input handlers
    fun onLeftStickMoved(x: Float, y: Float) {
        if (!acceptsInput()) return
        input.lx=x; input.ly=y; submitInput()
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
        if (!acceptsInput()) return
        input.rx=x; input.ry=y; submitInput()
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
        if (!acceptsInput()) return
        if (isLeft) input.lt=pressure else input.rt=pressure
        submitInput()
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

    private fun onButtonPressed(buttonName: String) {
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

    fun setDeadzone(pct: Int) {
        releaseControls()
        _quickSettings.update { it.copy(deadzonePct = pct.coerceIn(0,25)) }
    }

    fun setSendInterval(ms: Int) {
        require(ms in listOf(4,8,16))
        releaseControls()
        _quickSettings.update { it.copy(sendIntervalMs=ms) }
        bluetooth.service?.setPeriod(ms)
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
