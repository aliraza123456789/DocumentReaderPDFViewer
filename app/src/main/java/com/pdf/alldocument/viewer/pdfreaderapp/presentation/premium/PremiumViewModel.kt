package com.pdf.alldocument.viewer.pdfreaderapp.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.pdf.alldocument.viewer.pdfreaderapp.core.billing.BillingManager
import com.pdf.alldocument.viewer.pdfreaderapp.core.billing.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val purchaseState: StateFlow<PurchaseState> = billingManager.purchaseState
        .stateIn(viewModelScope, SharingStarted.Eagerly, PurchaseState.Free)

    val products: StateFlow<List<ProductDetails>> = billingManager.productDetailsList
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun buyProduct(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchPurchaseFlow(activity, productDetails)
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
    }
}
