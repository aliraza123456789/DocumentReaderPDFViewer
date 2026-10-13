package com.pdf.alldocument.office.pdfreaderapp.core.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.TEST_BANNER_ID
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.TEST_NATIVE_ID
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

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
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                factory = { ctx ->
                    val adView = NativeAdView(ctx)
                    populateNativeAdView(nativeAd!!, adView)
                    adView
                },
                update = { adView ->
                    nativeAd?.let { populateNativeAdView(it, adView) }
                }
            )
        }
    }
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // Dynamically build or populate views
    val headline = TextView(adView.context).apply {
        text = nativeAd.headline
        textSize = 15f
        setTypeface(null, android.graphics.Typeface.BOLD)
    }
    val body = TextView(adView.context).apply {
        text = nativeAd.body
        textSize = 13f
        maxLines = 2
    }
    val callToAction = Button(adView.context).apply {
        text = nativeAd.callToAction ?: "Install"
        textSize = 12f
    }

    val layout = android.widget.LinearLayout(adView.context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        addView(headline)
        if (nativeAd.body != null) addView(body)
        addView(callToAction)
    }

    adView.removeAllViews()
    adView.addView(layout)

    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = callToAction
    adView.setNativeAd(nativeAd)
}
