package com.pdf.alldocument.office.pdfreaderapp.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.R
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val target by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        when (target) {
            is SplashNavigationTarget.PrivacyPolicy -> onNavigateToPrivacyPolicy()
            is SplashNavigationTarget.Onboarding -> onNavigateToOnboarding()
            is SplashNavigationTarget.Home -> onNavigateToHome()
            else -> Unit
        }
    }

    // --- ANIMATION CONTROLLERS ---

    // 1. Logo Entrance Animation (spring bounce only — no fade)
    val logoScale = remember { Animatable(0.4f) }

    // 2. Continuous Ambient Floating & Breathing Aura
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_splash")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    // 3. Staggered Elements Reveal (slide only — no fade)
    val contentSlideY = remember { Animatable(24f) }

    // 4. Progress Loading Simulation (0% -> 100% across the 2-second splash window)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Start hero icon entrance (scale only)
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Reveal title and badges after short delay (slide only)
        launch {
            delay(280)
            contentSlideY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Fill progress smoothly
        launch {
            delay(150)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1900, easing = FastOutSlowInEasing)
            )
        }
    }

    // Dynamic loading stage text based on progress
    val loadingStatusText = when {
        progress.value < 0.35f -> "Initializing document engine..."
        progress.value < 0.75f -> "Scanning PDF & Office files..."
        progress.value < 0.95f -> "Preparing workspace..."
        else -> "Ready!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7F5FF),
                        Color(0xFFFFFFFF),
                        Color(0xFFF3F0FF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glowing orbs
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(auraScale)
                .alpha(auraAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandPurpleContainer.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top spacer
            Spacer(modifier = Modifier.height(16.dp))

            // Main Central Content: Animated Hero Logo + Title + Badges
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hero 3D Document Stack Icon
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .offset(y = floatOffsetY.dp)
                        .scale(logoScale.value),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft circular drop glow behind emblem
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        BrandPurpleDeep.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Image(
                        painter = painterResource(id = R.drawable.splash_pdf_icon),
                        contentDescription = "Document Reader Icon",
                        modifier = Modifier
                            .size(165.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Title and Subtitle with Smooth Slide
                Column(
                    modifier = Modifier
                        .offset(y = contentSlideY.value.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Document Reader",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp,
                        color = OnBrandPurpleContainer
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "All-in-One PDF & Office Suite",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        color = OnSurfaceVariantLight
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Floating Format Badges: PDF, WORD, EXCEL, PPT, TXT
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormatBadge(label = "PDF", color = ColorPdf)
                        FormatBadge(label = "WORD", color = ColorWord)
                        FormatBadge(label = "EXCEL", color = ColorExcel)
                        FormatBadge(label = "PPT", color = ColorPpt)
                        FormatBadge(label = "TXT", color = ColorTxt)
                    }
                }
            }

            // Bottom Section: Animated Progress Bar + Loading State + Version
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status caption
                Text(
                    text = loadingStatusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPurpleDeep.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Modern Sleek Progress Bar
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE8E5F5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.value.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BrandPurpleDeep,
                                        BrandPurple,
                                        BrandPurpleEnd
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Highlights
                Text(
                    text = "FAST  •  SECURE  •  OFFLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = OnSurfaceVariantLight.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "v1.0.0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = OnSurfaceVariantLight.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Capsule format badge with tinted indicator and clean typography
 */
@Composable
private fun FormatBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
