package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.idphotomaker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.VibrantActionButton

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun IdPhotoScreen(onBack: () -> Unit, viewModel: IdPhotoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::onImagePicked) }

    val preview = remember(state.source, state.size, state.background, state.zoom, state.offsetX, state.offsetY) {
        viewModel.renderSinglePhoto(state)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ID Photo Maker", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card for picking photo
            if (state.source == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileClick { picker.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
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
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFE11D48), Color(0xFFBE123C)) // Rose gradient
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Select a Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose a clear portrait photo to create professional ID/Passport size photos.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileClick { picker.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, tint = Color(0xFFE11D48))
                        Spacer(Modifier.width(12.dp))
                        Text("Change Photo", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }
                }
            }

            preview?.let {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        ) {
                            Image(it.asImageBitmap(), "Preview", Modifier.height(240.dp).padding(4.dp))
                        }
                        
                        HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Zoom", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                                Text("${"%.1f".format(state.zoom)}x", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE11D48))
                            }
                            Slider(state.zoom, viewModel::setZoom, valueRange = 1f..3f, colors = SliderDefaults.colors(thumbColor = Color(0xFFE11D48), activeTrackColor = Color(0xFFE11D48)))
                            
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Horizontal Position", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                            }
                            Slider(state.offsetX, { viewModel.setOffset(it, state.offsetY) }, valueRange = -1f..1f, colors = SliderDefaults.colors(thumbColor = Color(0xFFE11D48), activeTrackColor = Color(0xFFE11D48)))
                            
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Vertical Position", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                            }
                            Slider(state.offsetY, { viewModel.setOffset(state.offsetX, it) }, valueRange = -1f..1f, colors = SliderDefaults.colors(thumbColor = Color(0xFFE11D48), activeTrackColor = Color(0xFFE11D48)))
                        }
                    }
                }
            }

            if (state.source != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Photo Size", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhotoSize.entries.forEach { s ->
                                FilterChip(
                                    selected = state.size == s, 
                                    onClick = { viewModel.setSize(s) }, 
                                    label = { Text(s.label) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF1F2), selectedLabelColor = Color(0xFFE11D48))
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Background Color", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhotoBackground.entries.forEach { b ->
                                FilterChip(
                                    selected = state.background == b, 
                                    onClick = { viewModel.setBackground(b) }, 
                                    label = { Text(b.label) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF1F2), selectedLabelColor = Color(0xFFE11D48))
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Print Sheet Setup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(6, 8, 12).forEach { n ->
                                FilterChip(
                                    selected = state.copies == n, 
                                    onClick = { viewModel.setCopies(n) }, 
                                    label = { Text("$n copies") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF1F2), selectedLabelColor = Color(0xFFE11D48))
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            SheetPaper.entries.forEach { p ->
                                FilterChip(
                                    selected = state.paper == p, 
                                    onClick = { viewModel.setPaper(p) }, 
                                    label = { Text(p.label) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF1F2), selectedLabelColor = Color(0xFFE11D48))
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        VibrantActionButton(
                            text = if(state.isExporting) "Saving..." else "Save JPEG",
                            icon = null,
                            onClick = viewModel::exportJpeg,
                            enabled = !state.isExporting,
                            gradientColors = listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VibrantActionButton(
                            text = if(state.isExporting) "Saving..." else "Save PDF",
                            icon = null,
                            onClick = viewModel::exportSheetPdf,
                            enabled = !state.isExporting,
                            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                        )
                    }
                }
            }
            
            state.error?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, color = Color(0xFFB91C1C), modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            state.output?.let { f ->
                val mime = if (f.extension == "pdf") "application/pdf" else "image/jpeg"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Saved successfully!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF065F46))
                            Text(f.name, fontSize = 11.sp, color = Color(0xFF047857))
                        }
                        TextButton(onClick = { context.startActivity(FileUtils.openIntent(context, f, mime)) }) { Text("View", color = Color(0xFF059669)) }
                        TextButton(onClick = { context.startActivity(FileUtils.shareIntent(context, f, mime)) }) { Text("Share", color = Color(0xFF059669)) }
                    }
                }
            }
        }
    }
}
