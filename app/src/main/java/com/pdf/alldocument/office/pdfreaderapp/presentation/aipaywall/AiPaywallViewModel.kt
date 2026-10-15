package com.pdf.alldocument.office.pdfreaderapp.presentation.aipaywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.analytics.AnalyticsManager
import com.pdf.alldocument.office.pdfreaderapp.core.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SubscriptionPlan {
    ANNUALLY,
    MONTHLY
}

sealed interface AiPaywallEvent {
    data object PurchaseSuccess : AiPaywallEvent
}

@HiltViewModel
class AiPaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _selectedPlan = MutableStateFlow(SubscriptionPlan.ANNUALLY)
    val selectedPlan: StateFlow<SubscriptionPlan> = _selectedPlan.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _events = MutableSharedFlow<AiPaywallEvent>()
    val events: SharedFlow<AiPaywallEvent> = _events.asSharedFlow()

    init {
        analyticsManager.logPaywallViewed("ai_paywall_screen")
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    /**
     * Executes a fake in-app purchase for testing as requested:
     * - Simulates Google Play payment sheet processing
     * - Activates fake premium in BillingManager
     * - Emits PurchaseSuccess event
     */
    fun processPurchase(onSuccess: () -> Unit) {
        if (_isPurchasing.value) return

        viewModelScope.launch {
            _isPurchasing.value = true
            analyticsManager.logBeginCheckout(
                productId = if (_selectedPlan.value == SubscriptionPlan.ANNUALLY) "yearly_sub" else "monthly_sub",
                priceMicros = if (_selectedPlan.value == SubscriptionPlan.ANNUALLY) 120000000L else 4000000L,
                currencyCode = "USD"
            )

            // Simulated Google Play verification delay
            delay(800L)

            // Activate fake premium in BillingManager
            billingManager.setFakePremium(true)
            analyticsManager.logSubscriptionSuccess(
                productId = if (_selectedPlan.value == SubscriptionPlan.ANNUALLY) "yearly_sub" else "monthly_sub",
                orderId = "GPA.FAKE-${System.currentTimeMillis()}"
            )

            _isPurchasing.value = false
            _events.emit(AiPaywallEvent.PurchaseSuccess)
            onSuccess()
        }
    }
}
