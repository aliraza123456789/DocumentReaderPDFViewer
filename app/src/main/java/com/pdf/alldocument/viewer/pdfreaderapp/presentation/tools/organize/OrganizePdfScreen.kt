package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.organize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem.LottieLoadingIndicator
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.tactileClick
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.pdftools.tools.VibrantActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizePdfScreen(
    filePath: String,
    title: String,
    viewModel: OrganizePdfViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val temp = FileUtils.copyUriToTempFile(context, it, "organize_${System.currentTimeMillis()}.pdf")
            if (temp != null) {
                viewModel.loadPdf(temp.absolutePath, temp.name)
            }
        }
    }

    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            viewModel.loadPdf(filePath, title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organize Pages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.pages.isNotEmpty()) {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = "Export", tint = Color(0xFF10B981))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            if (state.pages.isNotEmpty()) {
                val selectedCount = state.pages.count { it.isSelected }
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Pages",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedCount > 0) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (selectedCount > 0) Color(0xFFA7F3D0) else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = "$selectedCount of ${state.pages.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selectedCount > 0) Color(0xFF059669) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        VibrantActionButton(
                            text = "Export $selectedCount Pages",
                            icon = Icons.Default.Save,
                            onClick = { showExportDialog = true },
                            enabled = selectedCount > 0,
                            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB)) // Blue gradient
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading || state.isExporting) {
                LottieLoadingIndicator(
                    rawRes = "ad_loading.json",
                    message = if (state.isExporting) "Generating organized PDF..." else "Loading PDF pages...",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.pages.isEmpty()) {
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
                                            listOf(Color(0xFF3B82F6), Color(0xFF2563EB)) // Blue gradient
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ViewAgenda, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Organize & Split PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Extract, rearrange, or delete individual pages from any PDF document to create a new one.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(state.pages) { index, item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .tactileClick { viewModel.togglePageSelection(index) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.isSelected) Color.White else Color(0xFFF1F5F9),
                            border = BorderStroke(2.dp, if (item.isSelected) Color(0xFF3B82F6) else Color.Transparent),
                            shadowElevation = if (item.isSelected) 4.dp else 0.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = item.bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = if (item.isSelected) 1.0f else 0.5f
                                )

                                // Checkbox overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isSelected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.8f))
                                        .border(1.dp, if (item.isSelected) Color.Transparent else Color(0xFF94A3B8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Page Number badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(100.dp)) // padding for bottom bar
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            var fileName by remember { mutableStateOf("Organized_${System.currentTimeMillis()}") }
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Document", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Output PDF Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportOrganizedPdf(fileName)
                        },
                        enabled = fileName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                containerColor = Color.White
            )
        }

        // Success Dialog
        state.exportedFilePath?.let { path ->
            AlertDialog(
                onDismissRequest = { viewModel.clearResult() },
                title = { Text("Export Successful! 📄", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = { Text("Your new PDF is saved.", color = Color(0xFF475569)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearResult()
                            onNavigateToViewer(path, "Organized PDF")
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
