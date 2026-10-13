package com.pdf.alldocument.office.pdfreaderapp.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AnalyticsManager
 *
 * Handles all Google Analytics & Firebase Conversion tracking.
 *
 * ARCHITECTURAL SAFETY NOTE:
 * - When AdMob is linked to Firebase in the AdMob & Firebase Console,
 *   the Google Mobile Ads SDK automatically streams the standard `ad_impression` event.
 *   DO NOT manually log the standard `ad_impression` event here to prevent double-counting revenue!
 * - When Google Play Console is linked to Firebase, `in_app_purchase` is automatically logged.
 *   Here we track key funnel actions (paywall views, begin_checkout, tool engagement, user properties)
 *   to train the Google Ads ML algorithm for App Campaigns (ACi & CPA/ROAS).
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    /**
     * User opens the Paywall / Subscription screen.
     * High purchase intent signal for Google Ads.
     */
    fun logPaywallViewed(source: String = "unknown") {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, "subscription_plans")
            putString(FirebaseAnalytics.Param.ITEM_NAME, "Premium Upgrade Screen")
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "paywall")
            putString("screen_source", source)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
    }

    /**
     * User clicks any subscribe/buy button.
     * Standard Google Analytics funnel event for ROAS / CPA bidding.
     */
    fun logBeginCheckout(
        productId: String,
        priceMicros: Long = 0L,
        currencyCode: String = "USD"
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, productId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, productId)
            putString(FirebaseAnalytics.Param.CURRENCY, currencyCode)
            if (priceMicros > 0L) {
                putDouble(FirebaseAnalytics.Param.VALUE, priceMicros / 1_000_000.0)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, bundle)
    }

    /**
     * In-App Purchase successfully completed and acknowledged.
     */
    fun logSubscriptionSuccess(productId: String, orderId: String? = null) {
        val bundle = Bundle().apply {
            putString("subscription_id", productId)
            if (!orderId.isNullOrBlank()) {
                putString("order_id", orderId)
            }
        }
        firebaseAnalytics.logEvent("subscription_purchased", bundle)
        setUserPremiumStatus(isPremium = true)
    }

    /**
     * User property updates: Enables segmentation of paying vs free users in Google Ads & Firebase.
     */
    fun setUserPremiumStatus(isPremium: Boolean) {
        firebaseAnalytics.setUserProperty("is_premium", if (isPremium) "true" else "false")
        firebaseAnalytics.setUserProperty("user_type", if (isPremium) "premium" else "free")
    }

    /**
     * Core feature engagement: Tracks when user uses any PDF tool (OCR, Convert, Merge, Split, etc.).
     */
    fun logToolUsed(toolName: String) {
        val bundle = Bundle().apply {
            putString("tool_name", toolName)
        }
        firebaseAnalytics.logEvent("tool_used", bundle)
    }

    /**
     * Core reader engagement: Tracks when user opens a document to view.
     */
    fun logDocumentOpened(fileType: String) {
        val bundle = Bundle().apply {
            putString("file_type", fileType)
        }
        firebaseAnalytics.logEvent("document_opened", bundle)
    }

    /**
     * Safe ad event: Tracks ad display without colliding with AdMob's automated `ad_impression`.
     */
    fun logAdDisplay(format: String, adUnitId: String) {
        val bundle = Bundle().apply {
            putString("ad_format", format)
            putString("ad_unit_id", adUnitId)
        }
        firebaseAnalytics.logEvent("ad_display_success", bundle)
    }
}
