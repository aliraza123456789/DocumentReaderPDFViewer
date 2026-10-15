package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.merge

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import kotlin.math.cos
import kotlin.math.sin

private val VibrantGreen = Color(0xFF00C853)
private val LightGreenDisabled = Color(0xFFB8EED0)
private val DarkTextColor = Color(0xFF0F172A)
private val SubtitleGray = Color(0xFF94A3B8)
private val ProgressRed = Color(0xFFEF4444)
private val AdGold = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    viewModel: MergePdfViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }

    // External file picker launcher in case user wants to pick files from SAF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedUris(context, uris)
        }
    }

    // Display error messages as Toast
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    // Handle system back button properly based on the active flow step
    BackHandler {
        when {
            state.showFileNameDialog -> viewModel.dismissFileNameDialog()
            state.isMerging -> { /* Merging in progress, prevent accidental exit */ }
            state.step == MergeFlowStep.SUCCESS -> {
                viewModel.resetFlow()
                onNavigateBack()
            }
            state.step == MergeFlowStep.REORDER_FILES -> viewModel.backToSelection()
            state.isSearchActive -> viewModel.toggleSearch(false)
            else -> onNavigateBack()
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
                MergeFlowStep.SELECT_FILES -> {
                    MergeFileSelectionContent(
                        state = state,
                        onBackClick = onNavigateBack,
                        onSearchToggle = { viewModel.toggleSearch() },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onToggleSelection = { viewModel.toggleFileSelection(it) },
                        onDoneClick = {
                            if (state.selectedFiles.size < 2) {
                                Toast.makeText(
                                    context,
                                    "Please select at least 2 PDF files to merge",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.proceedToReorder()
                            }
                        },
                        onPickFromStorage = {
                            filePickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
                }

                MergeFlowStep.REORDER_FILES -> {
                    MergeReorderContent(
                        state = state,
                        onBackClick = { viewModel.backToSelection() },
                        onAddMoreClick = { viewModel.backToSelection() },
                        onRemoveFile = { viewModel.removeSelectedFile(it) },
                        onMoveFile = { from, to -> viewModel.moveFile(from, to) },
                        onMergeClick = {
                            if (state.selectedFiles.size < 2) {
                                Toast.makeText(
                                    context,
                                    "Please select at least 2 PDF files to merge",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.openFileNameDialog()
                            }
                        }
                    )
                }

                MergeFlowStep.SUCCESS -> {
                    MergedSuccessContent(
                        state = state,
                        onRenameClick = { showRenameDialog = true },
                        onShareClick = {
                            state.mergedFilePath?.let { path ->
                                ShareUtils.shareFile(context, path)
                            }
                        },
                        onOpenClick = {
                            state.mergedFilePath?.let { path ->
                                onNavigateToViewer(path, state.mergedFileName ?: "Merged PDF")
                            }
                        }
                    )
                }
            }

            // Step 3: File Name BottomSheet Dialog (Image 51.png)
            if (state.showFileNameDialog) {
                FileNameBottomSheet(
                    initialName = state.proposedFileName,
                    onDismiss = { viewModel.dismissFileNameDialog() },
                    onConfirm = { fileName ->
                        viewModel.mergePdfs(fileName)
                    }
                )
            }

            // Step 4 & 5: Merging Progress BottomSheet (Image 52.png)
            if (state.isMerging) {
                MergingProgressBottomSheet(
                    progress = state.mergeProgress
                )
            }

            // Rename dialog for merged file
            if (showRenameDialog && state.mergedFileName != null) {
                var newName by remember { mutableStateOf(state.mergedFileName ?: "") }
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text("Rename Merged File", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("File Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRenameDialog = false
                                viewModel.renameMergedFile(newName)
                            },
                            enabled = newName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                    },
                    containerColor = Color.White
                )
            }
        }
    }
}

/**
 * Step 1 & 2: PDF Selection Screen (Images 48.png & 49.png)
 */
@Composable
private fun MergeFileSelectionContent(
    state: MergePdfState,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSelection: (DocumentItem) -> Unit,
    onDoneClick: () -> Unit,
    onPickFromStorage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkTextColor
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "${state.selectedFiles.size} Selected",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkTextColor,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = DarkTextColor
                )
            }
        }

        // Search Bar (when active)
        AnimatedVisibility(
            visible = state.isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = DarkTextColor),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Search PDFs...", fontSize = 15.sp, color = Color(0xFF94A3B8))
                            }
                            innerTextField()
                        }
                    )
                    if (state.searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }
            }
        }

        // List of PDF items
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.isLoadingFiles) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibrantGreen)
                }
            } else if (state.filteredFiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No PDF files found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPickFromStorage,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Browse from Storage")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(state.filteredFiles, key = { it.path }) { document ->
                        val isSelected = state.selectedFiles.any { it.path == document.path }
                        PdfSelectionItemRow(
                            document = document,
                            isSelected = isSelected,
                            onToggle = { onToggleSelection(document) }
                        )
                    }
                }
            }
        }

        // Bottom "Done" Button (Image 48.png disabled, Image 49.png enabled)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val isEnabled = state.selectedFiles.isNotEmpty()
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabled) VibrantGreen else LightGreenDisabled,
                        contentColor = Color.White,
                        disabledContainerColor = LightGreenDisabled,
                        disabledContentColor = Color.White
                    ),
                    enabled = true // Clickable to trigger toast if < 2 selected
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Single item row in the PDF selection list (matching 48.png & 49.png)
 */
