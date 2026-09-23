package com.example.ui.components

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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ControllerElementId
import com.example.data.model.ElementLayoutConfig
import com.example.data.model.StickStylePreset
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.OnPrimaryBlue
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerHigh
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
import com.example.ui.theme.XboxRedB
import com.example.ui.theme.XboxYellowY
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance, full-featured Canvas-based UI component for dragging, dropping,
 * positioning, scaling, and testing custom controller layouts.
 */
@Composable
fun CanvasControllerLayoutBuilder(
    elements: Map<ControllerElementId, ElementLayoutConfig>,
    selectedId: ControllerElementId?,
    snapGridMode: String, // "8DP", "16DP", "OFF"
    isTestMode: Boolean,
    onSelectElement: (ControllerElementId) -> Unit,
    onPositionChanged: (ControllerElementId, Float, Float) -> Unit,
    onScaleChanged: (ControllerElementId, Float) -> Unit,
    onAddElement: (ControllerElementId, Float, Float) -> Unit,
    onRemoveElement: (ControllerElementId) -> Unit,
    onSetSnapGridMode: (String) -> Unit,
    onToggleTestMode: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onResetLayout: () -> Unit,
    onSaveLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    // Dragged item from palette tracking
    var draggingPaletteItem by remember { mutableStateOf<ControllerElementId?>(null) }
    var paletteDragGlobalOffset by remember { mutableStateOf<Offset?>(null) }
    var canvasBoundsOnScreen by remember { mutableStateOf<Rect?>(null) }

    // Live test touch tracking
    var activeTestTouches by remember { mutableStateOf<Map<ControllerElementId, Offset>>(emptyMap()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .testTag("canvas_controller_layout_builder")
    ) {
        // 1. Top Control Bar (Snap Mode, Presets, Test Mode, Save)
        BuilderToolbar(
            snapGridMode = snapGridMode,
            isTestMode = isTestMode,
            onSetSnapGridMode = onSetSnapGridMode,
            onToggleTestMode = onToggleTestMode,
            onApplyPreset = onApplyPreset,
            onResetLayout = onResetLayout,
            onSaveLayout = onSaveLayout
        )

        // 2. Draggable Button Palette / Toolbox Dock
        if (!isTestMode) {
            DraggableButtonPaletteDock(
                existingElements = elements.keys,
                onPaletteDragStart = { item, offset ->
                    draggingPaletteItem = item
                    paletteDragGlobalOffset = offset
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                },
                onPaletteDrag = { offset ->
                    paletteDragGlobalOffset = offset
                },
                onPaletteDragEnd = {
                    val draggedItem = draggingPaletteItem
                    val dragOffset = paletteDragGlobalOffset
                    val bounds = canvasBoundsOnScreen

                    if (draggedItem != null && dragOffset != null && bounds != null) {
                        if (bounds.contains(dragOffset)) {
                            // Compute relative percent on Canvas
                            val relX = ((dragOffset.x - bounds.left) / bounds.width * 100f).coerceIn(4f, 92f)
                            val relY = ((dragOffset.y - bounds.top) / bounds.height * 100f).coerceIn(4f, 90f)
                            onAddElement(draggedItem, relX, relY)
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                    draggingPaletteItem = null
                    paletteDragGlobalOffset = null
                },
                onQuickAdd = { item ->
                    // Add near center or default ergonomic slot
                    val defaultPos = when (item) {
                        ControllerElementId.LT_LB -> Pair(8f, 8f)
                        ControllerElementId.RT_RB -> Pair(84f, 8f)
                        ControllerElementId.LEFT_STICK -> Pair(12f, 48f)
                        ControllerElementId.RIGHT_STICK -> Pair(64f, 48f)
                        ControllerElementId.DPAD -> Pair(28f, 50f)
                        ControllerElementId.ABXY -> Pair(82f, 48f)
                        ControllerElementId.AUX_BUTTONS -> Pair(48f, 32f)
                        ControllerElementId.PADDLE_P1 -> Pair(16f, 76f)
                        ControllerElementId.PADDLE_P2 -> Pair(78f, 76f)
                        ControllerElementId.TURBO_BTN -> Pair(50f, 68f)
                    }
                    onAddElement(item, defaultPos.first, defaultPos.second)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            )
        }

        // 3. The Interactive Blueprint Canvas Workspace
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDefault)
                .border(1.5.dp, if (isTestMode) XboxGreenA.copy(alpha = 0.5f) else ControlBorderSubtle, RoundedCornerShape(16.dp))
        ) {
            InteractiveBlueprintCanvas(
                elements = elements,
                selectedId = selectedId,
                snapGridMode = snapGridMode,
                isTestMode = isTestMode,
                draggingPaletteItem = draggingPaletteItem,
                paletteDragOffset = paletteDragGlobalOffset,
                activeTestTouches = activeTestTouches,
                onSelectElement = onSelectElement,
                onPositionChanged = onPositionChanged,
                onScaleChanged = onScaleChanged,
                onRemoveElement = onRemoveElement,
                onCanvasBoundsCalculated = { bounds ->
                    canvasBoundsOnScreen = bounds
                },
                onTestTouch = { id, offset ->
                    activeTestTouches = if (offset == null) {
                        activeTestTouches - id
                    } else {
                        activeTestTouches + (id to offset)
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            )
        }

        // 4. Bottom Selected Element Inspector & Actions Bar (only in edit mode)
        if (!isTestMode && selectedId != null && elements.containsKey(selectedId)) {
            val selectedConfig = elements[selectedId]!!
            SelectedElementBottomDock(
                elementId = selectedId,
                config = selectedConfig,
                onScaleChange = { newScale -> onScaleChanged(selectedId, newScale) },
                onCenterH = {
                    onPositionChanged(selectedId, 50f, selectedConfig.yPercent)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onCenterV = {
                    onPositionChanged(selectedId, selectedConfig.xPercent, 50f)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onDelete = { onRemoveElement(selectedId) }
            )
        }
    }
}

/**
 * Top Toolbar with Presets, Snapping, and Test Mode controls
 */
@Composable
private fun BuilderToolbar(
    snapGridMode: String,
    isTestMode: Boolean,
    onSetSnapGridMode: (String) -> Unit,
    onToggleTestMode: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onResetLayout: () -> Unit,
    onSaveLayout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceControlRaised.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Mode & Snap Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mode toggle button (Blueprint vs Playtest)
            Button(
                onClick = onToggleTestMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTestMode) XboxGreenA else SurfaceControl
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("toggle_test_mode_button")
            ) {
                Icon(
                    imageVector = if (isTestMode) Icons.Default.TouchApp else Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (isTestMode) Color.White else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTestMode) "LIVE PLAYTEST" else "BLUEPRINT EDIT",
                    color = if (isTestMode) Color.White else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Snapping selector chips
            if (!isTestMode) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceControl)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SnapChip(
                        label = "Snap 8",
                        isSelected = snapGridMode == "8DP",
                        onClick = { onSetSnapGridMode("8DP") },
                        testTag = "snap_8dp_chip"
                    )
                    SnapChip(
                        label = "Snap 16",
                        isSelected = snapGridMode == "16DP",
                        onClick = { onSetSnapGridMode("16DP") },
                        testTag = "snap_16dp_chip"
                    )
                    SnapChip(
                        label = "Free",
                        isSelected = snapGridMode == "OFF",
                        onClick = { onSetSnapGridMode("OFF") },
                        testTag = "snap_off_chip"
                    )
                }
            }
        }

        // Right side: Presets & Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isTestMode) {
                // Preset drop selector pills
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetPill("Default", onClick = { onApplyPreset("DEFAULT") })
                    PresetPill("FPS Pro", onClick = { onApplyPreset("FPS") })
                    PresetPill("Arcade", onClick = { onApplyPreset("FIGHTING") })
                    PresetPill("Southpaw", onClick = { onApplyPreset("SOUTHPAW") })
                }

                // Reset button
                IconButton(
                    onClick = onResetLayout,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceControl)
                        .testTag("reset_layout_icon_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Layout",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Save Layout Button
            Button(
                onClick = onSaveLayout,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(36.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = PrimaryContainerBlue)
                    .testTag("canvas_save_layout_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SnapChip(label: String, isSelected: Boolean, onClick: () -> Unit, testTag: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryContainerBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextTertiary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PresetPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Draggable Button Palette / Toolbox Dock
 */
@Composable
private fun DraggableButtonPaletteDock(
    existingElements: Set<ControllerElementId>,
    onPaletteDragStart: (ControllerElementId, Offset) -> Unit,
    onPaletteDrag: (Offset) -> Unit,
    onPaletteDragEnd: () -> Unit,
    onQuickAdd: (ControllerElementId) -> Unit
) {
    val allToolboxItems = listOf(
        ControllerElementId.LEFT_STICK,
        ControllerElementId.RIGHT_STICK,
        ControllerElementId.DPAD,
        ControllerElementId.ABXY,
        ControllerElementId.LT_LB,
        ControllerElementId.RT_RB,
        ControllerElementId.AUX_BUTTONS,
        ControllerElementId.PADDLE_P1,
        ControllerElementId.PADDLE_P2,
        ControllerElementId.TURBO_BTN
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.3f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "CONTROLLER BUTTON PALETTE",
                    color = PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "Drag onto canvas or tap to add",
                color = TextTertiary,
                fontSize = 10.sp
            )
        }

        // Horizontal palette row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allToolboxItems.forEach { item ->
                val isAlreadyPlaced = existingElements.contains(item)
                DraggablePaletteItemChip(
                    elementId = item,
                    isAlreadyPlaced = isAlreadyPlaced,
                    onDragStart = { offset -> onPaletteDragStart(item, offset) },
                    onDrag = onPaletteDrag,
                    onDragEnd = onPaletteDragEnd,
                    onTap = { onQuickAdd(item) }
                )
            }
        }
    }
}

@Composable
private fun DraggablePaletteItemChip(
    elementId: ControllerElementId,
    isAlreadyPlaced: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit
) {
    var globalTouchPos by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isAlreadyPlaced) SurfaceCard else ActiveControlFill)
            .border(
                1.dp,
                if (isAlreadyPlaced) ControlBorderSubtle else ControlBorderGlow,
                RoundedCornerShape(10.dp)
            )
            .clickable { onTap() }
            .pointerInput(elementId) {
                detectDragGestures(
                    onDragStart = { offset ->
                        globalTouchPos = offset
                        onDragStart(offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        globalTouchPos += dragAmount
                        onDrag(globalTouchPos)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("palette_item_${elementId.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = when (elementId) {
                ControllerElementId.LEFT_STICK, ControllerElementId.RIGHT_STICK -> Icons.Default.Adjust
                ControllerElementId.ABXY -> Icons.Default.SportsEsports
                ControllerElementId.DPAD -> Icons.Default.Gamepad
                ControllerElementId.TURBO_BTN -> Icons.Default.FlashOn
                else -> Icons.Default.Tune
            },
            contentDescription = null,
            tint = if (isAlreadyPlaced) TextSecondary else Color.White,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = elementId.displayName,
            color = if (isAlreadyPlaced) TextSecondary else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        if (isAlreadyPlaced) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(XboxGreenA)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = PrimaryBlue,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * The Interactive Blueprint Canvas:
 * Renders controller elements, grid, alignment lines, selection box, and gesture manipulators.
 */
@Composable
private fun InteractiveBlueprintCanvas(
    elements: Map<ControllerElementId, ElementLayoutConfig>,
    selectedId: ControllerElementId?,
    snapGridMode: String,
    isTestMode: Boolean,
    draggingPaletteItem: ControllerElementId?,
    paletteDragOffset: Offset?,
    activeTestTouches: Map<ControllerElementId, Offset>,
    onSelectElement: (ControllerElementId) -> Unit,
    onPositionChanged: (ControllerElementId, Float, Float) -> Unit,
    onScaleChanged: (ControllerElementId, Float) -> Unit,
    onRemoveElement: (ControllerElementId) -> Unit,
    onCanvasBoundsCalculated: (Rect) -> Unit,
    onTestTouch: (ControllerElementId, Offset?) -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "blueprintGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Drag tracking state for on-canvas direct manipulation
    var activeDraggingElement by remember { mutableStateOf<ControllerElementId?>(null) }
    var activeDraggingResizeHandle by remember { mutableStateOf<ControllerElementId?>(null) }
    var dragStartPos by remember { mutableStateOf(Offset.Zero) }
    var initialConfigOnDrag by remember { mutableStateOf<ElementLayoutConfig?>(null) }

    // Smart alignment guides state
    var showAlignGuideX by remember { mutableStateOf<Float?>(null) }
    var showAlignGuideY by remember { mutableStateOf<Float?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_blueprint_canvas")
            .pointerInput(isTestMode, elements, selectedId) {
                if (isTestMode) {
                    // Test Mode: direct touch interaction
                    detectTapGestures(
                        onPress = { touchOffset ->
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            // Find which element was touched
                            val hitElement = elements.entries.find { (_, config) ->
                                val elemXPx = canvasW * (config.xPercent / 100f)
                                val elemYPx = canvasH * (config.yPercent / 100f)
                                val elemRadiusPx = 50.dp.toPx() * config.scale
                                val dist = sqrt((touchOffset.x - elemXPx) * (touchOffset.x - elemXPx) + (touchOffset.y - elemYPx) * (touchOffset.y - elemYPx))
                                dist <= elemRadiusPx
                            }?.key

                            if (hitElement != null) {
                                onTestTouch(hitElement, touchOffset)
                                tryAwaitRelease()
                                onTestTouch(hitElement, null)
                            }
                        }
                    )
                } else {
                    // Blueprint Edit Mode: drag, select, and scale handling
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            // Check if tapped delete button on currently selected element
                            if (selectedId != null && elements.containsKey(selectedId)) {
                                val config = elements[selectedId]!!
                                val elemXPx = canvasW * (config.xPercent / 100f)
                                val elemYPx = canvasH * (config.yPercent / 100f)
                                val boxHalfPx = 48.dp.toPx() * config.scale
                                val deleteBtnCenter = Offset(elemXPx + boxHalfPx + 8.dp.toPx(), elemYPx - boxHalfPx - 8.dp.toPx())
                                val distToDelete = (tapOffset - deleteBtnCenter).getDistance()
                                if (distToDelete <= 20.dp.toPx()) {
                                    onRemoveElement(selectedId)
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    return@detectTapGestures
                                }
                            }

                            // Find clicked element
                            val clicked = elements.entries.find { (_, config) ->
                                val elemXPx = canvasW * (config.xPercent / 100f)
                                val elemYPx = canvasH * (config.yPercent / 100f)
                                val elemRadiusPx = 52.dp.toPx() * config.scale
                                val dist = (tapOffset - Offset(elemXPx, elemYPx)).getDistance()
                                dist <= elemRadiusPx
                            }?.key

                            if (clicked != null) {
                                onSelectElement(clicked)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }
                    )
                }
            }
            .pointerInput(isTestMode, elements, selectedId, snapGridMode) {
                if (!isTestMode) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            // First, check if clicked on Resize Handle of selected element
                            if (selectedId != null && elements.containsKey(selectedId)) {
                                val config = elements[selectedId]!!
                                val elemXPx = canvasW * (config.xPercent / 100f)
                                val elemYPx = canvasH * (config.yPercent / 100f)
                                val boxHalfPx = 48.dp.toPx() * config.scale
                                val resizeHandlePos = Offset(elemXPx + boxHalfPx + 6.dp.toPx(), elemYPx + boxHalfPx + 6.dp.toPx())
                                if ((startOffset - resizeHandlePos).getDistance() <= 24.dp.toPx()) {
                                    activeDraggingResizeHandle = selectedId
                                    dragStartPos = startOffset
                                    initialConfigOnDrag = config
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    return@detectDragGestures
                                }
                            }

                            // Otherwise check if touching an element directly
                            val touched = elements.entries.find { (_, config) ->
                                val elemXPx = canvasW * (config.xPercent / 100f)
                                val elemYPx = canvasH * (config.yPercent / 100f)
                                val elemRadiusPx = 54.dp.toPx() * config.scale
                                (startOffset - Offset(elemXPx, elemYPx)).getDistance() <= elemRadiusPx
                            }?.key

                            if (touched != null) {
                                activeDraggingElement = touched
                                dragStartPos = startOffset
                                initialConfigOnDrag = elements[touched]
                                onSelectElement(touched)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            if (activeDraggingResizeHandle != null && initialConfigOnDrag != null) {
                                // Scaling element dynamically
                                val currentDist = (change.position - Offset(canvasW * (initialConfigOnDrag!!.xPercent / 100f), canvasH * (initialConfigOnDrag!!.yPercent / 100f))).getDistance()
                                val baseDist = 48.dp.toPx()
                                val newScale = (currentDist / baseDist).coerceIn(0.65f, 1.55f)
                                onScaleChanged(activeDraggingResizeHandle!!, newScale)
                            } else if (activeDraggingElement != null && initialConfigOnDrag != null) {
                                // Moving element with grid snapping & smart alignment guides
                                val origXPx = canvasW * (initialConfigOnDrag!!.xPercent / 100f)
                                val origYPx = canvasH * (initialConfigOnDrag!!.yPercent / 100f)
                                var rawTargetXPx = origXPx + (change.position.x - dragStartPos.x)
                                var rawTargetYPx = origYPx + (change.position.y - dragStartPos.y)

                                // Snap to 8dp or 16dp grid if enabled
                                if (snapGridMode != "OFF") {
                                    val snapPx = with(density) { if (snapGridMode == "16DP") 16.dp.toPx() else 8.dp.toPx() }
                                    rawTargetXPx = (rawTargetXPx / snapPx).roundToInt() * snapPx
                                    rawTargetYPx = (rawTargetYPx / snapPx).roundToInt() * snapPx
                                }

                                var targetXPct = (rawTargetXPx / canvasW * 100f).coerceIn(4f, 92f)
                                var targetYPct = (rawTargetYPx / canvasH * 100f).coerceIn(4f, 90f)

                                // Smart alignment snap to center axes (50%)
                                if (abs(targetXPct - 50f) < 1.2f) {
                                    targetXPct = 50f
                                    showAlignGuideX = canvasW * 0.5f
                                } else {
                                    showAlignGuideX = null
                                }

                                if (abs(targetYPct - 50f) < 1.2f) {
                                    targetYPct = 50f
                                    showAlignGuideY = canvasH * 0.5f
                                } else {
                                    showAlignGuideY = null
                                }

                                onPositionChanged(activeDraggingElement!!, targetXPct, targetYPct)
                            }
                        },
                        onDragEnd = {
                            activeDraggingElement = null
                            activeDraggingResizeHandle = null
                            initialConfigOnDrag = null
                            showAlignGuideX = null
                            showAlignGuideY = null
                        },
                        onDragCancel = {
                            activeDraggingElement = null
                            activeDraggingResizeHandle = null
                            initialConfigOnDrag = null
                            showAlignGuideX = null
                            showAlignGuideY = null
                        }
                    )
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Pass canvas bounds back to parent for drop calculation
        onCanvasBoundsCalculated(Rect(0f, 0f, widthPx, heightPx))

        // Main Jetpack Compose Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw Blueprint Dot Grid
            drawBlueprintGrid(
                width = size.width,
                height = size.height,
                snapMode = snapGridMode
            )

            // 2. Draw Controller Ergonomic Resting Zones (Left grip, Right grip, Center bridge)
            drawErgonomicZoning(
                width = size.width,
                height = size.height
            )

            // 3. Draw Smart Snapping Alignment Guidelines if active
            showAlignGuideX?.let { guideX ->
                drawLine(
                    color = XboxGreenA,
                    start = Offset(guideX, 0f),
                    end = Offset(guideX, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
            showAlignGuideY?.let { guideY ->
                drawLine(
                    color = XboxGreenA,
                    start = Offset(0f, guideY),
                    end = Offset(size.width, guideY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // 4. Draw Drop Target Ghost if dragging item from palette
            if (draggingPaletteItem != null && paletteDragOffset != null) {
                drawDropTargetGhost(
                    item = draggingPaletteItem,
                    center = paletteDragOffset,
                    pulseAlpha = pulseAlpha
                )
            }

            // 5. Render All Controller Elements directly on Canvas
            elements.forEach { (id, config) ->
                val centerPx = Offset(
                    x = size.width * (config.xPercent / 100f),
                    y = size.height * (config.yPercent / 100f)
                )
                val isSelected = (id == selectedId) && !isTestMode
                val isBeingTested = isTestMode && activeTestTouches.containsKey(id)

                drawControllerElementOnCanvas(
                    id = id,
                    center = centerPx,
                    scale = config.scale,
                    isSelected = isSelected,
                    isPressed = isBeingTested,
                    pulseAlpha = pulseAlpha
                )
            }
        }
    }
}

/**
 * Draws the high-precision dot matrix and coordinate axes.
 */
private fun DrawScope.drawBlueprintGrid(width: Float, height: Float, snapMode: String) {
    val spacingPx = when (snapMode) {
        "16DP" -> 32.dp.toPx()
        "OFF" -> 20.dp.toPx()
        else -> 24.dp.toPx()
    }
    val dotColor = ControlBorderGlow.copy(alpha = 0.20f)
    val dotRadius = 1.2.dp.toPx()

    var x = spacingPx
    while (x < width) {
        var y = spacingPx
        while (y < height) {
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(x, y)
            )
            y += spacingPx
        }
        x += spacingPx
    }

    // Center Crosshairs (Subtle Precision axes)
    val centerLineColor = ControlBorderGlow.copy(alpha = 0.28f)
    val centerX = width * 0.5f
    val centerY = height * 0.5f

    drawLine(
        color = centerLineColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )
    drawLine(
        color = centerLineColor,
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )
}

/**
 * Draws ergonomic hand zone boundaries (Left thumb zone, Right thumb zone, Upper trigger index zones).
 */
private fun DrawScope.drawErgonomicZoning(width: Float, height: Float) {
    val zoneColor = SurfaceControlRaised.copy(alpha = 0.25f)
    val strokeWidth = 1.dp.toPx()
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

    // Left Grip Ergonomic Arc
    val leftGripRect = Rect(width * 0.03f, height * 0.22f, width * 0.44f, height * 0.95f)
    drawRoundRect(
        color = zoneColor,
        topLeft = leftGripRect.topLeft,
        size = leftGripRect.size,
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
    )

    // Right Grip Ergonomic Arc
    val rightGripRect = Rect(width * 0.56f, height * 0.22f, width * 0.97f, height * 0.95f)
    drawRoundRect(
        color = zoneColor,
        topLeft = rightGripRect.topLeft,
        size = rightGripRect.size,
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
    )

    // Trigger Upper Shelf
    val triggerShelfRect = Rect(width * 0.03f, height * 0.03f, width * 0.97f, height * 0.20f)
    drawRoundRect(
        color = zoneColor.copy(alpha = 0.15f),
        topLeft = triggerShelfRect.topLeft,
        size = triggerShelfRect.size,
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
    )
}

/**
 * Draws a glowing drop ghost indicator under the cursor when dragging from the palette.
 */
private fun DrawScope.drawDropTargetGhost(
    item: ControllerElementId,
    center: Offset,
    pulseAlpha: Float
) {
    val radius = 48.dp.toPx()
    drawCircle(
        color = PrimaryContainerBlue.copy(alpha = 0.15f * pulseAlpha),
        radius = radius,
        center = center
    )
    drawCircle(
        color = PrimaryContainerBlue.copy(alpha = pulseAlpha),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
    )
    // Crosshair target in center
    drawLine(
        color = PrimaryContainerBlue,
        start = Offset(center.x - 12.dp.toPx(), center.y),
        end = Offset(center.x + 12.dp.toPx(), center.y),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = PrimaryContainerBlue,
        start = Offset(center.x, center.y - 12.dp.toPx()),
        end = Offset(center.x, center.y + 12.dp.toPx()),
        strokeWidth = 2.dp.toPx()
    )
}

/**
 * Renders each controller element using Canvas draw primitives with rich gaming fidelity.
 */
private fun DrawScope.drawControllerElementOnCanvas(
    id: ControllerElementId,
    center: Offset,
    scale: Float,
    isSelected: Boolean,
    isPressed: Boolean,
    pulseAlpha: Float
) {
    when (id) {
        ControllerElementId.LEFT_STICK, ControllerElementId.RIGHT_STICK -> {
            drawAnalogStick(center, scale, isLeft = id == ControllerElementId.LEFT_STICK, isPressed = isPressed)
        }
        ControllerElementId.DPAD -> {
            drawDPad(center, scale, isPressed = isPressed)
        }
        ControllerElementId.ABXY -> {
            drawABXYCluster(center, scale, isPressed = isPressed)
        }
        ControllerElementId.LT_LB -> {
            drawTriggerBumper(center, scale, isLeft = true, isPressed = isPressed)
        }
        ControllerElementId.RT_RB -> {
            drawTriggerBumper(center, scale, isLeft = false, isPressed = isPressed)
        }
        ControllerElementId.AUX_BUTTONS -> {
            drawAuxButtons(center, scale, isPressed = isPressed)
        }
        ControllerElementId.PADDLE_P1 -> {
            drawRearPaddle(center, scale, isLeft = true, label = "P1", isPressed = isPressed)
        }
        ControllerElementId.PADDLE_P2 -> {
            drawRearPaddle(center, scale, isLeft = false, label = "P2", isPressed = isPressed)
        }
        ControllerElementId.TURBO_BTN -> {
            drawTurboButton(center, scale, isPressed = isPressed)
        }
    }

    // Selection Bounds, Handles, and Coordinate Tooltip
    if (isSelected) {
        val boxRadius = 52.dp.toPx() * scale
        val boundsRect = Rect(center.x - boxRadius, center.y - boxRadius, center.x + boxRadius, center.y + boxRadius)

        // Glowing selection rectangle
        drawRoundRect(
            color = XboxBlueX.copy(alpha = 0.12f),
            topLeft = boundsRect.topLeft,
            size = boundsRect.size,
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )
        drawRoundRect(
            color = XboxBlueX.copy(alpha = pulseAlpha),
            topLeft = boundsRect.topLeft,
            size = boundsRect.size,
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // 4 Corner brackets
        val bracketLen = 12.dp.toPx()
        // Top-left
        drawLine(XboxBlueX, boundsRect.topLeft, boundsRect.topLeft + Offset(bracketLen, 0f), 2.5.dp.toPx())
        drawLine(XboxBlueX, boundsRect.topLeft, boundsRect.topLeft + Offset(0f, bracketLen), 2.5.dp.toPx())
        // Top-right
        drawLine(XboxBlueX, boundsRect.topRight, boundsRect.topRight + Offset(-bracketLen, 0f), 2.5.dp.toPx())
        drawLine(XboxBlueX, boundsRect.topRight, boundsRect.topRight + Offset(0f, bracketLen), 2.5.dp.toPx())
        // Bottom-left
        drawLine(XboxBlueX, boundsRect.bottomLeft, boundsRect.bottomLeft + Offset(bracketLen, 0f), 2.5.dp.toPx())
        drawLine(XboxBlueX, boundsRect.bottomLeft, boundsRect.bottomLeft + Offset(0f, -bracketLen), 2.5.dp.toPx())
        // Bottom-right
        drawLine(XboxBlueX, boundsRect.bottomRight, boundsRect.bottomRight + Offset(-bracketLen, 0f), 2.5.dp.toPx())
        drawLine(XboxBlueX, boundsRect.bottomRight, boundsRect.bottomRight + Offset(0f, -bracketLen), 2.5.dp.toPx())

        // Bottom-right Corner Scale Handle
        val resizeHandleCenter = Offset(boundsRect.right + 6.dp.toPx(), boundsRect.bottom + 6.dp.toPx())
        drawCircle(color = XboxBlueX, radius = 9.dp.toPx(), center = resizeHandleCenter)
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = resizeHandleCenter)

        // Top-right Corner Delete Handle
        val deleteHandleCenter = Offset(boundsRect.right + 6.dp.toPx(), boundsRect.top - 6.dp.toPx())
        drawCircle(color = StatusError, radius = 9.dp.toPx(), center = deleteHandleCenter)
        // 'X' mark inside delete button
        val crossHalf = 3.5.dp.toPx()
        drawLine(Color.White, deleteHandleCenter - Offset(crossHalf, crossHalf), deleteHandleCenter + Offset(crossHalf, crossHalf), 1.8.dp.toPx())
        drawLine(Color.White, deleteHandleCenter - Offset(-crossHalf, crossHalf), deleteHandleCenter + Offset(-crossHalf, crossHalf), 1.8.dp.toPx())
    }
}

/**
 * Analog Thumbstick drawing
 */
private fun DrawScope.drawAnalogStick(
    center: Offset,
    scale: Float,
    isLeft: Boolean,
    isPressed: Boolean
) {
    val outerRadius = 46.dp.toPx() * scale
    val innerRadius = 28.dp.toPx() * scale
    val stickOffset = if (isPressed) Offset(6.dp.toPx(), -6.dp.toPx()) else Offset.Zero

    // Outer Bezel Ring
    drawCircle(
        brush = Brush.radialGradient(
            listOf(SurfaceControlRaised, SurfaceCanvas),
            center = center,
            radius = outerRadius
        ),
        radius = outerRadius,
        center = center
    )
    drawCircle(
        color = ControlBorderGlow.copy(alpha = 0.5f),
        radius = outerRadius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 12 Circumference Ticks
    for (i in 0 until 12) {
        val angleRad = (i * 30.0) * (Math.PI / 180.0)
        val p1 = Offset(
            center.x + (outerRadius - 4.dp.toPx()) * cos(angleRad).toFloat(),
            center.y + (outerRadius - 4.dp.toPx()) * sin(angleRad).toFloat()
        )
        val p2 = Offset(
            center.x + outerRadius * cos(angleRad).toFloat(),
            center.y + outerRadius * sin(angleRad).toFloat()
        )
        drawLine(ControlBorderSubtle, p1, p2, 1.2.dp.toPx())
    }

    // Inner Stick Cap
    val capCenter = center + stickOffset
    drawCircle(
        color = Color(0xFF101418),
        radius = innerRadius,
        center = capCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(SurfaceControlRaised, Color(0xFF1C2228)),
            center = capCenter,
            radius = innerRadius
        ),
        radius = innerRadius - 2.dp.toPx(),
        center = capCenter
    )
    // Grip Ring
    drawCircle(
        color = if (isPressed) XboxGreenA else PrimaryBlue.copy(alpha = 0.6f),
        radius = innerRadius - 6.dp.toPx(),
        center = capCenter,
        style = Stroke(width = 1.5.dp.toPx())
    )
    // Center Text Identifier (LS / RS)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 11.sp.toPx() * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(if (isLeft) "LS" else "RS", capCenter.x, capCenter.y + 4.dp.toPx(), paint)
    }
}

/**
 * D-Pad drawing
 */
private fun DrawScope.drawDPad(center: Offset, scale: Float, isPressed: Boolean) {
    val armLen = 42.dp.toPx() * scale
    val armWidth = 26.dp.toPx() * scale
    val halfW = armWidth / 2f

    // Draw horizontal and vertical rounded crossbars
    val crossColor = if (isPressed) ActiveControlFill else SurfaceControlRaised
    // Horizontal bar
    drawRoundRect(
        color = crossColor,
        topLeft = Offset(center.x - armLen, center.y - halfW),
        size = Size(armLen * 2f, armWidth),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    // Vertical bar
    drawRoundRect(
        color = crossColor,
        topLeft = Offset(center.x - halfW, center.y - armLen),
        size = Size(armWidth, armLen * 2f),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    // Outline
    drawRoundRect(
        color = ControlBorderGlow.copy(alpha = 0.6f),
        topLeft = Offset(center.x - armLen, center.y - halfW),
        size = Size(armLen * 2f, armWidth),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(1.2.dp.toPx())
    )
    drawRoundRect(
        color = ControlBorderGlow.copy(alpha = 0.6f),
        topLeft = Offset(center.x - halfW, center.y - armLen),
        size = Size(armWidth, armLen * 2f),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(1.2.dp.toPx())
    )

    // Center concave dish
    drawCircle(
        color = SurfaceCanvas,
        radius = 10.dp.toPx() * scale,
        center = center
    )

    // Directional Arrow Chevrons
    val arrowDist = 26.dp.toPx() * scale
    drawDirectionalChevron(center + Offset(0f, -arrowDist), 0f, scale) // UP
    drawDirectionalChevron(center + Offset(arrowDist, 0f), 90f, scale) // RIGHT
    drawDirectionalChevron(center + Offset(0f, arrowDist), 180f, scale) // DOWN
    drawDirectionalChevron(center + Offset(-arrowDist, 0f), 270f, scale) // LEFT
}

private fun DrawScope.drawDirectionalChevron(pos: Offset, rotationDeg: Float, scale: Float) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        textSize = 9.sp.toPx() * scale
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    val glyph = when (rotationDeg) {
        0f -> "▲"
        90f -> "▶"
        180f -> "▼"
        else -> "◀"
    }
    drawContext.canvas.nativeCanvas.drawText(glyph, pos.x, pos.y + 3.dp.toPx(), paint)
}

/**
 * ABXY Action Cluster drawing
 */
private fun DrawScope.drawABXYCluster(center: Offset, scale: Float, isPressed: Boolean) {
    val baseRadius = 46.dp.toPx() * scale
    val btnDist = 24.dp.toPx() * scale
    val btnRadius = 14.dp.toPx() * scale

    // Base plate
    drawCircle(
        brush = Brush.radialGradient(
            listOf(SurfaceControlRaised.copy(alpha = 0.8f), SurfaceDefault),
            center = center,
            radius = baseRadius
        ),
        radius = baseRadius,
        center = center
    )
    drawCircle(
        color = ControlBorderSubtle,
        radius = baseRadius,
        center = center,
        style = Stroke(1.2.dp.toPx())
    )

    // 4 Buttons: Y (Top), B (Right), A (Bottom), X (Left)
    drawSingleActionFaceButton(center + Offset(0f, -btnDist), btnRadius, "Y", XboxYellowY, scale, isPressed)
    drawSingleActionFaceButton(center + Offset(btnDist, 0f), btnRadius, "B", XboxRedB, scale, isPressed)
    drawSingleActionFaceButton(center + Offset(0f, btnDist), btnRadius, "A", XboxGreenA, scale, isPressed)
    drawSingleActionFaceButton(center + Offset(-btnDist, 0f), btnRadius, "X", XboxBlueX, scale, isPressed)
}

private fun DrawScope.drawSingleActionFaceButton(
    btnCenter: Offset,
    radius: Float,
    letter: String,
    accentColor: Color,
    scale: Float,
    isPressed: Boolean
) {
    // Drop shadow
    drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = radius + 1.dp.toPx(), center = btnCenter + Offset(0f, 2.dp.toPx()))

    // Button body
    drawCircle(
        brush = Brush.radialGradient(
            listOf(SurfaceControlRaised, Color(0xFF14181C)),
            center = btnCenter,
            radius = radius
        ),
        radius = radius,
        center = btnCenter
    )
    // Colored glowing rim
    drawCircle(
        color = accentColor,
        radius = radius,
        center = btnCenter,
        style = Stroke(1.5.dp.toPx())
    )

    // Letter Glyph
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = accentColor.hashCode()
            textSize = 12.sp.toPx() * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(letter, btnCenter.x, btnCenter.y + 4.5.dp.toPx() * scale, paint)
    }
}

/**
 * Triggers & Bumpers (LT/LB or RT/RB)
 */
private fun DrawScope.drawTriggerBumper(
    center: Offset,
    scale: Float,
    isLeft: Boolean,
    isPressed: Boolean
) {
    val width = 76.dp.toPx() * scale
    val height = 36.dp.toPx() * scale
    val halfW = width / 2f
    val halfH = height / 2f

    // Trigger Capsule Body
    val bodyColor = if (isPressed) ActiveControlFill else SurfaceControlRaised
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(width, height),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )
    drawRoundRect(
        color = PrimaryBlue.copy(alpha = 0.6f),
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(width, height),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        style = Stroke(1.2.dp.toPx())
    )

    // Travel Pressure Groove
    val grooveWidth = width * 0.75f
    drawLine(
        color = if (isLeft) XboxBlueX else XboxGreenA,
        start = Offset(center.x - grooveWidth / 2f, center.y + 8.dp.toPx()),
        end = Offset(center.x + grooveWidth / 2f, center.y + 8.dp.toPx()),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Label Text (LT / LB or RT / RB)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10.sp.toPx() * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        val label = if (isLeft) "LT • LB" else "RB • RT"
        drawText(label, center.x, center.y - 2.dp.toPx(), paint)
    }
}

/**
 * Auxiliary / System Buttons (View / Menu)
 */
private fun DrawScope.drawAuxButtons(center: Offset, scale: Float, isPressed: Boolean) {
    val width = 56.dp.toPx() * scale
    val height = 24.dp.toPx() * scale
    val halfW = width / 2f
    val halfH = height / 2f

    drawRoundRect(
        color = SurfaceControl,
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawRoundRect(
        color = ControlBorderSubtle,
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        style = Stroke(1.dp.toPx())
    )

    // Icons: View (2 overlapping squares) & Menu (3 lines)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 9.sp.toPx() * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText("VIEW ❙ ☰", center.x, center.y + 3.dp.toPx(), paint)
    }
}

/**
 * Rear Paddles (P1 / P2)
 */
private fun DrawScope.drawRearPaddle(
    center: Offset,
    scale: Float,
    isLeft: Boolean,
    label: String,
    isPressed: Boolean
) {
    val width = 48.dp.toPx() * scale
    val height = 28.dp.toPx() * scale
    val halfW = width / 2f
    val halfH = height / 2f

    drawRoundRect(
        color = if (isPressed) XboxGreenA.copy(alpha = 0.3f) else SurfaceControlRaised,
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawRoundRect(
        color = if (isPressed) XboxGreenA else ControlBorderGlow,
        topLeft = Offset(center.x - halfW, center.y - halfH),
        size = Size(width, height),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        style = Stroke(1.2.dp.toPx())
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10.sp.toPx() * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText(label, center.x, center.y + 3.5.dp.toPx(), paint)
    }
}

/**
 * Turbo / Rapid Fire Button
 */
private fun DrawScope.drawTurboButton(center: Offset, scale: Float, isPressed: Boolean) {
    val radius = 22.dp.toPx() * scale

    drawCircle(
        color = if (isPressed) XboxYellowY else Color(0xFF261D12),
        radius = radius,
        center = center
    )
    drawCircle(
        color = XboxYellowY,
        radius = radius,
        center = center,
        style = Stroke(1.5.dp.toPx())
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = if (isPressed) android.graphics.Color.BLACK else android.graphics.Color.YELLOW
            textSize = 11.sp.toPx() * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawText("⚡TURBO", center.x, center.y + 4.dp.toPx(), paint)
    }
}

/**
 * Selected Element Inspector Bottom Dock with Quick Actions
 */
@Composable
private fun SelectedElementBottomDock(
    elementId: ControllerElementId,
    config: ElementLayoutConfig,
    onScaleChange: (Float) -> Unit,
    onCenterH: () -> Unit,
    onCenterV: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Element Title & Coordinate Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryContainerBlue)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = elementId.displayName.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "X: ${config.xPercent.roundToInt()}%  Y: ${config.yPercent.roundToInt()}%",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // Scale Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(180.dp)
        ) {
            Text("Scale", color = TextTertiary, fontSize = 10.sp)
            Slider(
                value = config.scale,
                onValueChange = onScaleChange,
                valueRange = 0.7f..1.5f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryContainerBlue,
                    inactiveTrackColor = SurfaceControl
                ),
                modifier = Modifier.weight(1f)
            )
            Text("${String.format("%.2f", config.scale)}x", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // Alignment & Delete buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onCenterH,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Center X", fontSize = 10.sp)
            }

            Button(
                onClick = onCenterV,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Center Y", fontSize = 10.sp)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusError.copy(alpha = 0.15f))
                    .testTag("delete_selected_element_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Element",
                    tint = StatusError,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
