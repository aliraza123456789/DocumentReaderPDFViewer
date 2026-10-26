package com.pdf.alldocument.office.pdfreaderapp.presentation.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdCard
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdStyle
import com.pdf.alldocument.office.pdfreaderapp.core.localization.LanguageManager
import com.pdf.alldocument.office.pdfreaderapp.core.localization.SupportedLanguage
import kotlinx.coroutines.launch

// Green colour that matches the "Done" button and selected radio in the reference design
private val LangGreen = Color(0xFF34C759)

@Composable
fun LanguageScreen(
    isFirstLaunch: Boolean = false,
    viewModel: LanguageViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onLanguageSelected: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val savedCode by viewModel.selectedLanguageCode.collectAsStateWithLifecycle()
    var userSelectedCode by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCode = userSelectedCode ?: savedCode

    // Preload Interstitial ad
    LaunchedEffect(Unit) {
        viewModel.interstitialAdHelper.loadAd(context)
    }

    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val onConfirmSelection = {
        if (!isProcessing) {
            isProcessing = true
            val proceedNext = {
                scope.launch {
                    viewModel.saveLanguageSuspend(selectedCode)
                    LanguageManager.setAppLocale(context, selectedCode)
                    onLanguageSelected()
                }
            }

            if (activity != null) {
                viewModel.interstitialAdHelper.showAd(
                    activity = activity,
                    adKey = com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager.KEY_LANGUAGE_INTER,
                    frequencyEveryClicks = 1,
                    onAdDismissed = { proceedNext() }
                )
            } else {
                proceedNext()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            // Custom top bar replicating the reference image: "Languages" bold title + green "Done" button
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isFirstLaunch && onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1A1A1A)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "Languages",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }

                    // Green "Done" pill button
                    Button(
                        onClick = onConfirmSelection,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LangGreen,
                            disabledContainerColor = LangGreen.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    NativeAdCard(
                        style = NativeAdStyle.HORIZONTAL_MEDIA
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            items(viewModel.languages, key = { it.code }) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language.code == selectedCode,
                    onClick = {
                        userSelectedCode = language.code
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: SupportedLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: circular flag icon + language names
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular flag
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                ) {
                    if (language.flagRes != 0) {
                        Icon(
                            painter = painterResource(id = language.flagRes),
                            contentDescription = "${language.displayName} flag",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            tint = Color.Unspecified
                        )
                    } else {
                        // Fallback: emoji centred in circle
                        Text(
                            text = language.flagEmoji,
                            fontSize = 24.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Language name + native name
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = language.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${language.nativeName})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF8A8A8A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: radio button circle
            RadioCircle(isSelected = isSelected)
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = Color(0xFFE0E0E0)
    )
}

/**
 * Custom radio circle that renders exactly like the reference image:
 * - Selected: filled green circle with white centre dot
 * - Unselected: empty circle with grey border
 */
@Composable
private fun RadioCircle(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(LangGreen)
                } else {
                    Modifier
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFCCCCCC), CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Inner white dot
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
