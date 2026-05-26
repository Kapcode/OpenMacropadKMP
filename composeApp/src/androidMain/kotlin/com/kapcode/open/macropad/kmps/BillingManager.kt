package com.kapcode.open.macropad.kmps

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.android.billingclient.api.*
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    private val context: Context,
    private val settingsViewModel: SettingsViewModel,
    private val scope: CoroutineScope
) {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult: BillingResult, purchases: List<Purchase>? ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .enablePendingPurchases()
        .build()

    private val _isAdFree = MutableStateFlow(value = false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    checkPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle retry logic
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PRO_ONE_TIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PRO_SUB)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_AD_FREE_SUB)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { productDetailsMap[it.productId] = it }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            Toast.makeText(context, "Product details not found. Please ensure you have a stable internet connection and try again.", Toast.LENGTH_LONG).show()
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    // For subscriptions, set the offer token
                    if (productDetails.productType == BillingClient.ProductType.SUBS) {
                        productDetails.subscriptionOfferDetails?.getOrNull(0)?.offerToken?.let {
                            setOfferToken(it)
                        }
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun checkPurchases() {
        // Query In-App purchases
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchaseList.forEach { handlePurchase(it) }
            }
        }

        // Query Subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchaseList.forEach { handlePurchase(it) }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        applyPurchase(purchase)
                    }
                }
            } else {
                applyPurchase(purchase)
            }
        }
    }

    private fun applyPurchase(purchase: Purchase) {
        scope.launch {
            if (purchase.products.contains(BillingConstants.PRODUCT_ID_PRO_ONE_TIME) ||
                purchase.products.contains(BillingConstants.PRODUCT_ID_PRO_SUB)) {
                settingsViewModel.setIsPro(true)
            }
            
            if (purchase.products.contains(BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME) ||
                purchase.products.contains(BillingConstants.PRODUCT_ID_AD_FREE_SUB)) {
                settingsViewModel.setIsAdFree(true)
                _isAdFree.value = true
            }
        }
    }
}