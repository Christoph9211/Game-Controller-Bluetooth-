package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.CustomLayoutEntity
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxGreenA
import com.example.viewmodel.GamepadViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedLayoutsManagerDialog(
    viewModel: GamepadViewModel,
    onDismiss: () -> Unit
) {
    val savedLayouts by viewModel.savedLayouts.collectAsState()
    val activeLayoutId by viewModel.activeSavedLayoutId.collectAsState()
    val editingLayout by viewModel.editingLayout.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var newLayoutName by remember { mutableStateOf("") }
    var newLayoutDescription by remember { mutableStateOf("") }
    var showSaveSection by remember { mutableStateOf(false) }
    var layoutToDelete by remember { mutableStateOf<CustomLayoutEntity?>(null) }

    val filteredLayouts = remember(savedLayouts, searchQuery) {
        if (searchQuery.isBlank()) savedLayouts
        else savedLayouts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, SurfaceControlRaised, RoundedCornerShape(20.dp))
                    .testTag("saved_layouts_dialog"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDefault)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier=Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainerBlue.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryContainerBlue.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = PrimaryContainerBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Layout profiles",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Saved on this phone • ${savedLayouts.size} saved",
                                    color = TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceControl)
                                .testTag("close_saved_layouts_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Bar: "Save Current Layout As..." button + Search bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showSaveSection = !showSaveSection
                                if (showSaveSection && newLayoutName.isBlank()) {
                                    newLayoutName = "My ${editingLayout.name} Custom"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showSaveSection) ActiveControlFill else PrimaryContainerBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("toggle_save_new_layout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showSaveSection) "Cancel Save" else "Save Current As New",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Search layouts...", color = TextTertiary, fontSize = 12.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("search_layouts_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = ControlBorderGlow,
                                unfocusedBorderColor = ControlBorderSubtle.copy(alpha = 0.5f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Collapsible "Save Current Layout" Section
                    AnimatedVisibility(visible = showSaveSection) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerLow)
                                .border(1.dp, PrimaryContainerBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Save controller layout",
                                color = PrimaryBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Preset Name Suggestion Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val suggestions = listOf("Apex FPS Pro", "Claw Grip", "Fighter 6-Btn", "Racing Sim")
                                suggestions.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceControl)
                                            .clickable { newLayoutName = tag }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newLayoutName,
                                    onValueChange = { newLayoutName = it },
                                    label = { Text("Layout Name", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. CoD Tournament Claw", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("new_layout_name_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceCard,
                                        unfocusedContainerColor = SurfaceCard,
                                        focusedBorderColor = ControlBorderGlow,
                                        unfocusedBorderColor = ControlBorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = newLayoutDescription,
                                    onValueChange = { newLayoutDescription = it },
                                    label = { Text("Description (Optional)", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Right stick shifted + paddles", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("new_layout_description_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceCard,
                                        unfocusedContainerColor = SurfaceCard,
                                        focusedBorderColor = ControlBorderGlow,
                                        unfocusedBorderColor = ControlBorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${editingLayout.elements.size} elements included",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )

                                Button(
                                    onClick = {
                                        viewModel.saveCurrentLayoutAs(newLayoutName, newLayoutDescription)
                                        showSaveSection = false
                                        newLayoutName = ""
                                        newLayoutDescription = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = XboxGreenA),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("confirm_save_layout_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save layout", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Layouts List from Room Database
                    if (filteredLayouts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("No layout configurations found", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Save your current controller layout as a new profile above.", color = TextTertiary, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_layouts_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            filteredLayouts.forEach { layout ->
                                val isActive = layout.id == activeLayoutId
                                LayoutCardItem(
                                    layout = layout,
                                    isActive = isActive,
                                    onLoad = {
                                        viewModel.loadSavedLayout(layout.id)
                                    },
                                    onDuplicate = {
                                        viewModel.duplicateSavedLayout(layout.id)
                                    },
                                    onDelete = {
                                        layoutToDelete = layout
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Confirm Delete Dialog
            if (layoutToDelete != null) {
                Dialog(onDismissRequest = { layoutToDelete = null }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .border(1.dp, StatusError.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Delete Layout Configuration?",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Are you sure you want to permanently delete \"${layoutToDelete?.name}\" from the Room database?",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { layoutToDelete = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel", color = TextPrimary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        layoutToDelete?.id?.let { viewModel.deleteSavedLayout(it) }
                                        layoutToDelete = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("confirm_delete_btn")
                                ) {
                                    Text("Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutCardItem(
    layout: CustomLayoutEntity, isActive: Boolean,
    onLoad: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth().testTag("layout_card_${layout.id}"),
        colors=CardDefaults.cardColors(containerColor=SurfaceCard),shape=RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(layout.name,color=TextPrimary,fontSize=15.sp,fontWeight=FontWeight.Bold,
                maxLines=2,overflow=TextOverflow.Ellipsis)
            if (layout.description.isNotBlank()) Text(layout.description,color=TextSecondary,fontSize=12.sp,
                maxLines=2,overflow=TextOverflow.Ellipsis)
            Text("${layout.elementCount} stored controls" + if (layout.isPreset) " / Built-in" else " / Custom",color=TextTertiary,fontSize=11.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,
                verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onDuplicate,modifier=Modifier.testTag("duplicate_layout_${layout.id}")) {
                    Icon(Icons.Default.ContentCopy,contentDescription="Duplicate ${layout.name}",tint=TextSecondary)
                }
                IconButton(onClick=onDelete,modifier=Modifier.testTag("delete_layout_${layout.id}")) {
                    Icon(Icons.Default.Delete,contentDescription="Delete ${layout.name}",tint=StatusError)
                }
                Button(onClick=onLoad,modifier=Modifier.testTag("load_layout_${layout.id}")) {
                    Text(if (isActive) "Active" else "Load")
                }
            }
        }
    }
}
