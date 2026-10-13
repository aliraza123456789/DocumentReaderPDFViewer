package com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ */
/* 1. Top app bar                                                      */
/* ------------------------------------------------------------------ */

@Composable
fun FmHamburgerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HamburgerBg)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open navigation menu" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = null,
            tint = HamburgerIcon,
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * Rounded scope selector + real text field, matching the spec's "All in purple
 * bold + Search All hint" pill.
 *
 * The scope chip is a separate tap target from the input so opening the filter
 * row never steals focus from typing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FmSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    scopeLabel: String = "All",
    hint: String = "Search All",
    onScopeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .widthIn(min = 120.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(SearchBarBg)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onScopeClick)
                .semantics(mergeDescendants = true) { contentDescription = "Filter by $scopeLabel" }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = scopeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PurpleStart,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PurpleStart,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .width(1.dp)
                .height(15.dp)
                .background(PurpleStart.copy(alpha = 0.22f))
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(PurpleStart),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .semantics { contentDescription = "Search documents" },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Top-bar action button. Matches [FmHamburgerButton]: a 40dp light-grey rounded
 * square, so the whole bar reads as one row of identical chips.
 */
@Composable
private fun FmTopBarIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = HamburgerIcon
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HamburgerBg)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FileManagerTopBar(
    query: String,
    scopeLabel: String,
    onQueryChange: (String) -> Unit,
    onScopeClick: () -> Unit,
    onMenuClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onScanClick: () -> Unit,
    onSortClick: () -> Unit,
    onViewToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FmHamburgerButton(onClick = onMenuClick)
        Spacer(Modifier.width(10.dp))
        FmSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            scopeLabel = scopeLabel,
            onScopeClick = onScopeClick,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        FmTopBarIcon(FmIcons.Crown, "Premium", onPremiumClick, tint = CrownOrange)
        FmTopBarIcon(Icons.Default.QrCodeScanner, "Scan document", onScanClick)
        FmTopBarIcon(Icons.AutoMirrored.Filled.Sort, "Sort files", onSortClick)
        FmTopBarIcon(Icons.Default.GridView, "Toggle grid view", onViewToggleClick)
    }
}

/* ------------------------------------------------------------------ */
/* 2. Filter tabs                                                      */
/* ------------------------------------------------------------------ */

@Composable
fun FilterTab(
    filter: FileFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = filter.label
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd))
                } else {
                    Brush.horizontalGradient(listOf(SearchBarBg, SearchBarBg))
                }
            )
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label filter"
                this.selected = selected
            }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = FmIcons.DocGlyph,
                contentDescription = null,
                tint = when {
                    selected -> Color.White.copy(alpha = 0.92f)
                    // "All" is the brand rather than a file type, so it takes the purple.
                    filter == FileFilter.ALL -> PurpleStart
                    else -> filter.iconType.extColor
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FilterTabRow(
    selected: FileFilter,
    onSelect: (FileFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FileFilter.entries.size) { index ->
            val filter = FileFilter.entries[index]
            FilterTab(
                filter = filter,
                selected = filter == selected,
                onClick = { onSelect(filter) }
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 3. File icon + file card                                            */
/* ------------------------------------------------------------------ */

/** White folded-page silhouette drawn behind the [FileType] label. */
@Composable
private fun FilePageShape(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val left = size.width * 0.28f
        val top = size.height * 0.11f
        val right = size.width * 0.80f
        val bottom = size.height * 0.89f
        val foldX = size.width * 0.64f
        val foldY = size.height * 0.30f

        drawPath(
            path = Path().apply {
                moveTo(left, top)
                lineTo(foldX, top)
                lineTo(right, foldY)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            },
            color = Color.White.copy(alpha = 0.93f)
        )
        drawPath(
            path = Path().apply {
                moveTo(foldX, top)
                lineTo(foldX, foldY)
                lineTo(right, foldY)
                close()
            },
            color = Color.White
        )
    }
}

@Composable
fun FileTypeIcon(
    type: FileType,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    // Inner label + glyph scale with the tile so the tile can be reused at
    // 56dp in the list, 56dp in the grid, and any size in a preview.
    val scale = size.value / 46f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.3f))
            .background(Brush.linearGradient(listOf(type.gradient.first, type.gradient.second)))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        FilePageShape(Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = type.label,
                fontSize = (12f * scale).sp,
                lineHeight = (13f * scale).sp,
                fontWeight = FontWeight.Black,
                color = type.gradient.second
            )
            if (type.showScissors) {
                Spacer(Modifier.height(1.dp))
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = type.gradient.second,
                    modifier = Modifier.size(12.dp * scale)
                )
            }
        }
    }
}

