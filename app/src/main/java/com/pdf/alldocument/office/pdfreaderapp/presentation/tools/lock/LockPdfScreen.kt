package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.lock

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.ads.NativeAdCard
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Exact mockup color constants
private val BrandGreen = Color(0xFF22C55E)
private val BrandGreenDark = Color(0xFF16A34A)
private val DisabledGreen = Color(0xFFA7F3D0)
private val DarkTextColor = Color(0xFF0F172A)
private val MutedTextColor = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockPdfScreen(
    onNavigateBack: () -> Unit = {},
    onBack: () -> Unit = onNavigateBack,
    onNavigateToViewer: (String, String) -> Unit = { _, _ -> },
    viewModel: LockPdfViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val temp = FileUtils.copyUriToTempFile(context, it, "lock_${System.currentTimeMillis()}.pdf")
            if (temp != null) {
                viewModel.selectCustomFile(temp.absolutePath, temp.name)
            }
        }
    }

    // Handle back button according to flow state
    BackHandler {
        val handled = viewModel.handleBackPress()
        if (!handled) {
            onBack()
        }
    }

    // Display error messages as Toast
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
                LockFlowStep.SELECT_FILE -> {
                    // SCREEN 1: Select a File (66.png & 67.png background)
                    SelectFileLockScreen(
                        state = state,
                        onBack = {
                            val handled = viewModel.handleBackPress()
                            if (!handled) onBack()
                        },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onToggleSearch = { viewModel.toggleSearch(it) },
                        onFileSelected = { doc -> viewModel.selectPdf(doc) },
                        onOpenStorage = { filePickerLauncher.launch(arrayOf("application/pdf")) }
                    )

                    // SCREEN 2: Lock PDF Enter Password Bottom Sheet (67.png modal overlay)
                    if (state.showPasswordDialog) {
                        EnterPasswordBottomSheet(
                            password = state.passwordInput,
                            isPasswordVisible = state.isPasswordVisible,
                            onPasswordChange = { viewModel.setPassword(it) },
                            onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
                            onDismiss = { viewModel.dismissPasswordDialog() },
                            onDone = { viewModel.lockSelectedPdf() }
                        )
                    }

                    // Locking In Progress Dialog
                    if (state.isLocking) {
                        LockingProgressDialog()
                    }
                }

                LockFlowStep.SUCCESS -> {
                    // SCREEN 3: Success Screen
                    LockSuccessScreen(
                        filePath = state.lockedResultPath.orEmpty(),
                        fileName = state.lockedResultName.orEmpty(),
                        fileSize = state.lockedResultSize.orEmpty(),
                        onBack = {
                            viewModel.resetFlow()
                            onBack()
                        },
                        onDone = {
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
 * 1. SCREEN 1: SELECT A FILE (Matches 66.png & 67.png background)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectFileLockScreen(
    state: LockPdfUiState,
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
                        text = if (state.showPasswordDialog) "0 Selected" else "Select a file",
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

        // PDF Documents list
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
                    val isSelected = state.showPasswordDialog && (doc.path == state.selectedDoc?.path)
                    LockPdfFileRow(
                        doc = doc,
                        showRadio = state.showPasswordDialog,
                        isSelected = isSelected,
                        onClick = { onFileSelected(doc) }
                    )
                }
            }
        }
    }
}

/**
 * PDF Item row matching 66.png & 67.png
 */
@Composable
private fun LockPdfFileRow(
    doc: DocumentItem,
    showRadio: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val baseName = doc.name.substringBeforeLast('.', doc.name)
    val extension = doc.name.substringAfterLast('.', "pdf")
    val formattedDateStr = remember(doc.dateModified) {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        sdf.format(Date(doc.dateModified))
    }

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
                text = "$formattedDateStr . ${doc.formattedSize}",
                fontSize = 13.sp,
                color = MutedTextColor
            )
        }

        // Selection Radio/Check icon (displayed when in selection dialog mode matching 67.png)
        if (showRadio) {
            Spacer(modifier = Modifier.width(12.dp))
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
                        .clip(CircleShape)
                        .border(BorderStroke(1.5.dp, Color(0xFFCBD5E1)), CircleShape)
                )
            }
        }
    }
}

/* =========================================================================================
 * 2. SCREEN 2: ENTER PASSWORD BOTTOM SHEET MODAL (Exact match to 67.png)
 * ========================================================================================= */
