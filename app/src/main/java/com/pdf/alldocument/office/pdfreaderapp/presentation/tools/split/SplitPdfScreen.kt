package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.split

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdCard
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.LottieLoadingIndicator
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick

// Colors matching the design mockup images
private val BrandGreen = Color(0xFF22C55E)
private val BrandGreenDark = Color(0xFF16A34A)
private val DisabledGreen = Color(0xFFA7F3D0)
private val DarkTextColor = Color(0xFF0F172A)
private val MutedTextColor = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)
private val CardBorderSelected = Color(0xFFEF4444)
private val ProgressRed = Color(0xFFEF4444)
private val LightBg = Color(0xFFF8FAFC)
private val AdBadgeYellow = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    onNavigateBack: () -> Unit = {},
    onBack: () -> Unit = onNavigateBack,
    onNavigateToViewer: (String, String) -> Unit = { _, _ -> },
    viewModel: SplitPdfViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val temp = FileUtils.copyUriToTempFile(context, it, "split_${System.currentTimeMillis()}.pdf")
            if (temp != null) {
                viewModel.selectCustomFile(temp.absolutePath, temp.name)
            }
        }
    }

    // Handle back button according to flow step
    BackHandler {
        val handled = viewModel.handleBackPress()
        if (!handled) {
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.step) {
                SplitFlowStep.SELECT_FILE -> {
                    // SCREEN 1: Select a File (Image 3)
                    SelectFileScreen(
                        state = state,
                        onBack = onBack,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onToggleSearch = { viewModel.toggleSearch(it) },
                        onFileSelected = { doc -> viewModel.selectPdf(doc) },
                        onOpenStorage = { filePickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                }

                SplitFlowStep.SELECT_PAGES -> {
                    if (state.isLoadingPages) {
                        LottieLoadingIndicator(
                            rawRes = "ad_loading.json",
                            message = "Loading PDF pages...",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // SCREEN 2: Select Pages Grid (Images 4 & 5)
                        SelectPagesScreen(
                            pages = state.pdfPages,
                            isEditing = state.editingPartId != null,
                            onBack = { viewModel.handleBackPress() },
                            onToggleSelection = { index -> viewModel.togglePageSelection(index) },
                            onToggleSelectAll = { viewModel.toggleSelectAll() },
                            onContinue = { viewModel.confirmPageSelection() }
                        )
                    }
                }

                SplitFlowStep.SPLIT_LIST -> {
                    // SCREEN 3: Split PDF List Screen (Image 1)
                    SplitPartsListScreen(
                        state = state,
                        onBack = { viewModel.handleBackPress() },
                        onAddMore = { viewModel.startAddMorePages() },
                        onEditPart = { partId -> viewModel.startEditPart(partId) },
                        onDeletePart = { partId -> viewModel.removePart(partId) },
                        onSplitClick = { viewModel.executeSplit() }
                    )
                }

                SplitFlowStep.SPLIT_SUCCESS -> {
                    // SCREEN 5: Split Success View (Generated missing screen)
                    SplitSuccessScreen(
                        results = state.splitResults,
                        onBack = {
                            viewModel.resetToSelectFile()
                            onBack()
                        },
                        onDone = {
                            viewModel.resetToSelectFile()
                            onBack()
                        },
                        onOpen = { path, title ->
                            onNavigateToViewer(path, title)
                        },
                        onShare = { path ->
                            ShareUtils.shareFile(context, path)
                        }
                    )
                }
            }

            // SCREEN 4: Splitting Progress Dialog / Bottom Sheet (Image 2)
            if (state.isSplitting) {
                SplittingProgressDialog(
                    progress = state.splitProgress
                )
            }

            // Error snackbar / toast
            if (state.errorMessage != null) {
                LaunchedEffect(state.errorMessage) {
                    Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

/* =========================================================================================
 * 1. SCREEN 1: SELECT A FILE (Image 3: media_1791831312914_74e27b63.png)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectFileScreen(
    state: SplitPdfUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onFileSelected: (DocumentItem) -> Unit,
    onOpenStorage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                if (state.isSearching) {
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkTextColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.searchQuery.isEmpty()) {
                                    Text("Search PDF...", color = MutedTextColor, fontSize = 18.sp)
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    Text(
                        text = "Select a file",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkTextColor
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (state.isSearching) onToggleSearch(false)
                    else onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkTextColor
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    if (state.isSearching) {
                        onSearchQueryChange("")
                        onToggleSearch(false)
                    } else {
                        onToggleSearch(true)
                    }
                }) {
                    Icon(
                        imageVector = if (state.isSearching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (state.isSearching) "Clear" else "Search",
                        tint = DarkTextColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Storage picker row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .tactileClick(onOpenStorage),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = BrandGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Browse from device",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = DarkTextColor
                    )
                    Text(
                        text = "Pick any PDF from your files",
                        fontSize = 12.sp,
                        color = MutedTextColor
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MutedTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Document list
        if (state.filteredPdfs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PdfFoldedIcon(size = 64.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (state.searchQuery.isBlank()) "No PDF documents found" else "No matching PDFs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Browse from device' to open a PDF file.",
                        fontSize = 13.sp,
                        color = MutedTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(state.filteredPdfs, key = { it.path }) { doc ->
                    PdfFileRow(
                        doc = doc,
                        onClick = { onFileSelected(doc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfFileRow(
    doc: DocumentItem,
    onClick: () -> Unit
) {
    val baseName = doc.name.substringBeforeLast('.', doc.name)
    val extension = doc.name.substringAfterLast('.', "pdf")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PdfFoldedIcon(size = 46.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$baseName. ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = extension,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${doc.formattedDate} . ${doc.formattedSize}",
                fontSize = 13.sp,
                color = MutedTextColor
            )
        }
    }
}

/* =========================================================================================
 * 2. SCREEN 2: SELECT PAGES GRID (Images 4 & 5: media_1791831312950 & media_1791831313004)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPagesScreen(
    pages: List<PdfPageItem>,
    isEditing: Boolean,
    onBack: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onToggleSelectAll: () -> Unit,
    onContinue: () -> Unit
) {
    val selectedCount = pages.count { it.isSelected }
    val allSelected = pages.isNotEmpty() && pages.all { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar: "0 Selected" or "4 Selected" + "Select All"
        TopAppBar(
            title = {
                Text(
                    text = "$selectedCount Selected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkTextColor
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkTextColor
                    )
                }
            },
            actions = {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onToggleSelectAll)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select All",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Select All icon matching design (square outline with inner box/dash)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (allSelected) BrandGreen else DarkTextColor,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (allSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(BrandGreen, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // 2-Column Grid of PDF Pages
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(pages, key = { it.pageIndex }) { pageItem ->
                PdfPageCard(
                    pageItem = pageItem,
                    onClick = { onToggleSelection(pageItem.pageIndex) }
                )
            }
        }

        // Bottom Bar: "Continue" button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onContinue,
                    enabled = selectedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White,
                        disabledContainerColor = DisabledGreen,
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (isEditing) "Update Selection" else "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPageCard(
    pageItem: PdfPageItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f) // Standard page aspect ratio
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (pageItem.isSelected) 2.dp else 1.dp,
            color = if (pageItem.isSelected) CardBorderSelected else BorderGray
        ),
        shadowElevation = if (pageItem.isSelected) 2.dp else 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Rendered Page Thumbnail
            Image(
                bitmap = pageItem.bitmap.asImageBitmap(),
                contentDescription = "Page ${pageItem.pageNumber}",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Fit
            )

            // Bottom-Left Page Number Badge (Matching mockup: semi-transparent grey badge)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 10.dp)
                    .size(width = 34.dp, height = 30.dp)
                    .background(Color(0x994B5563), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${pageItem.pageNumber}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Top-Right Selection Circle (Matching mockup: Green ring or filled checkmark)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pageItem.isSelected) {
                    // Filled green circle with white checkmark
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Green circular stroke outline
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(BorderStroke(2.dp, BrandGreen), CircleShape)
                    )
                }
            }
        }
    }
}

/* =========================================================================================
 * 3. SCREEN 3: SPLIT PDF LIST SCREEN (Image 1: media_1791831312903_55245945.png)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitPartsListScreen(
    state: SplitPdfUiState,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onEditPart: (String) -> Unit,
    onDeletePart: (String) -> Unit,
    onSplitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar: "Split PDF" + "+ Add"
        TopAppBar(
            title = {
                Text(
                    text = "Split PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkTextColor
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkTextColor
                    )
                }
            },
            actions = {
                TextButton(onClick = onAddMore) {
                    Text(
                        text = "+ Add",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = BrandGreen
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // List of Split Part Items
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.splitParts, key = { it.id }) { part ->
                SplitPartItemRow(
                    part = part,
                    onEdit = { onEditPart(part.id) },
                    onDelete = { onDeletePart(part.id) }
                )
            }
        }

        // Bottom Bar: "Split (N)" button with "Ad 🎁" badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onSplitClick,
                    enabled = state.splitParts.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Split (${state.splitParts.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        // Ad 🎁 Badge on right edge (as shown in Image 1)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AdBadgeYellow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ad",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "🎁",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitPartItemRow(
    part: SplitPart,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val baseName = part.name.substringBeforeLast('.', part.name)
    val extension = part.name.substringAfterLast('.', "pdf")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // PDF Green Folded Icon
        PdfFoldedIcon(size = 46.dp)

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Pages Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$baseName. ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = extension,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = part.pageRangeText,
                fontSize = 13.sp,
                color = MutedTextColor
            )
        }

        // Action 1: Edit button (Pencil icon)
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit pages",
                tint = MutedTextColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Action 2: Remove button (Grey circular 'x')
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color(0xFF94A3B8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/* =========================================================================================
 * 4. SCREEN 4: SPLITTING PROGRESS DIALOG / BOTTOM SHEET (Image 2: media_1791831312906)
 * ========================================================================================= */
@Composable
private fun SplittingProgressDialog(
    progress: Int
) {
    Dialog(
        onDismissRequest = { /* Modal: keep showing while active */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // Bottom Sheet container
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // White rounded content card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp), // space for floating circular progress
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        // Title
                        Text(
                            text = "Splitting your files",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle
                        Text(
                            text = "Please wait a minute...",
                            fontSize = 15.sp,
                            color = MutedTextColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Native Ad Card (Real AdMob or fallback mockup card)
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NativeAdCard()
                        }
                    }
                }

                // Overlapping Circular Progress (Floating over top-right of the sheet)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 28.dp)
                        .size(68.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .background(Color.White, CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress / 100f,
                        label = "progress"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 3.5.dp.toPx()
                        // Background track
                        drawCircle(
                            color = Color(0xFFF1F5F9),
                            style = Stroke(width = stroke)
                        )
                        // Progress arc in red (matching mockup 64% in Image 2)
                        drawArc(
                            color = ProgressRed,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "$progress%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )
                }
            }
        }
    }
}

/* =========================================================================================
 * 5. SCREEN 5: SPLIT SUCCESS SCREEN (Missing screen - thinking & generated)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitSuccessScreen(
    results: List<SplitResultItem>,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpen: (String, String) -> Unit,
    onShare: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Split PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkTextColor
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkTextColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Celebration Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(BrandGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(BrandGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Files Split Successfully!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${results.size} separate PDF files created and saved to your device",
                fontSize = 14.sp,
                color = MutedTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Results List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderGray),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PdfFoldedIcon(size = 42.dp)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.pageCount} Pages • ${item.fileSize}",
                                    fontSize = 12.sp,
                                    color = MutedTextColor
                                )
                            }

                            // Open button
                            FilledTonalButton(
                                onClick = { onOpen(item.filePath, item.fileName) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrandGreen.copy(alpha = 0.12f),
                                    contentColor = BrandGreenDark
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Open", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Share icon button
                            IconButton(
                                onClick = { onShare(item.filePath) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MutedTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Done Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Done",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/* =========================================================================================
 * SHARED PDF FOLDED ICON COMPONENT (Matching Screenshots)
 * ========================================================================================= */
@Composable
fun PdfFoldedIcon(size: androidx.compose.ui.unit.Dp = 46.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Background white page with folded top-right corner
            val backPath = Path().apply {
                val left = w * 0.16f
                val top = h * 0.05f
                val right = w * 0.90f
                val bottom = h * 0.88f
                val fold = w * 0.18f

                moveTo(left, top)
                lineTo(right - fold, top)
                lineTo(right, top + fold)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            drawPath(backPath, color = Color(0xFFE2E8F0))

            // Main Green Badge in front
            drawRoundRect(
                color = BrandGreen,
                topLeft = Offset(0f, h * 0.12f),
                size = Size(w * 0.80f, h * 0.84f),
                cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
            )
        }

        // "PDF" text on the green badge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = (size.value * 0.18).dp, top = (size.value * 0.10).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PDF",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.28).sp,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
