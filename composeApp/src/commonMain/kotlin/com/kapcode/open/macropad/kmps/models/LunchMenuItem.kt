package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.Serializable

@Serializable
data class LunchMenuItem(
    val id: String,
    val name: String,
    val price: Double,
)
