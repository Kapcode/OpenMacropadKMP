package switchdektoptocompose.logic

import com.kapcode.open.macropad.kmps.models.MacroPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PackManager {
    private val _installedPacks = MutableStateFlow<List<MacroPack>>(emptyList())
    val installedPacks: StateFlow<List<MacroPack>> = _installedPacks.asStateFlow()

    private val _activePack = MutableStateFlow<MacroPack?>(null)
    val activePack: StateFlow<MacroPack?> = _activePack.asStateFlow()

    private val _activeLayerId = MutableStateFlow<String>("default")
    val activeLayerId: StateFlow<String> = _activeLayerId.asStateFlow()

    fun setPacks(packs: List<MacroPack>) {
        _installedPacks.value = packs
    }

    fun activatePack(packId: String) {
        val pack = _installedPacks.value.find { it.id == packId }
        _activePack.value = pack
        _activeLayerId.value = "default"
    }

    fun switchLayer(layerId: String) {
        _activeLayerId.value = layerId
    }

    fun handleActiveProcessChanged(processName: String?) {
        val autoPack = _installedPacks.value.find { 
            it.targetProcess?.equals(processName, ignoreCase = true) == true 
        }
        if (autoPack != null) {
            _activePack.value = autoPack
            _activeLayerId.value = "default"
        }
    }
}