/** `"Business Plan."` in black + `".pdf"` in the type colour, as one line. */
private fun fileTitle(item: FileItem): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
        append(item.name)
    }
    withStyle(SpanStyle(color = item.iconType.extColor, fontWeight = FontWeight.Bold)) {
        append(item.extWithDot)
    }
}

@Composable
fun FileItemCard(
    item: FileItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 74.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(type = item.iconType)
            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileTitle(item),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetaBit(Icons.Default.CalendarToday, item.date)
                    Spacer(Modifier.width(7.dp))
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(TextSecondary.copy(alpha = 0.55f))
                    )
                    Spacer(Modifier.width(7.dp))
                    MetaBit(Icons.AutoMirrored.Filled.InsertDriveFile, item.size)
                }
            }

            IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options for ${item.name}",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetaBit(icon: ImageVector, label: String) {    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

/* ------------------------------------------------------------------ */
/* 4. Ad banner                                                        */
/* ------------------------------------------------------------------ */

/** 2x2 grid of brand-coloured squares used as the ad logo. */
@Composable
private fun FourSquareLogo(size: Dp = 26.dp) {
    val gap = size * 0.16f
    val cell = (size - gap) / 2
    val colors = listOf(
        Color(0xFFFF5E7A), Color(0xFF4A9BFF), Color(0xFF43A047), Color(0xFFFFA726)
    )
    Box(Modifier.size(size)) {
        colors.forEachIndexed { i, c ->
            Box(
                modifier = Modifier
                    .offset(x = (cell + gap) * (i % 2), y = (cell + gap) * (i / 2))
                    .size(cell)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c)
            )
        }
    }
}

@Composable
fun AdBanner(
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AdCardStart),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(AdCardStart, AdCardEnd)))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, AdLabelGrey.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "AD",
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdLabelGrey
                    )
                }
                Spacer(Modifier.weight(1f))
                FourSquareLogo()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Get Unlimited Access",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Remove ads and unlock every PDF tool",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd)))
                    .clickable(onClick = onSignUpClick)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sign up",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 5. Centre FAB                                                       */
/* ------------------------------------------------------------------ */

@Composable
fun ScrollToTopFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(listOf(PurpleStart, PurpleEnd)))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Scroll to top" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = FmIcons.ChevronUpDouble,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/* ------------------------------------------------------------------ */
/* 6. Bottom navigation                                                */
/* ------------------------------------------------------------------ */

@Composable
fun BottomNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = ScreenWhite,
        tonalElevation = 0.dp
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(listOf(PurpleStart, PurpleEnd))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            // Label below already names the tab; null avoids "Home, Home".
                            contentDescription = null,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PurpleStart else TextSecondary,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .width(16.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd))
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    }
                                )
                        )
                    }
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = TextSecondary,
                    selectedTextColor = PurpleStart,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 7. Bottom promo card                                                */
/* ------------------------------------------------------------------ */

@Composable
fun PromoCard(
    title: String = "Buy one month, get one free",
    subtitle: String = "Limited time offer",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PromoCardBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(PromoAccentPink, PromoAccentPurple))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PromoAccentPurple,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 3b. Grid tile (used when the grid-view toggle is on)                 */
/* ------------------------------------------------------------------ */

/**
 * Compact square tile for the grid layout. Same visual language as
 * [FileItemCard] — gradient icon, name with a coloured extension, meta line.
 */
@Composable
fun DocumentGridTile(
    item: FileItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FileTypeIcon(type = item.iconType, size = 56.dp)
                Box(
                    modifier = Modifier.align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options for ${item.name}",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = fileTitle(item),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.size,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Previews                                                            */
/* ------------------------------------------------------------------ */

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun FilterTabRowPreview() {
    Column {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(16.dp)
                .semantics { heading() }
        )
        FilterTabRow(selected = FileFilter.ALL, onSelect = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7, widthDp = 400)
@Composable
private fun CardsPreview() {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sampleFiles.take(3).forEach { file ->
            FileItemCard(item = file, onClick = {}, onMenuClick = {})
        }
        AdBanner(onSignUpClick = {})
        PromoCard(onClick = {})
    }
}
