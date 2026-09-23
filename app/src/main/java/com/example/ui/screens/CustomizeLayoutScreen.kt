package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ControllerElementId
import com.example.data.model.ElementLayoutConfig
import com.example.data.model.GamepadScreen
import com.example.data.model.StickStylePreset
import com.example.ui.components.ABXYCluster
import com.example.ui.components.AnalogStick
import com.example.ui.components.CanvasControllerLayoutBuilder
import com.example.ui.components.DPadView
import com.example.ui.components.QuickAuxButtons
import com.example.ui.components.TriggerBumperGroup
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxBlueX
import com.example.ui.theme.XboxGreenA
import com.example.viewmodel.GamepadViewModel
import kotlin.math.roundToInt

@Composable
fun CustomizeLayoutScreen(
    viewModel: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    val editingLayout by viewModel.editingLayout.collectAsState()
    val selectedElementId by viewModel.selectedElementId.collectAsState()
    val editorTab by viewModel.editorActiveTab.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snapGridMode by viewModel.snapGridMode.collectAsState()
    val isTestMode by viewModel.isTestMode.collectAsState()

    var showAdvancedModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .testTag("customize_layout_screen")
    ) {
        // Header & Tabs Bar
        EditorHeader(
            activeTab = editorTab,
            onTabSelect = { viewModel.setEditorTab(it) },
            onBack = { viewModel.navigateTo(GamepadScreen.CONTROLLER) },
            onReset = { viewModel.resetLayoutToDefault() }
        )

        // Main Editor Canvas + Inspector Split
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Blueprint Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(SurfaceCanvas)
            ) {
                when (editorTab) {
                    "layout" -> {
                        CanvasControllerLayoutBuilder(
                            elements = editingLayout.elements,
                            selectedId = selectedElementId,
                            snapGridMode = snapGridMode,
                            isTestMode = isTestMode,
                            onSelectElement = { viewModel.selectElement(it) },
                            onPositionChanged = { id, x, y ->
                                viewModel.updateElementPosition(id, x, y)
                            },
                            onScaleChanged = { id, scale ->
                                viewModel.updateElementScale(id, scale)
                            },
                            onAddElement = { id, x, y ->
                                viewModel.addElement(id, x, y)
                            },
                            onRemoveElement = { viewModel.removeElement(it) },
                            onSetSnapGridMode = { viewModel.setSnapGridMode(it) },
                            onToggleTestMode = { viewModel.toggleTestMode() },
                            onApplyPreset = { viewModel.applyLayoutPreset(it) },
                            onResetLayout = { viewModel.resetLayoutToDefault() },
                            onSaveLayout = { viewModel.saveLayout() }
                        )
                    }
                    "mapping" -> {
                        ButtonMappingTab(viewModel = viewModel)
                    }
                    "sticks" -> {
                        SticksCalibrationTab(viewModel = viewModel)
                    }
                }
            }

            // Right-Hand Inspector Sidebar (visible in mapping or sticks tabs, or when not in test mode)
            if (editorTab != "layout" && !isTestMode) {
                InspectorSidebar(
                    selectedId = selectedElementId,
                    config = editingLayout.elements[selectedElementId] ?: ElementLayoutConfig(selectedElementId, 50f, 50f),
                    onScaleChange = { viewModel.updateElementScale(selectedElementId, it) },
                    onStyleChange = { viewModel.updateElementStyle(selectedElementId, it) },
                    onOpenAdvanced = { showAdvancedModal = true }
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

    // Advanced Settings Modal Dialog
    if (showAdvancedModal) {
        AdvancedSettingsDialog(
            selectedId = selectedElementId,
            onDismiss = { showAdvancedModal = false }
        )
    }
}

@Composable
private fun EditorHeader(
    activeTab: String,
    onTabSelect: (String) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDefault.copy(alpha = 0.98f))
            .border(1.dp, SurfaceControlRaised.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Customize Layout",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
            }

            // Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .border(1.dp, ControlBorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { onReset() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("reset_default_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset to Default",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reset to Default",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Sub Row: Segmented Switcher Tabs & Live Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Tabs Bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EditorPillTab(
                    title = "Layout",
                    icon = Icons.Default.GridView,
                    isSelected = activeTab == "layout",
                    onClick = { onTabSelect("layout") },
                    testTag = "tab_layout"
                )
                EditorPillTab(
                    title = "Mapping",
                    icon = Icons.Default.SportsEsports,
                    isSelected = activeTab == "mapping",
                    onClick = { onTabSelect("mapping") },
                    testTag = "tab_mapping"
                )
                EditorPillTab(
                    title = "Sticks",
                    icon = Icons.Default.Adjust,
                    isSelected = activeTab == "sticks",
                    onClick = { onTabSelect("sticks") },
                    testTag = "tab_sticks"
                )
            }

            // Snapping HUD indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(XboxGreenA)
                )
                Text(
                    text = "SNAP TO 8DP GRID",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(text = "•", color = SurfaceControlRaised)
                Text(
                    text = "LAYER: ACTIVE DECK",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun EditorPillTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryContainerBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) TextPrimary else TextTertiary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextTertiary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun LayoutCanvasView(
    elements: Map<ControllerElementId, ElementLayoutConfig>,
    selectedId: ControllerElementId,
    onSelect: (ControllerElementId) -> Unit,
    onPositionChanged: (ControllerElementId, Float, Float) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val view = LocalView.current
    var isDraggingAny by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("blueprint_editor_canvas")
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()

        // Blueprint dot matrix grid canvas background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotSpacing = 16.dp.toPx()
            val dotRadius = 1.dp.toPx()
            val dotColor = ControlBorderGlow.copy(alpha = 0.22f)

            var x = dotSpacing
            while (x < size.width) {
                var y = dotSpacing
                while (y < size.height) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += dotSpacing
                }
                x += dotSpacing
            }

            // Subtle centerlines
            drawLine(
                color = ControlBorderSubtle.copy(alpha = 0.25f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ControlBorderSubtle.copy(alpha = 0.25f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Render each controller element with draggable bounding box
        elements.forEach { (id, config) ->
            val isSelected = id == selectedId

            val leftPx = (canvasWidthPx * (config.xPercent / 100f))
            val topPx = (canvasHeightPx * (config.yPercent / 100f))

            MovableElementWrapper(
                elementId = id,
                config = config,
                isSelected = isSelected,
                leftPx = leftPx,
                topPx = topPx,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                onSelect = { onSelect(id) },
                onDragMoved = { newXPx, newYPx ->
                    isDraggingAny = true
                    val xPct = (newXPx / canvasWidthPx * 100f).coerceIn(2f, 92f)
                    val yPct = (newYPx / canvasHeightPx * 100f).coerceIn(2f, 90f)
                    onPositionChanged(id, xPct, yPct)
                },
                onDragEnded = {
                    isDraggingAny = false
                }
            )
        }

        // Bottom Action Bar: Cancel & Save Layout
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("cancel-layout-btn")
            ) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = PrimaryContainerBlue)
                    .testTag("save-layout-btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Layout", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MovableElementWrapper(
    elementId: ControllerElementId,
    config: ElementLayoutConfig,
    isSelected: Boolean,
    leftPx: Float,
    topPx: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    onSelect: () -> Unit,
    onDragMoved: (Float, Float) -> Unit,
    onDragEnded: () -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current

    // Pulsing corner animation for selected element
    val infiniteTransition = rememberInfiniteTransition(label = "cornerPulse")
    val cornerPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(leftPx.roundToInt(), topPx.roundToInt())
            }
            .pointerInput(elementId) {
                detectTapGestures(
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSelect()
                    }
                )
            }
            .pointerInput(elementId) {
                detectDragGestures(
                    onDragStart = {
                        onSelect()
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    },
                    onDragEnd = onDragEnded,
                    onDragCancel = onDragEnded,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newLeft = leftPx + dragAmount.x
                        val newTop = topPx + dragAmount.y
                        // 8dp grid snap
                        val snapPx = with(density) { 8.dp.toPx() }
                        val snappedLeft = (newLeft / snapPx).roundToInt() * snapPx
                        val snappedTop = (newTop / snapPx).roundToInt() * snapPx
                        onDragMoved(snappedLeft, snappedTop)
                    }
                )
            }
            .padding(10.dp)
    ) {
        // Selection bounding box with cyan glow & handles
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(XboxBlueX.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.5.dp, XboxBlueX, RoundedCornerShape(16.dp))
            ) {
                // Top-Left corner handle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(8.dp)
                        .scale(cornerPulseScale)
                        .clip(RoundedCornerShape(2.dp))
                        .background(XboxBlueX)
                )
                // Top-Right corner handle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(8.dp)
                        .scale(cornerPulseScale)
                        .clip(RoundedCornerShape(2.dp))
                        .background(XboxBlueX)
                )
                // Bottom-Left corner handle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(8.dp)
                        .scale(cornerPulseScale)
                        .clip(RoundedCornerShape(2.dp))
                        .background(XboxBlueX)
                )
                // Bottom-Right corner handle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(8.dp)
                        .scale(cornerPulseScale)
                        .clip(RoundedCornerShape(2.dp))
                        .background(XboxBlueX)
                )
                // Floating element badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = (-24).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryContainerBlue)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = elementId.displayName.uppercase(),
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Render actual element
        when (elementId) {
            ControllerElementId.LT_LB -> {
                TriggerBumperGroup(isLeft = true, scale = config.scale)
            }
            ControllerElementId.RT_RB -> {
                TriggerBumperGroup(isLeft = false, scale = config.scale)
            }
            ControllerElementId.LEFT_STICK -> {
                AnalogStick(
                    sizeDp = (116 * config.scale).dp,
                    label = "Left Stick",
                    stylePreset = config.stylePreset
                )
            }
            ControllerElementId.RIGHT_STICK -> {
                AnalogStick(
                    sizeDp = (116 * config.scale).dp,
                    label = "Right Stick",
                    stylePreset = config.stylePreset
                )
            }
            ControllerElementId.DPAD -> {
                DPadView(sizeDp = (116 * config.scale).dp)
            }
            ControllerElementId.AUX_BUTTONS -> {
                QuickAuxButtons(scale = config.scale)
            }
            ControllerElementId.ABXY -> {
                ABXYCluster(sizeDp = (120 * config.scale).dp)
            }
            else -> {
                // Paddles & Turbo
                Box(
                    modifier = Modifier
                        .size((80 * config.scale).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceControlRaised)
                        .border(1.dp, ControlBorderGlow, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = elementId.displayName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectorSidebar(
    selectedId: ControllerElementId,
    config: ElementLayoutConfig,
    onScaleChange: (Float) -> Unit,
    onStyleChange: (StickStylePreset) -> Unit,
    onOpenAdvanced: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(SurfaceCard.copy(alpha = 0.98f))
            .border(1.dp, SurfaceControlRaised.copy(alpha = 0.6f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("inspector_sidebar"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Inspector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when (selectedId) {
                    ControllerElementId.LEFT_STICK, ControllerElementId.RIGHT_STICK -> Icons.Default.Adjust
                    ControllerElementId.ABXY -> Icons.Default.SportsEsports
                    else -> Icons.Default.Tune
                },
                contentDescription = null,
                tint = XboxBlueX,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = selectedId.displayName,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Position Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "POSITION",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // X Coordinate
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDefault)
                        .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "X", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${config.xPercent.toInt()}%",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Y Coordinate
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDefault)
                        .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Y", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${config.yPercent.toInt()}%",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Size Slider (80% - 140%)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIZE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${(config.scale * 100).toInt()}%",
                    color = XboxBlueX,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = config.scale,
                onValueChange = onScaleChange,
                valueRange = 0.8f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryContainerBlue,
                    activeTrackColor = PrimaryContainerBlue,
                    inactiveTrackColor = SurfaceContainerLowest
                ),
                modifier = Modifier.testTag("size_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Compact (80%)", color = TextTertiary, fontSize = 9.sp)
                Text("Default", color = TextTertiary, fontSize = 9.sp)
                Text("Expanded (140%)", color = TextTertiary, fontSize = 9.sp)
            }
        }

        // Stick Style Preset Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "STYLE PRESET",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StylePresetOption(
                    title = "Halo",
                    isSelected = config.stylePreset == StickStylePreset.HALO,
                    onClick = { onStyleChange(StickStylePreset.HALO) }
                )
                StylePresetOption(
                    title = "Target",
                    isSelected = config.stylePreset == StickStylePreset.TARGET,
                    onClick = { onStyleChange(StickStylePreset.TARGET) }
                )
                StylePresetOption(
                    title = "Minimal",
                    isSelected = config.stylePreset == StickStylePreset.MINIMAL,
                    onClick = { onStyleChange(StickStylePreset.MINIMAL) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Advanced Settings Button
        Button(
            onClick = onOpenAdvanced,
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("adv-settings-btn")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Advanced Settings", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StylePresetOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDefault)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) XboxBlueX else ControlBorderSubtle.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(SurfaceControlRaised)
                .border(1.dp, if (isSelected) XboxBlueX else ControlBorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(XboxBlueX)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ButtonMappingTab(viewModel: GamepadViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Button Remapping & Macro Assignments",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Remap digital and analog controls to custom XInput or HID output triggers.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        val mappings = listOf(
            "Button A" to "Primary Action (Jump / Select)",
            "Button B" to "Secondary Action (Cancel / Crouch)",
            "Button X" to "Tertiary Action (Reload / Interact)",
            "Button Y" to "Quaternary Action (Switch Weapon)",
            "Left Bumper (LB)" to "Left Tactical Equipment",
            "Right Bumper (RB)" to "Right Lethal Equipment",
            "Left Trigger (LT)" to "Aim Down Sights (ADS)",
            "Right Trigger (RT)" to "Primary Fire (Full Auto)"
        )

        mappings.forEach { (btn, assignment) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = btn, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = assignment, color = XboxBlueX, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SticksCalibrationTab(viewModel: GamepadViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Thumbstick Deadzones & Calibration",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hardware zero-drift calibration, linear response curve, and outer threshold smoothing.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Stick Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Left Stick Calibration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Deadzone: 5%", color = TextSecondary, fontSize = 12.sp)
                Text("Outer Threshold: 98%", color = TextSecondary, fontSize = 12.sp)
                Text("Response Curve: Linear 1:1", color = XboxGreenA, fontSize = 12.sp)
            }

            // Right Stick Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Right Stick Calibration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Deadzone: 3% (Precision Aim)", color = TextSecondary, fontSize = 12.sp)
                Text("Outer Threshold: 100%", color = TextSecondary, fontSize = 12.sp)
                Text("Response Curve: Dynamic S-Curve", color = XboxBlueX, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AdvancedSettingsDialog(
    selectedId: ControllerElementId,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, ControlBorderGlow, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .clickable(enabled = false) {},
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "${selectedId.displayName} Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fine-tune deadzone filters, haptic intensity, and touch response profile.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Anti-Deadzone Smoothing", color = TextPrimary, fontSize = 13.sp)
                Switch(
                    checked = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryContainerBlue)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invert Y Axis", color = TextPrimary, fontSize = 13.sp)
                Switch(
                    checked = false,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryContainerBlue)
                )
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
