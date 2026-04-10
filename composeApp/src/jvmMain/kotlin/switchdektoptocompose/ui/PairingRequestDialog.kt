package switchdektoptocompose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.model.ClientInfo
import switchdektoptocompose.viewmodel.ConsoleViewModel
import switchdektoptocompose.viewmodel.SettingsViewModel as DesktopSettingsViewModel
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import switchdektoptocompose.utils.QrCodeGenerator

@Composable
fun PairingRequestDialog(
    requests: List<ClientInfo>,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    desktopSettingsViewModel: DesktopSettingsViewModel,
    sharedSettingsViewModel: SharedSettingsViewModel,
    isAlwaysAllowAvailable: Boolean = true,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    onCancelAll: () -> Unit
) {
    val fleetMode by desktopSettingsViewModel.fleetModeEnabled.collectAsState()
    
    // Dynamically update window size when fleetMode changes
    val windowState = rememberWindowState(
        width = if (fleetMode) 1600.dp else 1000.dp,
        height = if (fleetMode) 1200.dp else 800.dp
    )
    
    // Sync window state if fleetMode changes after initial composition
    LaunchedEffect(fleetMode) {
        windowState.size = if (fleetMode) {
            androidx.compose.ui.unit.DpSize(1600.dp, 1200.dp)
        } else {
            androidx.compose.ui.unit.DpSize(1000.dp, 800.dp)
        }
    }

    AppDialog(
        onCloseRequest = onCancelAll,
        state = windowState,
        title = if (fleetMode) "FLEET SYNC (${requests.size} Devices)" else "DEVICE SYNC",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        resizable = true
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isSmallScreen = maxWidth < 1000.dp || maxHeight < 700.dp
            
            if (isSmallScreen || !fleetMode) {
                SmallPairingLayout(
                    requests = requests,
                    onApprove = onApprove,
                    onDeny = onDeny,
                    onBan = onBan,
                    isAlwaysAllowAvailable = isAlwaysAllowAvailable,
                    onClose = onCancelAll,
                    settingsViewModel = desktopSettingsViewModel
                )
            } else {
                UnifiedPairingLayout(
                    requests = requests,
                    onApprove = onApprove,
                    onDeny = onDeny,
                    onBan = onBan,
                    isAlwaysAllowAvailable = isAlwaysAllowAvailable,
                    onClose = onCancelAll,
                    settingsViewModel = desktopSettingsViewModel
                )
            }
        }
    }
}

