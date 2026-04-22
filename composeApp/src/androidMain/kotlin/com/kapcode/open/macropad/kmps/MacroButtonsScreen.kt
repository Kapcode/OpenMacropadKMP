package com.kapcode.open.macropad.kmps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.models.SliderUpdateMode
import com.kapcode.open.macropad.kmps.models.WidgetType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val GoldCurrencyColor = Color(0xFFFFD700)

object IconMapper {
    fun getIcon(name: String?): ImageVector {
        return when (name?.lowercase()) {
            "volume_up", "volumeup" -> Icons.Default.VolumeUp
            "volume_down", "volumedown" -> Icons.Default.VolumeDown
            "volume_off", "volumeoff" -> Icons.Default.VolumeOff
            "play", "play_arrow" -> Icons.Default.PlayArrow
            "pause" -> Icons.Default.Pause
            "stop" -> Icons.Default.Stop
            "skip_next", "next" -> Icons.Default.SkipNext
            "skip_previous", "prev" -> Icons.Default.SkipPrevious
            "brightness_high", "brightness" -> Icons.Default.BrightnessHigh
            "brightness_low" -> Icons.Default.BrightnessLow
            "mic", "microphone" -> Icons.Default.Mic
            "mic_off" -> Icons.Default.MicOff
            "videocam", "video" -> Icons.Default.Videocam
            "videocam_off" -> Icons.Default.VideocamOff
            "lightbulb", "light" -> Icons.Default.Lightbulb
            "settings" -> Icons.Default.Settings
            "home" -> Icons.Default.Home
            "search" -> Icons.Default.Search
            "refresh" -> Icons.Default.Refresh
            "close" -> Icons.Default.Close
            "done", "check" -> Icons.Default.Done
            "add" -> Icons.Default.Add
            "remove" -> Icons.Default.Remove
            "power" -> Icons.Default.PowerSettingsNew
            "bluetooth" -> Icons.Default.Bluetooth
            "wifi" -> Icons.Default.Wifi
            "keyboard" -> Icons.Default.Keyboard
            "mouse" -> Icons.Default.Mouse
            else -> Icons.Default.Extension
        }
    }
}

@Composable
fun EmptyPacksPlaceholder(onNavigateToMarket: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Extension,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "No Macro Packs Installed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Macro Packs are collections of specialized buttons for specific apps (like Photoshop, VS Code, or Games). They automatically activate when you switch apps!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateToMarket,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Browse Marketplace")
        }
    }
}

