package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.merge

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem.LottieLoadingIndicator
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.tactileClick
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.pdftools.tools.VibrantActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    viewModel: MergePdfViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFileNameDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedUris(context, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            if (state.selectedFiles.size >= 2) {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        VibrantActionButton(
                            text = "Merge ${state.selectedFiles.size} PDFs",
                            icon = Icons.AutoMirrored.Filled.CallMerge,
                            onClick = { showFileNameDialog = true },
                            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)) // Purple gradient
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.selectedFiles.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add PDF")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isMerging) {
                LottieLoadingIndicator(
                    rawRes = "ad_loading.json",
                    message = "Merging PDF files, please wait...",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.selectedFiles.isEmpty()) {
                // Empty state card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactileClick { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CallMerge, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Select PDFs to Merge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap here to select 2 or more PDF files you want to combine into a single document.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Use arrows to reorder merge priority. Files merge from top to bottom.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                        }
                    }

                    itemsIndexed(state.selectedFiles, key = { _, item -> item.id }) { index, file ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = file.formattedSize,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.moveFile(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = if (index > 0) Color(0xFF64748B) else Color(0xFFCBD5E1))
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveFile(index, index + 1) },
                                        enabled = index < state.selectedFiles.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = if (index < state.selectedFiles.size - 1) Color(0xFF64748B) else Color(0xFFCBD5E1))
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeFile(file) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(72.dp)) // padding for bottom bar
                    }
                }
            }
        }

        // File name dialog
        if (showFileNameDialog) {
            var fileName by remember { mutableStateOf("Merged_${System.currentTimeMillis()}") }
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                title = { Text("Output PDF Name", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFileNameDialog = false
                            viewModel.mergePdfs(fileName)
                        },
                        enabled = fileName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Merge")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFileNameDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                containerColor = Color.White
            )
        }

        // Merge Success Dialog
        state.mergedFilePath?.let { path ->
            AlertDialog(
                onDismissRequest = { viewModel.clearResult() },
                title = { Text("Merge Complete! 🎉", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = { Text("Your merged PDF is ready.", color = Color(0xFF475569)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearResult()
                            onNavigateToViewer(path, "Merged PDF")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Open File")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            ShareUtils.shareFile(context, path)
                            viewModel.clearResult()
                        }
                    ) {
                        Text("Share", color = Color(0xFF059669))
                    }
                },
                containerColor = Color.White
            )
        }
    }
}
