package com.pdf.alldocument.office.pdfreaderapp.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.pdf.alldocument.office.pdfreaderapp.core.analytics.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: AnalyticsManager
) : PurchasesUpdatedListener {

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Free)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList: StateFlow<List<ProductDetails>> = _productDetailsList.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    companion object {
        const val PRODUCT_WEEKLY = "weekly_sub"
        const val PRODUCT_MONTHLY = "monthly_sub"
        const val PRODUCT_YEARLY = "yearly_sub"
        const val PRODUCT_LIFETIME = "lifetime_pro"
    }

    fun setFakePremium(isPremium: Boolean = true) {
        _purchaseState.value = if (isPremium) PurchaseState.Subscribed else PurchaseState.Free
        analyticsManager.setUserPremiumStatus(isPremium)
    }

    init {
        startConnection()
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection logic
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_WEEKLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetailsList.value = productDetailsResult.productDetailsList
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return
        executeRestoreQuery(null)
    }

    fun restorePurchases(onResult: (isSuccess: Boolean, message: String) -> Unit) {
        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProducts()
                        executeRestoreQuery(onResult)
                    } else {
                        onResult(false, "Failed to connect to Google Play store.")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    onResult(false, "Google Play service disconnected. Please try again.")
                }
            })
            return
        }

        executeRestoreQuery(onResult)
    }

    private fun executeRestoreQuery(onResult: ((isSuccess: Boolean, message: String) -> Unit)?) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (hasActiveSub) {
                    _purchaseState.value = PurchaseState.Subscribed
                    analyticsManager.setUserPremiumStatus(true)
                    onResult?.invoke(true, "Subscription restored successfully! All features unlocked.")
                    return@queryPurchasesAsync
                }
            }

            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            ) { inAppResult, inAppPurchases ->
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasLifetime = inAppPurchases.any {
                        it.products.contains(PRODUCT_LIFETIME) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    if (hasLifetime) {
                        _purchaseState.value = PurchaseState.LifetimePro
                        analyticsManager.setUserPremiumStatus(true)
                        onResult?.invoke(true, "Lifetime Pro purchase restored successfully!")
                        return@queryPurchasesAsync
                    }
                }
                _purchaseState.value = PurchaseState.Free
                analyticsManager.setUserPremiumStatus(false)
                onResult?.invoke(false, "No active subscriptions or purchases found to restore.")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val pricingPhase = productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
        val priceMicros = pricingPhase?.priceAmountMicros ?: productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L
        val currencyCode = pricingPhase?.priceCurrencyCode ?: productDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: "USD"
        analyticsManager.logBeginCheckout(
            productId = productDetails.productId,
            priceMicros = priceMicros,
            currencyCode = currencyCode
        )

        val productDetailsParamsList = if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val productId = purchase.products.firstOrNull() ?: "unknown"
            analyticsManager.logSubscriptionSuccess(productId = productId, orderId = purchase.orderId)

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) {
                    queryPurchases()
                }
            } else {
                queryPurchases()
            }
        }
    }
}
