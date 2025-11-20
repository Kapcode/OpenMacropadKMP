package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
    private val _tokenBalance = MutableStateFlow(prefs.getInt("token_balance", BillingConstants.STARTING_TOKENS))
    val tokenBalance = _tokenBalance.asStateFlow()

    fun awardTokens(amount: Int) {
        val newBalance = _tokenBalance.value + amount
        _tokenBalance.value = newBalance
        prefs.edit().putInt("token_balance", newBalance).apply()
    }

    fun spendTokens(amount: Int): Boolean {
        return if (_tokenBalance.value >= amount) {
            val newBalance = _tokenBalance.value - amount
            _tokenBalance.value = newBalance
            prefs.edit().putInt("token_balance", newBalance).apply()
            true
        } else {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                val instance = TokenManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}