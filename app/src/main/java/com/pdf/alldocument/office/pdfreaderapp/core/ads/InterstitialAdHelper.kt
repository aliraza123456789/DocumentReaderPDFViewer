package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.pdf.alldocument.office.pdfreaderapp.core.analytics.AnalyticsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterstitialAdHelper @Inject constructor(
    private val analyticsManager: AnalyticsManager
) {

    companion object {
        private const val TAG = "InterstitialAdHelper"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var actionCounter = 0
    var isPremiumUser = false

    fun loadAd(context: Context, adUnitId: String? = null) {
        if (interstitialAd != null || isAdLoading || isPremiumUser) return

        val targetId = adUnitId ?: AdMobManager.getInterstitialId(context)
        isAdLoading = true
        val adRequest = AdRequest.Builder(targetId).build()
        InterstitialAd.load(
            adRequest,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "GMA Next-Gen InterstitialAd loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.e(TAG, "GMA Next-Gen InterstitialAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows an Interstitial Ad with optional frequency capping (e.g. every Nth user click).
     * @param frequencyEveryClicks Default 1 (shows on every call if loaded). Set to 2 to show every 2nd action.
     */
    fun showAd(
        activity: Activity,
        adKey: String = AdMobManager.KEY_ALL_INTERNAL_INTER,
        frequencyEveryClicks: Int = 1,
        onAdDismissed: () -> Unit
    ) {
        if (isPremiumUser || !AdMobManager.isAdEnabled(adKey)) {
            onAdDismissed()
            return
        }

        actionCounter++
        if (actionCounter < frequencyEveryClicks) {
            onAdDismissed()
            return
        }
        actionCounter = 0 // Reset counter

        val ad = interstitialAd
        if (ad != null) {
            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    AdMobManager.isInterstitialShowing = false
                    AppOpenAdHelper.isAlreadyShowingAd = false
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    interstitialAd = null
                    AdMobManager.isInterstitialShowing = false
                    AppOpenAdHelper.isAlreadyShowingAd = false
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    AdMobManager.isInterstitialShowing = true
                    AppOpenAdHelper.isAlreadyShowingAd = true
                    analyticsManager.logAdDisplay("interstitial", AdMobManager.getInterstitialId(activity))
                }
            }
            ad.show(activity)
        } else {
            loadAd(activity)
            onAdDismissed()
        }
    }
}
