package com.example.buttonmapper.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.buttonmapper.ButtonMapperService
import com.example.buttonmapper.KeyDetectionRegistry
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.ScheduledTask
import com.example.buttonmapper.StorageHelper
import androidx.navigation3.runtime.NavKey

enum class Tab {
    KeyBindings,
    Schedules
}

@Composable
fun SidebarMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isDark = isSystemInDarkTheme()

    val containerColor = when {
        isFocused -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
        isSelected -> if (isDark) Color(0xFF374151).copy(alpha = 0.8f) else Color(0xFFE8DEF8)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> if (isDark) Color.Black else Color.White
        isSelected -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF21005D)
        else -> if (isDark) Color.LightGray else Color.DarkGray
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, ButtonMapperService::class.java))
    }
    var isWriteSettingsEnabled by remember {
        mutableStateOf(Settings.System.canWrite(context))
    }

    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(Tab.KeyBindings) }
    var showToast by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val enabledAcc = isAccessibilityServiceEnabled(context, ButtonMapperService::class.java)
                val enabledWrite = Settings.System.canWrite(context)
                if (!enabledAcc || !enabledWrite) {
                    showToast = true
                }
                isAccessibilityEnabled = enabledAcc
                isWriteSettingsEnabled = enabledWrite
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    if (showToast) {
        val msg = when {
            !isAccessibilityEnabled && !isWriteSettingsEnabled -> "Accessibility & Write Settings permissions are required!"
            !isAccessibilityEnabled -> "Accessibility Service permission is required!"
            !isWriteSettingsEnabled -> "Write Settings permission is required!"
            else -> null
        }
        msg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
        showToast = false
    }

    val isDark = isSystemInDarkTheme()
    val backgroundGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF111827), Color(0xFF1F2937))
        } else {
            listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB))
        }
    )
    val textColor = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) Color(0xFF1F2937) else Color.White
    val borderColor = if (isDark) Color(0xFF374151) else Color(0xFFD1D5DB)
    val subtitleColor = if (isDark) Color.LightGray else Color.DarkGray
    val headerCardBg = if (isDark) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFE5E7EB)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Panel (Sidebar): Header, Menu Items, Permissions (Bottom)
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = headerCardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Button Mapper",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                        )
                        Text(
                            text = "Native Android TV key remapping & task automation",
                            fontSize = 12.sp,
                            color = subtitleColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Menu Items
                SidebarMenuItem(
                    text = "Key Bindings",
                    icon = Icons.Default.Build,
                    isSelected = selectedTab == Tab.KeyBindings,
                    onClick = { selectedTab = Tab.KeyBindings }
                )

                SidebarMenuItem(
                    text = "Schedules",
                    icon = Icons.Default.Notifications,
                    isSelected = selectedTab == Tab.Schedules,
                    onClick = { selectedTab = Tab.Schedules }
                )

                // Spacer to push permissions to the bottom
                Spacer(modifier = Modifier.weight(1f))

                // Permissions Card (Bottom of Sidebar)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Required Permissions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        // Accessibility Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Accessibility Service", fontSize = 12.sp, color = subtitleColor)
                            Text(
                                text = if (isAccessibilityEnabled) "Active ✓" else "Inactive ✕",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccessibilityEnabled) Color.Green else Color.Red
                            )
                        }
                        if (!isAccessibilityEnabled) {
                            TvButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                },
                                containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                focusedContainerColor = if (isDark) Color.White else Color(0xFFE8DEF8),
                                contentColor = if (isDark) Color.Black else Color.White,
                                focusedContentColor = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Accessibility", fontSize = 11.sp)
                            }
                        }

                        // Write Settings Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Modify System Settings", fontSize = 12.sp, color = subtitleColor)
                            Text(
                                text = if (isWriteSettingsEnabled) "Granted ✓" else "Denied ✕",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWriteSettingsEnabled) Color.Green else Color.Red
                            )
                        }
                        if (!isWriteSettingsEnabled) {
                            TvButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                },
                                containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                focusedContainerColor = if (isDark) Color.White else Color(0xFFE8DEF8),
                                contentColor = if (isDark) Color.Black else Color.White,
                                focusedContentColor = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Write Settings", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Right Panel: Content switching based on selected tab
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is MainScreenUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4))
                        }
                    }
                    is MainScreenUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error: ${(state as MainScreenUiState.Error).throwable.localizedMessage}",
                                color = Color.Red,
                                fontSize = 16.sp
                            )
                        }
                    }
                    is MainScreenUiState.Success -> {
                        val success = state as MainScreenUiState.Success
                        when (selectedTab) {
                            Tab.KeyBindings -> {
                                KeyBindingsContent(
                                    keyMappings = success.keyMappings,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Tab.Schedules -> {
                                SchedulesContent(
                                    scheduledTasks = success.scheduledTasks,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyBindingsContent(
    keyMappings: List<KeyMapping>,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    var showAddKeyMappingDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) Color(0xFF1F2937) else Color.White
    val borderColor = if (isDark) Color(0xFF374151) else Color(0xFFD1D5DB)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Mappings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                TvButton(
                    onClick = { showAddKeyMappingDialog = true },
                    containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                    focusedContainerColor = if (isDark) Color.White else Color(0xFFE8DEF8),
                    contentColor = if (isDark) Color.Black else Color.White,
                    focusedContentColor = Color.Black
                ) {
                    Text("Add Mapping", fontSize = 12.sp)
                }
            }
        }

        if (keyMappings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No key mappings configured yet.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(keyMappings) { mapping ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isApp = mapping.action != "volume_up" &&
                                     mapping.action != "volume_down" &&
                                     mapping.action != "brightness_up" &&
                                     mapping.action != "brightness_down" &&
                                     mapping.action != "launch_app"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isApp) {
                                AppIcon(
                                    packageName = mapping.action,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(end = 12.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = when(mapping.action) {
                                        "volume_up" -> Icons.Default.KeyboardArrowUp
                                        "volume_down" -> Icons.Default.KeyboardArrowDown
                                        "brightness_up" -> Icons.Default.Add
                                        "brightness_down" -> Icons.Default.Close
                                        else -> Icons.Default.Build
                                    },
                                    contentDescription = "System Icon",
                                    tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(end = 12.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Key Code: ${mapping.keyCode}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Text(
                                    text = "Action: ${StorageHelper.getActionLabel(context, mapping.action)}",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color.LightGray else Color.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TvButton(
                            onClick = { viewModel.removeKeyMapping(context, mapping.keyCode) },
                            containerColor = Color(0xFFEF4444),
                            focusedContainerColor = Color(0xFFFCA5A5),
                            contentColor = Color.White,
                            focusedContentColor = Color.Black,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddKeyMappingDialog) {
        AddKeyMappingDialog(
            installedApps = installedApps,
            onAdd = { keyCode, action ->
                viewModel.addKeyMapping(context, KeyMapping(keyCode, action))
                showAddKeyMappingDialog = false
            },
            onDismiss = { showAddKeyMappingDialog = false }
        )
    }
}

fun getDayOfWeekLabel(day: Int): String {
    return when (day) {
        0 -> "Every Day"
        java.util.Calendar.SUNDAY -> "Sunday"
        java.util.Calendar.MONDAY -> "Monday"
        java.util.Calendar.TUESDAY -> "Tuesday"
        java.util.Calendar.WEDNESDAY -> "Wednesday"
        java.util.Calendar.THURSDAY -> "Thursday"
        java.util.Calendar.FRIDAY -> "Friday"
        java.util.Calendar.SATURDAY -> "Saturday"
        else -> "Unknown"
    }
}

fun getHourLabel(hour: Int): String {
    val ampm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%02d:00 (%d %s)", hour, displayHour, ampm)
}

@Composable
fun SchedulesContent(
    scheduledTasks: List<ScheduledTask>,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) Color(0xFF1F2937) else Color.White
    val borderColor = if (isDark) Color(0xFF374151) else Color(0xFFD1D5DB)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schedules",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                TvButton(
                    onClick = { showAddScheduleDialog = true },
                    containerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                    focusedContainerColor = if (isDark) Color.White else Color(0xFFE8DEF8),
                    contentColor = if (isDark) Color.Black else Color.White,
                    focusedContentColor = Color.Black
                ) {
                    Text("Add Schedule", fontSize = 12.sp)
                }
            }
        }

        if (scheduledTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No schedules configured yet.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(scheduledTasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${getDayOfWeekLabel(task.dayOfWeek)} at ${getHourLabel(task.hour)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                            )
                            val maxVol = remember {
                                val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                                am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            }
                            val percentVal = if (task.action == "volume") {
                                Math.round((task.value.toFloat() / maxVol) * 100)
                            } else {
                                Math.round((task.value.toFloat() / 255f) * 100)
                            }
                            Text(
                                text = "Action: ${task.action.replaceFirstChar { it.uppercase() }} | Value: $percentVal% (ID: ${task.id})",
                                fontSize = 12.sp,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                        }
                        TvButton(
                            onClick = { viewModel.removeScheduledTask(context, task.id) },
                            containerColor = Color(0xFFEF4444),
                            focusedContainerColor = Color(0xFFFCA5A5),
                            contentColor = Color.White,
                            focusedContentColor = Color.Black,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onAdd = { id, dayOfWeek, hour, action, value ->
                viewModel.addScheduledTask(context, ScheduledTask(id, dayOfWeek, hour, action, value))
                showAddScheduleDialog = false
            },
            onDismiss = { showAddScheduleDialog = false }
        )
    }
}



@Composable
fun AddKeyMappingDialog(
    installedApps: List<Pair<String, String>>,
    onAdd: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyCodeStr by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("volume_up") }
    var showActionSelector by remember { mutableStateOf(false) }

    val actionOptions = remember(installedApps) {
        listOf(
            "volume_up" to "Volume Up",
            "volume_down" to "Volume Down",
            "brightness_up" to "Brightness Up",
            "brightness_down" to "Brightness Down",
            "launch_app" to "Launch Button Mapper App"
        ) + installedApps.map { it.second to "Launch: ${it.first}" }
    }

    var selectedOptionText by remember {
        mutableStateOf(actionOptions.first().second)
    }

    var isKeyCodeFieldFocused by remember { mutableStateOf(false) }

    DisposableEffect(isKeyCodeFieldFocused) {
        KeyDetectionRegistry.isDetectionModeActive = isKeyCodeFieldFocused
        KeyDetectionRegistry.onKeyDetected = { detectedCode ->
            keyCodeStr = detectedCode.toString()
        }
        onDispose {
            KeyDetectionRegistry.isDetectionModeActive = false
            KeyDetectionRegistry.onKeyDetected = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Key Mapping", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val helperText = when {
                    !isKeyCodeFieldFocused -> "Focus the Key Code field to auto-detect"
                    keyCodeStr.isEmpty() -> "Press any key on remote to detect keycode..."
                    else -> "Detected Key Code: $keyCodeStr"
                }
                val helperColor = when {
                    !isKeyCodeFieldFocused -> Color.LightGray
                    keyCodeStr.isEmpty() -> Color(0xFFCCC2DC)
                    else -> Color(0xFFD0BCFF)
                }

                Text(
                    text = helperText,
                    color = helperColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = keyCodeStr,
                    onValueChange = { keyCodeStr = it },
                    label = { Text("Key Code (e.g. 24 for Vol Up)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        focusedLabelColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            isKeyCodeFieldFocused = it.isFocused
                        }
                )

                Column {
                    Text("Select Action", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { showActionSelector = true }
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedOptionText,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TvButton(
                onClick = {
                    val code = keyCodeStr.toIntOrNull()
                    if (code != null) {
                        onAdd(code, action)
                    } else {
                        Toast.makeText(context, "Please enter a valid key code integer", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = Color(0xFFD0BCFF),
                focusedContainerColor = Color.White,
                contentColor = Color.Black,
                focusedContentColor = Color.Black
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TvTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showActionSelector) {
        ActionSelectorDialog(
            actionOptions = actionOptions,
            onSelect = { option ->
                action = option.first
                selectedOptionText = option.second
                showActionSelector = false
            },
            onDismiss = { showActionSelector = false }
        )
    }
}

@Composable
fun ActionSelectorDialog(
    actionOptions: List<Pair<String, String>>,
    onSelect: (Pair<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredOptions = remember(searchQuery, actionOptions) {
        if (searchQuery.isBlank()) {
            actionOptions
        } else {
            actionOptions.filter {
                it.second.contains(searchQuery, ignoreCase = true) ||
                it.first.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            border = BorderStroke(1.dp, Color(0xFF374151)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Action or Application",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps or actions...", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        focusedLabelColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (filteredOptions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No matching applications or actions.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredOptions) { option ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()
                                
                                val isApp = option.first != "volume_up" &&
                                             option.first != "volume_down" &&
                                             option.first != "brightness_up" &&
                                             option.first != "brightness_down" &&
                                             option.first != "launch_app"
                                             
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isFocused) Color(0xFFD0BCFF).copy(alpha = 0.25f) else Color(0xFF374151).copy(alpha = 0.3f))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF374151),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { onSelect(option) }
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isApp) {
                                        AppIcon(
                                            packageName = option.first,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .padding(end = 12.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when(option.first) {
                                                "volume_up" -> Icons.Default.KeyboardArrowUp
                                                "volume_down" -> Icons.Default.KeyboardArrowDown
                                                "brightness_up" -> Icons.Default.Add
                                                "brightness_down" -> Icons.Default.Close
                                                else -> Icons.Default.Build
                                            },
                                            contentDescription = "System Icon",
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .padding(end = 12.dp)
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = option.second,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isApp) {
                                            Text(
                                                text = option.first,
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TvTextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    onAdd: (String, Int, Int, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var id by remember { mutableStateOf("") }
    
    val dayOptions = remember {
        listOf(
            0 to "Every Day",
            java.util.Calendar.SUNDAY to "Sunday",
            java.util.Calendar.MONDAY to "Monday",
            java.util.Calendar.TUESDAY to "Tuesday",
            java.util.Calendar.WEDNESDAY to "Wednesday",
            java.util.Calendar.THURSDAY to "Thursday",
            java.util.Calendar.FRIDAY to "Friday",
            java.util.Calendar.SATURDAY to "Saturday"
        )
    }
    var selectedDay by remember { mutableStateOf(0) }
    var expandedDayDropdown by remember { mutableStateOf(false) }

    val hourOptions = remember { (0..23).toList() }
    var selectedHour by remember { mutableStateOf(12) } // Default 12:00 PM
    var expandedHourDropdown by remember { mutableStateOf(false) }

    var action by remember { mutableStateOf("volume") }
    
    val percentOptions = remember { listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100) }
    var selectedPercent by remember { mutableStateOf(50) } // Default 50%
    var expandedPercentDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scheduled Task", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("Unique Schedule ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        focusedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                Column {
                    Text("Day of Week", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val dayInteractionSource = remember { MutableInteractionSource() }
                    val isDayFocused by dayInteractionSource.collectIsFocusedAsState()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = dayInteractionSource,
                                indication = null,
                                onClick = { expandedDayDropdown = true }
                            )
                            .border(
                                width = if (isDayFocused) 2.dp else 1.dp,
                                color = if (isDayFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayOptions.find { it.first == selectedDay }?.second ?: "Monday",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                Column {
                    Text("Hour of Day", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val hourInteractionSource = remember { MutableInteractionSource() }
                    val isHourFocused by hourInteractionSource.collectIsFocusedAsState()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = hourInteractionSource,
                                indication = null,
                                onClick = { expandedHourDropdown = true }
                            )
                            .border(
                                width = if (isHourFocused) 2.dp else 1.dp,
                                color = if (isHourFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getHourLabel(selectedHour),
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                Column {
                    Text("Action Type", fontSize = 12.sp, color = Color.LightGray)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = action == "volume",
                                onClick = { action = "volume" }
                            )
                            Text("Volume", color = Color.White, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = action == "brightness",
                                onClick = { action = "brightness" }
                            )
                            Text("Brightness", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Column {
                    Text("Select Level (Percentage)", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { expandedPercentDropdown = true }
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFF1F2937).copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedPercent%",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TvButton(
                onClick = {
                    val calculatedValue = if (action == "volume") {
                        Math.round(maxVolume * (selectedPercent / 100f))
                    } else {
                        Math.round(255 * (selectedPercent / 100f)).coerceIn(10, 255)
                    }

                    if (id.trim().isEmpty()) {
                        Toast.makeText(context, "ID cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        onAdd(id.trim(), selectedDay, selectedHour, action, calculatedValue)
                    }
                },
                containerColor = Color(0xFFD0BCFF),
                focusedContainerColor = Color.White,
                contentColor = Color.Black,
                focusedContentColor = Color.Black
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TvTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (expandedDayDropdown) {
        Dialog(onDismissRequest = { expandedDayDropdown = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.6f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                border = BorderStroke(1.dp, Color(0xFF374151)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Day of Week",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(dayOptions) { option ->
                            val itemInteractionSource = remember { MutableInteractionSource() }
                            val isItemFocused by itemInteractionSource.collectIsFocusedAsState()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isItemFocused) Color(0xFFD0BCFF).copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isItemFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = null,
                                        onClick = {
                                            selectedDay = option.first
                                            expandedDayDropdown = false
                                        }
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(option.second, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (expandedHourDropdown) {
        Dialog(onDismissRequest = { expandedHourDropdown = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.6f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                border = BorderStroke(1.dp, Color(0xFF374151)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Hour of Day",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(hourOptions) { hr ->
                            val itemInteractionSource = remember { MutableInteractionSource() }
                            val isItemFocused by itemInteractionSource.collectIsFocusedAsState()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isItemFocused) Color(0xFFD0BCFF).copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isItemFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = null,
                                        onClick = {
                                            selectedHour = hr
                                            expandedHourDropdown = false
                                        }
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getHourLabel(hr), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (expandedPercentDropdown) {
        Dialog(onDismissRequest = { expandedPercentDropdown = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.6f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                border = BorderStroke(1.dp, Color(0xFF374151)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Percentage",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(percentOptions) { pct ->
                            val itemInteractionSource = remember { MutableInteractionSource() }
                            val isItemFocused by itemInteractionSource.collectIsFocusedAsState()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isItemFocused) Color(0xFFD0BCFF).copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isItemFocused) Color(0xFFD0BCFF) else Color(0xFF374151),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = null,
                                        onClick = {
                                            selectedPercent = pct
                                            expandedPercentDropdown = false
                                        }
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$pct%", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pm = remember(context) { context.packageManager }
    val iconPainter = remember(packageName, pm) {
        try {
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(
                width = 96,
                height = 96
            )
            BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            null
        }
    }

    if (iconPainter != null) {
        Image(
            painter = iconPainter,
            contentDescription = "App Icon",
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Default Icon",
            tint = Color.LightGray,
            modifier = modifier
        )
    }
}

@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF374151),
    focusedContainerColor: Color = Color(0xFFD0BCFF),
    contentColor: Color = Color.White,
    focusedContentColor: Color = Color.Black,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) focusedContainerColor else containerColor,
            contentColor = if (isFocused) focusedContentColor else contentColor
        ),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun TvTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.LightGray,
    focusedContentColor: Color = Color(0xFFD0BCFF),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isFocused) focusedContentColor else contentColor
        ),
        modifier = modifier
    ) {
        content()
    }
}


fun isAccessibilityServiceEnabled(
    context: Context,
    serviceClass: Class<out android.accessibilityservice.AccessibilityService>
): Boolean {
    val expected = "${context.packageName}/${serviceClass.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
}
