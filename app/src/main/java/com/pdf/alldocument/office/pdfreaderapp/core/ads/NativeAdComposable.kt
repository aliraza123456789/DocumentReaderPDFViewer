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

/**
 * Standard Small Banner Ad (320x50 standard banner) using GMA Next-Gen SDK.
 */
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
                        override fun onAdFailedToLoad(error: LoadAdError) {}
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
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,
    adKey: String = AdMobManager.KEY_FILE_READER_NATIVE
) {
    if (!AdMobManager.isAdEnabled(adKey)) return

    val context = LocalContext.current
    val targetId = adUnitId ?: AdMobManager.getNativeId(context)
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(targetId) {
        val request = NativeAdRequest.Builder(
            targetId,
            listOf(com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd.NativeAdType.NATIVE)
        ).build()
        NativeAdLoader.load(
            request,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(ad: NativeAd) {
                    nativeAd?.destroy()
                    nativeAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                }
            }
        )

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (nativeAd != null) {
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
                    val inflater = LayoutInflater.from(ctx)
                    val nativeAdView = inflater.inflate(
                        R.layout.lyt_native_small_banner,
                        null,
                        false
                    ) as NativeAdView
                    populateNativeView(nativeAd!!, nativeAdView)
                    nativeAdView
                },
                update = { adView ->
                    nativeAd?.let { populateNativeView(it, adView) }
                }
            )
        }
    }
}

private fun populateNativeView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    val headlineView = nativeAdView.findViewById<TextView>(R.id.tv_primary)
    val secondaryView = nativeAdView.findViewById<TextView>(R.id.tv_secondary)
    val iconView = nativeAdView.findViewById<ImageView>(R.id.iv_icon)
    val ctaButton = nativeAdView.findViewById<Button>(R.id.btn_cta)

    headlineView?.text = nativeAd.headline
    nativeAdView.headlineView = headlineView

    val bodyOrAdvertiser = nativeAd.body ?: nativeAd.advertiser ?: nativeAd.store ?: ""
    if (bodyOrAdvertiser.isNotBlank()) {
        secondaryView?.text = bodyOrAdvertiser
        secondaryView?.visibility = View.VISIBLE
        nativeAdView.bodyView = secondaryView
    } else {
        secondaryView?.visibility = View.GONE
    }

    if (nativeAd.icon != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
        nativeAdView.iconView = iconView
    } else {
        iconView?.visibility = View.GONE
    }

    if (!nativeAd.callToAction.isNullOrBlank()) {
        ctaButton?.text = nativeAd.callToAction
        ctaButton?.visibility = View.VISIBLE
        nativeAdView.callToActionView = ctaButton
    } else {
        ctaButton?.visibility = View.GONE
    }

    nativeAdView.registerNativeAd(nativeAd, null)
}
