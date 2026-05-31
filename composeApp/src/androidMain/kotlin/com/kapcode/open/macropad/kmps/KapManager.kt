package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class KapManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
    private val _kapBalance = MutableStateFlow(prefs.getInt("token_balance", BillingConstants.STARTING_KAPS))
    val kapBalance = _kapBalance.asStateFlow()

    private val _lastSpentTime = MutableStateFlow(0L)
    val lastSpentTime = _lastSpentTime.asStateFlow()

    fun awardKaps(amount: Int) {
        val newBalance = _kapBalance.value + amount
        _kapBalance.value = newBalance
        prefs.edit().putInt("token_balance", newBalance).apply()
    }

    /**
     * @return 0 if grace period allowed it for free,
     *         positive amount if Kaps were deducted,
     *         -1 if insufficient balance and not in grace period.
     */
    fun spendKapsWithResult(amount: Int): Int {
        val now = System.currentTimeMillis()
        val lastTime = _lastSpentTime.value

        if (now - lastTime < BillingConstants.GRACE_PERIOD_MS) {
            return 0
        }

        return if (_kapBalance.value >= amount) {
            val newBalance = _kapBalance.value - amount
            _kapBalance.value = newBalance
            prefs.edit().putInt("token_balance", newBalance).apply()
            _lastSpentTime.value = now
            amount
        } else {
            -1
        }
    }

    fun canAfford(amount: Int): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = _lastSpentTime.value
        return (now - lastTime < BillingConstants.GRACE_PERIOD_MS) || (_kapBalance.value >= amount)
    }

    // Deprecated, use spendKapsWithResult
    fun spendKaps(amount: Int): Boolean {
        return spendKapsWithResult(amount) >= 0
    }

    companion object {
        @Volatile
        private var INSTANCE: KapManager? = null

        fun getInstance(context: Context): KapManager {
            return INSTANCE ?: synchronized(this) {
                val instance = KapManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}