@Composable
private fun PdfSelectionItemRow(
    document: DocumentItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val baseName = document.name.substringBeforeLast('.', document.name)
    val extension = document.name.substringAfterLast('.', "pdf")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green PDF document badge
        MergePdfDocIcon()

        Spacer(modifier = Modifier.width(14.dp))

        // Title and date/size
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$baseName.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = extension,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = VibrantGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${document.formattedDate} . ${document.formattedSize}",
                fontSize = 12.sp,
                color = SubtitleGray
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Checkbox / Radio Circle
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(VibrantGreen),
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
                    .clip(CircleShape)
                    .border(BorderStroke(1.5.dp, Color(0xFFCBD5E1)), CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Step 2: Merge PDF Reorder & Confirmation Screen (Image 50.png)
 */
@Composable
private fun MergeReorderContent(
    state: MergePdfState,
    onBackClick: () -> Unit,
    onAddMoreClick: () -> Unit,
    onRemoveFile: (DocumentItem) -> Unit,
    onMoveFile: (Int, Int) -> Unit,
    onMergeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkTextColor
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Merge PDF",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkTextColor,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onAddMoreClick) {
                Text(
                    text = "+ Add",
                    color = VibrantGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // List of Selected PDFs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(state.selectedFiles, key = { _, item -> item.path }) { index, file ->
                    PdfReorderItemRow(
                        document = file,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.selectedFiles.size - 1,
                        onMoveUp = { onMoveFile(index, index - 1) },
                        onMoveDown = { onMoveFile(index, index + 1) },
                        onRemove = { onRemoveFile(file) }
                    )
                }
            }
        }

        // Bottom "Merge (N)" button with Ad gift tag (Image 50.png)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onMergeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantGreen,
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Merge (${state.selectedFiles.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )

                        // Ad gift tag on the right
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AdGold)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ad 🎁",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single item row in the reorder list (matching 50.png)
 */
@Composable
private fun PdfReorderItemRow(
    document: DocumentItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val baseName = document.name.substringBeforeLast('.', document.name)
    val extension = document.name.substringAfterLast('.', "pdf")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle "=" (two horizontal bars) + quick up/down controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 2.dp)
                        .background(Color(0xFF64748B), RoundedCornerShape(1.dp))
                )
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 2.dp)
                        .background(Color(0xFF64748B), RoundedCornerShape(1.dp))
                )
            }
        }

        // Green PDF document badge
        MergePdfDocIcon()

        Spacer(modifier = Modifier.width(14.dp))

        // Title and date/size
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$baseName.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = extension,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = VibrantGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${document.formattedDate} . ${document.formattedSize}",
                fontSize = 12.sp,
                color = SubtitleGray
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete button "(X)"
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Cancel,
                contentDescription = "Remove",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Step 3: File Name BottomSheet Dialog (Image 51.png)
 */
