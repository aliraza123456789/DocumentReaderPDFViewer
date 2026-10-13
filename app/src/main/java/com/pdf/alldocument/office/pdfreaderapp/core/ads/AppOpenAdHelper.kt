package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
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
    }

    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0
    var isShowingAd = false
    var isPremiumUser = false

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Respect Remote Config switch, Premium status, and Interstitial state
        if (isPremiumUser || !AdMobManager.isAdEnabled(AdMobManager.KEY_APP_OPEN)) {
            Log.d(TAG, "App open ad disabled by config or premium status.")
            return
        }

        // Never show App Open Ad while an Interstitial Ad is showing or requested
        if (AdMobManager.isInterstitialShowing || isAlreadyShowingAd) {
            Log.d(TAG, "Skipping App Open ad because Interstitial/another ad is currently visible.")
            return
        }

        currentActivity?.let {
            showAdIfAvailable(it)
        }
    }

    fun loadAd(activity: Activity) {
        if (isAdAvailable() || isAdLoading || isPremiumUser || !AdMobManager.isAdEnabled(AdMobManager.KEY_APP_OPEN)) {
            return
        }

        isAdLoading = true
        val targetId = AdMobManager.getAppOpenId(activity)
        val request = AdRequest.Builder(targetId).build()
        AppOpenAd.load(
            request,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAdLoading = false
                    loadTime = Date().time
                    Log.d(TAG, "GMA Next-Gen AppOpenAd loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isAdLoading = false
                    Log.e(TAG, "GMA Next-Gen AppOpenAd failed to load: ${loadAdError.message}")
                }
            }
        )
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
        if (isShowingAd || isAlreadyShowingAd || isPremiumUser || AdMobManager.isInterstitialShowing) {
            onDismiss()
            return
        }

        if (!isAdAvailable()) {
            loadAd(activity)
            onDismiss()
            return
        }

        val ad = appOpenAd
        if (ad != null) {
            ad.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    isAlreadyShowingAd = false
                    loadAd(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    appOpenAd = null
                    isShowingAd = false
                    isAlreadyShowingAd = false
                    loadAd(activity)
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    isAlreadyShowingAd = true
                    analyticsManager.logAdDisplay("app_open", AdMobManager.getAppOpenId(activity))
                }
            }
            ad.show(activity)
        } else {
            loadAd(activity)
            onDismiss()
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
