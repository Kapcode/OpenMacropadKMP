package com.kapcode.open.macropad.kmp

import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Represents a single macro file with its UI state.
 */
data class MacroFileState(
    val file: File,
    val name: String = file.nameWithoutExtension,
    val content: String = "",
    val isActive: Boolean = false,
    val isSelectedForDeletion: Boolean = false
)

/**
 * ViewModel for the Macro Manager UI.
 * This class handles the business logic for loading, displaying, and managing macros.
 */
class MacroManagerViewModel {

    private val _macroFiles = MutableStateFlow<List<MacroFileState>>(emptyList())
    val macroFiles: StateFlow<List<MacroFileState>> = _macroFiles.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    init {
        // In a real app, you would inject dependencies like the ActiveMacroManager.
        // For now, we'll load macros directly.
        loadMacrosFromDisk()
        // Here you would also start the file watcher.
    }

    /**
     * Toggles the UI between normal and selection mode.
     */
    fun toggleSelectionMode() {
        _isSelectionMode.update { !it }
        // When leaving selection mode, clear all selections.
        if (!_isSelectionMode.value) {
            _macroFiles.update { list ->
                list.map { it.copy(isSelectedForDeletion = false) }
            }
        }
    }

    /**
     * Marks a specific macro to be included in a batch deletion.
     */
    fun selectMacroForDeletion(file: File, select: Boolean) {
        _macroFiles.update { list ->
            list.map {
                if (it.file.absolutePath == file.absolutePath) {
                    it.copy(isSelectedForDeletion = select)
                } else {
                    it
                }
            }
        }
    }

    /**
     * Deletes all macros that have been marked for deletion.
     */
    fun deleteSelectedMacros() {
        val filesToDelete = _macroFiles.value.filter { it.isSelectedForDeletion }
        if (filesToDelete.isEmpty()) return

        // In a real implementation, you would trigger a confirmation dialog via an event.
        // For now, we will delete them directly.
        
        filesToDelete.forEach { state ->
            // 1. Remove from active macros (logic to be added)
            // activeMacroManager.removeActiveMacro(state.file)

            // 2. Close tab in editor (event to be sent to UI)
            
            // 3. Delete the file
            // state.file.delete()
            println("DELETING (simulated): ${state.file.name}")
        }
        
        // Reload the list from disk and exit selection mode.
        loadMacrosFromDisk()
        _isSelectionMode.value = false
    }

    /**
     * Simulates loading macros from the AppSettings directory.
     * This will eventually contain real file I/O.
     */
    private fun loadMacrosFromDisk() {
        // Placeholder: In the future, this will read from AppSettings.macroDirectory
        _macroFiles.value = listOf(
            MacroFileState(File("Default Macro 1.json"), content = "{\"events\":[]}"),
            MacroFileState(File("My Awesome Macro.json"), content = "{\"events\":[]}", isActive = true),
            MacroFileState(File("Another Macro.json"), content = "{\"events\":[]}")
        )
    }

    // --- Functions to be called by the UI for individual actions ---
    
    fun onPlayMacro(file: File) {
        // Logic to play the macro using MacroPlayer
        println("PLAYING (simulated): ${file.name}")
    }
    
    fun onEditMacro(file: File) {
        // Logic to open the macro in the editor (e.g., via a SharedFlow event)
        println("EDITING (simulated): ${file.name}")
    }
    
    fun onDeleteMacro(file: File) {
        // Logic for single-file deletion (show confirmation, then delete)
        println("DELETING (single, simulated): ${file.name}")
    }

    fun onToggleMacroActive(file: File, isActive: Boolean) {
        // Logic to add/remove macro from ActiveMacroManager
        _macroFiles.update { list ->
            list.map {
                if (it.file.absolutePath == file.absolutePath) {
                    it.copy(isActive = isActive)
                } else {
                    it
                }
            }
        }
        println("TOGGLE ACTIVE (simulated): ${file.name} to $isActive")
    }
}