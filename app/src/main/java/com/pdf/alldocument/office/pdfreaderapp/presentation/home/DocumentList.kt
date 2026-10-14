package com.pdf.alldocument.office.pdfreaderapp.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.pdf.alldocument.office.pdfreaderapp.core.ads.AdMobManager
import com.pdf.alldocument.office.pdfreaderapp.core.ads.FeedNativeAdView
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.*
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder


/**
 * Per-type accent for a selected filter tab — PDF turns red, Word blue, PPT
 * orange, Text purple, matching each type's own brand color; "All" (and
 * anything unmapped) falls back to the app's purple brand accent.
 */
private fun tabAccentColor(tab: DocumentType): Color = when (tab.title.lowercase()) {
    "pdf" -> ColorPdf
    "word" -> ColorWord
    "excel" -> ColorExcel
    "ppt" -> ColorPpt
    "text", "txt" -> ColorTxt
    else -> BrandPurple
}

/**
 * Text-only pill tabs (All / PDF / Word / PPT / Text …) — selected tab gets the
 * brand-gradient fill + white bold label, unselected tabs sit on a flat
 * surfaceVariant pill so every tab always reads as a "chip", matching the mock.
 */
@Composable
fun DocumentTabs(
    selectedTab: DocumentType,
    onTabSelected: (DocumentType) -> Unit
) {
    val tabs = DocumentType.values()

    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 12.dp,
        divider = {},
        indicator = {} // no underline indicator — the pill fill IS the indicator
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = if (isSelected) {
                                val accent = tabAccentColor(tab)
                                if (tab.title.equals("all", ignoreCase = true)) {
                                    Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleEnd))
                                } else {
                                    SolidColor(accent)
                                }
                            } else {
                                SolidColor(Color(0xFFF3F3F7))
                            }
                        )
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) {
                            Color.White
                        } else {
                            Color(0xFF7E7E91)
                        }
                    )
                }
            }
        }
    }
}


/**
 * How many real items sit between two ad-banner slots in the feed.
 */
private const val AD_SLOT_INTERVAL = 4

