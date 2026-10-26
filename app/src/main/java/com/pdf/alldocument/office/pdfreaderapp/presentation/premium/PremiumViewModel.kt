package com.pdf.alldocument.office.pdfreaderapp.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.pdf.alldocument.office.pdfreaderapp.core.analytics.AnalyticsManager
import com.pdf.alldocument.office.pdfreaderapp.core.billing.BillingManager
import com.pdf.alldocument.office.pdfreaderapp.core.billing.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val analyticsManager: AnalyticsManager,
    val interstitialAdHelper: com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
) : ViewModel() {

    init {
        analyticsManager.logPaywallViewed("premium_screen")
    }

    val purchaseState: StateFlow<PurchaseState> = billingManager.purchaseState
        .stateIn(viewModelScope, SharingStarted.Eagerly, PurchaseState.Free)

    val products: StateFlow<List<ProductDetails>> = billingManager.productDetailsList
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _restoreMessage = MutableSharedFlow<String>()
    val restoreMessage: SharedFlow<String> = _restoreMessage.asSharedFlow()

    fun buyProduct(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchPurchaseFlow(activity, productDetails)
    }

    fun restorePurchases() {
        if (_isRestoring.value) return
        _isRestoring.value = true
        viewModelScope.launch {
            billingManager.restorePurchases { _, message ->
                _isRestoring.value = false
                viewModelScope.launch {
                    _restoreMessage.emit(message)
                }
            }
        }
    }
}
