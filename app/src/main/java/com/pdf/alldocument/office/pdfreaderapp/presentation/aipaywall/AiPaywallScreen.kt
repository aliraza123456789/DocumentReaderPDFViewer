package com.pdf.alldocument.office.pdfreaderapp.presentation.aipaywall

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AiPaywallScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiReading: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    viewModel: AiPaywallViewModel = hiltViewModel()
) {
    val selectedPlan by viewModel.selectedPlan.collectAsStateWithLifecycle()
    val isPurchasing by viewModel.isPurchasing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDetailSheet by remember { mutableStateOf(false) }
    var showSetDefaultSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Soft pastel gradient mesh matching 133.png
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            // Subtle mint green glow on top-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD1FAE5).copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.85f
                )
            )
            // Soft blush rose glow on top-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE4E6).copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(size.width, 0f),
                    radius = size.width * 0.85f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar ("Detail" on left, Close "X" on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // "Detail" text button
                Text(
                    text = "Detail",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showDetailSheet = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )

                // Circular close button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrollable content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Title: "Chat with AI"
                Text(
                    text = "Chat with AI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Mock Chat Feature Card matching 133.png
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = Color(0x1F000000)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // User Prompt Bubble (light pink/peach container)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF1F2), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "How does PDF AI utilize GPT- 3.5 technology to enhance user experience?",
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // AI Response Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "AI",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 1.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Dune is the story of a feudal society in the far distant future. It focuses on a Duke and his family, who have been forced to become stewards of the desert planet, Arrakis.",
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF334155)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mock Chat Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "What is compounents",
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Coral / Red Send Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFEF4444), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Dots Carousel Indicator matching 133.png
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFE2E8F0), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // "Choose a plan" Heading
                Text(
                    text = "Choose a plan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Plan 1: Annually
                PlanCard(
                    title = "Annually",
                    badge = "Best choice",
                    price = "$120.00",
                    period = "/yearly",
                    isSelected = selectedPlan == SubscriptionPlan.ANNUALLY,
                    onClick = { viewModel.selectPlan(SubscriptionPlan.ANNUALLY) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Plan 2: Monthly
                PlanCard(
                    title = "Monthly",
                    badge = null,
                    price = "$04.00",
                    period = "/weekly",
                    isSelected = selectedPlan == SubscriptionPlan.MONTHLY,
                    onClick = { viewModel.selectPlan(SubscriptionPlan.MONTHLY) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Continue Button
                Button(
                    onClick = {
                        viewModel.processPurchase {
                            Toast.makeText(
                                context,
                                "Test Purchase Successful! Premium Activated.",
                                Toast.LENGTH_SHORT
                            ).show()
                            showSetDefaultSheet = true
                        }
                    },
                    enabled = !isPurchasing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981) // Green matching 133.png
                    )
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3 days free trial subtext matching 133.png
                Text(
                    text = "3 days free trail, PKR-2,900/yearly after trails end.\ncancel anytime at least 24 hours renewal.",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Term of Service | Privacy links
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Term of Service",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.clickable { onNavigateToPrivacy() }
                    )
                    Text(
                        text = "  |  ",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Privacy",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.clickable { onNavigateToPrivacy() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Subscription Detail Bottom Sheet (137.png)
    if (showDetailSheet) {
        SubscriptionDetailBottomSheet(
            onDismissRequest = { showDetailSheet = false }
        )
    }

    // Set Default PDF Reader Bottom Sheet (125 (1).png)
    if (showSetDefaultSheet) {
        SetDefaultReaderBottomSheet(
            onLaterClick = {
                showSetDefaultSheet = false
                onNavigateToAiReading()
            },
            onContinueForward = {
                showSetDefaultSheet = false
                onNavigateToAiReading()
            },
            onDismissRequest = {
                showSetDefaultSheet = false
                onNavigateToAiReading()
            }
        )
    }
}

/**
 * Plan selection card matching screenshot 133.png
 */
@Composable
private fun PlanCard(
    title: String,
    badge: String?,
    price: String,
    period: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFECFDF5) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF10B981) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFEDD5), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = price,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = " $period",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Radio Button matching screenshot 133.png
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color(0xFF10B981) else Color(0xFFCBD5E1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Red/coral inner dot as visible in 133.png
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                }
            }
        }
    }
}
