package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.organize

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdCard
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val VibrantGreen = Color(0xFF00C853)
private val LightGreenDisabled = Color(0xFFB8EED0)
private val DarkTextColor = Color(0xFF0F172A)
private val SubtitleGray = Color(0xFF94A3B8)
private val ProgressRed = Color(0xFFEF4444)
private val AdGold = Color(0xFFFFC107)
private val BannerBg = Color(0xFFFFEAEA)
private val BannerTextGreen = Color(0xFF00A86B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizePdfScreen(
    filePath: String = "",
    title: String = "",
    viewModel: OrganizePdfViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }

    // External file picker launcher in case user wants to pick files via SAF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val temp = FileUtils.copyUriToTempFile(context, it, "organize_${System.currentTimeMillis()}.pdf")
            if (temp != null) {
                viewModel.selectAndLoadPdf(temp.absolutePath, temp.name)
            }
        }
    }

    // Initialize with passed filePath if available
    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            viewModel.initInitialFile(filePath, title)
        }
    }

    // Display error messages as Toast
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    // Handle system back button properly according to active step
    BackHandler {
        when {
            state.isPreparing -> { /* Preparing in progress, prevent accidental exit */ }
            state.step == OrganizeFlowStep.SUCCESS -> {
                viewModel.resetFlow()
                onNavigateBack()
            }
            state.step == OrganizeFlowStep.REORDER_PAGES -> {
                if (state.initialFilePath.isNotBlank()) {
                    onNavigateBack()
                } else {
                    viewModel.backToSelectFile()
                }
            }
            state.step == OrganizeFlowStep.SELECT_FILE -> {
                if (state.isSearchActive) {
                    viewModel.toggleSearch(false)
                } else {
                    onNavigateBack()
                }
            }
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
                OrganizeFlowStep.SELECT_FILE -> {
                    OrganizeSelectFileContent(
                        state = state,
                        onBackClick = onNavigateBack,
                        onSearchToggle = { viewModel.toggleSearch() },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSelectFile = { doc ->
                            viewModel.selectAndLoadPdf(doc.path, doc.name)
                        },
                        onPickFromStorage = {
                            filePickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
                }

                OrganizeFlowStep.REORDER_PAGES -> {
                    OrganizePagesOrderContent(
                        state = state,
                        onBackClick = {
                            if (state.initialFilePath.isNotBlank()) {
                                onNavigateBack()
                            } else {
                                viewModel.backToSelectFile()
                            }
                        },
                        onMovePage = { from, to -> viewModel.movePage(from, to) },
                        onDeletePage = { index -> viewModel.deletePage(index) },
                        onDoneClick = { viewModel.prepareOrganizedPdf() }
                    )
                }

                OrganizeFlowStep.SUCCESS -> {
                    OrganizeSuccessContent(
                        state = state,
                        onRenameClick = { showRenameDialog = true },
                        onShareClick = {
                            state.outputFilePath?.let { path ->
                                ShareUtils.shareFile(context, path)
                            }
                        },
                        onOpenClick = {
                            state.outputFilePath?.let { path ->
                                onNavigateToViewer(path, state.outputFileName ?: "Organized PDF")
                            }
                        }
                    )
                }
            }

            // Preparing PDF Pages BottomSheet Dialog (Image 72.png)
            if (state.isPreparing) {
                PreparingProgressBottomSheet(
                    progress = state.prepareProgress
                )
            }

            // Rename dialog for output file
            if (showRenameDialog && state.outputFileName != null) {
                var newName by remember { mutableStateOf(state.outputFileName ?: "") }
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text("Rename File", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                                viewModel.renameOutputFile(newName)
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
 * Step 1: Select a file Screen (Image 70.png)
 */
@Composable
private fun OrganizeSelectFileContent(
    state: OrganizePdfState,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectFile: (DocumentItem) -> Unit,
    onPickFromStorage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar matching 70.png
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
                text = "Select a file",
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
            } else if (state.filteredPdfs.isEmpty()) {
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
                    items(state.filteredPdfs, key = { it.path }) { document ->
                        OrganizeSelectFileRow(
                            document = document,
                            onClick = { onSelectFile(document) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single file item row in Select a file list (matching 70.png)
 */
@Composable
private fun OrganizeSelectFileRow(
    document: DocumentItem,
    onClick: () -> Unit
) {
    val baseName = document.name.substringBeforeLast('.', document.name)
    val extension = document.name.substringAfterLast('.', "pdf")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green PDF document badge
        OrganizePdfDocIcon()

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
    }
}

/**
 * Step 2: Pages Order Screen (Image 71.png)
 */
@Composable
private fun OrganizePagesOrderContent(
    state: OrganizePdfState,
    onBackClick: () -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onDoneClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar matching 71.png
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
                text = "Pages Order",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkTextColor,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { /* Search/filter action */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = DarkTextColor
                )
            }
        }

        // Instruction Banner matching 71.png
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BannerBg)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Press and hold images to rearrange",
                color = BannerTextGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        // Pages Grid Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.isLoadingPages) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = VibrantGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading PDF pages...",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else if (state.pages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pages available",
                        fontSize = 15.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                var draggedIndex by remember { mutableStateOf<Int?>(null) }
                var dragOffset by remember { mutableStateOf(Offset.Zero) }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(state.pages, key = { _, item -> item.id }) { index, item ->
                        val isDragging = draggedIndex == index

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        shadowElevation = 12.dp.toPx()
                                    }
                                }
                                .pointerInput(index, state.pages.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount

                                            val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            val thresholdX = size.width * 0.5f
                                            val thresholdY = size.height * 0.5f

                                            // Horizontal swap
                                            if (dragOffset.x > thresholdX && currentIdx % 2 == 0 && currentIdx + 1 < state.pages.size) {
                                                onMovePage(currentIdx, currentIdx + 1)
                                                draggedIndex = currentIdx + 1
                                                dragOffset = Offset(dragOffset.x - size.width, dragOffset.y)
                                            } else if (dragOffset.x < -thresholdX && currentIdx % 2 == 1 && currentIdx - 1 >= 0) {
                                                onMovePage(currentIdx, currentIdx - 1)
                                                draggedIndex = currentIdx - 1
                                                dragOffset = Offset(dragOffset.x + size.width, dragOffset.y)
                                            }

                                            // Vertical swap
                                            if (dragOffset.y > thresholdY && currentIdx + 2 < state.pages.size) {
                                                onMovePage(currentIdx, currentIdx + 2)
                                                draggedIndex = currentIdx + 2
                                                dragOffset = Offset(dragOffset.x, dragOffset.y - size.height)
                                            } else if (dragOffset.y < -thresholdY && currentIdx - 2 >= 0) {
                                                onMovePage(currentIdx, currentIdx - 2)
                                                draggedIndex = currentIdx - 2
                                                dragOffset = Offset(dragOffset.x, dragOffset.y + size.height)
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                        ) {
                            PageGridCard(
                                item = item,
                                displayIndex = index + 1,
                                onDeleteClick = { onDeletePage(index) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom "Done" Button matching 71.png
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
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantGreen,
                        contentColor = Color.White
                    ),
                    enabled = state.pages.isNotEmpty()
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
 * Single Page Card in Grid (matching 71.png)
 */
@Composable
private fun PageGridCard(
    item: OrganizePageItem,
    displayIndex: Int,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Rendered Page Thumbnail
            Image(
                bitmap = item.bitmap.asImageBitmap(),
                contentDescription = "Page $displayIndex",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Top-Right Dark Circle Close Button ('X')
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Page",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom-Left Semi-transparent Dark Badge with Page Number
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$displayIndex",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Step 3: Preparing PDF Pages BottomSheet (Image 72.png)
 */
@Composable
private fun PreparingProgressBottomSheet(
    progress: Int
) {
    Dialog(
        onDismissRequest = { /* Modal while preparing */ },
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
                            text = "Preparing PDF Pages",
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

                // Floating circular progress ring badge at top right (Image 72.png)
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
 * Step 4: Prepared Successfully Screen (Image 73.png)
 */
@Composable
private fun OrganizeSuccessContent(
    state: OrganizePdfState,
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

        // Title matching 73.png
        Text(
            text = "Prepared Successfully!",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = DarkTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Confetti explosion + Red Checkmark Badge
        Box(
            modifier = Modifier.size(200.dp),
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

        // File Name with Edit/Rename Pencil Icon matching 73.png
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = state.outputFileName ?: "Business Plan.pdf",
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

        // File path matching 73.png
        Text(
            text = state.outputFilePath ?: "",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons Row: Share & Open matching 73.png
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

        // Bottom Native Ad container matching 73.png
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
 * Green PDF Document Icon Badge (matching 70.png)
 */
@Composable
private fun OrganizePdfDocIcon(
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
 * Native Ad Card Wrapper with fallback mockup matching Images 72.png & 73.png
 */
@Composable
private fun NativeAdCardWrapper(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Live AdMob Native ad
            NativeAdCard(modifier = Modifier.fillMaxWidth())

            // Aesthetic fallback ad mockup card matching 72.png & 73.png
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Graphic / Juice Image placeholder
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF3366), Color(0xFFFF5E3A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JUICE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Ad text & CTA button
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AdGold)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AD",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Associate with...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DarkTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Partner with KC Overseas Education...",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { /* Ad Click */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Sign Up",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
