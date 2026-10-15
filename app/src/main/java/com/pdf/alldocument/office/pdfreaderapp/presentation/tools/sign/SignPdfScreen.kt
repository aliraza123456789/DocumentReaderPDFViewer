package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.sign

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.LottieLoadingIndicator
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick

// Colors used across the Sign PDF flow matching the design
private val BrandGreen = Color(0xFF22C55E)
private val BrandGreenDark = Color(0xFF16A34A)
private val BrandGreenLight = Color(0xFF52D68A)
private val DarkTextColor = Color(0xFF0F172A)
private val MutedTextColor = Color(0xFF64748B)
private val LightBgColor = Color(0xFFF1F5F9)
private val RedAccent = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    filePath: String,
    title: String,
    viewModel: SignPdfViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showMySignatureSheet by remember { mutableStateOf(false) }
    var showCreateSignatureSheet by remember { mutableStateOf(false) }
    var showSaveSuccessSheet by remember { mutableStateOf(false) }
    var showFileNameDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val temp = FileUtils.copyUriToTempFile(context, it, "sign_${System.currentTimeMillis()}.pdf")
            if (temp != null) {
                viewModel.loadPdf(temp.absolutePath, temp.name)
            }
        }
    }

    LaunchedEffect(filePath) {
        if (filePath.isNotBlank() && state.filePath != filePath) {
            viewModel.loadPdf(filePath, title)
        }
    }

    LaunchedEffect(state.exportedFilePath) {
        if (state.exportedFilePath != null) {
            showSaveSuccessSheet = true
        }
    }

    // Handle back button: if document is loaded from selection screen, go back to selection screen
    BackHandler {
        if (state.filePath.isNotBlank() && filePath.isBlank()) {
            viewModel.loadPdf("", "")
        } else {
            onNavigateBack()
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
            if (state.isLoading || state.isExporting) {
                LottieLoadingIndicator(
                    rawRes = "ad_loading.json",
                    message = if (state.isExporting) "Saving signed document..." else "Rendering PDF...",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.filePath.isBlank() || state.pageBitmaps.isEmpty()) {
                // SCREEN 1: "Select a file" Screen
                SelectFileView(
                    state = state,
                    onBack = onNavigateBack,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleSearch = { viewModel.toggleSearch(it) },
                    onFileSelected = { doc ->
                        viewModel.loadPdf(doc.path, doc.name)
                    },
                    onOpenStorage = {
                        filePickerLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            } else {
                // SCREEN 2: PDF Signing Screen
                PdfSignEditorView(
                    state = state,
                    onBack = {
                        if (filePath.isBlank()) {
                            viewModel.loadPdf("", "")
                        } else {
                            onNavigateBack()
                        }
                    },
                    onToggleFit = { viewModel.toggleFitToWidth() },
                    onSaveClick = {
                        if (state.signatureBitmap == null) {
                            Toast.makeText(context, "Please add a signature first", Toast.LENGTH_SHORT).show()
                            showMySignatureSheet = true
                        } else {
                            showFileNameDialog = true
                        }
                    },
                    onOpenSignatureSheet = { showMySignatureSheet = true },
                    onPageSelected = { viewModel.setPageIndex(it) },
                    onSigMoved = { dx, dy -> viewModel.updateSigPosition(dx, dy) },
                    onSigResized = { dWidth -> viewModel.updateSigWidth(dWidth) },
                    onSigDeleted = { viewModel.removeCurrentSignature() },
                    onSigFlipped = { viewModel.toggleFlipHorizontal() }
                )
            }
        }
    }

    // SCREEN 3: "My Signature" Bottom Sheet
    if (showMySignatureSheet) {
        MySignatureBottomSheet(
            savedSignatures = state.savedSignatures,
            onCreateSignatureClick = {
                showMySignatureSheet = false
                showCreateSignatureSheet = true
            },
            onSelectSignature = { bmp ->
                viewModel.selectSavedSignature(bmp)
                showMySignatureSheet = false
            },
            onDeleteSignature = { bmp ->
                viewModel.deleteSavedSignature(bmp)
            },
            onClose = { showMySignatureSheet = false }
        )
    }

    // SCREEN 4: "Create Signature" Bottom Sheet
    if (showCreateSignatureSheet) {
        CreateSignatureBottomSheet(
            onDismiss = { showCreateSignatureSheet = false },
            onSignatureDone = { bmp ->
                viewModel.setSignature(bmp)
                showCreateSignatureSheet = false
            }
        )
    }

    // SCREEN 5: "Saved Successfully" Bottom Sheet
    if (showSaveSuccessSheet && state.exportedFilePath != null) {
        SavedSuccessfullyBottomSheet(
            filePath = state.exportedFilePath!!,
            title = state.title.ifBlank { "Business Plan.pdf" },
            pageCount = state.pageBitmaps.size,
            onDismiss = {
                showSaveSuccessSheet = false
                viewModel.clearResult()
            },
            onShare = {
                ShareUtils.shareFile(context, state.exportedFilePath!!)
                showSaveSuccessSheet = false
                viewModel.clearResult()
            },
            onOpen = {
                val path = state.exportedFilePath!!
                showSaveSuccessSheet = false
                viewModel.clearResult()
                onNavigateToViewer(path, "Signed Document")
            }
        )
    }

    // Name Prompt Dialog before saving
    if (showFileNameDialog) {
        var fileName by remember {
            val base = state.title.substringBeforeLast(".", "Signed_Document")
            mutableStateOf("${base}_Signed")
        }
        AlertDialog(
            onDismissRequest = { showFileNameDialog = false },
            title = { Text("Save Signed PDF", fontWeight = FontWeight.Bold, color = DarkTextColor) },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        focusedLabelColor = BrandGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFileNameDialog = false
                        viewModel.exportSignedPdf(fileName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFileNameDialog = false }) {
                    Text("Cancel", color = MutedTextColor)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/* =========================================================================================
 * 1. SELECT A FILE VIEW (Screenshot 1)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFileView(
    state: SignPdfState,
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkTextColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.searchQuery.isEmpty()) {
                                    Text("Search PDF...", color = MutedTextColor, fontSize = 17.sp)
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    Text(
                        text = "Select a file",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
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

        // Storage picker banner row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .tactileClick(onOpenStorage),
            shape = RoundedCornerShape(12.dp),
            color = LightBgColor
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

        // PDF List
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
                    SelectFileItem(
                        doc = doc,
                        onClick = { onFileSelected(doc) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectFileItem(
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
        // Green folded PDF icon matching screenshot 1
        PdfFoldedIcon(size = 46.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // "Business Plan. pdf" with 'pdf' in bright green
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
            // "03/24/2024 . 257.1 KB"
            Text(
                text = "${doc.formattedDate} . ${doc.formattedSize}",
                fontSize = 13.sp,
                color = MutedTextColor
            )
        }
    }
}

/* =========================================================================================
 * 2. MAIN SIGN EDITOR VIEW (Screenshot 2)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSignEditorView(
    state: SignPdfState,
    onBack: () -> Unit,
    onToggleFit: () -> Unit,
    onSaveClick: () -> Unit,
    onOpenSignatureSheet: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onSigMoved: (Float, Float) -> Unit,
    onSigResized: (Float) -> Unit,
    onSigDeleted: () -> Unit,
    onSigFlipped: () -> Unit
) {
    val baseName = state.title.substringBeforeLast('.', state.title)
    val extension = state.title.substringAfterLast('.', "pdf")
    val currentPageBitmap = state.pageBitmaps.getOrNull(state.currentPageIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEBF0F5))
    ) {
        // TOP APP BAR (Screenshot 2)
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$baseName. ",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = extension,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedAccent // Red .pdf in Screenshot 2 top bar
                    )
                }
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
                // Fit / Crop Free Icon
                IconButton(onClick = onToggleFit) {
                    Icon(
                        imageVector = Icons.Outlined.CropFree,
                        contentDescription = "Fit to Screen",
                        tint = Color(0xFF64748B)
                    )
                }
                // Red Checkmark Save Button (Screenshot 2)
                IconButton(onClick = onSaveClick) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = RedAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // MAIN DOCUMENT CANVAS AREA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (currentPageBitmap != null) {
                var previewSize by remember { mutableStateOf(IntSize.Zero) }

                val pageAspect = currentPageBitmap.width.toFloat() / currentPageBitmap.height.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Document Sheet Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(if (state.isFitToWidth) 1.0f else 0.94f)
                            .aspectRatio(pageAspect)
                            .shadow(10.dp, RoundedCornerShape(2.dp))
                            .onSizeChanged { previewSize = it },
                        color = Color.White,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = currentPageBitmap.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )

                            // SIGNATURE OVERLAY (Screenshot 2)
                            if (state.signatureBitmap != null && previewSize.width > 0 && previewSize.height > 0) {
                                val sigBmp = state.signatureBitmap
                                val sigAspect = sigBmp.height.toFloat() / sigBmp.width.toFloat()

                                val sigWidthPx = (previewSize.width * state.sigNormWidth).coerceAtLeast(60f)
                                val sigHeightPx = (sigWidthPx * sigAspect).coerceAtLeast(30f)

                                val maxPosX = previewSize.width - sigWidthPx
                                val maxPosY = previewSize.height - sigHeightPx

                                val posX = (previewSize.width * state.sigNormX).coerceIn(0f, maxPosX.coerceAtLeast(0f))
                                val posY = (previewSize.height * state.sigNormY).coerceIn(0f, maxPosY.coerceAtLeast(0f))

                                val density = LocalDensity.current
                                val sigWidthDp = with(density) { sigWidthPx.toDp() }
                                val sigHeightDp = with(density) { sigHeightPx.toDp() }

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(posX.toInt(), posY.toInt()) }
                                        .size(sigWidthDp, sigHeightDp)
                                ) {
                                    // Bounding Box & Signature
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(2.dp))
                                            .pointerInput(previewSize) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val normDx = dragAmount.x / previewSize.width.toFloat()
                                                    val normDy = dragAmount.y / previewSize.height.toFloat()
                                                    onSigMoved(normDx, normDy)
                                                }
                                            }
                                    ) {
                                        Image(
                                            bitmap = sigBmp.asImageBitmap(),
                                            contentDescription = "Signature",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    if (state.isFlippedHorizontally) {
                                                        scaleX = -1f
                                                    }
                                                },
                                            contentScale = ContentScale.FillBounds
                                        )
                                    }

                                    // Top-Left Handle: Green circle with trash can (Delete)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .align(Alignment.TopStart)
                                            .offset(x = (-13).dp, y = (-13).dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen)
                                            .clickable(onClick = onSigDeleted),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Signature",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // Middle-Right Handle: Green circle with horizontal resize/flip (Screenshot 2)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .align(Alignment.CenterEnd)
                                            .offset(x = 13.dp, y = 0.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen)
                                            .clickable(onClick = onSigFlipped),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                            contentDescription = "Flip Signature",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Bottom-Right Handle: Green circle with diagonal resize (Screenshot 2)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 13.dp, y = 13.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen)
                                            .pointerInput(previewSize) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val normDelta = dragAmount.x / previewSize.width.toFloat()
                                                    onSigResized(normDelta)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInFull,
                                            contentDescription = "Resize Signature",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating dark mode / zoom icon at top right of PDF viewer
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                            .clickable(onClick = onToggleFit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Expand Mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Prominent Sign Button if no signature added yet
                    if (state.signatureBitmap == null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .tactileClick(onOpenSignatureSheet),
                            shape = RoundedCornerShape(24.dp),
                            color = BrandGreen,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Draw,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add Signature",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // PAGE NUMBER PILL ("1/5" - Screenshot 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.currentPageIndex + 1}/${state.pageBitmaps.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // BOTTOM THUMBNAIL CAROUSEL (Screenshot 2)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(state.pageBitmaps) { index, bmp ->
                    val isSelected = index == state.currentPageIndex
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(78.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) BrandGreen else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onPageSelected(index) }
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Bottom-right corner chip: Green if selected, grey if unselected
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(topStart = 4.dp))
                                .background(if (isSelected) BrandGreen else Color(0xFF94A3B8))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================================
 * 3. "MY SIGNATURE" BOTTOM SHEET (Screenshot 3)
 * ========================================================================================= */
@Composable
fun MySignatureBottomSheet(
    savedSignatures: List<Bitmap>,
    onCreateSignatureClick: () -> Unit,
    onSelectSignature: (Bitmap) -> Unit,
    onDeleteSignature: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Bottom card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "My Signature",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Green "Create Signature" Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .tactileClick(onCreateSignatureClick),
                            shape = RoundedCornerShape(14.dp),
                            color = BrandGreenLight
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Create Signature",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                                Icon(
                                    imageVector = Icons.Outlined.AddCircle,
                                    contentDescription = "Create",
                                    tint = DarkTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Saved Signatures List
                        if (savedSignatures.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            savedSignatures.forEach { bmp ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clickable { onSelectSignature(bmp) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = LightBgColor
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Saved Signature",
                                            modifier = Modifier
                                                .height(44.dp)
                                                .width(120.dp),
                                            contentScale = ContentScale.Fit
                                        )

                                        IconButton(onClick = { onDeleteSignature(bmp) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.RemoveCircleOutline,
                                                contentDescription = "Delete",
                                                tint = DarkTextColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Outlined "Close" Button
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.End)
                                .width(120.dp)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextColor)
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor
                            )
                        }
                    }
                }

                // Top-Right Overlapping Badge with document & pen illustration (Screenshot 3)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 28.dp)
                ) {
                    SignatureOverlapBadge()
                }
            }
        }
    }
}

