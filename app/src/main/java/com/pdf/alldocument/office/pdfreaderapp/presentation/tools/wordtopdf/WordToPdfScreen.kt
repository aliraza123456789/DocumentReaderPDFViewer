package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.wordtopdf

import android.content.Context
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdCard
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick

// Colors exactly matching the UI design images (74.png - 78.png)
private val BrandGreen = Color(0xFF22C55E)
private val BrandGreenDark = Color(0xFF16A34A)
private val DisabledGreen = Color(0xFFA7F3D0)
private val DarkTextColor = Color(0xFF0F172A)
private val MutedTextColor = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)
private val ProgressRed = Color(0xFFEF4444)
private val CheckmarkRed = Color(0xFFEF4444)
private val DocTagGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordToPdfScreen(
    onNavigateBack: () -> Unit = {},
    onBack: () -> Unit = onNavigateBack,
    onNavigateToViewer: (String, String) -> Unit = { _, _ -> },
    viewModel: WordToPdfViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val name = context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && i >= 0) c.getString(i) else null
            } ?: "document.docx"
            viewModel.selectCustomDocument(it, name)
        }
    }

    // Handle back button according to flow state
    BackHandler {
        val handled = viewModel.handleBackPress()
        if (!handled) {
            onBack()
        }
    }

    // Toast error messages if conversion fails
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
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
                WordToPdfStep.SELECT_FILE -> {
                    // SCREEN 1 & 2: Select a File (74.png empty state & 75.png selected state)
                    SelectWordFileScreen(
                        state = state,
                        onBack = {
                            val handled = viewModel.handleBackPress()
                            if (!handled) onBack()
                        },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onToggleSearch = { viewModel.toggleSearch(it) },
                        onDocSelected = { doc -> viewModel.selectDocument(doc) },
                        onOpenStorage = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/msword"
                                )
                            )
                        },
                        onConvertClick = { viewModel.onConvertClicked() }
                    )

                    // SCREEN 3: File Name Rename Dialog (76.png)
                    if (state.isRenaming) {
                        RenameFileDialog(
                            fileName = state.customFileName,
                            onFileNameChange = { viewModel.updateCustomFileName(it) },
                            onDismiss = { viewModel.dismissRenameDialog() },
                            onDone = { viewModel.startConversion() }
                        )
                    }

                    // SCREEN 4: Converting Word to PDF Dialog / Bottom Sheet (77.png)
                    if (state.isConverting) {
                        ConvertingWordToPdfDialog(
                            progress = state.conversionProgress
                        )
                    }
                }

                WordToPdfStep.SUCCESS -> {
                    // SCREEN 5: Converted Successfully Screen (78.png)
                    ConvertedSuccessScreen(
                        filePath = state.convertedFilePath.orEmpty(),
                        fileName = state.convertedFileName.orEmpty(),
                        onBack = {
                            viewModel.resetFlow()
                            onBack()
                        },
                        onOpen = { path, name ->
                            onNavigateToViewer(path, name)
                        },
                        onShare = { path ->
                            ShareUtils.shareFile(context, path)
                        }
                    )
                }
            }
        }
    }
}

/* =========================================================================================
 * 1. SCREEN 1 & 2: SELECT A FILE (74.png & 75.png)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectWordFileScreen(
    state: WordToPdfUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onDocSelected: (DocumentItem) -> Unit,
    onOpenStorage: () -> Unit,
    onConvertClick: () -> Unit
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
                                    Text("Search Word files...", color = MutedTextColor, fontSize = 18.sp)
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
                IconButton(onClick = { onToggleSearch(!state.isSearching) }) {
                    Icon(
                        imageVector = if (state.isSearching) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DarkTextColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Storage picker card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onOpenStorage),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BrandGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
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
                        text = "Pick Word document (.docx, .doc)",
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

        Spacer(modifier = Modifier.height(6.dp))

        // Document list
        if (state.filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WordDocFoldedIcon(size = 54.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (state.searchQuery.isBlank()) "No Word documents found" else "No matching files",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Browse from device' to pick a Word file.",
                        fontSize = 13.sp,
                        color = MutedTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(state.filteredDocs, key = { it.path }) { doc ->
                    val isSelected = state.selectedDoc?.path == doc.path
                    WordFileListItem(
                        doc = doc,
                        isSelected = isSelected,
                        onClick = { onDocSelected(doc) }
                    )
                }
            }
        }

        // Bottom CTA Button (74.png disabled "Done" vs 75.png enabled "Convert Word to PDF")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val hasSelection = state.selectedDoc != null
                Button(
                    onClick = {
                        if (hasSelection) onConvertClick()
                    },
                    enabled = hasSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .tactileClick {
                            if (hasSelection) onConvertClick()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White,
                        disabledContainerColor = DisabledGreen,
                        disabledContentColor = Color.White.copy(alpha = 0.85f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (hasSelection) "Convert Word to PDF" else "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Word File List Row Item with radio / green checkmark indicator (74.png & 75.png)
 */
