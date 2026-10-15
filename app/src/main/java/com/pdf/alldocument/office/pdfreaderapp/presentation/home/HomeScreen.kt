package com.pdf.alldocument.office.pdfreaderapp.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.R
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.BrandPurple
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.ConfirmDeleteDialog
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.RenameDialog
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * This screen is designed to always look like the light mock, even when the
 * device/app is in dark mode — so its colors are pinned here instead of
 * following MaterialTheme.colorScheme (which was resolving to the dark
 * palette and turning the whole screen near-black).
 */
private val HomeLightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF1FE),
    onPrimaryContainer = Color(0xFF3730A3),
    surface = Color.White,
    onSurface = Color(0xFF1B1B24),
    surfaceVariant = Color(0xFFF3F3F6),
    onSurfaceVariant = Color(0xFF8A8A99),
    background = Color.White,
    onBackground = Color(0xFF1B1B24),
    outline = Color(0xFFD8D8E0),
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFE53935),
    onError = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPdfViewer: (DocumentItem) -> Unit,
    onNavigateToOfficeViewer: (DocumentItem) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var documentToDelete by remember { mutableStateOf<DocumentItem?>(null) }
    var documentToRename by remember { mutableStateOf<DocumentItem?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    // Preload Interstitial ad
    LaunchedEffect(Unit) {
        viewModel.interstitialAdHelper.loadAd(context)
    }

    // Collect side effects
    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.RefreshRequested)
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPdfViewer -> {
                    onNavigateToPdfViewer(effect.document)
                    val adKey = com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager.KEY_PDF_INTERNAL_INTER
                    if (activity != null) {
                        viewModel.interstitialAdHelper.showAd(
                            activity = activity,
                            adKey = adKey,
                            frequencyEveryClicks = 2
                        )
                    }
                }
                is HomeEffect.NavigateToOfficeViewer -> {
                    onNavigateToOfficeViewer(effect.document)
                    val adKey = com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager.KEY_ALL_INTERNAL_INTER
                    if (activity != null) {
                        viewModel.interstitialAdHelper.showAd(
                            activity = activity,
                            adKey = adKey,
                            frequencyEveryClicks = 2
                        )
                    }
                }
                is HomeEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is HomeEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Refresh documents when returning to MainActivity (e.g. after granting storage permissions in Settings or after tool actions)
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(HomeEvent.RefreshRequested)
    }

    MaterialTheme(
        colorScheme = HomeLightColors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                onMenuClick = onNavigateToLanguage,
                onSearchClick = onNavigateToSearch
            )
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab row — sits right under the icon row, matching the mock (no big page title).
            DocumentTabs(
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.onEvent(HomeEvent.TabChanged(it)) }
            )

            // "All Files" header row with sort action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Files",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                )
                IconButton(
                    onClick = { viewModel.onEvent(HomeEvent.SortSheetOpened) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Keep the previous list visible during quick refreshes.
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && state.filteredDocuments.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    DocumentList(
                        documents = state.filteredDocuments,
                        isGridView = state.isGridView,
                        onDocumentClick = { viewModel.onEvent(HomeEvent.DocumentClicked(it)) },
                        onFavoriteClick = { viewModel.onEvent(HomeEvent.FavoriteToggled(it)) },
                        onDeleteClick = { documentToDelete = it },
                        onRenameClick = { documentToRename = it },
                        listState = listState
                    )
                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                        )
                    }

                    // Floating double-chevron "back to top" pill, centered above the bottom bar.
                    ScrollToTopButton(
                        visible = showScrollToTop && !state.isGridView,
                        onClick = {
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }

        // Sort Bottom Sheet
        if (state.showSortSheet) {
            SortBottomSheet(
                currentSort = state.sortOrder,
                onSortSelected = { viewModel.onEvent(HomeEvent.SortChanged(it)) },
                onDismiss = { viewModel.onEvent(HomeEvent.SortSheetDismissed) }
            )
        }

        // Delete confirmation dialog
        documentToDelete?.let { doc ->
            ConfirmDeleteDialog(
                fileName = doc.name,
                onDismiss = { documentToDelete = null },
                onConfirm = {
                    viewModel.onEvent(HomeEvent.DocumentDeleted(doc))
                    documentToDelete = null
                }
            )
        }

        // Rename dialog
        documentToRename?.let { doc ->
            RenameDialog(
                initialName = doc.name.substringBeforeLast('.'),
                onDismiss = { documentToRename = null },
                onConfirm = { newName ->
                    viewModel.onEvent(HomeEvent.DocumentRenamed(doc, newName))
                    documentToRename = null
                }
            )
        }
    }
    }
}

/**
 * Top bar matching the "My Documents" mock:
 * Left — bold title "My Documents" + grey subtitle.
 * Right — Search, Crown (premium), Language globe icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPremiumClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT — title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "My Documents",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF111827)
            )
            Text(
                text = "Your personal library",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // RIGHT — Search | Crown | Globe
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Search
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Crown — premium upsell (orange gradient icon from SVG drawable)
            IconButton(onClick = onPremiumClick) {
                Icon(
                    painter = painterResource(id = R.drawable.crownicon),
                    contentDescription = "Premium",
                    tint = Color.Unspecified,   // keeps SVG gradient colours
                    modifier = Modifier.size(22.dp)
                )
            }

            // Language / Globe
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}


