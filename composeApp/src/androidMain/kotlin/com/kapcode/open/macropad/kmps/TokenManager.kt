package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class TokenManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
    private val _tokenBalance = MutableStateFlow(prefs.getInt("token_balance", BillingConstants.STARTING_TOKENS))
    val tokenBalance = _tokenBalance.asStateFlow()

    private val lastSpentTime = AtomicLong(0L)

    fun awardTokens(amount: Int) {
        val newBalance = _tokenBalance.value + amount
        _tokenBalance.value = newBalance
        prefs.edit().putInt("token_balance", newBalance).apply()
    }

    /**
     * @return 0 if grace period allowed it for free,
     *         positive amount if tokens were deducted,
     *         -1 if insufficient balance and not in grace period.
     */
    fun spendTokensWithResult(amount: Int): Int {
        val now = System.currentTimeMillis()
        val lastTime = lastSpentTime.get()

        if (now - lastTime < BillingConstants.GRACE_PERIOD_MS) {
            return 0
        }

        return if (_tokenBalance.value >= amount) {
            val newBalance = _tokenBalance.value - amount
            _tokenBalance.value = newBalance
            prefs.edit().putInt("token_balance", newBalance).apply()
            lastSpentTime.set(now)
            amount
        } else {
            -1
        }
    }

    fun canAfford(amount: Int): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = lastSpentTime.get()
        return (now - lastTime < BillingConstants.GRACE_PERIOD_MS) || (_tokenBalance.value >= amount)
    }

    // Deprecated, use spendTokensWithResult
    fun spendTokens(amount: Int): Boolean {
        return spendTokensWithResult(amount) >= 0
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