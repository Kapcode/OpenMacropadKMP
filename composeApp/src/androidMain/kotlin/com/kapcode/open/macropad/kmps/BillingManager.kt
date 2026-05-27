package com.kapcode.open.macropad.kmps

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var settingsViewModel: SettingsViewModel? = null

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener { billingResult: BillingResult, purchases: List<Purchase>? ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && (purchases != null)) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .enablePendingPurchases()
        .build()

    private val _isAdFree = MutableStateFlow(value = false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val _formattedPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val formattedPrices: StateFlow<Map<String, String>> = _formattedPrices.asStateFlow()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    private var isConnecting = false

    companion object {
        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
        }
    }

    fun startConnection(settingsViewModel: SettingsViewModel) {
        this.settingsViewModel = settingsViewModel
        if (billingClient.isReady) {
            Log.d("BillingManager", "BillingClient is already ready. Querying products...")
            queryProductDetails()
            checkPurchases()
            return
        }

        if (isConnecting) return
        isConnecting = true

        Log.d("BillingManager", "Starting BillingClient connection...")
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnecting = false
                    Log.d("BillingManager", "Billing setup finished. Response code: ${billingResult.responseCode}")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProductDetails()
                        checkPurchases()
                        
                        // Periodically refresh product details to handle eventual consistency/sync issues
                        scope.launch {
                            while (isActive) {
                                delay(300_000) // 5 minutes
                                queryProductDetails()
                            }
                        }
                    } else {
                        Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting = false
                    Log.w("BillingManager", "Billing service disconnected. Retrying in 5s...")
                    scope.launch {
                        delay(5000)
                        startConnection(settingsViewModel)
                    }
                }
            }
        )
    }

    private fun queryProductDetails() {
        Log.d("BillingManager", "Querying product details (INAPP and SUBS separately)...")
        
        // 1. Query INAPP products
        val inAppProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PRO_ONE_TIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppProductList).build()
        
        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, productDetailsList ->
            Log.d("BillingManager", "INAPP query response code: ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("BillingManager", "Found ${productDetailsList.size} INAPP products")
                val newPrices = _formattedPrices.value.toMutableMap()
                productDetailsList.forEach { 
                    Log.d("BillingManager", "Loaded INAPP: ${it.productId} (${it.name}) -> Price: ${it.oneTimePurchaseOfferDetails?.formattedPrice}")
                    productDetailsMap[it.productId] = it 
                    it.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                        newPrices[it.productId] = price
                    }
                }
                _formattedPrices.value = newPrices
            } else {
                Log.e("BillingManager", "INAPP query failed: ${billingResult.debugMessage}")
            }
        }

        // 2. Query SUBS products
        val subProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PRO_SUB)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_AD_FREE_SUB)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val subParams = QueryProductDetailsParams.newBuilder().setProductList(subProductList).build()

        billingClient.queryProductDetailsAsync(subParams) { billingResult, productDetailsList ->
            Log.d("BillingManager", "SUBS query response code: ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("BillingManager", "Found ${productDetailsList.size} SUBS products")
                val newPrices = _formattedPrices.value.toMutableMap()
                productDetailsList.forEach { 
                    Log.d("BillingManager", "Loaded SUBS: ${it.productId} (${it.name}) -> Offers: ${it.subscriptionOfferDetails?.size}")
                    productDetailsMap[it.productId] = it 
                    it.subscriptionOfferDetails?.getOrNull(0)?.pricingPhases?.pricingPhaseList?.getOrNull(0)?.formattedPrice?.let { price ->
                        newPrices[it.productId] = price
                    }
                }
                _formattedPrices.value = newPrices
            } else {
                Log.e("BillingManager", "SUBS query failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            val available = productDetailsMap.keys.joinToString(", ")
            Toast.makeText(appContext, "Product ID '$productId' not found. Available: [$available]. Check connection or Play Console config.", Toast.LENGTH_LONG).show()
            // Force a refresh if not found
            queryProductDetails()
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
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
        if (!billingClient.isReady) return

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchaseList.forEach { handlePurchase(it) }
            }
        }

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
                settingsViewModel?.setIsPro(pro = true)
            }
            
            if (purchase.products.contains(BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME) ||
                purchase.products.contains(BillingConstants.PRODUCT_ID_AD_FREE_SUB)) {
                settingsViewModel?.setIsAdFree(adFree = true)
                _isAdFree.value = true
            }
        }
    }
}
