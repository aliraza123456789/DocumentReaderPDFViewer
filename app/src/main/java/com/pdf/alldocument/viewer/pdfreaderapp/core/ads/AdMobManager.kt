package com.pdf.alldocument.viewer.pdfreaderapp.core.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor() {

    // Test Ad Unit IDs (Google Sample IDs for development / safe testing)
    companion object {
        const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257399415"
        const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context) {
                isInitialized = true
            }
        }
    }
}
