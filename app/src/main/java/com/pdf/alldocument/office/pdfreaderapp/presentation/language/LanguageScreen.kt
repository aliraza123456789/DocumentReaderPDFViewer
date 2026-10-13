package com.pdf.alldocument.office.pdfreaderapp.presentation.language

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdCard
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AppTopBar
import com.pdf.alldocument.office.pdfreaderapp.core.localization.LanguageManager
import com.pdf.alldocument.office.pdfreaderapp.core.localization.SupportedLanguage
import kotlinx.coroutines.launch

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
    var selectedCode by remember(savedCode) { mutableStateOf(savedCode) }

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
        topBar = {
            AppTopBar(
                title = "Select Language",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = onConfirmSelection,
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Select Language",
                            tint = if (isProcessing) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NativeAdCard()
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(viewModel.languages, key = { it.code }) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language.code == selectedCode,
                    onClick = {
                        if (selectedCode != language.code) {
                            selectedCode = language.code
                            LanguageManager.setAppLocale(context, language.code)
                        }
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
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = language.flagEmoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
