package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.pdf.alldocument.office.pdfreaderapp.core.analytics.AnalyticsManager
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenAdHelper @Inject constructor(
    private val analyticsManager: AnalyticsManager
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppOpenAdHelper"
        var isAlreadyShowingAd = false
        var lastInterstitialDismissedTime: Long = 0L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var isAdRequested = false
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0
    var isShowingAd = false
    var isPremiumUser = false

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // First time request is sent right from AppOpenAdHelper on startup
        loadAd(application)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val activity = currentActivity ?: return

        // Respect Remote Config switch, Premium status, and Interstitial state
        if (isPremiumUser || !AdMobManager.isAdEnabled(AdMobManager.KEY_APP_OPEN)) {
            Log.d(TAG, "App open ad disabled by config or premium status.")
            return
        }

        // Never show App Open Ad while an Interstitial Ad is showing or was just dismissed
        if (AdMobManager.isInterstitialShowing || isAlreadyShowingAd || (System.currentTimeMillis() - lastInterstitialDismissedTime < 3000L)) {
            Log.d(TAG, "Skipping App Open ad because Interstitial/another ad is currently visible or recently closed.")
            return
        }

        currentActivity?.let {
            showAdIfAvailable(it)
        }
    }

    fun loadAd(context: android.content.Context) {
        // Enforce rule: second request must NEVER be sent until the loaded ad is actually shown.
        if (isAdLoading || (isAdRequested && appOpenAd != null) || isPremiumUser || !AdMobManager.isAdEnabled(AdMobManager.KEY_APP_OPEN)) {
            Log.d(TAG, "loadAd skipped: isAdLoading=$isAdLoading, isAdRequested=$isAdRequested, hasAd=${appOpenAd != null}")
            return
        }

        isAdLoading = true
        isAdRequested = true
        val targetId = AdMobManager.getAppOpenId(context)
        val request = AdRequest.Builder(targetId).build()
        try {
            AppOpenAd.load(
                request,
                object : AdLoadCallback<AppOpenAd> {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        mainHandler.post {
                            appOpenAd = ad
                            isAdLoading = false
                            loadTime = Date().time
                            Log.d(TAG, "GMA Next-Gen AppOpenAd loaded successfully.")
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        mainHandler.post {
                            appOpenAd = null
                            isAdLoading = false
                            isAdRequested = false // Reset only on load failure to allow retry
                            Log.e(TAG, "GMA Next-Gen AppOpenAd failed to load: ${adError.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            mainHandler.post {
                appOpenAd = null
                isAdLoading = false
                isAdRequested = false
                Log.e(TAG, "Failed to call AppOpenAd.load", e)
            }
        }
    }

    private fun wasLoadTimeLessThan4HoursAgo(): Boolean {
        val numHours = 4
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThan4HoursAgo()
    }

    fun showAdIfAvailable(activity: Activity, onDismiss: () -> Unit = {}) {
        val safeDismiss = {
            runOnMainThread { onDismiss() }
        }

        if (isShowingAd || isAlreadyShowingAd || isPremiumUser || AdMobManager.isInterstitialShowing) {
            safeDismiss()
            return
        }

        if (!isAdAvailable()) {
            if (!isAdRequested && !isAdLoading && appOpenAd == null) {
                loadAd(activity)
            }
            safeDismiss()
            return
        }

        val ad = appOpenAd
        if (ad != null) {
            ad.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    runOnMainThread {
                        appOpenAd = null
                        isShowingAd = false
                        isAlreadyShowingAd = false
                        // Now that the ad was successfully shown and dismissed, reset and load the next ad
                        isAdRequested = false
                        loadAd(activity)
                        safeDismiss()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    runOnMainThread {
                        appOpenAd = null
                        isShowingAd = false
                        isAlreadyShowingAd = false
                        isAdRequested = false
                        safeDismiss()
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    runOnMainThread {
                        isShowingAd = true
                        isAlreadyShowingAd = true
                        analyticsManager.logAdDisplay("app_open", AdMobManager.getAppOpenId(activity))
                    }
                }
            }
            runOnMainThread {
                ad.show(activity)
            }
        } else {
            safeDismiss()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd && !AdMobManager.isInterstitialShowing) {
            currentActivity = activity
        }
    }
    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd && !AdMobManager.isInterstitialShowing) {
            currentActivity = activity
        }
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
