package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var actionCounter = 0
    var isPremiumUser = false

    private var onAdLoadedListener: ((InterstitialAd) -> Unit)? = null
    private var onAdFailedListener: (() -> Unit)? = null

    fun loadAd(context: Context, adUnitId: String? = null) {
        if (interstitialAd != null || isAdLoading || isPremiumUser) return

        val targetId = adUnitId ?: AdMobManager.getInterstitialId(context)
        isAdLoading = true
        val adRequest = AdRequest.Builder(targetId).build()
        try {
            InterstitialAd.load(
                adRequest,
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        runOnMainThread {
                            interstitialAd = ad
                            isAdLoading = false
                            Log.d(TAG, "GMA Next-Gen InterstitialAd loaded successfully.")
                            val listener = onAdLoadedListener
                            onAdLoadedListener = null
                            onAdFailedListener = null
                            listener?.invoke(ad)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnMainThread {
                            interstitialAd = null
                            isAdLoading = false
                            Log.e(TAG, "GMA Next-Gen InterstitialAd failed to load: ${adError.message}")
                            val listener = onAdFailedListener
                            onAdLoadedListener = null
                            onAdFailedListener = null
                            listener?.invoke()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            runOnMainThread {
                interstitialAd = null
                isAdLoading = false
                Log.e(TAG, "Failed to call InterstitialAd.load", e)
                val listener = onAdFailedListener
                onAdLoadedListener = null
                onAdFailedListener = null
                listener?.invoke()
            }
        }
    }

    /**
     * Shows an Interstitial Ad with optional frequency capping (e.g. every Nth user click).
     * @param frequencyEveryClicks Default 1 (shows on every call if loaded). Set to 2 to show every 2nd action.
     */
    fun showAd(
        activity: Activity,
        adKey: String = AdMobManager.KEY_ALL_INTERNAL_INTER,
        frequencyEveryClicks: Int = 1,
        onAdDismissed: () -> Unit = {}
    ) {
        val safeDismiss = {
            runOnMainThread { onAdDismissed() }
        }

        if (isPremiumUser || !AdMobManager.isAdEnabled(adKey)) {
            safeDismiss()
            return
        }

        actionCounter++
        if (actionCounter < frequencyEveryClicks) {
            safeDismiss()
            return
        }
        actionCounter = 0 // Reset counter

        val ad = interstitialAd
        if (ad != null) {
            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    runOnMainThread {
                        interstitialAd = null
                        AdMobManager.isInterstitialShowing = false
                        AppOpenAdHelper.isAlreadyShowingAd = false
                        AppOpenAdHelper.lastInterstitialDismissedTime = System.currentTimeMillis()
                        loadAd(activity)
                        safeDismiss()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    runOnMainThread {
                        interstitialAd = null
                        AdMobManager.isInterstitialShowing = false
                        AppOpenAdHelper.isAlreadyShowingAd = false
                        AppOpenAdHelper.lastInterstitialDismissedTime = System.currentTimeMillis()
                        loadAd(activity)
                        safeDismiss()
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    runOnMainThread {
                        AdMobManager.isInterstitialShowing = true
                        AppOpenAdHelper.isAlreadyShowingAd = true
                        analyticsManager.logAdDisplay("interstitial", AdMobManager.getInterstitialId(activity))
                    }
                }
            }
            runOnMainThread {
                ad.show(activity)
            }
        } else {
            loadAd(activity)
            safeDismiss()
        }
    }

    /**
     * Dedicated method for Splash screen:
     * If the ad is already loaded, shows immediately.
     * If still loading, waits up to [maxWaitMs] (e.g. 2500ms) for the network response.
     * As soon as loaded, shows immediately.
     */
    fun showSplashAd(
        activity: Activity,
        adKey: String = AdMobManager.KEY_SPLASH_INTER,
        maxWaitMs: Long = 2500L,
        onDismiss: () -> Unit
    ) {
        val safeDismiss = {
            runOnMainThread { onDismiss() }
        }

        if (isPremiumUser || !AdMobManager.isAdEnabled(adKey)) {
            safeDismiss()
            return
        }

        if (interstitialAd != null) {
            showAd(activity, adKey, frequencyEveryClicks = 1, onAdDismissed = onDismiss)
            return
        }

        if (!isAdLoading) {
            loadAd(activity)
            safeDismiss()
            return
        }

        var hasFinished = false
        val timeoutRunnable = Runnable {
            if (!hasFinished) {
                hasFinished = true
                onAdLoadedListener = null
                onAdFailedListener = null
                Log.d(TAG, "Splash ad wait timed out. Continuing navigation.")
                safeDismiss()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, maxWaitMs)

        onAdLoadedListener = {
            if (!hasFinished) {
                hasFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                showAd(activity, adKey, frequencyEveryClicks = 1, onAdDismissed = onDismiss)
            }
        }

        onAdFailedListener = {
            if (!hasFinished) {
                hasFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                safeDismiss()
            }
        }
    }
}