@Composable
private fun EnterPasswordBottomSheet(
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dimmed Scrim Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Bottom Sheet Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // White Dialog Card with rounded top corners
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 38.dp) // Half clearance for overlapping floating lock badge
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks inside modal
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(30.dp)) // Space for overlapping lock badge

                Text(
                    text = "Enter password",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Set a password to protect the PDF",
                    fontSize = 14.sp,
                    color = MutedTextColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Password Input Field
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkTextColor
                            ),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation('*'),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (password.isEmpty()) {
                                        Text(
                                            text = "••••••••••••",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 16.sp,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        IconButton(
                            onClick = onTogglePasswordVisibility,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel and Done Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cancel Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .tactileClick(onDismiss),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextColor
                            )
                        }
                    }

                    // Done Button
                    val isDoneEnabled = password.isNotBlank()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(if (isDoneEnabled) Modifier.tactileClick(onDone) else Modifier),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDoneEnabled) BrandGreen else DisabledGreen
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Done",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
            }

            // Floating Lock Badge aligned to TopCenter (matching 67.png)
            LockFloatingBadge(
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * Floating Circular Lock Badge matching 67.png
 */
@Composable
private fun LockFloatingBadge(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size * 0.58f)) {
                val w = this.size.width
                val h = this.size.height

                // Shackle (inverted U) in dark slate blue
                val shackleColor = Color(0xFF28567A)
                val shackleStroke = w * 0.14f
                val shackleLeft = w * 0.24f
                val shackleRight = w * 0.76f
                val shackleTop = h * 0.04f
                val shackleBottom = h * 0.50f

                val shacklePath = Path().apply {
                    moveTo(shackleLeft, shackleBottom)
                    lineTo(shackleLeft, shackleTop + (shackleRight - shackleLeft) / 2)
                    arcTo(
                        rect = Rect(
                            left = shackleLeft,
                            top = shackleTop,
                            right = shackleRight,
                            bottom = shackleTop + (shackleRight - shackleLeft)
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    lineTo(shackleRight, shackleBottom)
                }
                drawPath(
                    path = shacklePath,
                    color = shackleColor,
                    style = Stroke(width = shackleStroke, cap = StrokeCap.Round)
                )

                // Body (rounded rectangle) in warm orange / golden amber
                val bodyColor = Color(0xFFF5A623)
                val bodyTop = h * 0.40f
                val bodyHeight = h * 0.58f
                val bodyWidth = w * 0.88f
                val bodyLeft = (w - bodyWidth) / 2f
                val cornerRadius = w * 0.12f

                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )

                // Keyhole in dark slate
                val keyholeColor = Color(0xFF0F172A)
                val keyholeCenterY = bodyTop + bodyHeight * 0.38f
                val keyholeCircleRadius = w * 0.08f
                drawCircle(
                    color = keyholeColor,
                    radius = keyholeCircleRadius,
                    center = Offset(w / 2f, keyholeCenterY)
                )

                // Keyhole stem / slot
                val stemWidth = w * 0.08f
                val stemHeight = bodyHeight * 0.30f
                val stemPath = Path().apply {
                    moveTo(w / 2f - stemWidth / 2f, keyholeCenterY)
                    lineTo(w / 2f - stemWidth * 0.65f, keyholeCenterY + stemHeight)
                    lineTo(w / 2f + stemWidth * 0.65f, keyholeCenterY + stemHeight)
                    lineTo(w / 2f + stemWidth / 2f, keyholeCenterY)
                    close()
                }
                drawPath(stemPath, color = keyholeColor)
            }
        }
    }
}

/**
 * Locking in progress dialog
 */
@Composable
private fun LockingProgressDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = BrandGreen,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Encrypting PDF...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DarkTextColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Securing your document with password",
                    fontSize = 13.sp,
                    color = MutedTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* =========================================================================================
 * 3. SCREEN 3: SUCCESS VIEW
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockSuccessScreen(
    filePath: String,
    fileName: String,
    fileSize: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpen: (String, String) -> Unit,
    onShare: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Password Protected",
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
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Large Success Check badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(BrandGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BrandGreen),
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PDF Locked Successfully!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = DarkTextColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your document is now password protected",
                fontSize = 14.sp,
                color = MutedTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // File Information Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PdfFoldedIcon(size = 46.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Password Protected . $fileSize",
                                fontSize = 12.sp,
                                color = BrandGreenDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // "Open PDF" Action Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tactileClick { onOpen(filePath, fileName) },
                shape = RoundedCornerShape(12.dp),
                color = BrandGreen
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Share Document" Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tactileClick { onShare(filePath) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = DarkTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share PDF",
                        color = DarkTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Open in External App" Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tactileClick {
                        try {
                            val intent = FileUtils.openIntent(context, java.io.File(filePath), "application/pdf")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No external PDF app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = DarkTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open in External App",
                        color = DarkTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Done" Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tactileClick(onDone),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        color = DarkTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Native Ad Card at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                NativeAdCard()
            }
        }
    }
}

/**
 * Green Folded PDF Icon matching 66.png & 67.png
 */
@Composable
private fun PdfFoldedIcon(size: Dp = 46.dp) {
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