/* =========================================================================================
 * 4. "CREATE SIGNATURE" BOTTOM SHEET (Screenshot 4)
 * ========================================================================================= */
data class DrawingPoint(val x: Float, val y: Float)
data class StrokeLine(val points: List<DrawingPoint>, val color: Color, val strokeWidth: Float)

@Composable
fun CreateSignatureBottomSheet(
    onDismiss: () -> Unit,
    onSignatureDone: (Bitmap) -> Unit
) {
    val completedStrokes = remember { mutableStateListOf<StrokeLine>() }
    val currentPoints = remember { mutableStateListOf<DrawingPoint>() }

    var selectedColor by remember { mutableStateOf(Color(0xFF1E293B)) } // Navy/Black
    var selectedStrokeWidth by remember { mutableFloatStateOf(6f) } // Medium

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Create Signature",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Signature Drawing Canvas (Screenshot 4)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LightBgColor)
                                .pointerInput(selectedColor, selectedStrokeWidth) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints.clear()
                                            currentPoints.add(DrawingPoint(offset.x, offset.y))
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPoints.add(DrawingPoint(change.position.x, change.position.y))
                                        },
                                        onDragEnd = {
                                            if (currentPoints.isNotEmpty()) {
                                                completedStrokes.add(
                                                    StrokeLine(
                                                        points = currentPoints.toList(),
                                                        color = selectedColor,
                                                        strokeWidth = selectedStrokeWidth
                                                    )
                                                )
                                                currentPoints.clear()
                                            }
                                        },
                                        onDragCancel = {
                                            currentPoints.clear()
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw completed strokes
                                completedStrokes.forEach { stroke ->
                                    drawSmoothStroke(stroke.points, stroke.color, stroke.strokeWidth)
                                }
                                // Draw active stroke
                                if (currentPoints.isNotEmpty()) {
                                    drawSmoothStroke(currentPoints, selectedColor, selectedStrokeWidth)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Palette and Controls Bar (Screenshot 4)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colors: Navy/Black, Red, Blue
                            val colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFFEF4444),
                                Color(0xFF2563EB)
                            )
                            colors.forEach { c ->
                                val isSelected = selectedColor == c
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) BrandGreen else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = c }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(Color(0xFFCBD5E1))
                            )
                            Spacer(modifier = Modifier.width(14.dp))

                            // Stroke widths (Thin, Medium, Thick)
                            val strokeOptions = listOf(4f, 7f, 12f)
                            strokeOptions.forEach { w ->
                                val isSelected = selectedStrokeWidth == w
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFE2E8F0) else Color.Transparent)
                                        .clickable { selectedStrokeWidth = w },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Wave squiggle icon representing stroke thickness
                                    WaveStrokeIcon(strokeWidth = w, tint = DarkTextColor)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // "Clear" Button
                            Text(
                                text = "Clear",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedTextColor,
                                modifier = Modifier
                                    .clickable {
                                        completedStrokes.clear()
                                        currentPoints.clear()
                                    }
                                    .padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bottom Actions: Cancel & Done (Screenshot 4)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextColor)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                            }

                            Button(
                                onClick = {
                                    val allPoints = completedStrokes.flatMap { it.points }
                                    if (allPoints.isNotEmpty()) {
                                        val capturedBitmap = renderStrokesToTightlyCroppedBitmap(completedStrokes)
                                        onSignatureDone(capturedBitmap)
                                    }
                                },
                                enabled = completedStrokes.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreen,
                                    disabledContainerColor = Color(0xFFA7F3D0)
                                )
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Top-Right Overlapping Badge (Screenshot 4)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 28.dp)
                ) {
                    SignatureOverlapBadge()
                }
            }
        }
    }
}

