package com.kapcode.open.macropad.kmps

enum class AdLocation {
    MAIN, CLIENT, MARKETPLACE, PRO_DIALOG, SETTINGS
}

object BillingConstants {
    const val KAPS_PER_REWARDED_AD = 100
    const val KAPS_PER_MACRO_PRESS = 1
    const val STARTING_KAPS = 500
    const val GRACE_PERIOD_MS = 10000L

    // Google Play Billing Product IDs
    const val PRODUCT_ID_PRO_ONE_TIME = "lifetime_openmacropadkmp_pro"
    const val PRODUCT_ID_PRO_SUB = "openmacropadkmp_pro_subscription"
    const val PRODUCT_ID_AD_FREE_ONE_TIME = "openmacropadkmp_remove_intrusive_ads"
    const val PRODUCT_ID_AD_FREE_SUB = "openmacropadkmp_ad_free_subscription"

    // AdMob IDs
    const val ADMOB_APP_ID = "ca-app-pub-2579373758747951~3368268164"
    const val ADMOB_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val ADMOB_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Production Rewarded ID
    const val ADMOB_REWARDED_UNIT_ID = "ca-app-pub-2579373758747951/5909982002"

    // Placeholders for production banner IDs - using test ID for now as per user instruction
    const val ADMOB_BANNER_MAIN_UNIT_ID = ADMOB_TEST_BANNER_ID
    const val ADMOB_BANNER_CLIENT_UNIT_ID = ADMOB_TEST_BANNER_ID
    const val ADMOB_BANNER_MARKETPLACE_UNIT_ID = ADMOB_TEST_BANNER_ID
    const val ADMOB_BANNER_PRO_DIALOG_UNIT_ID = ADMOB_TEST_BANNER_ID
    const val ADMOB_BANNER_SETTINGS_UNIT_ID = ADMOB_TEST_BANNER_ID

    const val IS_TEST_MODE = false

    fun getBannerId(location: AdLocation): String {
        if (IS_TEST_MODE) return ADMOB_TEST_BANNER_ID
        return when (location) {
            AdLocation.MAIN -> ADMOB_BANNER_MAIN_UNIT_ID
            AdLocation.CLIENT -> ADMOB_BANNER_CLIENT_UNIT_ID
            AdLocation.MARKETPLACE -> ADMOB_BANNER_MARKETPLACE_UNIT_ID
            AdLocation.PRO_DIALOG -> ADMOB_BANNER_PRO_DIALOG_UNIT_ID
            AdLocation.SETTINGS -> ADMOB_BANNER_SETTINGS_UNIT_ID
        }
    }

    fun getRewardedId(): String {
        return if (IS_TEST_MODE) ADMOB_TEST_REWARDED_ID else ADMOB_REWARDED_UNIT_ID
    }
}
