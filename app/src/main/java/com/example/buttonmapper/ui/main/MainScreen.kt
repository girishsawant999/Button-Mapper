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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.example.buttonmapper.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.buttonmapper.ButtonMapperService
import com.example.buttonmapper.KeyDetectionRegistry
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.StorageHelper
import androidx.activity.compose.BackHandler
import androidx.navigation3.runtime.NavKey

enum class Tab {
    KeyBindings
}

sealed interface ActivePanel {
    object AddKeyMapping : ActivePanel
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
        isFocused -> if (isDark) PastelPrimaryDark else PastelPrimaryLight
        isSelected -> if (isDark) PastelBorderDark.copy(alpha = 0.8f) else PastelFocusBgLight
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> if (isDark) Color.Black else Color.White
        isSelected -> if (isDark) PastelPrimaryDark else PastelOnPrimaryLight
        else -> if (isDark) PastelSubtitleDark else PastelSubtitleLight
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

    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(Tab.KeyBindings) }
    var activePanel by remember { mutableStateOf<ActivePanel?>(null) }
    var showToast by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val enabledAcc = isAccessibilityServiceEnabled(context, ButtonMapperService::class.java)
                if (!enabledAcc) {
                    showToast = true
                }
                isAccessibilityEnabled = enabledAcc
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
            !isAccessibilityEnabled -> "Accessibility Service permission is required!"
            else -> null
        }
        msg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
        showToast = false
    }

    var isPanelInputFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = activePanel != null && !isPanelInputFocused) {
        activePanel = null
    }

    val isDark = isSystemInDarkTheme()
    val backgroundGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(PastelBgStartDark, PastelCardDark)
        } else {
            listOf(PastelBgStartLight, PastelCardLight)
        }
    )
    val textColor = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) PastelCardDark else Color.White
    val borderColor = if (isDark) PastelBorderDark else PastelBorderLight
    val subtitleColor = if (isDark) PastelSubtitleDark else PastelSubtitleLight
    val headerCardBg = if (isDark) PastelBorderDark.copy(alpha = 0.5f) else PastelCardLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(16.dp)
    ) {
        val isPanelOpen = activePanel != null

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel (Sidebar): Header, Menu Items, Permissions (Bottom)
            if (!isPanelOpen) {
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
                            color = if (isDark) PastelPrimaryDark else PastelPrimaryLight
                        )
                        Text(
                            text = "Native Android TV key remapping",
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
                    onClick = { selectedTab = Tab.KeyBindings; activePanel = null }
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
                                containerColor = if (isDark) PastelPrimaryDark else PastelPrimaryLight,
                                focusedContainerColor = if (isDark) PastelFocusBgDark else PastelFocusBgLight,
                                contentColor = if (isDark) PastelOnPrimaryDark else PastelOnPrimaryLight,
                                focusedContentColor = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Accessibility", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            } // Close if (!isPanelOpen)

            // Center Panel: Content switching based on selected tab
            Column(
                modifier = Modifier
                    .weight(if (isPanelOpen) 1f else 0.65f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is MainScreenUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = if (isDark) PastelPrimaryDark else PastelPrimaryLight)
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
                                    onAddClick = { activePanel = ActivePanel.AddKeyMapping },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Right Panel: Side-panel for Add/Edit forms
            AnimatedVisibility(
                visible = isPanelOpen,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Card(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                        .onFocusChanged { state ->
                            isPanelInputFocused = state.hasFocus
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) PastelCardDark else PastelCardLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) PastelBorderDark else PastelBorderLight)
                ) {
                    when (val panel = activePanel) {
                        is ActivePanel.AddKeyMapping -> {
                            val installedApps by viewModel.installedApps.collectAsState()
                            AddKeyMappingPanel(
                                installedApps = installedApps,
                                onAdd = { keyCode, action ->
                                    viewModel.addKeyMapping(context, KeyMapping(keyCode, action))
                                    activePanel = null
                                },
                                onCancel = { activePanel = null }
                            )
                        }

                        null -> { /* Panel not active */ }
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
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val cardBg = if (isDark) PastelCardDark else PastelCardLight
    val borderColor = if (isDark) PastelBorderDark else PastelBorderLight

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
                    onClick = onAddClick,
                    containerColor = if (isDark) PastelPrimaryDark else PastelPrimaryLight,
                    focusedContainerColor = if (isDark) PastelFocusBgDark else PastelFocusBgLight,
                    contentColor = if (isDark) PastelOnPrimaryDark else PastelOnPrimaryLight,
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
                                        else -> Icons.Default.Build
                                    },
                                    contentDescription = "System Icon",
                                    tint = if (isDark) PastelPrimaryDark else PastelPrimaryLight,
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
                                    color = if (isDark) PastelSubtitleDark else PastelSubtitleLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TvHoldToDeleteButton(
                            onDelete = { viewModel.removeKeyMapping(context, mapping.keyCode) },
                            containerColor = if (isDark) PastelDangerDark else PastelDangerLight,
                            focusedContainerColor = if (isDark) PastelDangerFocusedDark else PastelDangerFocusedLight,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Hold to Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Side panel handles adding now
}

@Composable
fun AddKeyMappingPanel(
    installedApps: List<Pair<String, String>>,
    onAdd: (Int, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val labelColor = if (isDark) PastelSubtitleDark else PastelSubtitleLight

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var keyCodeStr by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("volume_up") }
    var showActionSelector by remember { mutableStateOf(false) }

    val actionOptions = remember(installedApps) {
        listOf(
            "volume_up" to "Volume Up",
            "volume_down" to "Volume Down"
        ) + installedApps.map { it.second to "${it.first}" }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Add Key Mapping",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) PastelPrimaryDark else PastelPrimaryLight
            )
        }

        item {
            val helperText = when {
                !isKeyCodeFieldFocused -> "Focus the Key Code field to auto-detect"
                keyCodeStr.isEmpty() -> "Press any key on remote to detect keycode..."
                else -> "Detected Key Code: $keyCodeStr"
            }
            val helperColor = when {
                !isKeyCodeFieldFocused -> labelColor
                keyCodeStr.isEmpty() -> if (isDark) PastelSubtitleDark else PastelPrimaryLight
                else -> if (isDark) PastelPrimaryDark else PastelPrimaryLight
            }

            Text(
                text = helperText,
                color = helperColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        item {
            OutlinedTextField(
                value = keyCodeStr,
                onValueChange = { keyCodeStr = it },
                label = { Text("Key Code") },
                placeholder = { Text("Press key which you want to map") },
                readOnly = true,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) PastelPrimaryDark else PastelPrimaryLight,
                    focusedLabelColor = if (isDark) PastelPrimaryDark else PastelPrimaryLight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isKeyCodeFieldFocused = it.isFocused
                    }
            )
        }

        item {
            Column {
                Text("Select Action", fontSize = 12.sp, color = labelColor)
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
                            color = if (isFocused) {
                                if (isDark) PastelPrimaryDark else PastelPrimaryLight
                            } else {
                                if (isDark) PastelBorderDark else PastelBorderLight
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(
                            if (isDark) PastelInputBgDark.copy(alpha = 0.5f)
                            else PastelInputBgLight.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedOptionText,
                            color = textColor,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = labelColor
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TvTextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                TvButton(
                    onClick = {
                        val code = keyCodeStr.toIntOrNull()
                        if (code != null) {
                            onAdd(code, action)
                        } else {
                            Toast.makeText(context, "Please enter a valid key code integer", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = if (isDark) PastelPrimaryDark else PastelPrimaryLight,
                    focusedContainerColor = if (isDark) PastelFocusBgDark else PastelFocusBgLight,
                    contentColor = if (isDark) PastelOnPrimaryDark else PastelOnPrimaryLight,
                    focusedContentColor = Color.Black
                ) {
                    Text("Add")
                }
            }
        }
    }

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
fun AddKeyMappingDialog(
    installedApps: List<Pair<String, String>>,
    onAdd: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var keyCodeStr by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("volume_up") }
    var showActionSelector by remember { mutableStateOf(false) }

    val actionOptions = remember(installedApps) {
        listOf(
            "volume_up" to "Volume Up",
            "volume_down" to "Volume Down"
        ) + installedApps.map { it.second to "${it.first}" }
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
                    keyCodeStr.isEmpty() -> PastelSubtitleDark
                    else -> PastelPrimaryDark
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
                    label = { Text("Key Code") },
                    placeholder = { Text("Press key which you want to map") },
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PastelPrimaryDark,
                        focusedLabelColor = PastelPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
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
                                color = if (isFocused) PastelPrimaryDark else PastelBorderDark,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(PastelInputBgDark.copy(alpha = 0.5f))
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
                containerColor = PastelPrimaryDark,
                focusedContainerColor = PastelFocusBgDark,
                contentColor = PastelOnPrimaryDark,
                focusedContentColor = PastelOnPrimaryDark
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
            colors = CardDefaults.cardColors(containerColor = PastelCardDark),
            border = BorderStroke(1.dp, PastelBorderDark),
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
                        focusedBorderColor = PastelPrimaryDark,
                        focusedLabelColor = PastelPrimaryDark,
                        unfocusedBorderColor = PastelBorderDark,
                        focusedContainerColor = PastelBgStartDark,
                        unfocusedContainerColor = PastelBgStartDark
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
                                             option.first != "launch_app"
                                             
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isFocused) PastelPrimaryDark.copy(alpha = 0.25f) else PastelBorderDark.copy(alpha = 0.3f))
                                        .border(
                                            width = 1.dp,
                                            color = PastelBorderDark,
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
                                                else -> Icons.Default.Build
                                            },
                                            contentDescription = "System Icon",
                                            tint = PastelPrimaryDark,
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
    containerColor: Color = PastelCardDark,
    focusedContainerColor: Color = PastelPrimaryDark,
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
fun TvHoldToDeleteButton(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PastelCardDark,
    focusedContainerColor: Color = PastelPrimaryDark,
    contentColor: Color = Color.White,
    focusedContentColor: Color = Color.Black,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
            if (progress.value >= 1f) {
                onDelete()
                progress.snapTo(0f)
            }
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    Button(
        onClick = {}, // Handled by hold gesture
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) focusedContainerColor else containerColor,
            contentColor = if (isFocused) focusedContentColor else contentColor
        ),
        shape = shape,
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                // Draw the actual content (text)
                drawContent()
                
                // Draw red progress line at the bottom
                if (progress.value > 0f) {
                    val lineHeight = 3.dp.toPx()
                    val fillWidth = size.width * progress.value
                    
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val clipPath = when (outline) {
                        is Outline.Rectangle -> {
                            Path().apply {
                                addRect(outline.rect)
                            }
                        }
                        is Outline.Rounded -> {
                            Path().apply {
                                addRoundRect(outline.roundRect)
                            }
                        }
                        is Outline.Generic -> outline.path
                    }
                    
                    clipPath(clipPath) {
                        drawRect(
                            color = Color(0xFFE53935), // Vibrant red progress line
                            topLeft = Offset(x = 0f, y = size.height - lineHeight),
                            size = Size(width = fillWidth, height = lineHeight)
                        )
                    }
                }
            }
    ) {
        content()
    }
}

@Composable
fun TvTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.LightGray,
    focusedContentColor: Color = PastelPrimaryDark,
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