@Composable
fun SmallPairingLayout(
    requests: List<ClientInfo>,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    isAlwaysAllowAvailable: Boolean,
    onClose: () -> Unit,
    settingsViewModel: DesktopSettingsViewModel
) {
    val fleetMode by settingsViewModel.fleetModeEnabled.collectAsState()
    val qrBitmaps = remember(requests) {
        requests.associate { request ->
            request.id to QrCodeGenerator.generateQrCode(request.verificationCode ?: "", 300)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SYNC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fleet Mode", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = fleetMode,
                    onCheckedChange = { settingsViewModel.setFleetModeEnabled(it) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TextButton(onClick = onClose) {
                    Text("DISMISS ALL")
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests) { request ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Small QR for the specific device
                        Box(modifier = Modifier.size(120.dp).background(Color.White, MaterialTheme.shapes.small).padding(4.dp)) {
                            qrBitmaps[request.id]?.let {
                                Image(it, "QR", modifier = Modifier.fillMaxSize())
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(request.name.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Code: ${request.verificationCode}", style = MaterialTheme.typography.bodyLarge, letterSpacing = 1.sp)
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApprove(request.id, request.name, false) },
                                    enabled = request.codeMatched,
                                    modifier = Modifier.weight(1f)
                                ) { Text("ONCE") }
                                
                                if (isAlwaysAllowAvailable) {
                                    Button(
                                        onClick = { onApprove(request.id, request.name, true) },
                                        enabled = request.codeMatched,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ALWAYS") }
                                }
                            }
                            
                            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onDeny(request.id) }, modifier = Modifier.weight(1f)) { Text("DENY") }
                                IconButton(onClick = { onBan(request.id, request.name) }) {
                                    Icon(Icons.Default.Warning, "Ban", tint = MaterialTheme.colorScheme.error)
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
fun QrGridBox(
    requests: List<ClientInfo>,
    bitmaps: Map<String, ImageBitmap>,
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(rows) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(cols) { c ->
                        val index = r * cols + c
                        QrItem(requests.getOrNull(index), bitmaps)
                    }
                }
            }
        }
    }
}

@Composable
fun QrItem(request: ClientInfo?, bitmaps: Map<String, ImageBitmap>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QrImage(request?.let { bitmaps[it.id] })
        Box(modifier = Modifier.width(100.dp).height(24.dp), contentAlignment = Alignment.Center) {
            if (request != null) {
                Text(
                    text = request.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QrImage(bitmap: ImageBitmap?) {
    Box(modifier = Modifier.size(100.dp)) {
        if (bitmap != null) {
            Image(bitmap, "QR", modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.1f)))
        }
    }
}

@Composable
fun UnifiedPairingLayout(
    requests: List<ClientInfo>,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    isAlwaysAllowAvailable: Boolean,
    onClose: () -> Unit,
    settingsViewModel: DesktopSettingsViewModel
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val fleetMode by settingsViewModel.fleetModeEnabled.collectAsState()
    var gridRows by remember { mutableStateOf(3) }
    var gridCols by remember { mutableStateOf(3) }
    val totalGridSize = gridRows * gridCols

    // Dynamically identify which devices are visible in the scrollable list
    val visibleIndices = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }
        }
    }

    // Pre-generate bitmaps and link them to IDs
    val qrBitmaps = remember(requests) {
        requests.associate { request ->
            request.id to QrCodeGenerator.generateQrCode(request.verificationCode ?: "", 300)
        }
    }

    // Identify which devices to display across the 6 grids
    // We take exactly totalGridSize items starting from the first visible item in the list
    val displayRequests = remember(requests, visibleIndices.value, totalGridSize) {
        if (requests.isEmpty()) return@remember emptyList()
        
        val firstVisible = visibleIndices.value.firstOrNull() ?: 0
        val base = requests.drop(firstVisible).take(totalGridSize)
        
        if (base.isEmpty()) {
            // Fallback if we scrolled past everything somehow
            val lastBase = requests.takeLast(totalGridSize)
            List(totalGridSize) { index -> lastBase[index % lastBase.size] }
        } else {
            // Cycle the available subset to fill all grid slots
            List(totalGridSize) { index -> base[index % base.size] }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // High-visibility QR arrays (Top, Bottom, and Corners)
        if (displayRequests.isNotEmpty()) {
            // Adjust padding based on grid size to avoid edge crowding
            val gridPadding = 16.dp
            
            // Corners
            QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.TopStart).padding(gridPadding))
            QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.TopEnd).padding(gridPadding))
            QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.BottomStart).padding(gridPadding))
            QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.BottomEnd).padding(gridPadding))
            
            // Centers
            QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.TopCenter).padding(gridPadding))
            QrGridBox(displayRequests, qrBitmaps, gridRows, gridCols, modifier = Modifier.align(Alignment.BottomCenter).padding(gridPadding))
        }

        // Center control area - Scrollable list of all devices
        // Height is somewhat linked to grid density now to keep "visible" count sane
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
                            "SYNC (FLEET)",
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
                            // Simple controls for grid size
                            IconButton(onClick = { if (gridRows > 1) gridRows-- }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "Less Rows")
                            }
                            IconButton(onClick = { if (gridRows < 4) gridRows++ }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "More Rows")
                            }
                            Text("R", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { if (gridCols > 1) gridCols-- }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Less Cols")
                            }
                            IconButton(onClick = { if (gridCols < 4) gridCols++ }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "More Cols")
                            }
                            Text("C", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Fleet Mode", style = MaterialTheme.typography.labelLarge)
                            Switch(
                                checked = fleetMode,
                                onCheckedChange = { settingsViewModel.setFleetModeEnabled(it) },
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
                    items(requests) { request ->
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
