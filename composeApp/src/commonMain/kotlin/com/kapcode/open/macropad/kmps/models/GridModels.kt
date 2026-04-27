package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.Serializable
import com.kapcode.open.macropad.kmps.models.AutomationRoutine

@Serializable
enum class WidgetType { BUTTON, TOGGLE, SLIDER_HORIZONTAL, SLIDER_VERTICAL }

@Serializable
enum class SliderUpdateMode { LIVE, ON_RELEASE }

@Serializable
data class GridWidget(
    val id: String,
    val macroId: String,
    val label: String,
    val color: Long, // Hex color e.g. 0xFFBB86FC
    val icon: String? = null,
    val row: Int,
    val col: Int,
    val type: WidgetType = WidgetType.BUTTON,
    // State
    val state: Boolean = false,
    val value: Float = 0f,
    // Config
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val sliderUpdateMode: SliderUpdateMode = SliderUpdateMode.ON_RELEASE
)

@Serializable
data class MacroPack(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val targetProcess: String? = null, // e.g., "photoshop.exe"
    val targetWindowTitle: String? = null, // e.g., "Google Chrome"
    val isActive: Boolean = true,
    val widgets: List<GridWidget> = emptyList(),
    val routines: List<AutomationRoutine> = emptyList()
)

@Serializable
data class MarketplaceItem(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val downloadUrl: String,
    val thumbnailUrl: String? = null
)
