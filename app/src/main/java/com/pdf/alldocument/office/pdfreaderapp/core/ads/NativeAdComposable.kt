package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.pdf.alldocument.office.pdfreaderapp.R


@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,
    adKey: String = AdMobManager.KEY_MAIN_BANNER
) {
    if (!AdMobManager.isAdEnabled(adKey)) return

    val context = LocalContext.current
    val targetId = adUnitId ?: AdMobManager.getBannerId(context)

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(context).apply {
                val bannerRequest = BannerAdRequest.Builder(targetId, AdSize.BANNER).build()
                loadAd(
                    bannerRequest,
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {}
                        override fun onAdFailedToLoad(adError: LoadAdError) {}
                    }
                )
            }
        }
    )
}

/**
 * Professional Native Ad Composable inflating the clean XML banner layout,
 * with proper asset view bindings (headline, body/advertiser, icon, CTA)
 * ensuring 100% compliant AdMob impressions and clicks using GMA Next-Gen SDK.
 */
enum class NativeAdStyle {
    MEDIUM,
    SMALL,
    HORIZONTAL_MEDIA
}

/**
 * Professional Native Ad Composable supporting Medium, Small & Horizontal Media styles,
 * with proper asset view bindings (headline, body/advertiser, icon, media view, CTA)
 * ensuring 100% compliant AdMob impressions and clicks using GMA Next-Gen SDK.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,
    adKey: String = AdMobManager.KEY_FILE_READER_NATIVE,
    style: NativeAdStyle = NativeAdStyle.MEDIUM
) {
    if (!AdMobManager.isAdEnabled(adKey)) return

    val context = LocalContext.current
    val targetId = adUnitId ?: AdMobManager.getNativeId(context)
    var currentNativeAd by remember { mutableStateOf<NativeAd?>(AdMobManager.consumePreloadedNativeAd()) }

    DisposableEffect(targetId) {
        if (currentNativeAd == null) {
            val request = NativeAdRequest.Builder(
                targetId,
                listOf(com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd.NativeAdType.NATIVE)
            ).build()
            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        currentNativeAd?.destroy()
                        currentNativeAd = nativeAd
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        currentNativeAd = null
                    }
                }
            )
        }

        onDispose {
            currentNativeAd?.destroy()
            AdMobManager.preloadNativeAd(context)
        }
    }

    if (currentNativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val layoutId = when (style) {
                        NativeAdStyle.HORIZONTAL_MEDIA -> R.layout.layout_native_ad_horizontal_media
                        NativeAdStyle.MEDIUM -> R.layout.layout_native_ad_view_medium
                        NativeAdStyle.SMALL -> R.layout.lyt_native_small_banner
                    }
                    val inflater = LayoutInflater.from(ctx)
                    val nativeAdView = inflater.inflate(
                        layoutId,
                        null,
                        false
                    ) as NativeAdView
                    populateNativeView(currentNativeAd!!, nativeAdView)
                    nativeAdView
                },
                update = { adView ->
                    currentNativeAd?.let { populateNativeView(it, adView) }
                }
            )
        }
    }
}

/**
 * Helper shortcut to explicitly invoke Medium Native Ad.
 */
@Composable
fun MediumNativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,
    adKey: String = AdMobManager.KEY_FILE_READER_NATIVE
) {
    NativeAdCard(
        modifier = modifier,
        adUnitId = adUnitId,
        adKey = adKey,
        style = NativeAdStyle.MEDIUM
    )
}

private fun populateNativeView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    val headlineView = nativeAdView.findViewById<TextView>(R.id.tv_primary)
    val secondaryView = nativeAdView.findViewById<TextView>(R.id.tv_secondary)
    val bodyView = nativeAdView.findViewById<TextView>(R.id.tv_body)
    val iconView = nativeAdView.findViewById<ImageView>(R.id.iv_icon)
    val ctaButton = nativeAdView.findViewById<Button>(R.id.btn_cta)
    val mediaView = nativeAdView.findViewById<com.google.android.libraries.ads.mobile.sdk.nativead.MediaView>(R.id.media_view)

    headlineView?.text = nativeAd.headline
    nativeAdView.headlineView = headlineView

    val advertiserOrStore = nativeAd.advertiser ?: nativeAd.store ?: ""
    if (advertiserOrStore.isNotBlank()) {
        secondaryView?.text = advertiserOrStore
        secondaryView?.visibility = View.VISIBLE
        nativeAdView.bodyView = secondaryView
    } else {
        val body = nativeAd.body ?: ""
        if (bodyView == null && body.isNotBlank()) {
            secondaryView?.text = body
            secondaryView?.visibility = View.VISIBLE
            nativeAdView.bodyView = secondaryView
        } else {
            secondaryView?.visibility = View.GONE
        }
    }

    if (bodyView != null) {
        val body = nativeAd.body ?: ""
        if (body.isNotBlank()) {
            bodyView.text = body
            bodyView.visibility = View.VISIBLE
            nativeAdView.bodyView = bodyView
        } else {
            bodyView.visibility = View.GONE
        }
    }

    if (nativeAd.icon != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
        nativeAdView.iconView = iconView
    } else {
        iconView?.visibility = View.GONE
    }

    val ctaText = if (!nativeAd.callToAction.isNullOrBlank()) nativeAd.callToAction else "Install"
    ctaButton?.text = ctaText
    ctaButton?.visibility = View.VISIBLE
    nativeAdView.callToActionView = ctaButton

    nativeAdView.registerNativeAd(nativeAd, mediaView)
}