@Composable
private fun WordFileListItem(
    doc: DocumentItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val baseName = doc.name.substringBeforeLast('.', doc.name)
    val extension = doc.name.substringAfterLast('.', "doc")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green folded DOC icon
        WordDocFoldedIcon(size = 46.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // "Medical Report. doc" with 'doc' in bright green
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
                    color = DocTagGreen
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

        Spacer(modifier = Modifier.width(12.dp))

        // Checkmark indicator (Green circle with checkmark if selected, gray circle if not)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandGreen),
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
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(BorderStroke(2.dp, Color(0xFFCBD5E1)), CircleShape)
            )
        }
    }
}

/* =========================================================================================
 * 2. SCREEN 3: RENAME FILE MODAL (76.png)
 * ========================================================================================= */
@Composable
private fun RenameFileDialog(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // White rounded card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp), // Space for top overlapping pencil badge
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
                            text = "File Name",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text input field with rounded gray pill & clear button (76.png)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, BorderGray)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = fileName,
                                    onValueChange = onFileNameChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkTextColor
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (fileName.isEmpty()) {
                                                Text(
                                                    text = "Enter file name",
                                                    color = MutedTextColor,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            innerTextField()
                                            Text(
                                                text = ".doc",
                                                color = DocTagGreen,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                )

                                if (fileName.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF94A3B8))
                                            .clickable { onFileNameChange("") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons: Cancel & Done (76.png)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, BorderGray),
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
                                onClick = onDone,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
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

                // Overlapping Badge with Notepad & Pencil (top-right of modal as shown in 76.png)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 28.dp)
                ) {
                    NotepadPencilBadge()
                }
            }
        }
    }
}

/* =========================================================================================
 * 3. SCREEN 4: CONVERTING WORD TO PDF BOTTOM SHEET / MODAL (77.png)
 * ========================================================================================= */
