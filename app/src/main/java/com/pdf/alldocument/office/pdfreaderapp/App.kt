package com.pdf.alldocument.office.pdfreaderapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager
import com.pdf.alldocument.office.pdfreaderapp.core.ads.AppOpenAdHelper
import com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
import com.pdf.alldocument.office.pdfreaderapp.core.billing.BillingManager
import com.pdf.alldocument.office.pdfreaderapp.core.billing.PurchaseState
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var appOpenAdHelper: AppOpenAdHelper

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var interstitialAdHelper: InterstitialAdHelper

    companion object {
        const val CHANNEL_ID = "pdf_reader_channel"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // PDFBox loads its bundled font/configuration resources asynchronously to prevent cold start lag.
        CoroutineScope(Dispatchers.IO).launch {
            PDFBoxResourceLoader.init(applicationContext)
        }

        // Init Ads
        adMobManager.initialize(this)
        appOpenAdHelper.init(this)

        // Observe Billing State to disable Ads for Pro users
        CoroutineScope(Dispatchers.Main).launch {
            billingManager.purchaseState.collect { state ->
                val isPro = state is PurchaseState.Subscribed
                appOpenAdHelper.isPremiumUser = isPro
                interstitialAdHelper.isPremiumUser = isPro
            }
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Document Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for document processing, merging, and exports"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