@Composable
fun NavigationHeader(
    packs: List<MacroPack>,
    activePack: MacroPack?,
    onPackSelected: (MacroPack) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(packs) { pack ->
            val isSelected = pack.id == activePack?.id
            FilterChip(
                selected = isSelected,
                onClick = { onPackSelected(pack) },
                label = { Text(pack.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search Macro Packs...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun MacroPicker(
    macros: List<String>,
    onMacroSelected: (String, WidgetType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMacro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedMacro == null) "Select Macro" else "Select Widget Type") },
        text = {
            if (selectedMacro == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    items(macros) { macro ->
                        ListItem(
                            headlineContent = { Text(macro) },
                            modifier = Modifier.clickable {
                                selectedMacro = macro
                            },
                            leadingContent = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf(
                        WidgetType.BUTTON,
                        WidgetType.TOGGLE,
                        WidgetType.SLIDER_HORIZONTAL,
                        WidgetType.SLIDER_VERTICAL
                    )
                    types.forEach { type ->
                        ListItem(
                            headlineContent = { 
                                Text(when(type) {
                                    WidgetType.BUTTON -> "Button"
                                    WidgetType.TOGGLE -> "Toggle"
                                    WidgetType.SLIDER_HORIZONTAL -> "Slider (Horizontal)"
                                    WidgetType.SLIDER_VERTICAL -> "Slider (Vertical)"
                                })
                            },
                            modifier = Modifier.clickable {
                                onMacroSelected(selectedMacro!!, type)
                                onDismiss()
                            },
                            leadingContent = {
                                Icon(
                                    when(type) {
                                        WidgetType.BUTTON -> Icons.Default.SmartButton
                                        WidgetType.TOGGLE -> Icons.Default.ToggleOn
                                        WidgetType.SLIDER_HORIZONTAL -> Icons.Default.LinearScale
                                        WidgetType.SLIDER_VERTICAL -> Icons.Default.SettingsInputComponent
                                    },
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedMacro != null) {
                    selectedMacro = null
                } else {
                    onDismiss()
                }
            }) {
                Text(if (selectedMacro != null) "Back" else "Cancel")
            }
        }
    )
}

@Composable
fun MacroGridItem(
    widget: GridWidget,
    isExecuting: Boolean,
    isError: Boolean,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: IntOffset = IntOffset.Zero,
    currency: Long = 0,
    onInteraction: (GridWidget) -> Unit,
    onLongClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shakeRotation by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .padding(4.dp)
            .offset { if (isDragging) dragOffset else IntOffset.Zero }
            .graphicsLayer {
                if (isEditMode && !isDragging) {
                    rotationZ = shakeRotation
                }
                if (isDragging) {
                    scaleX = 1.1f
                    scaleY = 1.1f
                    shadowElevation = 8.dp.toPx()
                }
                alpha = if (isDragging) 0.9f else 1f
            }
    ) {
        val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer 
                            else if (isDragging) MaterialTheme.colorScheme.secondaryContainer
                            else Color(widget.color)
        
        val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer 
                          else if (isDragging) MaterialTheme.colorScheme.onSecondaryContainer
                          else if (Color(widget.color).luminance() > 0.5f) Color.Black 
                          else Color.White

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (widget.type == WidgetType.SLIDER_VERTICAL) 180.dp else 90.dp)
                .pointerInput(isEditMode) {
                    if (isEditMode) {
                        detectTapGestures(
                            onLongPress = { onLongClick() }
                        )
                    }
                },
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = if (isDragging) 8.dp else 2.dp,
            onClick = {
                if (!isEditMode && (widget.type == WidgetType.BUTTON || widget.type == WidgetType.TOGGLE)) {
                    onInteraction(widget)
                }
            }
        ) {
            when (widget.type) {
                WidgetType.BUTTON -> ButtonContent(widget, isExecuting, currency)
                WidgetType.TOGGLE -> ToggleContent(widget, isExecuting)
                WidgetType.SLIDER_HORIZONTAL -> SliderHorizontalContent(widget, onInteraction)
                WidgetType.SLIDER_VERTICAL -> SliderVerticalContent(widget, onInteraction)
            }
        }
    }
}

@Composable
fun ButtonContent(widget: GridWidget, isExecuting: Boolean, currency: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        Icon(IconMapper.getIcon(widget.icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            widget.label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isExecuting) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.8f).height(2.dp),
                color = LocalContentColor.current
            )
        }
    }
}

