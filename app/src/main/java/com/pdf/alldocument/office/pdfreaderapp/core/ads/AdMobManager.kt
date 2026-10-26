package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor() {

    companion object {
        private const val TAG = "AdMobManager"

        fun getAppId(context: Context): String =
            context.getString(com.pdf.alldocument.office.pdfreaderapp.R.string.app_Id)

        fun getBannerId(context: Context): String =
            context.getString(com.pdf.alldocument.office.pdfreaderapp.R.string.banner_Id)

        fun getNativeId(context: Context): String =
            context.getString(com.pdf.alldocument.office.pdfreaderapp.R.string.native_id)

        fun getInterstitialId(context: Context): String =
            context.getString(com.pdf.alldocument.office.pdfreaderapp.R.string.interstitial_Id)

        fun getAppOpenId(context: Context): String =
            context.getString(com.pdf.alldocument.office.pdfreaderapp.R.string.appopen_Id)

        fun getRewardedId(context: Context): String =
            context.getString(com.pdf.alldocument.office.pdfreaderapp.R.string.rewarded_interstitial_id)

        // Tracks whether an interstitial ad is currently showing on screen.
        // Prevents App Open Ad overlap.
        @Volatile
        var isInterstitialShowing: Boolean = false

        // Remote Config Keys (Matching source structure)
        const val REMOTE_CONFIG_KEY = "APPSTAN_ADS"
        const val KEY_APP_OPEN = "APP_OPEN"
        const val KEY_SPLASH_INTER = "SPLASH_INTER"
        const val KEY_LANGUAGE_INTER = "LANGUAGE_INTER"
        const val KEY_INTRO_INTER = "INTRO_INTER"
        const val KEY_ALL_INTER = "ALL_INTER"
        const val KEY_PDF_INTER = "PDF_INTER"
        const val KEY_WORD_INTER = "WORD_INTER"
        const val KEY_EXCEL_INTER = "EXCEL_INTER"
        const val KEY_POWER_POINT_INTER = "POWER_POINT_INTER"
        const val KEY_ALL_INTERNAL_INTER = "ALL_INTERNAL_INTER"
        const val KEY_PDF_INTERNAL_INTER = "PDF_INTERNAL_INTER"
        const val KEY_FILE_READER_NATIVE = "FILE_READER_NATIVE"
        const val KEY_MAIN_BANNER = "MAIN_BANNER"
        const val KEY_INTERNAL_BANNER = "INTERNAL_BANNER"

        // In-memory cache for remote ad values
        private val adValues = HashMap<String, String>().apply {
            put(KEY_APP_OPEN, "true")
            put(KEY_SPLASH_INTER, "true")
            put(KEY_LANGUAGE_INTER, "true")
            put(KEY_INTRO_INTER, "true")
            put(KEY_ALL_INTER, "true")
            put(KEY_PDF_INTER, "true")
            put(KEY_WORD_INTER, "true")
            put(KEY_EXCEL_INTER, "true")
            put(KEY_POWER_POINT_INTER, "true")
            put(KEY_ALL_INTERNAL_INTER, "true")
            put(KEY_PDF_INTERNAL_INTER, "true")
            put(KEY_FILE_READER_NATIVE, "true")
            put(KEY_MAIN_BANNER, "true")
            put(KEY_INTERNAL_BANNER, "true")
        }

        fun isAdEnabled(key: String, defaultValue: Boolean = true): Boolean {
            val value = adValues[key] ?: return defaultValue
            return value.equals("true", ignoreCase = true)
        }

        // Native Ad preloading is completely disabled.
        // Ads are requested and rendered on-demand in their respective activities/screens.
        fun consumePreloadedNativeAd(): com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd? = null
    }

    @Volatile
    var isInitialized = false
        private set

    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        if (isInitialized) {
            onComplete?.invoke()
            return
        }
        try {
            val appId = getAppId(context)
            val config = com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig.Builder(appId).build()
            MobileAds.initialize(
                context,
                config,
                object : com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener {
                    override fun onAdapterInitializationComplete(status: com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus) {
                        isInitialized = true
                        Log.d(TAG, "GMA Next-Gen MobileAds initialized successfully: $status")
                        onComplete?.invoke()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Next-Gen MobileAds", e)
        }
        fetchRemoteConfig()
    }

    private fun fetchRemoteConfig() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val jsonString = remoteConfig.getString(REMOTE_CONFIG_KEY)
                    if (jsonString.isNotBlank()) {
                        parseRemoteConfigJson(jsonString)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Remote Config: ${e.message}")
        }
    }

    private fun parseRemoteConfigJson(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                adValues[key] = jsonObject.optString(key, "true")
            }
            Log.d(TAG, "Remote ad values updated: $adValues")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse remote config JSON", e)
        }
    }
}
