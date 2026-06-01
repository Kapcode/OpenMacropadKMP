package com.kapcode.open.macropad.kmps

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

fun loadRewardedAd(context: Context, onAdLoaded: (RewardedAd) -> Unit, onAdFailedToLoad: () -> Unit) {
    val adRequest = AdRequest.Builder().build()
    RewardedAd.load(
        context,
        BillingConstants.getRewardedId(),
        adRequest,
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                onAdFailedToLoad()
            }
        }
    )
}

fun showRewardedAd(activity: Activity, rewardedAd: RewardedAd, onUserEarnedReward: () -> Unit) {
    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {}
        override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
        override fun onAdShowedFullScreenContent() {}
    }
    rewardedAd.show(activity) {
        onUserEarnedReward()
    }
}