@Composable
private fun ConvertingWordToPdfDialog(
    progress: Int
) {
    Dialog(
        onDismissRequest = { /* Modal while converting */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // White rounded bottom card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp),
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
                        // Title (77.png)
                        Text(
                            text = "Converting Word to PDF",
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

                        // Native Ad Card (Real AdMob or styled fallback card)
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NativeAdCard()
                        }
                    }
                }

                // Overlapping Circular Progress Indicator (Floating over top-right of sheet, 77.png)
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
                        // Progress arc in red (matching mockup 64% in 77.png)
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
 * 4. SCREEN 5: CONVERTED SUCCESSFULLY SCREEN (78.png)
 * ========================================================================================= */
@Composable
private fun ConvertedSuccessScreen(
    filePath: String,
    fileName: String,
    onBack: () -> Unit,
    onOpen: (String, String) -> Unit,
    onShare: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Title (78.png: "Converted Successfully!")
        Text(
            text = "Converted Successfully!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Red Circle with White Checkmark & Confetti bursts (78.png)
        SuccessConfettiCheckmark(size = 180.dp)

        Spacer(modifier = Modifier.height(48.dp))

        // File Title & Pencil edit icon (78.png: "Business Plan.pdf" with edit icon)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = fileName.ifBlank { "Business Plan.pdf" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit name",
                tint = DarkTextColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Emulated file path subtitle (78.png)
        Text(
            text = filePath.ifBlank { "/storage/emulated/0/Document/PDF Reader/converted/Business_Plan.pdf" },
            fontSize = 13.sp,
            color = MutedTextColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Action Buttons: Share & Open (78.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { onShare(filePath) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, BorderGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextColor)
            ) {
                Text(
                    text = "Share",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
            }

            Button(
                onClick = { onOpen(filePath, fileName) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text(
                    text = "Open",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Native Ad Card (78.png)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            NativeAdCard()
        }
    }
}

/* =========================================================================================
 * 5. CUSTOM ICONS & BADGES (Exact vector rendering matching 74.png - 78.png)
 * ========================================================================================= */

/**
 * Green folded document icon with "DOC" label (74.png, 75.png, 76.png)
 */
@Composable
fun WordDocFoldedIcon(size: Dp = 46.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Background white folded page
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

            // Main Green Badge
            drawRoundRect(
                color = DocTagGreen,
                topLeft = Offset(0f, h * 0.12f),
                size = Size(w * 0.80f, h * 0.84f),
                cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
            )
        }

        // "DOC" text on green badge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = (size.value * 0.18).dp, top = (size.value * 0.10).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DOC",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.28).sp,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

/**
 * Notepad & Pencil Overlapping Badge (76.png)
 */
@Composable
private fun NotepadPencilBadge() {
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

            // Notepad sheet (light cream / beige)
            val notePath = Path().apply {
                val left = w * 0.14f
                val top = h * 0.12f
                val right = w * 0.78f
                val bottom = h * 0.88f
                val fold = w * 0.16f

                moveTo(left, top)
                lineTo(right - fold, top)
                lineTo(right, top + fold)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            drawPath(notePath, color = Color(0xFFFEF3C7))

            // Notepad text lines
            val linePaint = Color(0xFF94A3B8)
            drawLine(linePaint, Offset(w * 0.22f, h * 0.32f), Offset(w * 0.65f, h * 0.32f), strokeWidth = 3f)
            drawLine(linePaint, Offset(w * 0.22f, h * 0.44f), Offset(w * 0.65f, h * 0.44f), strokeWidth = 3f)
            drawLine(linePaint, Offset(w * 0.22f, h * 0.56f), Offset(w * 0.55f, h * 0.56f), strokeWidth = 3f)

            // Orange/Yellow pencil writing diagonally across
            val pencilPath = Path().apply {
                moveTo(w * 0.55f, h * 0.78f)
                lineTo(w * 0.82f, h * 0.22f)
                lineTo(w * 0.90f, h * 0.26f)
                lineTo(w * 0.63f, h * 0.82f)
                close()
            }
            drawPath(pencilPath, color = Color(0xFFF59E0B))
            // Pencil tip
            val tipPath = Path().apply {
                moveTo(w * 0.55f, h * 0.78f)
                lineTo(w * 0.63f, h * 0.82f)
                lineTo(w * 0.50f, h * 0.86f)
                close()
            }
            drawPath(tipPath, color = Color(0xFF1E293B))
        }
    }
}

/**
 * Red circle with checkmark & celebratory confetti (78.png)
 */
@Composable
private fun SuccessConfettiCheckmark(size: Dp = 180.dp) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Festive colorful confetti particles around checkmark
            val confettiColors = listOf(
                Color(0xFFEF4444), // Red
                Color(0xFF3B82F6), // Blue
                Color(0xFF10B981), // Green
                Color(0xFFF59E0B), // Yellow
                Color(0xFFEC4899), // Pink
                Color(0xFF8B5CF6)  // Purple
            )

            // Scattered particles
            val particles = listOf(
                Offset(cx - 50f, cy - 65f) to confettiColors[0],
                Offset(cx + 45f, cy - 70f) to confettiColors[1],
                Offset(cx - 70f, cy - 25f) to confettiColors[2],
                Offset(cx + 65f, cy - 35f) to confettiColors[3],
                Offset(cx - 65f, cy + 45f) to confettiColors[4],
                Offset(cx + 60f, cy + 50f) to confettiColors[5],
                Offset(cx - 30f, cy + 70f) to confettiColors[1],
                Offset(cx + 35f, cy + 65f) to confettiColors[0],
                Offset(cx - 20f, cy - 75f) to confettiColors[4],
                Offset(cx + 10f, cy - 80f) to confettiColors[3]
            )

            particles.forEach { (pos, col) ->
                drawCircle(color = col, radius = 3.5f, center = pos)
            }
        }

        // Main Red Checkmark Circle
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .shadow(12.dp, CircleShape)
                .background(CheckmarkRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(size * 0.36f)
            )
        }
    }
}