/* =========================================================================================
 * 5. "SAVED SUCCESSFULLY" BOTTOM SHEET (Screenshot 5)
 * ========================================================================================= */
@Composable
fun SavedSuccessfullyBottomSheet(
    filePath: String,
    title: String,
    pageCount: Int,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val baseName = title.substringBeforeLast('.', title)
    val extension = title.substringAfterLast('.', "pdf")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Saved Successfully",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Document Row (Screenshot 5)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpen)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PdfFoldedIcon(size = 46.dp)

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$baseName. ",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = extension,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (pageCount > 1) "Pages 1-$pageCount" else "1 Page",
                                    fontSize = 13.sp,
                                    color = MutedTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // Bottom Actions: Cancel & Share (Screenshot 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextColor)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                            }

                            Button(
                                onClick = onShare,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Text(
                                    text = "Share",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Top-Right Overlapping Badge with PDF icon + Share bubble (Screenshot 5)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 28.dp)
                ) {
                    SavedSuccessOverlapBadge()
                }
            }
        }
    }
}

/* =========================================================================================
 * HELPER CANVAS DRAWING & BITMAP EXPORT FUNCTIONS
 * Completely eliminates any signature clipping or cut-off!
 * ========================================================================================= */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothStroke(
    points: List<DrawingPoint>,
    color: Color,
    strokeWidth: Float
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = strokeWidth / 2,
            center = Offset(points[0].x, points[0].y)
        )
        return
    }

    val path = Path()
    path.moveTo(points[0].x, points[0].y)

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Renders all strokes into a high-DPI, tightly-cropped transparent bitmap with generous padding.
 * This guarantees the user's signature is NEVER cut in half, regardless of canvas size.
 */
