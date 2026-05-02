package com.kapcode.open.macropad.kmps

import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(context: Context) {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult: BillingResult, purchases: List<Purchase>? ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                _isAdFree.value = true
            }
        }
        .build()

    private val _isAdFree = MutableStateFlow(value = false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle retry logic
            }
        })
    }

    private fun checkPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isAdFree.value = purchaseList.any { it.products.contains("ad_free_product") }
            }
        }
    }
}
