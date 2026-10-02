package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.scantopdf

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.tactileClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToPdfScreen(onBack: () -> Unit, viewModel: ScanToPdfViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            viewModel.onScanResult(scanResult?.pdf?.uri)
        } else {
            viewModel.onScanCancelledOrFailed()
        }
    }

    fun startScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(50)
            .setResultFormats(RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { sender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener { viewModel.onScanCancelledOrFailed(it.message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan to PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileClick(::startScan),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        } else {
                            Icon(Icons.Filled.DocumentScanner, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Smart Document Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Auto edge detection, smart cropping, shadow removal, and multi-page PDF generation.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = ::startScan,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        if (state.isLoading) {
                            Text("Initializing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start Scanning", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            
            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, color = Color(0xFFB91C1C), modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            state.lastScan?.let { file ->
                Spacer(Modifier.height(20.dp))
                Text("Just Scanned", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                ScanRow(
                    name = file.name,
                    onView = { context.startActivity(viewModel.openIntent(file)) },
                    onShare = { context.startActivity(viewModel.shareIntent(file)) },
                    isLatest = true
                )
            }

            if (state.recentScans.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Recent Scans", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recentScans) { file ->
                        ScanRow(
                            name = file.name,
                            onView = { context.startActivity(viewModel.openIntent(file)) },
                            onShare = { context.startActivity(viewModel.shareIntent(file)) },
                            isLatest = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanRow(name: String, onView: () -> Unit, onShare: () -> Unit, isLatest: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLatest) Color(0xFFEFF6FF) else Color.White,
        border = BorderStroke(1.dp, if (isLatest) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLatest) Color(0xFF3B82F6).copy(alpha = 0.1f) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, tint = if (isLatest) Color(0xFF2563EB) else Color(0xFF64748B), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = name, 
                modifier = Modifier.weight(1f), 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onView, modifier = Modifier.size(36.dp)) { 
                    Icon(Icons.Filled.Visibility, "View", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)) 
                }
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) { 
                    Icon(Icons.Filled.Share, "Share", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)) 
                }
            }
        }
    }
}
