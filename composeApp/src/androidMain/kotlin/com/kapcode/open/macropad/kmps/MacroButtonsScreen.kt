package com.kapcode.open.macropad.kmps

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapcode.open.macropad.kmps.models.MacroPack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val GoldCurrencyColor = Color(0xFFFFD700)

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
fun MacroButton(
    macroName: String,
    isExecuting: Boolean,
    isError: Boolean,
    isEditMode: Boolean = false,
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
            .graphicsLayer {
                if (isEditMode) {
                    rotationZ = rotation
                }
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
                    detectTapGestures(
                        onLongPress = { onLongClick() }
                    )
                },
            color = if (isError) MaterialTheme.colorScheme.errorContainer 
                    else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer 
                          else MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 2.dp
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
    currency: Long = 0,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(8.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(macros) { macroName ->
            MacroButton(
                macroName = macroName,
                isExecuting = executingMacros.contains(macroName),
                isError = failedMacros.contains(macroName),
                isEditMode = isEditMode,
                currency = currency,
                onClick = { onMacroClick(macroName) },
                onLongClick = { onMacroLongClick(macroName) }
            )
        }
    }
}
