package com.pdf.alldocument.office.pdfreaderapp.core.billing

sealed interface PurchaseState {
    data object Free : PurchaseState
    data object Subscribed : PurchaseState
}

data class SubscriptionProduct(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val isWeekly: Boolean = false,
    val isMonthly: Boolean = false,
    val isYearly: Boolean = false
)
