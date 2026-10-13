package com.pdf.alldocument.office.pdfreaderapp.presentation.viewer.pdf

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    title: String,
    lastReadPage: Int = 0,
    viewModel: PdfViewerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showJumpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        viewModel.initialize(filePath, title, lastReadPage)
    }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = lastReadPage)

    // Pinch-to-zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Night mode color filter
    val nightModeTint = if (state.isNightMode) Color(0xFFD0D0D0) else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.title.ifEmpty { title },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (state.totalPages > 0) {
                            Text(
                                text = "Page ${state.currentPage + 1} / ${state.totalPages}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleNightMode() }) {
                        Icon(
                            if (state.isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Night Mode"
                        )
                    }
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Jump to Page")
                    }
                    IconButton(onClick = { ShareUtils.shareFile(context, filePath) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { ShareUtils.printPdf(context, filePath, title) }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (state.isNightMode) Color(0xFF121212) else Color(0xFFF0F0F0))
        ) {
            if (state.isLoading && state.pages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1.0f, 5.0f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        count = state.pages.size,
                        key = { index -> "page_$index" }
                    ) { index ->
                        val page = state.pages.getOrNull(index)
                        if (page != null && page.cachedFilePath.isNotEmpty()) {
                            val imageFile = File(page.cachedFilePath)
                            if (imageFile.exists()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = nightModeTint)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageFile)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            } else {
                                PagePlaceholder(index)
                            }
                        } else {
                            PagePlaceholder(index)
                        }
                    }
                }

                // Track current page based on scroll
                LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                    viewModel.onPageChanged(lazyListState.firstVisibleItemIndex)
                }
            }
        }

        // Jump to Page Dialog
        if (showJumpDialog) {
            JumpToPageDialog(
                totalPages = state.totalPages,
                currentPage = state.currentPage + 1,
                onDismiss = { showJumpDialog = false },
                onConfirm = { pageNumber ->
                    showJumpDialog = false
                    viewModel.jumpToPage(pageNumber - 1)
                }
            )
        }

        // Password Protected Dialog
        if (state.isPasswordProtected) {
            EnterViewerPasswordDialog(
                errorMessage = state.passwordError,
                onDismiss = onBack,
                onUnlock = { pass -> viewModel.unlockWithPassword(pass) }
            )
        }
    }
}

@Composable
private fun EnterViewerPasswordDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Password Protected",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "This PDF document is encrypted. Enter password to view its contents.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide" else "Show"
                            )
                        }
                    },
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUnlock(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PagePlaceholder(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun JumpToPageDialog(
    totalPages: Int,
    currentPage: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pageText by remember { mutableStateOf(currentPage.toString()) }
    val pageNumber = pageText.toIntOrNull()
    val isValid = pageNumber != null && pageNumber in 1..totalPages

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to Page", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter page number (1 – $totalPages)")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it.filter { c -> c.isDigit() } },
                    label = { Text("Page") },
                    singleLine = true,
                    isError = !isValid && pageText.isNotEmpty()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { pageNumber?.let { onConfirm(it) } },
                enabled = isValid
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