private fun renderStrokesToTightlyCroppedBitmap(strokes: List<StrokeLine>): Bitmap {
    val allPoints = strokes.flatMap { it.points }
    if (allPoints.isEmpty()) {
        return Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
    }

    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxY = allPoints.maxOf { it.y }

    val padding = 36f
    val drawnWidth = (maxX - minX).coerceAtLeast(10f)
    val drawnHeight = (maxY - minY).coerceAtLeast(10f)

    val targetWidth = (drawnWidth + padding * 2).toInt().coerceAtLeast(80)
    val targetHeight = (drawnHeight + padding * 2).toInt().coerceAtLeast(40)

    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Translate so signature is padded nicely inside the transparent bitmap
    canvas.translate(-(minX - padding), -(minY - padding))

    strokes.forEach { stroke ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        if (stroke.points.size == 1) {
            canvas.drawCircle(stroke.points[0].x, stroke.points[0].y, stroke.strokeWidth / 2f, paint)
        } else if (stroke.points.size > 1) {
            val path = AndroidPath()
            path.moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                path.quadTo(prev.x, prev.y, midX, midY)
            }
            val last = stroke.points.last()
            path.lineTo(last.x, last.y)
            canvas.drawPath(path, paint)
        }
    }

    return bitmap
}

/* =========================================================================================
 * CUSTOM ICON & OVERLAPPING BADGE COMPOSABLES
 * ========================================================================================= */

