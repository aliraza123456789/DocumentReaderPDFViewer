package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.unlock

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick

private val BrandGreen = Color(0xFF22C55E)
private val BrandGreenDark = Color(0xFF16A34A)
private val DarkTextColor = Color(0xFF0F172A)
private val MutedTextColor = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)
private val ExtensionRed = Color(0xFFEF4444)
private val LockBadgeYellow = Color(0xFFF59E0B)
private val ProgressRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockPdfScreen(
    onNavigateBack: () -> Unit = {},
    onBack: () -> Unit = onNavigateBack,
    onNavigateToViewer: (String, String) -> Unit = { _, _ -> },
    viewModel: UnlockPdfViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectCustomUri(it) }
    }

    // System Back Press handling
    BackHandler {
        val handled = viewModel.handleBackPress()
        if (!handled) {
            onBack()
        }
    }

    // Error Toast
    state.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
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
                UnlockFlowStep.SELECT_FILE -> {
                    UnlockSelectFileScreen(
                        state = state,
                        onBack = onBack,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onToggleSearch = { viewModel.toggleSearch(it) },
                        onFileSelected = { viewModel.selectDocument(it) },
                        onOpenStorage = { filePickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                }

                UnlockFlowStep.SUCCESS -> {
                    UnlockSuccessScreen(
                        unlockedFile = state.unlockedFile,
                        onBack = {
                            viewModel.resetFlow()
                            onBack()
                        },
                        onDone = {
                            viewModel.resetFlow()
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

            // Remove Password Bottom Sheet (Matching 69.png)
            if (state.showPasswordSheet) {
                RemovePasswordBottomSheet(
                    password = state.passwordInput,
                    isPasswordVisible = state.isPasswordVisible,
                    onPasswordChange = { viewModel.updatePassword(it) },
                    onToggleVisibility = { viewModel.togglePasswordVisibility() },
                    onDismiss = { viewModel.dismissPasswordSheet() },
                    onDone = { viewModel.executeUnlock() }
                )
            }

            // Unlocking Progress Dialog
            if (state.isUnlocking) {
                UnlockingProgressDialog(progress = state.unlockProgress)
            }
        }
    }
}

/* =========================================================================================
 * 1. SELECT A FILE SCREEN (Matching 68.png & 69.png background)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlockSelectFileScreen(
    state: UnlockPdfUiState,
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
                        text = "Pick a password-protected PDF from your files",
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
                    LockedPdfFoldedIcon(size = 64.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (state.searchQuery.isBlank()) "No locked PDF documents found" else "No matching locked PDFs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Only password-protected PDFs appear here. You can lock a PDF using the 'Lock PDF' tool or browse a locked file from your device.",
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
                    val isSelected = state.selectedDoc?.path == doc.path
                    LockedPdfFileRow(
                        doc = doc,
                        isSelected = isSelected,
                        showRadio = state.showPasswordSheet,
                        onClick = { onFileSelected(doc) }
                    )
                }
            }
        }
    }
}

/* =========================================================================================
 * LOCKED PDF FILE ROW (Matching 68.png / 69.png)
 * ========================================================================================= */
@Composable
private fun LockedPdfFileRow(
    doc: DocumentItem,
    isSelected: Boolean,
    showRadio: Boolean,
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
        // Locked Folded Icon
        LockedPdfFoldedIcon(size = 48.dp)

        Spacer(modifier = Modifier.width(14.dp))

        // File Title & Subtitle
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
                    color = ExtensionRed
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${doc.formattedDate} . ${doc.formattedSize}",
                fontSize = 13.sp,
                color = MutedTextColor
            )
        }

        // Radio Button on the right when bottom sheet / selection is active (Matching 69.png)
        if (showRadio) {
            Spacer(modifier = Modifier.width(12.dp))
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ExtensionRed),
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
}

/* =========================================================================================
 * LOCKED PDF FOLDED ICON (Matches the 68.png / 69.png list icons)
 * Slate folded badge with "LOCK" text + subtle padlock
 * ========================================================================================= */
