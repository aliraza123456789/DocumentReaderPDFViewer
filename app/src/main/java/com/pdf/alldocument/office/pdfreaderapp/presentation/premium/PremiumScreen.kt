package com.pdf.alldocument.office.pdfreaderapp.presentation.premium

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.pdf.alldocument.office.pdfreaderapp.core.billing.BillingManager
import com.pdf.alldocument.office.pdfreaderapp.core.billing.PurchaseState
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AccentGold
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AppTopBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    isFromOnboarding: Boolean = false,
    viewModel: PremiumViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // 3-second countdown timer for Onboarding close button
    var remainingSeconds by remember { mutableIntStateOf(if (isFromOnboarding) 3 else 0) }

    // Preload Interstitial Ad early so it is ready when countdown finishes
    LaunchedEffect(Unit) {
        viewModel.interstitialAdHelper.loadAd(context)
    }

    LaunchedEffect(isFromOnboarding) {
        if (isFromOnboarding) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
        }
    }

    val onCloseWithAd = {
        if (activity != null) {
            viewModel.interstitialAdHelper.showAd(
                activity = activity,
                adKey = com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager.KEY_INTRO_INTER,
                frequencyEveryClicks = 1,
                onAdDismissed = onNavigateBack
            )
        } else {
            onNavigateBack()
        }
    }

    androidx.activity.compose.BackHandler(enabled = isFromOnboarding) {
        if (remainingSeconds == 0) {
            onCloseWithAd()
        }
    }

    // Feedback toast for Restore Purchases
    LaunchedEffect(Unit) {
        viewModel.restoreMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val isPro = purchaseState is PurchaseState.Subscribed

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Go Premium",
                onBackClick = if (isFromOnboarding) null else onNavigateBack,
                actions = {
                    TextButton(
                        onClick = { viewModel.restorePurchases() },
                        enabled = !isRestoring
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("Restore", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (isFromOnboarding) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AnimatedContent(
                            targetState = remainingSeconds,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "countdown_cross_transition"
                        ) { seconds ->
                            if (seconds > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${seconds}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onCloseWithAd,
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AccentGold, Color(0xFFFF8F00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isPro) "You Are a PRO Member! 👑" else "Upgrade to Document Reader PRO",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enjoy an ad-free experience, unlimited PDF merges, digital signatures, and offline office viewing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Pro Features List
            val features = listOf(
                "100% Ad-Free Experience",
                "Unlimited PDF Merging & Organization",
                "Digital Signature Pad & Watermark-Free Export",
                "High Performance Offline Office Rendering",
                "Priority Feature Updates"
            )

            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isPro) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Your subscription is active and all features are unlocked.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isFromOnboarding) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Continue to App", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Dynamic subscription plans matched by Product ID
                val weeklyProduct = products.find { it.productId == BillingManager.PRODUCT_WEEKLY }
                val monthlyProduct = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }
                val yearlyProduct = products.find { it.productId == BillingManager.PRODUCT_YEARLY }

                val weeklyPriceText = weeklyProduct?.let {
                    val p = it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    if (p != null) "$p / week" else null
                } ?: "$1.99 / week"

                val monthlyPriceText = monthlyProduct?.let {
                    val p = it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    if (p != null) "$p / month" else null
                } ?: "$4.99 / month"

                val yearlyPriceText = yearlyProduct?.let {
                    val p = it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    if (p != null) "$p / year (Save 80%)" else null
                } ?: "$19.99 / year (Save 80%)"

                data class UiPlan(
                    val title: String,
                    val priceText: String,
                    val isBest: Boolean,
                    val product: ProductDetails?
                )

                val plans = listOf(
                    UiPlan("Weekly Access", weeklyPriceText, false, weeklyProduct),
                    UiPlan("Monthly Access", monthlyPriceText, false, monthlyProduct),
                    UiPlan("Yearly Best Value", yearlyPriceText, true, yearlyProduct)
                )

                var selectedPlanIndex by remember { mutableIntStateOf(2) }

                plans.forEachIndexed { index, plan ->
                    val isSelected = selectedPlanIndex == index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedPlanIndex = index }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) AccentGold else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AccentGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = plan.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (plan.isBest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AccentGold)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "BEST",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = plan.priceText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPlanIndex = index }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val chosenPlan = plans.getOrNull(selectedPlanIndex)
                        val product = chosenPlan?.product
                        if (product != null && activity != null) {
                            viewModel.buyProduct(activity, product)
                        } else {
                            Toast.makeText(
                                context,
                                "Connecting to Google Play Store. If testing, please ensure you use an Internal Testing build.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue to Premium", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
