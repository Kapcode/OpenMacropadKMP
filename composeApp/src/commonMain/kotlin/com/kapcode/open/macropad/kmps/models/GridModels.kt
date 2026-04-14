package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.Serializable

@Serializable
data class GridWidget(
    val id: String,
    val macroId: String,
    val label: String,
    val color: Long, // Hex color e.g. 0xFFBB86FC
    val icon: String? = null,
    val row: Int,
    val col: Int
)

@Serializable
data class MacroPack(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val targetProcess: String? = null, // e.g., "photoshop.exe"
    val widgets: List<GridWidget> = emptyList()
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
