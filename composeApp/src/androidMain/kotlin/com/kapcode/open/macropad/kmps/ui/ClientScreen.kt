package com.kapcode.open.macropad.kmps.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.*
import com.kapcode.open.macropad.kmps.settings.ClientSettingsSection
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.MarketplaceScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalGetImage
@Composable
fun ClientScreen(
    uiState: ClientUiState,
    settingsViewModel: SettingsViewModel,
    clientViewModel: ClientViewModel,
    currency: Long = 0,
    onGetMacros: () -> Unit,
    onMacroClick: (String) -> Unit,
    onPairingCodeEntered: (String) -> Unit,
    onBackToMain: () -> Unit,
    onOkayTriggerSet: (() -> Unit) -> Unit = {},
    onCancelTriggerSet: (() -> Unit) -> Unit = {},
    onSlamTriggerSet: ((Boolean) -> Unit) -> Unit = {},
    onQrScannerToggle: (Boolean) -> Unit = {}
) {
    val connectionStatus = uiState.connectionStatus
    val serverName = uiState.serverName
    val disconnectReason = uiState.disconnectReason
    val macros = uiState.macros
    val executingMacros = uiState.executingMacros
    val failedMacros = uiState.failedMacros
    val showQrScanner = uiState.showQrScanner
    val isAutoZoomEnabled_State = uiState.isAutoZoomEnabled
    val isAutoFocusEnabled_State = uiState.isAutoFocusEnabled
    val manualZoomRatio_State = uiState.manualZoomRatio
    val manualFocusDistance_State = uiState.manualFocusDistance
    val isScannerTimedOut = uiState.isScannerTimedOut

    val scannerTimeoutHours by settingsViewModel.scannerTimeoutHours.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showMacroPicker by remember { mutableStateOf(false) }

    var scannerStartTime by remember(showQrScanner) { mutableStateOf(System.currentTimeMillis()) }
    var isLowPowerScannerMode by remember { mutableStateOf(false) }

    LaunchedEffect(showQrScanner) {
        if (showQrScanner) {
            scannerStartTime = System.currentTimeMillis()
            isLowPowerScannerMode = false
        }
    }

    LaunchedEffect(showQrScanner, isLowPowerScannerMode) {
        if (showQrScanner && !isLowPowerScannerMode) {
            while (true) {
                if (System.currentTimeMillis() - scannerStartTime > 3600_000L) { // 1 hour
                    isLowPowerScannerMode = true
                    break
                }
                delay(60_000L) // Check every minute
            }
        }
    }

    LaunchedEffect(onBackToMain) {
        onCancelTriggerSet(onBackToMain)
    }

    LaunchedEffect(macros.isEmpty(), connectionStatus) {
        onSlamTriggerSet { isDouble ->
            val isAtPairingStep = connectionStatus == "Pending Approval"
            
            if (isAtPairingStep && !showQrScanner) {
                if (!isDouble) {
                    onQrScannerToggle(true)
                }
            } else if (showQrScanner) {
                if (isDouble) {
                    onQrScannerToggle(false)
                }
            }
        }
    }

    val slamFireEnabled by settingsViewModel.slamFireEnabled.collectAsState()
    
    val onOkayAction = {
        if (showQrScanner) {
            // No obvious "Okay" for scanner
        }
    }

    LaunchedEffect(connectionStatus, showQrScanner) {
        onOkayTriggerSet(onOkayAction)
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    LaunchedEffect(showQrScanner, isScannerTimedOut, scannerTimeoutHours) {
        if (showQrScanner && !isScannerTimedOut && scannerTimeoutHours < 48) {
            delay(scannerTimeoutHours * 3600 * 1000L)
            clientViewModel.setScannerTimedOut(true)
        }
    }

    Scaffold(
        topBar = {
            CommonAppBar(
                title = if (showSettings) "Settings" else "Open Macropad",
                onSettingsClick = { showSettings = !showSettings },
                currency = uiState.currency,
                isQrScannerActive = showQrScanner && !showSettings,
                isAutoZoomEnabled = isAutoZoomEnabled_State,
                onAutoZoomToggle = { clientViewModel.setAutoZoomEnabled(it) },
                isAutoFocusEnabled = isAutoFocusEnabled_State,
                onAutoFocusToggle = { clientViewModel.setAutoFocusEnabled(it) },
                onZoomIn = {
                    clientViewModel.setAutoZoomEnabled(false)
                    activeCamera?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val maxZoom = zoomState?.maxZoomRatio ?: 1f
                        val newZoom = (manualZoomRatio_State + 0.2f).coerceAtMost(maxZoom)
                        clientViewModel.setManualZoomRatio(newZoom)
                        cam.cameraControl.setZoomRatio(newZoom)
                    }
                },
                onZoomOut = {
                    clientViewModel.setAutoZoomEnabled(false)
                    activeCamera?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val minZoom = zoomState?.minZoomRatio ?: 1f
                        val newZoom = (manualZoomRatio_State - 0.2f).coerceAtLeast(minZoom)
                        clientViewModel.setManualZoomRatio(newZoom)
                        cam.cameraControl.setZoomRatio(newZoom)
                    }
                },
                onCloseScanner = { onQrScannerToggle(false) },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                onGetMacros()
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Macros")
                        }
                    }
                },
                actions = {
                    if (!showSettings && macros.isNotEmpty() && slamFireEnabled) {
                        var expandedSingle by remember { mutableStateOf(false) }
                        var expandedDouble by remember { mutableStateOf(false) }
                        var showSlamInfoDialog by remember { mutableStateOf<String?>(null) }

                        if (showSlamInfoDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showSlamInfoDialog = null },
                                title = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TouchApp, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (showSlamInfoDialog == "single") "Slam Fire: Single Tap" else "Slam Fire: Double Tap")
                                    }
                                },
                                text = {
                                    Text(
                                        if (showSlamInfoDialog == "single") {
                                            "A single physical trigger (proximity sensor or volume key) will execute the selected macro immediately."
                                        } else {
                                            "A rapid double physical trigger will execute this separate macro."
                                        }
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = { 
                                        if (showSlamInfoDialog == "single") expandedSingle = true else expandedDouble = true
                                        showSlamInfoDialog = null 
                                    }) {
                                        Text("Select Macro")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSlamInfoDialog = null }) {
                                        Text("Dismiss")
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .width(180.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box {
                                TextButton(
                                    onClick = { showSlamInfoDialog = "single" },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = settingsViewModel.slamFireSelectedMacro.collectAsState().value ?: "None",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DropdownMenu(expanded = expandedSingle, onDismissRequest = { expandedSingle = false }) {
                                    DropdownMenuItem(
                                        text = { Text("None (OK)") },
                                        onClick = {
                                            settingsViewModel.setSlamFireSelectedMacro(null)
                                            expandedSingle = false
                                        }
                                    )
                                    macros.forEach { macro ->
                                        DropdownMenuItem(
                                            text = { Text(macro) },
                                            onClick = {
                                                settingsViewModel.setSlamFireSelectedMacro(macro)
                                                expandedSingle = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box {
                                TextButton(
                                    onClick = { expandedDouble = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.DoubleArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = settingsViewModel.slamFireDoubleSelectedMacro.collectAsState().value ?: "Cancel",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DropdownMenu(expanded = expandedDouble, onDismissRequest = { expandedDouble = false }) {
                                    DropdownMenuItem(
                                        text = { Text("None (Cancel)") },
                                        onClick = {
                                            settingsViewModel.setSlamFireDoubleSelectedMacro(null)
                                            expandedDouble = false
                                        }
                                    )
                                    macros.forEach { macro ->
                                        DropdownMenuItem(
                                            text = { Text(macro) },
                                            onClick = {
                                                settingsViewModel.setSlamFireDoubleSelectedMacro(macro)
                                                expandedDouble = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isConnected = connectionStatus == "Connected" && macros.isNotEmpty()
            if (!showSettings && !isLandscape && isConnected) {
                BottomAppBar { AdmobBanner() }
            }
        }
    ) { innerPadding ->
        if (showSettings) {
            SettingsScreen(
                viewModel = settingsViewModel,
                modifier = Modifier.padding(innerPadding)
            ) {
                ClientSettingsSection()
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        LazyColumn {
                            items(macros) { macro ->
                                Text(text = macro, modifier = Modifier.padding(all = 16.dp))
                            }
                        }
                    }
                },
                gesturesEnabled = drawerState.isOpen
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (connectionStatus == "Connected") {
                            TabRow(selectedTabIndex = uiState.currentTab) {
                                Tab(
                                    selected = uiState.currentTab == 0,
                                    onClick = { clientViewModel.setTab(0) },
                                    text = { Text("My Dashboard") }
                                )
                                Tab(
                                    selected = uiState.currentTab == 1,
                                    onClick = { clientViewModel.setTab(1) },
                                    text = { Text("Active Pack") }
                                )
                                Tab(
                                    selected = uiState.currentTab == 2,
                                    onClick = {
                                        clientViewModel.setTab(2)
                                        clientViewModel.requestMarketplace()
                                    },
                                    text = { Text("Marketplace") }
                                )
                            }
                        }

                        if (uiState.currentTab == 1) {
                            SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = { clientViewModel.setSearchQuery(it) }
                            )
                            NavigationHeader(
                                packs = uiState.filteredPacks,
                                activePack = uiState.activePack,
                                onPackSelected = { clientViewModel.setActivePack(it) }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (macros.isNotEmpty() && !showQrScanner) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    val displayMacros = if (uiState.currentTab == 1) {
                                        uiState.activePack?.widgets?.map { it.label } ?: macros
                                    } else {
                                        uiState.dashboardMacros.ifEmpty { macros }
                                    }

                                    if (uiState.currentTab == 1 && uiState.installedPacks.isEmpty()) {
                                        EmptyPacksPlaceholder(onNavigateToMarket = {
                                            clientViewModel.setTab(2)
                                            clientViewModel.requestMarketplace()
                                        })
                                    } else if (uiState.currentTab == 2) {
                                        if (uiState.isMarketplaceLoading) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                        } else {
                                            MarketplaceScreen(
                                                items = uiState.marketplaceItems,
                                                onDownload = { clientViewModel.downloadMarketplaceItem(it) }
                                            )
                                        }
                                    } else {
                                        MacroButtonsScreen(
                                            macros = displayMacros,
                                            executingMacros = executingMacros,
                                            failedMacros = failedMacros,
                                            isEditMode = uiState.isEditMode,
                                            onMacroClick = onMacroClick,
                                            onMacroLongClick = { macroName ->
                                                if (uiState.currentTab == 0) {
                                                    clientViewModel.removeFromDashboard(macroName)
                                                } else {
                                                    clientViewModel.addToDashboard(macroName)
                                                }
                                            },
                                            onMoveMacro = { from, to -> 
                                                if (uiState.currentTab == 0) {
                                                    clientViewModel.moveDashboardMacro(from, to)
                                                }
                                            },
                                            currency = uiState.currency,
                                            modifier = if (!uiState.isMacroExecutionEnabled) Modifier.alpha(0.5f) else Modifier
                                        )
                                    }

                                    if (showMacroPicker) {
                                        MacroPicker(
                                            macros = macros,
                                            onMacroSelected = { clientViewModel.addToDashboard(it) },
                                            onDismiss = { showMacroPicker = false }
                                        )
                                    }
                                    
                                    if (uiState.isEditMode) {
                                        FloatingActionButton(
                                            onClick = { clientViewModel.setEditMode(false) },
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 88.dp, end = 16.dp),
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Done")
                                        }
                                    }
                                    
                                    if (uiState.currentTab == 0) {
                                        FloatingActionButton(
                                            onClick = { 
                                                if (uiState.isEditMode) {
                                                    showMacroPicker = true
                                                } else {
                                                    clientViewModel.setEditMode(true)
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                            containerColor = if (uiState.isEditMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        ) {
                                            Icon(if (uiState.isEditMode) Icons.Default.Add else Icons.Default.Edit, contentDescription = if (uiState.isEditMode) "Add" else "Edit")
                                        }
                                    }
                                    
                                    if (!uiState.isMacroExecutionEnabled) {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color.Black.copy(alpha = 0.3f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Block, contentDescription = null)
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            "Execution Disabled (E-STOP)",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (showQrScanner) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isScannerTimedOut) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.BatteryAlert,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                "Scanner Timed Out",
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            Text(
                                                "Paused to save battery after $scannerTimeoutHours hours.",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(Modifier.height(24.dp))
                                            Button(onClick = { clientViewModel.setScannerTimedOut(false) }) {
                                                Icon(Icons.Default.Refresh, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Resume Scanning")
                                            }
                                        }
                                    } else {
                                        val configuration = LocalConfiguration.current
                                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                        Box(
                                            modifier = if (isLandscape) {
                                                Modifier.fillMaxHeight().aspectRatio(1f)
                                            } else {
                                                Modifier.fillMaxWidth().aspectRatio(1f)
                                            }
                                        ) {
                                            QrCodeScanner(
                                                onCodeScanned = { code: String ->
                                                    onPairingCodeEntered(code)
                                                    onQrScannerToggle(false)
                                                },
                                                onClose = { onQrScannerToggle(false) },
                                                isAutoZoomEnabled = isAutoZoomEnabled_State,
                                                isAutoFocusEnabled = isAutoFocusEnabled_State,
                                                manualZoomRatio = manualZoomRatio_State,
                                                manualFocusDistance = manualFocusDistance_State,
                                                onManualZoomChange = { clientViewModel.setManualZoomRatio(it) },
                                                onManualFocusChange = { clientViewModel.setManualFocusDistance(it) },
                                                onAutoZoomToggle = { clientViewModel.setAutoZoomEnabled(it) },
                                                onAutoFocusToggle = { clientViewModel.setAutoFocusEnabled(it) },
                                                onCameraReady = { activeCamera = it },
                                                isLowPowerMode = isLowPowerScannerMode
                                            )
                                        }
                                    }
                                }
                            } else {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (connectionStatus == "Pending Approval" || connectionStatus == "Code Matched") {
                                        var enteredCode by remember { mutableStateOf("") }
                                        val focusRequester = remember { FocusRequester() }
                                        val keyboardController = LocalSoftwareKeyboardController.current
                                        val focusManager = LocalFocusManager.current
                                        val configuration = LocalConfiguration.current
                                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                        val isKeyboardOpen = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

                                        LaunchedEffect(Unit) {
                                            if (connectionStatus == "Pending Approval") {
                                                focusRequester.requestFocus()
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            if (connectionStatus == "Code Matched") {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF008000),
                                                        modifier = Modifier.size(64.dp)
                                                    )
                                                    Spacer(Modifier.height(16.dp))
                                                    Text(
                                                        "Code Matched!",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color = Color(0xFF008000)
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        "Please click 'Allow' on your Desktop to finish pairing.",
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            } else {
                                                if (!isKeyboardOpen) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            "Please enter the 6-digit code shown on your Desktop:",
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                        Spacer(Modifier.height(16.dp))
                                                        CircularProgressIndicator()
                                                        Spacer(Modifier.height(24.dp))
                                                        OutlinedButton(
                                                            onClick = { onQrScannerToggle(true) }
                                                        ) {
                                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                                            Spacer(Modifier.width(8.dp))
                                                            Text("Scan QR Code")
                                                        }
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                                ) {
                                                    IconButton(onClick = onBackToMain) {
                                                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                                                    }

                                                    OutlinedTextField(
                                                        value = enteredCode,
                                                        onValueChange = {
                                                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                                enteredCode = it
                                                            }
                                                        },
                                                        label = { if (!isKeyboardOpen) Text("6-Digit Code") },
                                                        placeholder = { if (isKeyboardOpen) Text("Code") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier
                                                            .width(if (isLandscape && isKeyboardOpen) 120.dp else 180.dp)
                                                            .focusRequester(focusRequester)
                                                    )

                                                    IconButton(
                                                        onClick = {
                                                            if (isKeyboardOpen) {
                                                                keyboardController?.hide()
                                                                focusManager.clearFocus()
                                                            } else {
                                                                focusRequester.requestFocus()
                                                                keyboardController?.show()
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            if (isKeyboardOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                            contentDescription = "Toggle Keyboard"
                                                        )
                                                    }

                                                    IconButton(onClick = { onQrScannerToggle(true) }) {
                                                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                                                    }

                                                    Button(
                                                        onClick = { onPairingCodeEntered(enteredCode) },
                                                        enabled = enteredCode.length == 6,
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(Icons.Default.Done, contentDescription = "Submit")
                                                    }
                                                }

                                                if (isKeyboardOpen) {
                                                    LaunchedEffect(Unit) {
                                                        focusRequester.requestFocus()
                                                    }
                                                }
                                            }

                                            if (!isKeyboardOpen) {
                                                Text(
                                                    "Verification required to secure the connection.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                            }
                                        }
                                    } else if (disconnectReason != null) {
                                        val configuration = LocalConfiguration.current
                                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                        
                                        if (isLandscape) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack, 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(64.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                Spacer(Modifier.width(32.dp))
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Disconnected", style = MaterialTheme.typography.headlineMedium)
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(disconnectReason, style = MaterialTheme.typography.bodyLarge)
                                                    Spacer(Modifier.height(16.dp))
                                                    Button(onClick = onBackToMain) {
                                                        Text("Back to Server List")
                                                    }
                                                }
                                            }
                                        } else {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            Text("Disconnected", style = MaterialTheme.typography.headlineMedium)
                                            Spacer(Modifier.height(8.dp))
                                            Text(disconnectReason, style = MaterialTheme.typography.bodyLarge)
                                            Spacer(Modifier.height(32.dp))
                                            Button(onClick = onBackToMain) {
                                                Text("Back to Server List")
                                            }
                                        }
                                    } else {
                                        CircularProgressIndicator()
                                        Spacer(Modifier.height(16.dp))
                                        Text("Connected to:")
                                        Text(
                                            serverName ?: "N/A",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        Text("Status: $connectionStatus")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
