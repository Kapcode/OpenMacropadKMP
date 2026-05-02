package com.kapcode.open.macropad.kmps.lunch

import com.kapcode.open.macropad.kmps.models.LunchMenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class LunchMenuViewModel {
    private val _menuItems = MutableStateFlow<List<LunchMenuItem>>(emptyList())
    val menuItems: StateFlow<List<LunchMenuItem>> = _menuItems.asStateFlow()

    fun addMenuItem(name: String, price: Double) {
        val newItem = LunchMenuItem(
            id = Random.nextInt(10000, 99999).toString(),
            name = name,
            price = price
        )
        _menuItems.update { it + newItem }
    }

    fun deleteMenuItem(id: String) {
        _menuItems.update { it.filter { item -> item.id != id } }
    }
}
