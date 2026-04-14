package com.kapcode.open.macropad.kmps

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapcode.open.macropad.kmps.models.MacroPack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val GoldCurrencyColor = Color(0xFFFFD700)

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
    onMacroSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Macro to Dashboard") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                items(macros) { macro ->
                    ListItem(
                        headlineContent = { Text(macro) },
                        modifier = Modifier.clickable {
                            onMacroSelected(macro)
                            onDismiss()
                        },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MacroButton(
    macroName: String,
    isExecuting: Boolean,
    isError: Boolean,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: IntOffset = IntOffset.Zero,
    currency: Long = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val feedbackAlpha = remember { Animatable(0f) }
    
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(isExecuting) {
        if (isExecuting) {
            feedbackAlpha.animateTo(0.3f, animationSpec = tween(150))
        } else {
            feedbackAlpha.animateTo(0f, animationSpec = tween(300))
        }
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .offset { if (isDragging) dragOffset else IntOffset.Zero }
            .graphicsLayer {
                if (isEditMode && !isDragging) {
                    rotationZ = rotation
                }
                if (isDragging) {
                    scaleX = 1.1f
                    scaleY = 1.1f
                    shadowElevation = 8.dp.toPx()
                }
                alpha = if (isDragging) 0.9f else 1f
            }
    ) {
        Surface(
            onClick = {
                if (!isEditMode) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > 1000) {
                        lastClickTime = currentTime
                        onClick()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .pointerInput(isEditMode) {
                    if (isEditMode) {
                        detectTapGestures(
                            onLongPress = { onLongClick() }
                        )
                    }
                },
            color = if (isError) MaterialTheme.colorScheme.errorContainer 
                    else if (isDragging) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer 
                          else MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = if (isDragging) 8.dp else 2.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    macroName, 
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                if (currency > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = GoldCurrencyColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = currency.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldCurrencyColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isExecuting) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.8f).height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Visual tap feedback overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = feedbackAlpha.value), shape = MaterialTheme.shapes.medium)
        )
    }
}

@Composable
fun MacroButtonsScreen(
    macros: List<String>,
    executingMacros: Set<String> = emptySet(),
    failedMacros: Set<String> = emptySet(),
    isEditMode: Boolean = false,
    onMacroClick: (String) -> Unit,
    onMacroLongClick: (String) -> Unit = {},
    onMoveMacro: (Int, Int) -> Unit = { _, _ -> },
    currency: Long = 0,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
    
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(isEditMode, macros) {
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
                        
                        // Find the item we are hovering over
                        val draggingItem = gridState.layoutInfo.visibleItemsInfo.find { it.index == currentDraggingIndex } ?: return@detectDragGesturesAfterLongPress
                        val centerX = draggingItem.offset.x + draggingItem.size.width / 2 + dragOffset.x
                        val centerY = draggingItem.offset.y + draggingItem.size.height / 2 + dragOffset.y
                        
                        gridState.layoutInfo.visibleItemsInfo
                            .find { item ->
                                item.index != currentDraggingIndex &&
                                centerX in item.offset.x..(item.offset.x + item.size.width) &&
                                centerY in item.offset.y..(item.offset.y + item.size.height)
                            }
                            ?.let { hoverItem ->
                                val targetIndex = hoverItem.index
                                onMoveMacro(currentDraggingIndex, targetIndex)
                                draggingIndex = targetIndex
                                
                                // Reset drag offset to maintain the item under the pointer
                                // This helps keep the interaction smooth during the swap
                                val hoverCenterX = hoverItem.offset.x + hoverItem.size.width / 2
                                val hoverCenterY = hoverItem.offset.y + hoverItem.size.height / 2
                                dragOffset = IntOffset(
                                    centerX - hoverCenterX,
                                    centerY - hoverCenterY
                                )
                            }
                    },
                    onDragEnd = {
                        draggingIndex = null
                        dragOffset = IntOffset.Zero
                    },
                    onDragCancel = {
                        draggingIndex = null
                        dragOffset = IntOffset.Zero
                    }
                )
            },
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(macros, key = { _, macro -> macro }) { index, macroName ->
            MacroButton(
                macroName = macroName,
                isExecuting = executingMacros.contains(macroName),
                isError = failedMacros.contains(macroName),
                isEditMode = isEditMode,
                isDragging = draggingIndex == index,
                dragOffset = dragOffset,
                currency = currency,
                onClick = { onMacroClick(macroName) },
                onLongClick = { onMacroLongClick(macroName) }
            )
        }
    }
}

