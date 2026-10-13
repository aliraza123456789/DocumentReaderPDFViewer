package com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import kotlinx.coroutines.launch

/**
 * Stateful entry point: binds [FileManagerViewModel] to the stateless screen and
 * translates one-shot effects into navigation / snackbars.
 */
@Composable
fun FileManagerRoute(
    onNavigateToPdfViewer: (DocumentItem) -> Unit,
    onNavigateToOfficeViewer: (DocumentItem) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenFavourites: () -> Unit,
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    // Preload Interstitial ad
    LaunchedEffect(Unit) {
        viewModel.interstitialAdHelper.loadAd(context)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FileManagerEffect.NavigateToPdfViewer -> {
                    val adKey = com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager.KEY_PDF_INTERNAL_INTER
                    if (activity != null) {
                        viewModel.interstitialAdHelper.showAd(
                            activity = activity,
                            adKey = adKey,
                            frequencyEveryClicks = 2,
                            onAdDismissed = { onNavigateToPdfViewer(effect.document) }
                        )
                    } else {
                        onNavigateToPdfViewer(effect.document)
                    }
                }
                is FileManagerEffect.NavigateToOfficeViewer -> {
                    val adKey = com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager.KEY_ALL_INTERNAL_INTER
                    if (activity != null) {
                        viewModel.interstitialAdHelper.showAd(
                            activity = activity,
                            adKey = adKey,
                            frequencyEveryClicks = 2,
                            onAdDismissed = { onNavigateToOfficeViewer(effect.document) }
                        )
                    } else {
                        onNavigateToOfficeViewer(effect.document)
                    }
                }
                is FileManagerEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is FileManagerEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    FileManagerScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onMenuClick = viewModel::onMenuClick,
        onMenuFavorite = viewModel::onMenuFavorite,
        onMenuRename = viewModel::onMenuRename,
        onMenuDelete = viewModel::onMenuDelete,
        onRenameDialogDismiss = viewModel::onRenameDialogDismiss,
        onDeleteDialogDismiss = viewModel::onDeleteDialogDismiss,
        onOpenMore = onOpenMore,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToTools = onNavigateToTools,
        onNavigateToPremium = onNavigateToPremium,
        onOpenFavourites = onOpenFavourites,
        snackbarHostState = snackbarHostState
    )
}

/**
 * Stateless file manager.
 *
 * Layout top-to-bottom: top bar -> filter pills -> file list (with inline ads) ->
 * centre FAB -> promo card -> bottom nav.
 */
@Composable
fun FileManagerScreen(
    state: FileManagerState,
    onEvent: (FileManagerEvent) -> Unit,
    onMenuClick: (DocumentItem) -> Unit,
    onMenuFavorite: () -> Unit,
    onMenuRename: () -> Unit,
    onMenuDelete: () -> Unit,
    onRenameDialogDismiss: () -> Unit,
    onDeleteDialogDismiss: () -> Unit,
    onOpenMore: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onOpenFavourites: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val entries = state.entries

    // Favourites and Tools are real routes, so the bar shows no selected item there.
    val selectedTab = when (state.source) {
        DocSource.BROWSER -> NavTab.HOME
        DocSource.RECENT -> NavTab.RECENT
    }

    val onTabSelected: (NavTab) -> Unit = { tab ->
        when (tab) {
            NavTab.HOME -> onEvent(FileManagerEvent.SourceChanged(DocSource.BROWSER))
            NavTab.RECENT -> onEvent(FileManagerEvent.SourceChanged(DocSource.RECENT))
            NavTab.FAVOURITE -> onOpenFavourites()
            NavTab.TOOLS -> onNavigateToTools()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = ScreenWhite,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                FileManagerTopBar(
                    query = state.query,
                    scopeLabel = state.filter.label,
                    onQueryChange = { onEvent(FileManagerEvent.QueryChanged(it)) },
                    // The pill doubles as a "clear filter" affordance.
                    onScopeClick = {
                        if (state.filter != FileFilter.ALL) {
                            onEvent(FileManagerEvent.FilterChanged(FileFilter.ALL))
                        }
                    },
                    onMenuClick = onOpenMore,
                    onPremiumClick = onNavigateToPremium,
                    onScanClick = onNavigateToSearch,
                    onSortClick = { onEvent(FileManagerEvent.SortSheetOpened) },
                    onViewToggleClick = { onEvent(FileManagerEvent.GridViewToggled) }
                )
                FilterTabRow(
                    selected = state.filter,
                    onSelect = { onEvent(FileManagerEvent.FilterChanged(it)) }
                )
                Spacer(Modifier.height(8.dp))
            }
        },
        bottomBar = {
            Column(Modifier.background(ScreenWhite)) {
                // Promo sits ABOVE the nav bar, per the spec.
                PromoCard(onClick = onNavigateToPremium)
                BottomNavBar(selected = selectedTab, onSelect = onTabSelected)
            }
        },
        floatingActionButton = {
            ScrollToTopFab(
                onClick = {
                    scope.launch {
                        if (state.isGridView) {
                            gridState.animateScrollToItem(0)
                        } else {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(ScreenWhite)
        ) {
            val listBottomPadding = 96.dp

            if (state.isGridView) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = listBottomPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        count = entries.size,
                        key = { index -> entryKey(entries[index], index) },
                        // Ads must span the full row width; documents take one cell.
                        span = { index ->
                            if (entries[index] is ListEntry.Ad) {
                                GridItemSpan(maxLineSpan)
                            } else {
                                GridItemSpan(1)
                            }
                        }
                    ) { index ->
                        when (val entry = entries[index]) {
                            is ListEntry.Document -> {
                                // Memoised: the mapping allocates 5 strings + a
                                // FileItem, and this lambda re-runs on every
                                // list recomposition.
                                val item = remember(entry.document) {
                                    entry.document.toFileItem()
                                }
                                DocumentGridTile(
                                    item = item,
                                    onClick = { onEvent(FileManagerEvent.DocumentClicked(entry.document)) },
                                    onMenuClick = { onMenuClick(entry.document) }
                                )
                            }

                            is ListEntry.Ad ->
                                AdBanner(onSignUpClick = onNavigateToPremium)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = listBottomPadding
                    ),
                    // Spec: 12dp horizontal margin, 6dp vertical between cards.
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        count = entries.size,
                        key = { index -> entryKey(entries[index], index) }
                    ) { index ->
                        when (val entry = entries[index]) {
                            is ListEntry.Document -> {
                                val item = remember(entry.document) {
                                    entry.document.toFileItem()
                                }
                                FileItemCard(
                                    item = item,
                                    onClick = { onEvent(FileManagerEvent.DocumentClicked(entry.document)) },
                                    onMenuClick = { onMenuClick(entry.document) }
                                )
                            }

                            is ListEntry.Ad -> AdBanner(onSignUpClick = onNavigateToPremium)
                        }
                    }
                }
            }

            when {
                state.isLoading && entries.isEmpty() -> LoadingState()
                entries.isEmpty() -> EmptyFilterState(
                    filter = state.filter,
                    source = state.source,
                    query = state.query
                )
            }
        }
    }

    /* ---- overlays ---- */

    if (state.showSortSheet) {
        SortBottomSheet(
            current = state.sortOrder,
            onSortSelected = { onEvent(FileManagerEvent.SortChanged(it)) },
            onDismiss = { onEvent(FileManagerEvent.SortSheetDismissed) }
        )
    }

    state.menuDocument?.let { document ->
        DocumentMenuSheet(
            document = document,
            onToggleFavorite = onMenuFavorite,
            onRename = onMenuRename,
            onDelete = onMenuDelete,
            onDismiss = { onEvent(FileManagerEvent.MenuDismissed) }
        )
    }

    state.pendingRename?.let { document ->
        FileManagerRenameDialog(
            document = document,
            onDismiss = onRenameDialogDismiss,
            onConfirm = { newName ->
                onEvent(FileManagerEvent.RenameRequested(document, newName))
            }
        )
    }

    state.pendingDelete?.let { document ->
        FileManagerDeleteDialog(
            document = document,
            onDismiss = onDeleteDialogDismiss,
            onConfirm = { onEvent(FileManagerEvent.DeleteRequested(document)) }
        )
    }
}

