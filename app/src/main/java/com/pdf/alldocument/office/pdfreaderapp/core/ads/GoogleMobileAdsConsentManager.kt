package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google User Messaging Platform (UMP) consent lifecycle for GDPR / EU / UK compliance.
 *
 * Provides:
 * 1. gatherConsent(activity, onComplete): Updates consent information and shows the form if required.
 * 2. canRequestAds: Indicates if MobileAds can safely be initialized and ads requested.
 * 3. isPrivacyOptionsRequired: Indicates if the app settings must offer a "Privacy Settings" option.
 * 4. showPrivacyOptionsForm(activity, onDismiss): Re-opens privacy choices from settings.
 */
@Singleton
class GoogleMobileAdsConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConsentManager"

        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager =
            instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager(context.applicationContext).also {
                    instance = it
                }
            }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /**
     * Checks whether ads can be requested based on the latest consent state.
     * True if user provided required consent OR if user is located outside the EEA/UK.
     */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /**
     * Checks if a user is in a jurisdiction that requires privacy options (EEA/UK/etc).
     * When true, the app settings screen MUST render an entry to manage consent.
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Gathers user consent. If the user is in EEA/UK and has not consented yet,
     * the UMP consent form is loaded and displayed automatically.
     *
     * @param activity Hosting Activity required to present the UMP dialog.
     * @param isTestEea If true, forces EEA geography for local testing outside Europe.
     * @param testDeviceHashedId Optional hashed device ID from Logcat for testing.
     * @param onConsentGatheringCompleteListener Callback invoked once consent resolution is finished.
     */
    fun gatherConsent(
        activity: Activity,
        isTestEea: Boolean = false,
        testDeviceHashedId: String? = null,
        onConsentGatheringCompleteListener: (formError: FormError?) -> Unit
    ) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (isTestEea || !testDeviceHashedId.isNullOrBlank()) {
            val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
            if (isTestEea) {
                debugSettingsBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            }
            if (!testDeviceHashedId.isNullOrBlank()) {
                debugSettingsBuilder.addTestDeviceHashedId(testDeviceHashedId)
            }
            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
            Log.d(TAG, "Consent debug settings active (EEA: $isTestEea, testDevice: $testDeviceHashedId)")
        }

        val params = paramsBuilder.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info updated successfully; show form if required by regulations
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: [${formError.errorCode}] ${formError.message}")
                    } else {
                        Log.d(TAG, "Consent gathering completed. canRequestAds=$canRequestAds")
                    }
                    mainHandler.post {
                        onConsentGatheringCompleteListener(formError)
                    }
                }
            },
            { requestConsentError ->
                Log.e(
                    TAG,
                    "Failed to update consent info: [${requestConsentError.errorCode}] ${requestConsentError.message}"
                )
                mainHandler.post {
                    onConsentGatheringCompleteListener(requestConsentError)
                }
            }
        )
    }

    /**
     * Displays the privacy options form so users can alter or revoke consent from within app settings.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: (formError: FormError?) -> Unit
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Error displaying privacy options form: ${formError.message}")
            }
            mainHandler.post {
                onConsentFormDismissedListener(formError)
            }
        }
    }

    /**
     * Resets consent state back to UNKNOWN. Helpful during debugging.
     */
    fun reset() {
        consentInformation.reset()
        Log.d(TAG, "ConsentInformation state has been reset.")
    }
}
