package com.kapcode.open.macropad.kmps

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper class that handles Google User Messaging Platform (UMP) consent flow.
 */
class ConsentManager(private val activity: Activity) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    /**
     * Interface definition for a callback to be invoked when consent gathering is complete.
     */
    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: String?)
    }

    /**
     * Helper variable to determine if the app can request ads.
     */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * Helper variable to determine if the privacy options form is required.
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Standard helper method to gather consent from the user.
     */
    fun gatherConsent(
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        // For testing purposes, you can force a locale to see the dialog.
        val debugSettings = ConsentDebugSettings.Builder(activity)
            // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            // .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
            .build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent has been gathered.
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError?.message)
                }
            },
            { requestConsentError ->
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError.message)
            }
        )
    }

    /**
     * Helper method to show the privacy options form.
     */
    fun showPrivacyOptionsForm(
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }
}
