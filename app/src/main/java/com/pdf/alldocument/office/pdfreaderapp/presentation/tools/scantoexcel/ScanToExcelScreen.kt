package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.scantoexcel

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.VibrantActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToExcelScreen(onBack: () -> Unit, viewModel: ScanToExcelViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::onImage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan to Excel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card: Pick Image
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileClick { picker.launch("image/*") },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF047857)) // Green gradient for Excel
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Pick table / invoice image", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        Text("Select from Gallery", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }

            if (state.isProcessing) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color(0xFF10B981))
                        Spacer(Modifier.width(12.dp))
                        Text("Extracting table data...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            state.error?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "❌ $it",
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (state.table.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Extracted Data", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                            TextButton(onClick = viewModel::addRow, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                                Text("+ Add Row", fontSize = 12.sp, color = Color(0xFF10B981))
                            }
                        }
                        
                        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            LazyColumn {
                                itemsIndexed(state.table) { r, row ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.background(if (r % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                    ) {
                                        row.forEachIndexed { c, cell ->
                                            BasicTextField(
                                                value = cell,
                                                onValueChange = { viewModel.editCell(r, c, it) },
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .border(0.5.dp, Color(0xFFE2E8F0))
                                                    .padding(8.dp),
                                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E293B))
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteRow(r) }) { 
                                            Icon(Icons.Filled.Delete, "Delete row", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp)) 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        VibrantActionButton(
                            text = "Export XLSX",
                            icon = null,
                            onClick = viewModel::exportXlsx,
                            gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857))
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VibrantActionButton(
                            text = "Export CSV",
                            icon = null,
                            onClick = viewModel::exportCsv,
                            gradientColors = listOf(Color(0xFF64748B), Color(0xFF475569))
                        )
                    }
                }
            }

            state.exported?.let { f ->
                val mime = if (f.extension == "xlsx") "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else "text/csv"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Saved Successfully!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                            Text(f.name, fontSize = 11.sp, color = Color(0xFF047857))
                        }
                        TextButton(onClick = { context.startActivity(FileUtils.openIntent(context, f, mime)) }) { Text("Open", color = Color(0xFF059669)) }
                        TextButton(onClick = { context.startActivity(FileUtils.shareIntent(context, f, mime)) }) { Text("Share", color = Color(0xFF059669)) }
                    }
                }
            }
        }
    }
}