@Composable
private fun FileNameBottomSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Sheet container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent closing when tapping sheet
            ) {
                // Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp), // Space for floating badge
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Text(
                            text = "File Name",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Text Field Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = fileName,
                                    onValueChange = { fileName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkTextColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                if (fileName.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { fileName = "" }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons Row: Cancel & Done
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = DarkTextColor
                                )
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkTextColor
                                )
                            }

                            // Done Button
                            Button(
                                onClick = { onConfirm(fileName) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantGreen,
                                    contentColor = Color.White
                                ),
                                enabled = fileName.isNotBlank()
                            ) {
                                Text(
                                    text = "Done",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Floating circular badge at top right (Notepad & Pencil)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp)
                        .size(60.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(36.dp)) {
                        // Notepad document
                        val padWidth = size.width * 0.65f
                        val padHeight = size.height * 0.75f
                        val padLeft = (size.width - padWidth) / 2f
                        val padTop = (size.height - padHeight) / 2f

                        drawRoundRect(
                            color = Color(0xFFE2E8F0),
                            topLeft = Offset(padLeft, padTop),
                            size = Size(padWidth, padHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(padLeft + 1.dp.toPx(), padTop + 1.dp.toPx()),
                            size = Size(padWidth - 2.dp.toPx(), padHeight - 2.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                        )
                        // Notepad lines
                        val lineSpacing = padHeight / 5f
                        for (i in 1..3) {
                            val y = padTop + lineSpacing * i
                            drawLine(
                                color = Color(0xFF64748B),
                                start = Offset(padLeft + 4.dp.toPx(), y),
                                end = Offset(padLeft + padWidth - 4.dp.toPx(), y),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // Pencil tilted diagonally
                        val pencilPath = Path().apply {
                            moveTo(size.width * 0.85f, size.height * 0.35f)
                            lineTo(size.width * 0.50f, size.height * 0.85f)
                            lineTo(size.width * 0.40f, size.height * 0.90f)
                            lineTo(size.width * 0.45f, size.height * 0.80f)
                            close()
                        }
                        drawPath(pencilPath, color = Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}

/**
 * Step 4 & 5: Merging Progress BottomSheet (Image 52.png)
 */
@Composable
private fun MergingProgressBottomSheet(
    progress: Int
) {
    Dialog(
        onDismissRequest = { /* Cannot cancel while merging */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Text(
                            text = "Merging your file",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Please wait a minute...",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Native Ad container
                        NativeAdCardWrapper()
                    }
                }

                // Floating circular progress ring badge at top right (Image 52.png)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp)
                        .size(68.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(52.dp)) {
                        val strokeWidth = 4.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Light background track
                        drawArc(
                            color = Color(0xFFFFEBEE),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth)
                        )

                        // Active red progress arc
                        val sweep = (progress / 100f) * 360f
                        drawArc(
                            color = ProgressRed,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    Text(
                        text = "$progress%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkTextColor
                    )
                }
            }
        }
    }
}

/**
 * Step 6: Merged Successfully Screen (Image 53.png)
 */
@Composable
private fun MergedSuccessContent(
    state: MergePdfState,
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Title
        Text(
            text = "Merged Successfully!",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = DarkTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Confetti explosion + Red Checkmark Badge
        Box(
            modifier = Modifier
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Confetti canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val confettiColors = listOf(
                    Color(0xFF3B82F6), // blue
                    Color(0xFFEC4899), // pink
                    Color(0xFFF59E0B), // gold
                    Color(0xFF10B981), // green
                    Color(0xFF06B6D4), // cyan
                    Color(0xFFEF4444)  // red
                )
                val center = Offset(size.width / 2f, size.height / 2f)

                // 24 decorative confetti dots & ribbons around the center
                val angles = listOf(
                    15f, 35f, 60f, 85f, 110f, 135f, 160f, 185f,
                    210f, 235f, 260f, 285f, 310f, 335f, 355f, 45f,
                    75f, 120f, 170f, 225f, 270f, 300f, 330f, 195f
                )

                angles.forEachIndexed { idx, angleDeg ->
                    val color = confettiColors[idx % confettiColors.size]
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val dist = 62.dp.toPx() + (idx % 4) * 8.dp.toPx()
                    val x = center.x + (dist * cos(rad)).toFloat()
                    val y = center.y + (dist * sin(rad)).toFloat()

                    if (idx % 3 == 0) {
                        // Ribbon rectangle
                        drawRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(6.dp.toPx(), 3.dp.toPx())
                        )
                    } else {
                        // Confetti dot
                        drawCircle(
                            color = color,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Big red circle badge with checkmark
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ProgressRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Name with Edit/Rename Pencil Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = state.mergedFileName ?: "Merged.pdf",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onRenameClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Rename",
                    tint = DarkTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // File path
        Text(
            text = state.mergedFilePath ?: "",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons Row: Share & Open
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Share button
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = DarkTextColor
                )
            ) {
                Text(
                    text = "Share",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkTextColor
                )
            }

            // Open button
            Button(
                onClick = onOpenClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Open",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Native Ad container (Image 53.png)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            NativeAdCardWrapper()
        }
    }
}

/**
 * Green PDF Document Icon Badge (matching 48.png, 49.png, 50.png)
 */
@Composable
private fun MergePdfDocIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 48.dp)
    ) {
        // Background subtle shadow layer / secondary page
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 44.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE2E8F0))
        )

        // Main green front page
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 44.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF00C853), Color(0xFF00B048))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Folded corner tab at top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp))
                    .background(Color(0xFF81C784))
            )

            // "PDF" label
            Text(
                text = "PDF",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Native Ad Card Wrapper with fallback layout matching Images 52.png & 53.png
 */
@Composable
private fun NativeAdCardWrapper(
    modifier: Modifier = Modifier
) {
    NativeAdCard(modifier = modifier.fillMaxWidth())
}
