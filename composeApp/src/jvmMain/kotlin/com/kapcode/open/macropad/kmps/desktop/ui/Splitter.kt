package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.*
import org.slf4j.LoggerFactory
import kotlin.math.absoluteValue

import com.kapcode.open.macropad.kmps.desktop.AppConfig
import com.kapcode.open.macropad.kmps.desktop.model.LogLevel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.SettingsViewModel

private val logger = LoggerFactory.getLogger("Splitter")

private fun logSplitterEvent(consoleViewModel: ConsoleViewModel?, message: String, vararg args: Any?) {
    val formatted = message.replace("{}", "%s").format(*args)
    if (AppConfig.isVerboseOutputEnabled) {
        logger.info(message, *args)
        println("SPLITTER: $formatted")
    }
    consoleViewModel?.addLog(LogLevel.Verbose, "[Splitter] $formatted")
}

@Composable
fun GhostPane(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(24.dp).alpha(0.6f),
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 12.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun MoveableHorizontalSplitPane(
    modifier: Modifier = Modifier,
    name: String = "Horizontal Split",
    firstName: String = "Pane 1",
    secondName: String = "Pane 2",
    consoleViewModel: ConsoleViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    splitPaneState: SplitPaneState,
    firstMinSize: Dp = 100.dp,
    secondMinSize: Dp = 100.dp,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    var isSwapped by remember { mutableStateOf(settingsViewModel?.getSplitterSwapped(name, false) ?: false) }
    
    LaunchedEffect(name, consoleViewModel, splitPaneState) {
        consoleViewModel?.registerSplitter(name, splitPaneState)
    }

    LaunchedEffect(isSwapped) {
        settingsViewModel?.setSplitterSwapped(name, isSwapped)
    }

    LaunchedEffect(splitPaneState.positionPercentage) {
        settingsViewModel?.setSplitterPosition(name, splitPaneState.positionPercentage)
    }

    var dragOffset by remember { mutableStateOf(0f) }
    var isDraggingForSwap by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val currentFirstName = if (!isSwapped) firstName else secondName
    val currentSecondName = if (!isSwapped) secondName else firstName

    Box(modifier = modifier) {
        HorizontalSplitPane(modifier = Modifier.fillMaxSize(), splitPaneState = splitPaneState) {
            if (!isSwapped) {
                first(minSize = firstMinSize) { first() }
                second(minSize = secondMinSize) { second() }
            } else {
                first(minSize = secondMinSize) { second() }
                second(minSize = firstMinSize) { first() }
            }
            MoveableSplitter(
                name = name,
                consoleViewModel = consoleViewModel,
                settingsViewModel = settingsViewModel,
                splitPaneState = splitPaneState,
                isHorizontal = true,
                onSwap = { isSwapped = !isSwapped },
                onDragStart = { 
                    isDraggingForSwap = true
                    dragOffset = 0f
                },
                onDrag = { offset ->
                    dragOffset = offset.x
                },
                onDragEnd = { offset ->
                    if (offset.x.absoluteValue > 150f) {
                        isSwapped = !isSwapped
                        logSplitterEvent(consoleViewModel, "Splitter '{}' swapped via drag. New Position: {}", name, splitPaneState.positionPercentage)
                    }
                    isDraggingForSwap = false
                    dragOffset = 0f
                }
            )
        }

        if (isDraggingForSwap) {
            val offsetDp = with(density) { dragOffset.toDp() }
            val splitPos = splitPaneState.positionPercentage
            Box(Modifier.fillMaxSize()) {
                GhostPane(
                    name = currentFirstName,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(splitPos)
                        .align(Alignment.CenterStart)
                        .offset(x = offsetDp)
                )
                GhostPane(
                    name = currentSecondName,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f - splitPos)
                        .align(Alignment.CenterEnd)
                        .offset(x = offsetDp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun MoveableVerticalSplitPane(
    modifier: Modifier = Modifier,
    name: String = "Vertical Split",
    firstName: String = "Pane 1",
    secondName: String = "Pane 2",
    consoleViewModel: ConsoleViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    splitPaneState: SplitPaneState,
    firstMinSize: Dp = 100.dp,
    secondMinSize: Dp = 100.dp,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    var isSwapped by remember { mutableStateOf(settingsViewModel?.getSplitterSwapped(name, false) ?: false) }
    
    LaunchedEffect(name, consoleViewModel, splitPaneState) {
        consoleViewModel?.registerSplitter(name, splitPaneState)
    }

    LaunchedEffect(isSwapped) {
        settingsViewModel?.setSplitterSwapped(name, isSwapped)
    }

    LaunchedEffect(splitPaneState.positionPercentage) {
        settingsViewModel?.setSplitterPosition(name, splitPaneState.positionPercentage)
    }

    var dragOffset by remember { mutableStateOf(0f) }
    var isDraggingForSwap by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val currentFirstName = if (!isSwapped) firstName else secondName
    val currentSecondName = if (!isSwapped) secondName else firstName

    Box(modifier = modifier) {
        VerticalSplitPane(modifier = Modifier.fillMaxSize(), splitPaneState = splitPaneState) {
            if (!isSwapped) {
                first(minSize = firstMinSize) { first() }
                second(minSize = secondMinSize) { second() }
            } else {
                first(minSize = secondMinSize) { second() }
                second(minSize = firstMinSize) { first() }
            }
            MoveableSplitter(
                name = name,
                consoleViewModel = consoleViewModel,
                settingsViewModel = settingsViewModel,
                splitPaneState = splitPaneState,
                isHorizontal = false,
                onSwap = { isSwapped = !isSwapped },
                onDragStart = { 
                    isDraggingForSwap = true
                    dragOffset = 0f
                },
                onDrag = { offset ->
                    dragOffset = offset.y
                },
                onDragEnd = { offset ->
                    if (offset.y.absoluteValue > 150f) {
                        isSwapped = !isSwapped
                        logSplitterEvent(consoleViewModel, "Splitter '{}' swapped via drag. New Position: {}", name, splitPaneState.positionPercentage)
                    }
                    isDraggingForSwap = false
                    dragOffset = 0f
                }
            )
        }

        if (isDraggingForSwap) {
            val offsetDp = with(density) { dragOffset.toDp() }
            val splitPos = splitPaneState.positionPercentage
            Box(Modifier.fillMaxSize()) {
                GhostPane(
                    name = currentFirstName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(splitPos)
                        .align(Alignment.TopCenter)
                        .offset(y = offsetDp)
                )
                GhostPane(
                    name = currentSecondName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(1f - splitPos)
                        .align(Alignment.BottomCenter)
                        .offset(y = offsetDp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class, ExperimentalComposeUiApi::class)
fun SplitPaneScope.MoveableSplitter(
    name: String,
    consoleViewModel: ConsoleViewModel?,
    settingsViewModel: SettingsViewModel?,
    splitPaneState: SplitPaneState,
    isHorizontal: Boolean,
    onSwap: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: (Offset) -> Unit = {}
) {
    splitter {
        visiblePart {
            Box(Modifier.fillMaxSize()) {
                if (isHorizontal) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.KeyboardArrowLeft,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                        )
                        Icon(
                            Icons.AutoMirrored.Default.KeyboardArrowRight,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Box(
                            Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        handle {
            val swapModifier by settingsViewModel?.splitterSwapModifier?.collectAsState("Shift") ?: remember { mutableStateOf("Shift") }
            val infoModifier by settingsViewModel?.splitterInfoModifier?.collectAsState("Ctrl") ?: remember { mutableStateOf("Ctrl") }
            var isHovered by remember { mutableStateOf(false) }
            var isPressed by remember { mutableStateOf(false) }

            Box(
                Modifier
                    .markAsHandle()
                    .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                    .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = when {
                                isPressed -> 0.4f
                                isHovered -> 0.2f
                                else -> 0.05f
                            }
                        )
                    )
                    .pointerInput(name, swapModifier, infoModifier) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isSwapPressed = when (swapModifier) {
                                    "Shift" -> event.keyboardModifiers.isShiftPressed
                                    "Ctrl" -> event.keyboardModifiers.isCtrlPressed
                                    "Alt" -> event.keyboardModifiers.isAltPressed
                                    else -> event.keyboardModifiers.isShiftPressed
                                }
                                val isInfoPressed = when (infoModifier) {
                                    "Shift" -> event.keyboardModifiers.isShiftPressed
                                    "Ctrl" -> event.keyboardModifiers.isCtrlPressed
                                    "Alt" -> event.keyboardModifiers.isAltPressed
                                    else -> event.keyboardModifiers.isCtrlPressed
                                }

                                if (event.type == PointerEventType.Press) {
                                    isPressed = true
                                    if (isSwapPressed) {
                                        // Consume and start swap drag
                                        event.changes.forEach { it.consume() }
                                        onDragStart()
                                        
                                        var totalOffset = Offset.Zero
                                        while (true) {
                                            val dragEvent = awaitPointerEvent(PointerEventPass.Main)
                                            if (dragEvent.type == PointerEventType.Move) {
                                                dragEvent.changes.forEach { change ->
                                                    totalOffset += change.positionChange()
                                                    change.consume()
                                                }
                                                onDrag(totalOffset)
                                            } else if (dragEvent.type == PointerEventType.Release) {
                                                isPressed = false
                                                onDragEnd(totalOffset)
                                                break
                                            }
                                        }
                                    } else if (isInfoPressed) {
                                        consoleViewModel?.printAllSplitterPositions()
                                    }
                                } else if (event.type == PointerEventType.Release) {
                                    isPressed = false
                                    logSplitterEvent(consoleViewModel, "Splitter '{}' moved/resized. Final Position: {}", name, splitPaneState.positionPercentage)
                                }
                            }
                        }
                    }
                    .then(
                        if (isHorizontal) Modifier.fillMaxHeight().width(24.dp)
                        else Modifier.fillMaxWidth().height(24.dp)
                    )
            )
        }
    }
}