@Composable
fun ToggleContent(widget: GridWidget, isExecuting: Boolean) {
    Row(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Icon(IconMapper.getIcon(widget.icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                widget.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = widget.state,
            onCheckedChange = null, // Handled by parent Surface click
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SliderHorizontalContent(widget: GridWidget, onInteraction: (GridWidget) -> Unit) {
    var sliderValue by remember(widget.value) { mutableStateOf(widget.value) }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(widget.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("${sliderValue.toInt()}", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                if (widget.sliderUpdateMode == SliderUpdateMode.LIVE) {
                    onInteraction(widget.copy(value = it))
                }
            },
            onValueChangeFinished = {
                if (widget.sliderUpdateMode == SliderUpdateMode.ON_RELEASE) {
                    onInteraction(widget.copy(value = sliderValue))
                }
            },
            valueRange = widget.minValue..widget.maxValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SliderVerticalContent(widget: GridWidget, onInteraction: (GridWidget) -> Unit) {
    var sliderValue by remember(widget.value) { mutableStateOf(widget.value) }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(widget.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Using a custom rotated slider for vertical orientation
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    if (widget.sliderUpdateMode == SliderUpdateMode.LIVE) {
                        onInteraction(widget.copy(value = it))
                    }
                },
                onValueChangeFinished = {
                    if (widget.sliderUpdateMode == SliderUpdateMode.ON_RELEASE) {
                        onInteraction(widget.copy(value = sliderValue))
                    }
                },
                valueRange = widget.minValue..widget.maxValue,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                        translationX = 0f
                    }
                    .width(120.dp)
            )
        }
        Text("${sliderValue.toInt()}", style = MaterialTheme.typography.labelSmall)
    }
}

// Utility to calculate luminance for text contrast
fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

// Extension to help with Switch scaling
fun Modifier.scale(scale: Float) = graphicsLayer(scaleX = scale, scaleY = scale)

@Composable
fun MacroButtonsScreen(
    widgets: List<GridWidget>,
    executingMacros: Set<String> = emptySet(),
    failedMacros: Set<String> = emptySet(),
    isEditMode: Boolean = false,
    onWidgetInteraction: (GridWidget) -> Unit,
    onWidgetLongClick: (GridWidget) -> Unit = {},
    onRemoveWidget: (GridWidget) -> Unit = {},
    onMoveWidget: (Int, Int) -> Unit = { _, _ -> },
    currency: Long = 0,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isOverTrash by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(isEditMode, widgets) {
                    if (!isEditMode) return@pointerInput
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            gridState.layoutInfo.visibleItemsInfo
                                .find { item ->
                                    offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                                            offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                                }
                                ?.let {
                                    draggingIndex = it.index
                                }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = IntOffset(
                                (dragOffset.x + dragAmount.x).roundToInt(),
                                (dragOffset.y + dragAmount.y).roundToInt()
                            )
                            
                            val currentDraggingIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                            
                            val draggingItem = gridState.layoutInfo.visibleItemsInfo.find { it.index == currentDraggingIndex } ?: return@detectDragGesturesAfterLongPress
                            val centerX = draggingItem.offset.x + draggingItem.size.width / 2 + dragOffset.x
                            val centerY = draggingItem.offset.y + draggingItem.size.height / 2 + dragOffset.y
                            
                            // Trash can collision detection (roughly bottom center)
                            val screenHeight = size.height
                            val screenWidth = size.width
                            isOverTrash = centerY > screenHeight - 120 && centerX in (screenWidth / 2 - 60)..(screenWidth / 2 + 60)

                            if (!isOverTrash) {
                                gridState.layoutInfo.visibleItemsInfo
                                    .find { item ->
                                        item.index != currentDraggingIndex &&
                                        centerX in item.offset.x..(item.offset.x + item.size.width) &&
                                        centerY in item.offset.y..(item.offset.y + item.size.height)
                                    }
                                    ?.let { hoverItem ->
                                        val targetIndex = hoverItem.index
                                        onMoveWidget(currentDraggingIndex, targetIndex)
                                        draggingIndex = targetIndex
                                        
                                        val hoverCenterX = hoverItem.offset.x + hoverItem.size.width / 2
                                        val hoverCenterY = hoverItem.offset.y + hoverItem.size.height / 2
                                        dragOffset = IntOffset(
                                            centerX - hoverCenterX,
                                            centerY - hoverCenterY
                                        )
                                    }
                            }
                        },
                        onDragEnd = {
                            if (isOverTrash && draggingIndex != null) {
                                onRemoveWidget(widgets[draggingIndex!!])
                            }
                            draggingIndex = null
                            dragOffset = IntOffset.Zero
                            isOverTrash = false
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffset = IntOffset.Zero
                            isOverTrash = false
                        }
                    )
                },
            contentPadding = PaddingValues(bottom = 100.dp, start = 8.dp, end = 8.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(widgets, key = { _, widget -> widget.id }) { index, widget ->
                MacroGridItem(
                    widget = widget,
                    isExecuting = executingMacros.contains(widget.macroId),
                    isError = failedMacros.contains(widget.macroId),
                    isEditMode = isEditMode,
                    isDragging = draggingIndex == index,
                    dragOffset = dragOffset,
                    currency = currency,
                    onInteraction = onWidgetInteraction,
                    onLongClick = { onWidgetLongClick(widget) }
                )
            }
        }

        // Trash Can UI
        if (draggingIndex != null) {
            val trashScale by animateFloatAsState(if (isOverTrash) 1.5f else 1f)
            val trashColor by animateColorAsState(if (isOverTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = trashScale
                        scaleY = trashScale
                    }
                    .background(
                        color = if (isOverTrash) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = trashColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