@Composable
fun LockedPdfFoldedIcon(size: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Background white/gray page with folded top-right corner
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

            // Main Dark Slate / Navy Badge in front
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(0f, h * 0.12f),
                size = Size(w * 0.80f, h * 0.84f),
                cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
            )
        }

        // Content on the badge: "LOCK" text + Padlock emblem
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = (size.value * 0.14).dp, top = (size.value * 0.08).dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOCK",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.20).sp,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size((size.value * 0.28).dp)
            )
        }
    }
}

/* =========================================================================================
 * 2. REMOVE PASSWORD BOTTOM SHEET (Matching 69.png)
 * Rounded bottom sheet with protruding Lock icon, title, description,
 * password input with show/hide toggle, Cancel and Done buttons
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovePasswordBottomSheet(
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // White card sheet container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
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
                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = "Remove Password",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = "The PDF will no longer be password protected",
                        fontSize = 15.sp,
                        color = MutedTextColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password input field (rounded gray surface with eye toggle)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
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
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    if (password.isNotBlank()) onDone()
                                }),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkTextColor
                                ),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (password.isEmpty()) {
                                            Text(
                                                text = "Enter password",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            IconButton(
                                onClick = onToggleVisibility,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action buttons: Cancel (Outline) & Done (Solid Green)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DarkTextColor
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Done Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onDone()
                            },
                            enabled = password.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                contentColor = Color.White,
                                disabledContainerColor = BrandGreen.copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = "Done",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Protruding Padlock Icon (Overlapping the top edge of the sheet, matching 69.png)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 40.dp)
                    .size(80.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Gold / Amber Lock badge with keyhole
                PadlockEmblem(size = 56.dp)
            }
        }
    }
}

/* =========================================================================================
 * PADLOCK EMBLEM (Golden lock with keyhole matching 69.png)
 * ========================================================================================= */
@Composable
fun PadlockEmblem(size: androidx.compose.ui.unit.Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(Color(0xFFF59E0B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Keyhole circle & bar
            Box(
                modifier = Modifier
                    .size(size * 0.22f)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            )
            Box(
                modifier = Modifier
                    .width(size * 0.10f)
                    .height(size * 0.18f)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(Color(0xFF1E293B))
            )
        }
    }
}

/* =========================================================================================
 * 3. UNLOCKING PROGRESS DIALOG (Bottom sheet style matching app tools)
 * ========================================================================================= */
@Composable
private fun UnlockingProgressDialog(progress: Int) {
    Dialog(
        onDismissRequest = { /* Block dismiss while unlocking */ },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Bottom white sheet
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
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Unlocking your PDF",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Removing password protection...",
                            fontSize = 15.sp,
                            color = MutedTextColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            NativeAdCard()
                        }
                    }
                }

                // Overlapping Circular Progress Indicator
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
                        drawCircle(
                            color = Color(0xFFF1F5F9),
                            style = Stroke(width = stroke)
                        )
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
 * 4. SUCCESS SCREEN
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlockSuccessScreen(
    unlockedFile: java.io.File?,
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
        TopAppBar(
            title = {
                Text(
                    text = "Unlock PDF",
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

            // Success Badge
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
                text = "PDF Unlocked Successfully!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Password protection removed. Your file is ready to view.",
                fontSize = 14.sp,
                color = MutedTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // File Card
            if (unlockedFile != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderGray),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LockedPdfFoldedIcon(size = 46.dp)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = unlockedFile.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${unlockedFile.length() / 1024} KB",
                                fontSize = 12.sp,
                                color = MutedTextColor
                            )
                        }

                        // Open button
                        FilledTonalButton(
                            onClick = { onOpen(unlockedFile.absolutePath, unlockedFile.name) },
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

                        // Share button
                        IconButton(
                            onClick = { onShare(unlockedFile.absolutePath) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Native Ad
            Box(modifier = Modifier.fillMaxWidth()) {
                NativeAdCard()
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