@Composable
fun DocumentList(
    documents: List<DocumentItem>,
    isGridView: Boolean,
    onDocumentClick: (DocumentItem) -> Unit,
    onFavoriteClick: (DocumentItem) -> Unit,
    onDeleteClick: (DocumentItem) -> Unit,
    onRenameClick: (DocumentItem) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    showAds: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasStoragePermission = com.pdf.alldocument.office.pdfreaderapp.MainActivity.hasStoragePermission(context)

    val feedAdsMap = remember { mutableStateMapOf<Int, NativeAd>() }
    DisposableEffect(Unit) {
        onDispose {
            feedAdsMap.values.forEach { it.destroy() }
            feedAdsMap.clear()
            AdMobManager.preloadNativeAd(context)
        }
    }

    if (documents.isEmpty()) {
        if (!hasStoragePermission) {
            EmptyStateComposable(
                icon = Icons.Default.FolderOpen,
                title = "Storage Permission Required",
                subtitle = "Please allow access to your files to view and manage documents.",
                actionLabel = "Grant Permission",
                onActionClick = {
                    (context as? com.pdf.alldocument.office.pdfreaderapp.MainActivity)?.requestAppPermissions()
                },
                modifier = modifier
            )
        } else {
            EmptyStateComposable(
                icon = Icons.Default.FindInPage,
                title = "No documents found",
                subtitle = "Make sure you have documents on your device",
                modifier = modifier
            )
        }
        return
    }

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(documents, key = { it.path }) { doc ->
                DocumentGridCard(
                    document = doc,
                    onClick = { onDocumentClick(doc) },
                    onFavoriteClick = { onFavoriteClick(doc) },
                    onDeleteClick = { onDeleteClick(doc) },
                    onRenameClick = { onRenameClick(doc) }
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            itemsIndexed(
                items = documents,
                key = { _, doc -> doc.path }
            ) { index, doc ->
                DocumentListItem(
                    document = doc,
                    onClick = { onDocumentClick(doc) },
                    onFavoriteClick = { onFavoriteClick(doc) },
                    onDeleteClick = { onDeleteClick(doc) },
                    onRenameClick = { onRenameClick(doc) }
                )
                // Sponsored slot every few real rows — mirrors the in-feed "AD" card in the mock.
                val isSlot = showAds && index > 0 && (index + 1) % AD_SLOT_INTERVAL == 0
                if (isSlot) {
                    FeedNativeAdItem(
                        slotIndex = index / AD_SLOT_INTERVAL,
                        adsMap = feedAdsMap
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentListItem(
    document: DocumentItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val baseName = document.name.substringBeforeLast('.', document.name)
    val extension = document.name.substringAfterLast('.', "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DocTypeIcon(extension = document.extension)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            // "Business Plan." in the default text color, extension in its brand color/bold.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$baseName.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                if (extension.isNotEmpty()) {
                    Text(
                        text = extension,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = extensionColor(extension)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = document.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = document.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFFB0B0C0)
                )
            }
            if (showMenu) {
                DropdownMenu(expanded = true, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(if (document.isFavorite) "Remove from Favorites" else "Add to Favorites")
                        },
                        leadingIcon = {
                            Icon(
                                if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (document.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            onFavoriteClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun DocumentGridCard(
    document: DocumentItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                DocTypeIcon(extension = document.extension, size = 44.dp)
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    if (showMenu) {
                        DropdownMenu(expanded = true, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (document.isFavorite) "Unfavorite" else "Favorite") },
                                leadingIcon = {
                                    Icon(
                                        if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (document.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { showMenu = false; onFavoriteClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { showMenu = false; onRenameClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { showMenu = false; onDeleteClick() }
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = document.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = document.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Doc-type badge using the imported SVG drawables (pdficon, docicon, xlsicon,
 * ppticon, txticon) — exactly matching the mockup icon set.
 */
@Composable
fun DocTypeIcon(
    extension: String,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    val drawableRes = when (extension.lowercase()) {
        "pdf"                         -> com.pdf.alldocument.office.pdfreaderapp.R.drawable.pdficon
        "doc", "docx", "dot", "dotx" -> com.pdf.alldocument.office.pdfreaderapp.R.drawable.docicon
        "xls", "xlsx", "csv"         -> com.pdf.alldocument.office.pdfreaderapp.R.drawable.xlsicon
        "ppt", "pptx", "pps", "ppsx" -> com.pdf.alldocument.office.pdfreaderapp.R.drawable.ppticon
        "txt", "log"                  -> com.pdf.alldocument.office.pdfreaderapp.R.drawable.txticon
        else                          -> com.pdf.alldocument.office.pdfreaderapp.R.drawable.docicon
    }

    Image(
        painter = androidx.compose.ui.res.painterResource(id = drawableRes),
        contentDescription = extension.uppercase(),
        modifier = Modifier.size(size)
    )
}

/** Maps a file extension to the same brand color used on its [DocTypeIcon]. */
@Composable
private fun extensionColor(extension: String): Color = when (extension.lowercase()) {
    "pdf" -> ColorPdf
    "doc", "docx", "dot", "dotx" -> ColorWord
    "xls", "xlsx", "csv" -> ColorExcel
    "ppt", "pptx", "pps", "ppsx" -> ColorPpt
    "txt", "log" -> ColorTxt
    else -> MaterialTheme.colorScheme.primary
}

/**
 * In-feed sponsored Native Ad slot — loads real AdMob Native Ad and displays it
 * with the exact same lavender card design (app icon, AD disclosure, headline,
 * body description, and purple gradient CTA button).
 */
@Composable
fun FeedNativeAdItem(
    slotIndex: Int,
    adsMap: MutableMap<Int, NativeAd>,
    modifier: Modifier = Modifier
) {
    if (!AdMobManager.isAdEnabled(AdMobManager.KEY_FILE_READER_NATIVE)) return

    val context = androidx.compose.ui.platform.LocalContext.current
    var loadAttempted by remember(slotIndex) { mutableStateOf(false) }

    LaunchedEffect(slotIndex) {
        if (!adsMap.containsKey(slotIndex) && !loadAttempted) {
            loadAttempted = true
            val preloaded = AdMobManager.consumePreloadedNativeAd()
            if (preloaded != null) {
                adsMap[slotIndex] = preloaded
                AdMobManager.preloadNativeAd(context)
            } else {
                val targetId = AdMobManager.getNativeId(context)
                val request = NativeAdRequest.Builder(
                    targetId,
                    listOf(NativeAd.NativeAdType.NATIVE)
                ).build()
                NativeAdLoader.load(
                    request,
                    object : NativeAdLoaderCallback {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            adsMap[slotIndex] = nativeAd
                        }

                        override fun onAdFailedToLoad(adError: com.google.android.libraries.ads.mobile.sdk.common.LoadAdError) {
                            android.util.Log.w("FeedNativeAd", "Ad failed to load: ${adError.message}")
                        }
                    }
                )
            }
        }
    }

    val nativeAd = adsMap[slotIndex]
    if (nativeAd != null) {
        FeedNativeAdView(
            nativeAd = nativeAd,
            modifier = modifier
        )
    } else {
        AdBannerCard(modifier = modifier)
    }
}

/**
 * In-feed sponsored card — soft lavender surface, an "AD" tag, a 2x2 app-style
 * glyph, a two-line pitch, and a gradient "Sign up" pill. Purely decorative;
 * wire onSignUpClick / onDismiss to real ad-SDK callbacks when available.
 */
@Composable
fun AdBannerCard(
    onSignUpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Fixed light palette for the whole card — ad units render the same in
    // light and dark app themes, so every color here is hardcoded instead of
    // pulled from MaterialTheme (that's what was making the text invisible
    // in dark mode: it was inheriting the theme's light-on-dark content color).
    val adCardBg = Color(0xFFEFF1FE)
    val adHeadlineColor = Color(0xFF1B1B24)
    val adBodyColor = Color(0xFF6B6B7A)
    val adTagColor = Color(0xFF5B4FE0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = adCardBg, contentColor = adHeadlineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Small "AD" disclosure tag, top-right of the card.
            Text(
                text = "AD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = adBodyColor,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                FourSquareGlyph()
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = adTagColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enhance the Passenger Experience",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = adHeadlineColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Discover the cabin interior products of the future.",
                        style = MaterialTheme.typography.bodySmall,
                        color = adBodyColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleEnd)))
                        .clickable(onClick = onSignUpClick)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sign up",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** Four-color 2x2 square glyph (generic "app" mark) used on the ad card. */
@Composable
private fun FourSquareGlyph(size: androidx.compose.ui.unit.Dp = 44.dp) {
    val colors = listOf(Color(0xFFEF5350), Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFFFC107))
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(size / 3).clip(RoundedCornerShape(3.dp)).background(colors[0]))
                Box(Modifier.size(size / 3).clip(RoundedCornerShape(3.dp)).background(colors[1]))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(size / 3).clip(RoundedCornerShape(3.dp)).background(colors[2]))
                Box(Modifier.size(size / 3).clip(RoundedCornerShape(3.dp)).background(colors[3]))
            }
        }
    }
}

/**
 * Floating "scroll to top" pill (double chevron) shown once the list has been
 * scrolled down a bit — tap to smooth-scroll back to the first item.
 */
@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(),
        exit = scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEBEBF2), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardDoubleArrowUp,
                contentDescription = "Scroll to top",
                tint = BrandPurple,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            SortOrder.values().forEach { order ->
                val isCurrent = order == currentSort
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .selectable(
                            selected = isCurrent,
                            onClick = { onSortSelected(order) },
                            role = Role.RadioButton
                        ),
                    headlineContent = {
                        Text(
                            text = order.label,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingContent = {
                        if (isCurrent) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}
