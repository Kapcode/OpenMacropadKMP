package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.Serializable

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
    val sliderUpdateMode: SliderUpdateMode = SliderUpdateMode.ON_RELEASE,
)

@Serializable
enum class MatchTarget { PROCESS_NAME, APP_NAME, WINDOW_TITLE }

@Serializable
enum class MatchOperator { EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, REGEX }

@Serializable
data class AutoSwitchRule(
    val target: MatchTarget,
    val operator: MatchOperator,
    val value: String,
    val ignoreCase: Boolean = true
)

@Serializable
data class AutoSwitchGroup(
    val rules: List<AutoSwitchRule> = emptyList()
)

@Serializable
data class MacroPack(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val targetProcess: String? = null, // Deprecated: use autoSwitchGroups
    val targetWindowTitle: String? = null, // Deprecated: use autoSwitchGroups
    val autoSwitchGroups: List<AutoSwitchGroup> = emptyList(),
    val isActive: Boolean = true,
    val widgets: List<GridWidget> = emptyList(),
    val routines: List<AutomationRoutine> = emptyList(),
)

@Serializable
data class MarketplaceItem(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val downloadUrl: String,
    val thumbnailUrl: String? = null,
)
