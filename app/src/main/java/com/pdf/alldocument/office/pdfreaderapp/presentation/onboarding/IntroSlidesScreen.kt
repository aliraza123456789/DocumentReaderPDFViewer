package com.pdf.alldocument.office.pdfreaderapp.presentation.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdf.alldocument.office.pdfreaderapp.R
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

data class OnboardingPage(
    val illustrationRes: Int,
    val title: String,
    val description: String
)

val onboardingPages = listOf(
    OnboardingPage(
        illustrationRes = R.drawable.onboarding_1_all_in_one,
        title = "All Your Documents",
        description = "Read PDF, Word, Excel, PowerPoint — all in one beautiful reader"
    ),
    OnboardingPage(
        illustrationRes = R.drawable.onboarding_2_scan_to_pdf,
        title = "Scan & Convert",
        description = "Turn camera photos into searchable PDFs instantly — no cloud needed"
    ),
    OnboardingPage(
        illustrationRes = R.drawable.onboarding_3_edit_tools,
        title = "Powerful PDF Tools",
        description = "Merge, sign, lock and organize your documents with ease"
    )
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

private val NavyDark = Color(0xFF0F172A)
private val Indigo = Color(0xFF6366F1)

@Composable
fun IntroSlidesScreen(
    viewModel: IntroViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    val pages = onboardingPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Full screen HorizontalPager allows swiping anywhere on the screen
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Illustration area ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = pages[page].illustrationRes),
                        contentDescription = pages[page].title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(280.dp)
                    )
                }

                // ── Dark navy card content ───────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(NavyDark)
                        .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 28.dp)
                ) {
                    Column {
                        Text(
                            text = pages[page].title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = pages[page].description,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Start,
                            lineHeight = 22.sp,
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        // Space reservation for the floating bottom navigation row
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }
            }
        }

        // ── Bottom navigation controls overlay (dots + skip / next button) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 28.dp, end = 28.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot indicators
                Row(verticalAlignment = Alignment.CenterVertically) {
                    pages.indices.forEach { index ->
                        val isSelected = index == pagerState.currentPage
                        val animatedWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "dot_width_$index"
                        )
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .width(animatedWidth)
                                .height(8.dp)
                                .background(
                                    if (isSelected) Indigo
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isLastPage) {
                        TextButton(
                            onClick = {
                                viewModel.completeOnboarding()
                                onFinish()
                            }
                        ) {
                            Text(
                                text = "Skip",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (!isLastPage) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                viewModel.completeOnboarding()
                                onFinish()
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                        modifier = Modifier.size(52.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (isLastPage) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Get Started",
                                tint = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
