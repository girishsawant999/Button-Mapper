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
import com.example.buttonmapper.ScheduledAlarm
import androidx.navigation3.runtime.NavKey

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
    val logMessages by viewModel.logMessages.collectAsState()
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
        viewModel.appendLog("MainScreen UI Launched")
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

    // Gorgeous dark theme colors
    val darkBgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF111827), Color(0xFF1F2937))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkBgGradient)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Panel: Header, Permissions, Diagnostics
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF374151).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Button Mapper",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF)
                        )
                        Text(
                            text = "Native Android TV key remapping & task automation",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Permissions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Required Permissions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        // Accessibility Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Accessibility Service", fontSize = 13.sp, color = Color.LightGray)
                            Text(
                                text = if (isAccessibilityEnabled) "Active ✓" else "Inactive ✕",
                                fontSize = 13.sp,
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Accessibility", fontSize = 12.sp)
                            }
                        }

                        // Write Settings Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Modify System Settings", fontSize = 13.sp, color = Color.LightGray)
                            Text(
                                text = if (isWriteSettingsEnabled) "Granted ✓" else "Denied ✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWriteSettingsEnabled) Color.Green else Color.Red
                            )
                        }
                        if (!isWriteSettingsEnabled) {
                            Text(
                                text = "To enable: Go to Settings > Apps > Special app access > Modify system settings > Button Mapper, and select Allow.",
                                fontSize = 11.sp,
                                color = Color(0xFFCCC2DC),
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Diagnostics/Log Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Diagnostic Logs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCCC2DC)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logMessages.takeLast(10)) { msg ->
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // Right Panel: Mappings & Alarms configuration
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is MainScreenUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
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
                        MainScreenContent(
                            keyMappings = success.keyMappings,
                            scheduledAlarms = success.scheduledAlarms,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    keyMappings: List<KeyMapping>,
    scheduledAlarms: List<ScheduledAlarm>,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddKeyMappingDialog by remember { mutableStateOf(false) }
    var showAddAlarmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Key Mappings Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Mappings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TvButton(
                    onClick = { showAddKeyMappingDialog = true },
                    containerColor = Color(0xFFD0BCFF),
                    focusedContainerColor = Color.White,
                    contentColor = Color.Black,
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF374151).copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No key mappings configured yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(keyMappings) { mapping ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                    border = BorderStroke(1.dp, Color(0xFF374151))
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
                                        .size(32.dp)
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
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 12.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Key Code: ${mapping.keyCode}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Action: ${getActionLabel(context, mapping.action)}",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Delete", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Scheduled Alarms Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Alarms",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TvButton(
                    onClick = { showAddAlarmDialog = true },
                    containerColor = Color(0xFFD0BCFF),
                    focusedContainerColor = Color.White,
                    contentColor = Color.Black,
                    focusedContentColor = Color.Black
                ) {
                    Text("Add Alarm", fontSize = 12.sp)
                }
            }
        }

        if (scheduledAlarms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF374151).copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No scheduled alarms configured yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(scheduledAlarms) { alarm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                    border = BorderStroke(1.dp, Color(0xFF374151))
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
                                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0BCFF)
                            )
                            val maxVol = remember {
                                val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                                am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            }
                            val percentVal = if (alarm.action == "volume") {
                                Math.round((alarm.value.toFloat() / maxVol) * 100)
                            } else {
                                Math.round((alarm.value.toFloat() / 255f) * 100)
                            }
                            Text(
                                text = "Action: ${alarm.action.replaceFirstChar { it.uppercase() }} | Value: $percentVal% (ID: ${alarm.id})",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                        TvButton(
                            onClick = { viewModel.removeScheduledAlarm(context, alarm.id) },
                            containerColor = Color(0xFFEF4444),
                            focusedContainerColor = Color(0xFFFCA5A5),
                            contentColor = Color.White,
                            focusedContentColor = Color.Black,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Delete", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddKeyMappingDialog) {
        AddKeyMappingDialog(
            onAdd = { keyCode, action ->
                viewModel.addKeyMapping(context, KeyMapping(keyCode, action))
                showAddKeyMappingDialog = false
            },
            onDismiss = { showAddKeyMappingDialog = false }
        )
    }

    if (showAddAlarmDialog) {
        AddAlarmDialog(
            onAdd = { id, hour, minute, action, value ->
                viewModel.addScheduledAlarm(context, ScheduledAlarm(id, hour, minute, action, value))
                showAddAlarmDialog = false
            },
            onDismiss = { showAddAlarmDialog = false }
        )
    }
}

@Composable
fun AddKeyMappingDialog(
    onAdd: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyCodeStr by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("volume_up") }
    var showActionSelector by remember { mutableStateOf(false) }

    val pm = context.packageManager
    val apps = remember {
        try {
            pm.getInstalledPackages(0).mapNotNull { pkg ->
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                val appLabel = appInfo.loadLabel(pm).toString()
                appLabel to pkg.packageName
            }
            .filter { it.second != context.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val actionOptions = remember {
        listOf(
            "volume_up" to "Volume Up",
            "volume_down" to "Volume Down",
            "brightness_up" to "Brightness Up",
            "brightness_down" to "Brightness Down",
            "launch_app" to "Launch Button Mapper App"
        ) + apps.map { it.second to "Launch: ${it.first}" }
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
fun AddAlarmDialog(
    onAdd: (String, Int, Int, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var id by remember { mutableStateOf("") }
    var hourStr by remember { mutableStateOf("") }
    var minuteStr by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("volume") }
    
    val percentOptions = remember { listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100) }
    var selectedPercent by remember { mutableStateOf(50) } // Default 50%
    var expandedPercentDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scheduled Alarm", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("Unique Alarm ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        focusedLabelColor = Color(0xFFD0BCFF)
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourStr,
                        onValueChange = { hourStr = it },
                        label = { Text("Hour (0-23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            focusedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
                    OutlinedTextField(
                        value = minuteStr,
                        onValueChange = { minuteStr = it },
                        label = { Text("Min (0-59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            focusedLabelColor = Color(0xFFD0BCFF)
                        )
                    )
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
                    val hour = hourStr.toIntOrNull()
                    val minute = minuteStr.toIntOrNull()
                    val calculatedValue = if (action == "volume") {
                        Math.round(maxVolume * (selectedPercent / 100f))
                    } else {
                        Math.round(255 * (selectedPercent / 100f)).coerceIn(10, 255)
                    }

                    if (id.trim().isEmpty()) {
                        Toast.makeText(context, "ID cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (hour == null || hour !in 0..23) {
                        Toast.makeText(context, "Hour must be 0-23", Toast.LENGTH_SHORT).show()
                    } else if (minute == null || minute !in 0..59) {
                        Toast.makeText(context, "Minute must be 0-59", Toast.LENGTH_SHORT).show()
                    } else {
                        onAdd(id.trim(), hour, minute, action, calculatedValue)
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

fun getActionLabel(context: Context, action: String): String {
    return when (action) {
        "volume_up" -> "Volume Up"
        "volume_down" -> "Volume Down"
        "brightness_up" -> "Brightness Up"
        "brightness_down" -> "Brightness Down"
        "launch_app" -> "Launch Button Mapper App"
        else -> {
            val pm = context.packageManager
            try {
                val info = pm.getApplicationInfo(action, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                action
            }
        }
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
