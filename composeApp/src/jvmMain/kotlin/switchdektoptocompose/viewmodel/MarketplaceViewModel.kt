package switchdektoptocompose.viewmodel

import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.models.MarketplaceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import switchdektoptocompose.logic.AppSettings

data class MarketplaceState(
    val items: List<MarketplaceItem> = emptyList(),
    val isLoading: Boolean = false,
    val installStatus: Map<String, InstallStatus> = emptyMap()
)

sealed class InstallStatus {
    object Idle : InstallStatus()
    object Installing : InstallStatus()
    object Installed : InstallStatus()
    data class Error(val message: String) : InstallStatus()
}

class MarketplaceViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val macroManagerViewModel: MacroManagerViewModel
) {
    private val _uiState = MutableStateFlow(MarketplaceState())
    val uiState: StateFlow<MarketplaceState> = _uiState.asStateFlow()

    init {
        loadMockItems()
    }

    private fun loadMockItems() {
        val mockItems = listOf(
            MarketplaceItem(
                id = "photoshop-essentials",
                name = "Photoshop Essentials",
                description = "Core shortcuts for Photoshop: Layers, Brushes, and Selection tools.",
                author = "KapCode",
                version = "1.0.0",
                downloadUrl = "", // Mock
                thumbnailUrl = null
            ),
            MarketplaceItem(
                id = "premiere-pro-cut",
                name = "Premiere Pro Editor",
                description = "Speed up your timeline work with these optimized editing macros.",
                author = "EditorLife",
                version = "1.2.0",
                downloadUrl = "", // Mock
                thumbnailUrl = null
            ),
            MarketplaceItem(
                id = "vscode-master",
                name = "VS Code Power User",
                description = "Navigate, refactor, and debug faster with dedicated VS Code controls.",
                author = "DevTools",
                version = "2.0.1",
                downloadUrl = "", // Mock
                thumbnailUrl = null
            )
        )
        _uiState.update { it.copy(items = mockItems) }
    }

    fun installPack(item: MarketplaceItem) {
        _uiState.update { it.copy(installStatus = it.installStatus + (item.id to InstallStatus.Installing)) }
        
        // Mocking the download and install logic
        try {
            val macroDir = File(settingsViewModel.macroDirectory.value)
            if (!macroDir.exists()) macroDir.mkdirs()

            // Create a dummy MacroPack based on the item
            val pack = MacroPack(
                id = item.id,
                name = item.name,
                author = item.author,
                version = item.version,
                targetProcess = when(item.id) {
                    "photoshop-essentials" -> "photoshop.exe"
                    "premiere-pro-cut" -> "Adobe Premiere Pro.exe"
                    "vscode-master" -> "Code.exe"
                    else -> null
                },
                widgets = emptyList() // In a real scenario, this would come from the downloadUrl
            )

            val fileName = item.name.replace(Regex("[^a-zA-Z0-9]"), "_") + "_pack.json"
            val file = File(macroDir, fileName)
            val json = Json { prettyPrint = true }
            file.writeText(json.encodeToString(pack))

            macroManagerViewModel.refresh()
            _uiState.update { it.copy(installStatus = it.installStatus + (item.id to InstallStatus.Installed)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(installStatus = it.installStatus + (item.id to InstallStatus.Error(e.message ?: "Unknown error"))) }
        }
    }
}