/** Stable list key: documents by path, ads by their position. */
private fun entryKey(entry: ListEntry, index: Int): String = when (entry) {
    is ListEntry.Document -> "doc_${entry.document.path}"
    is ListEntry.Ad -> "ad_$index"
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PurpleStart, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun EmptyFilterState(
    filter: FileFilter,
    source: DocSource,
    query: String,
    modifier: Modifier = Modifier
) {
    val title = when {
        query.isNotBlank() -> "No results"
        source == DocSource.RECENT -> "No recent files"
        else -> "No ${filter.label} files"
    }
    val subtitle = when {
        query.isNotBlank() -> "Nothing matches \"$query\"."
        source == DocSource.RECENT -> "Files you open will show up here."
        else -> "Try a different filter or add a document."
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/* ------------------------------------------------------------------ */
/* Previews                                                            */
/* ------------------------------------------------------------------ */

/** Fake documents so the previews render without Hilt or a real filesystem. */
private fun previewDocuments(): List<DocumentItem> = listOf(
    "Business Plan.pdf", "Marketing Strategy.docx", "Meeting Notes.txt",
    "Quarterly Report.pptx", "Annual Budget.pdf", "Expense Summary.xlsx"
).mapIndexed { index, name ->
    val extension = name.substringAfterLast('.', "")
    DocumentItem(
        path = "/storage/emulated/0/Documents/$name",
        name = name,
        size = 257_100L * (index + 1),
        formattedSize = sampleFiles[index].size,
        dateModified = 0L,
        formattedDate = sampleFiles[index].date,
        extension = extension,
        type = DocumentType.fromExtension(extension)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 400, heightDp = 880)
@Composable
private fun FileManagerScreenPreview() {
    FileManagerScreen(
        state = FileManagerState(allDocuments = previewDocuments()),
        onEvent = {},
        onMenuClick = {},
        onMenuFavorite = {},
        onMenuRename = {},
        onMenuDelete = {},
        onRenameDialogDismiss = {},
        onDeleteDialogDismiss = {},
        onOpenMore = {},
        onNavigateToSearch = {},
        onNavigateToTools = {},
        onNavigateToPremium = {},
        onOpenFavourites = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 400, heightDp = 880)
@Composable
private fun FileManagerGridPreview() {
    FileManagerScreen(
        state = FileManagerState(allDocuments = previewDocuments(), isGridView = true),
        onEvent = {},
        onMenuClick = {},
        onMenuFavorite = {},
        onMenuRename = {},
        onMenuDelete = {},
        onRenameDialogDismiss = {},
        onDeleteDialogDismiss = {},
        onOpenMore = {},
        onNavigateToSearch = {},
        onNavigateToTools = {},
        onNavigateToPremium = {},
        onOpenFavourites = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}
