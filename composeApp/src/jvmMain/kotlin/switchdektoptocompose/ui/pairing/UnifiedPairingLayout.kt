package switchdektoptocompose.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import switchdektoptocompose.model.ClientInfo
import switchdektoptocompose.viewmodel.PairingViewModel

@Composable
fun UnifiedPairingLayout(
    requests: List<ClientInfo>,
    pairingViewModel: PairingViewModel,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    isAlwaysAllowAvailable: Boolean,
    onClose: () -> Unit,
    maxWidth: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp
) {
    val listState = rememberLazyListState()
    val fleetMode by pairingViewModel.fleetModeEnabled.collectAsState()
    val gridVisibility by pairingViewModel.fleetGridVisibility.collectAsState()
    val gridRows by pairingViewModel.gridRows.collectAsState()
    val gridCols by pairingViewModel.gridCols.collectAsState()
    val qrBitmaps by pairingViewModel.qrBitmaps.collectAsState()

    // Identify if we have active grids in the side gutters vs center column
    val sideGridsVisible = remember(gridVisibility) {
        gridVisibility.getOrElse(0) { true } || gridVisibility.getOrElse(1) { true } ||
        gridVisibility.getOrElse(2) { true } || gridVisibility.getOrElse(3) { true }
    }
    val centerGridsVisible = remember(gridVisibility) {
        gridVisibility.getOrElse(4) { true } || gridVisibility.getOrElse(5) { true }
    }

    val (smartMaxRows, smartMaxCols) = pairingViewModel.calculateSmartBounds(maxWidth, maxHeight, sideGridsVisible, centerGridsVisible)

    // Sync VM state with smart bounds
    LaunchedEffect(smartMaxRows, smartMaxCols) {
        if (gridRows > smartMaxRows) pairingViewModel.setGridRows(smartMaxRows.coerceAtLeast(1))
        if (gridCols > smartMaxCols) pairingViewModel.setGridCols(smartMaxCols.coerceAtLeast(1))
    }
    
    // Default initialization if needed
    LaunchedEffect(Unit) {
        if (gridRows == 1 && smartMaxRows > 1) pairingViewModel.setGridRows(smartMaxRows)
        if (gridCols == 1 && smartMaxCols > 1) pairingViewModel.setGridCols(smartMaxCols)
    }

    val totalGridSize = gridRows * gridCols

    // Dynamically identify which devices are visible in the scrollable list
    val visibleIndices = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }
        }
    }

    // Identify which devices to display across the 6 grids
    val displayRequests = remember(requests, visibleIndices.value, totalGridSize) {
        if (requests.isEmpty()) return@remember emptyList()
        
        val firstVisible = visibleIndices.value.firstOrNull() ?: 0
        val base = requests.drop(firstVisible).take(totalGridSize)
        
        if (base.isEmpty()) {
            val lastBase = requests.takeLast(totalGridSize)
            List(totalGridSize) { index -> lastBase[index % lastBase.size] }
        } else {
            List(totalGridSize) { index -> base[index % base.size] }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (displayRequests.isNotEmpty()) {
            val gridPadding = 16.dp
            
            if (gridVisibility.getOrElse(0) { true }) QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.TopStart).padding(gridPadding))
            if (gridVisibility.getOrElse(1) { true }) QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.TopEnd).padding(gridPadding))
            if (gridVisibility.getOrElse(2) { true }) QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.BottomStart).padding(gridPadding))
            if (gridVisibility.getOrElse(3) { true }) QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.BottomEnd).padding(gridPadding))
            
            if (gridVisibility.getOrElse(4) { true }) QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.TopCenter).padding(gridPadding))
            if (gridVisibility.getOrElse(5) { true }) QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.BottomCenter).padding(gridPadding))
        }

        val listHeight = (400 + (gridRows * 40)).dp 
        
        Surface(
            modifier = Modifier.align(Alignment.Center).width(900.dp).height(listHeight),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 12.dp,
            shadowElevation = 24.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "PAIRING AND SYNC (FLEET)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Grid/List Sync: ${gridRows}x${gridCols} (${totalGridSize} Items)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            IconButton(onClick = { if (gridRows > 1) pairingViewModel.setGridRows(gridRows - 1) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "Less Rows")
                            }
                            IconButton(
                                onClick = { if (gridRows < smartMaxRows) pairingViewModel.setGridRows(gridRows + 1) }, 
                                enabled = gridRows < smartMaxRows,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "More Rows")
                            }
                            Text("R", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { if (gridCols > 1) pairingViewModel.setGridCols(gridCols - 1) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Less Cols")
                            }
                            IconButton(
                                onClick = { if (gridCols < smartMaxCols) pairingViewModel.setGridCols(gridCols + 1) }, 
                                enabled = gridCols < smartMaxCols,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "More Cols")
                            }
                            Text("C", style = MaterialTheme.typography.labelSmall)
                            
                            Spacer(Modifier.width(24.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("GRIDS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                            listOf(0, 4, 1).forEach { idx ->
                                                Checkbox(
                                                    checked = gridVisibility.getOrElse(idx) { true },
                                                    onCheckedChange = { pairingViewModel.setFleetGridVisibility(idx, it) },
                                                    modifier = Modifier.size(20.dp).padding(2.dp)
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                            listOf(2, 5, 3).forEach { idx ->
                                                Checkbox(
                                                    checked = gridVisibility.getOrElse(idx) { true },
                                                    onCheckedChange = { pairingViewModel.setFleetGridVisibility(idx, it) },
                                                    modifier = Modifier.size(20.dp).padding(2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pairing & Sync (Fleet) Mode", style = MaterialTheme.typography.labelLarge)
                            Switch(
                                checked = fleetMode,
                                onCheckedChange = { pairingViewModel.setFleetModeEnabled(it) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        if (requests.size > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text("${requests.size} DEVICES", modifier = Modifier.padding(4.dp))
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onClose,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("CANCEL ALL")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(requests, key = { it.id }) { request ->
                        CompactPairingItem(
                            request = request,
                            onApprove = onApprove,
                            onDeny = onDeny,
                            onBan = onBan,
                            isAlwaysAllowAvailable = isAlwaysAllowAvailable
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactPairingItem(
    request: ClientInfo,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    isAlwaysAllowAvailable: Boolean
) {
    val code = request.verificationCode ?: "------"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (request.codeMatched) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (request.codeMatched) 
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF008000)) 
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "ID: ${request.id.take(8)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            if (code.length == 6) "${code.take(3)}-${code.drop(3)}" else code,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            letterSpacing = 2.sp
                        )
                    }
                    
                    if (request.codeMatched) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF008000), modifier = Modifier.size(24.dp))
                        Text("READY", color = Color(0xFF008000), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onDeny(request.id) }, modifier = Modifier.height(36.dp)) { 
                        Text("DENY") 
                    }
                    IconButton(onClick = { onBan(request.id, request.name) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Warning, "Ban", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApprove(request.id, request.name, false) },
                        enabled = request.codeMatched,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("ONCE")
                    }
                    if (isAlwaysAllowAvailable) {
                        Button(
                            onClick = { onApprove(request.id, request.name, true) },
                            enabled = request.codeMatched,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("ALWAYS")
                        }
                    }
                }
            }
        }
    }
}