/**
 * Green folded PDF icon matching Screenshots 1 & 5
 */
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
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.32).sp,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

/**
 * Overlapping badge for Screenshots 3 & 4 (Notepad with pencil writing signature)
 */
@Composable
fun SignatureOverlapBadge() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .shadow(8.dp, CircleShape)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(46.dp)) {
            val w = this.size.width
            val h = this.size.height

            // Notepad sheet (light blue)
            val notePath = Path().apply {
                val left = w * 0.12f
                val top = h * 0.15f
                val right = w * 0.78f
                val bottom = h * 0.92f
                val fold = w * 0.18f

                moveTo(left, top)
                lineTo(right - fold, top)
                lineTo(right, top + fold)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            drawPath(notePath, color = Color(0xFFE0F2FE))

            // Lines on sheet
            val linePaint = Color(0xFF93C5FD)
            drawLine(linePaint, Offset(w * 0.22f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 3f)
            drawLine(linePaint, Offset(w * 0.22f, h * 0.48f), Offset(w * 0.65f, h * 0.48f), strokeWidth = 3f)
            drawLine(linePaint, Offset(w * 0.22f, h * 0.61f), Offset(w * 0.55f, h * 0.61f), strokeWidth = 3f)

            // Green signature line
            val sigPath = Path().apply {
                moveTo(w * 0.20f, h * 0.78f)
                quadraticTo(w * 0.30f, h * 0.70f, w * 0.40f, h * 0.78f)
                quadraticTo(w * 0.50f, h * 0.86f, w * 0.60f, h * 0.76f)
            }
            drawPath(sigPath, color = BrandGreen, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

            // Orange pencil angled across
            val pencilPath = Path().apply {
                moveTo(w * 0.60f, h * 0.76f)
                lineTo(w * 0.82f, h * 0.18f)
                lineTo(w * 0.90f, h * 0.22f)
                lineTo(w * 0.68f, h * 0.80f)
                close()
            }
            drawPath(pencilPath, color = Color(0xFFF97316))
        }
    }
}

/**
 * Overlapping badge for Screenshot 5 (PDF badge + Blue share bubble)
 */
@Composable
fun SavedSuccessOverlapBadge() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .shadow(8.dp, CircleShape)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            // PDF document in center
            PdfFoldedIcon(size = 38.dp)

            // Blue circular share icon at bottom right of badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Wave stroke icon representing stroke width selection
 */
@Composable
fun WaveStrokeIcon(strokeWidth: Float, tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            quadraticTo(w * 0.35f, h * 0.15f, w * 0.5f, h * 0.5f)
            quadraticTo(w * 0.65f, h * 0.85f, w * 0.9f, h * 0.5f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth.coerceIn(2f, 7f), cap = StrokeCap.Round)
        )
    }
}
