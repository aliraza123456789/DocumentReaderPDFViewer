package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.imagetopdf

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.pdf.alldocument.viewer.pdfreaderapp.common.MarginStyle
import com.pdf.alldocument.viewer.pdfreaderapp.common.PageFormat
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.tactileClick
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.pdftools.tools.VibrantActionButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(onBack: () -> Unit, viewModel: ImageToPdfViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Images to PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { pickImages.launch("image/*") }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add images", tint = Color(0xFF6366F1))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            if (state.pages.isNotEmpty()) {
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
                            text = if (state.isConverting) "Converting..." else "Convert to PDF",
                            icon = Icons.Filled.PictureAsPdf,
                            onClick = viewModel::convertToPdf,
                            enabled = !state.isConverting,
                            gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            if (state.pages.isNotEmpty()) {
                OptionsCard(
                    format = state.format, onFormatChange = viewModel::setFormat,
                    margin = state.margin, onMarginChange = viewModel::setMargin,
                    quality = state.quality, onQualityChange = viewModel::setQuality
                )
            }

            if (state.pages.isEmpty()) {
                EmptyState(onPick = { pickImages.launch("image/*") })
            } else {
                val gridState = rememberLazyGridState()
                val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
                    viewModel.reorder(from.index, to.index)
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Long press and drag images to reorder pages.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(state.pages, key = { _, p -> p.id }) { index, page ->
                        ReorderableItem(reorderState, key = page.id) {
                            Box(Modifier.longPressDraggableHandle()) {
                                PageThumbnail(
                                    pageNumber = index + 1,
                                    uri = page.uri,
                                    rotation = page.rotationDegrees,
                                    onRotate = { viewModel.rotatePage(page.id) },
                                    onDelete = { viewModel.removePage(page.id) }
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
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

            state.resultFile?.let { file ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conversion Successful!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                            Text(file.name, fontSize = 11.sp, color = Color(0xFF047857))
                        }
                        TextButton(onClick = { context.startActivity(viewModel.openResultIntent(context)!!) }) { Text("View", color = Color(0xFF059669)) }
                        TextButton(onClick = { context.startActivity(viewModel.shareResultIntent(context)!!) }) { Text("Share", color = Color(0xFF059669)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit) {
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
                .tactileClick(onPick),
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
                                listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Select Images",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap here to select multiple images from your gallery to convert into a single PDF.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    pageNumber: Int,
    uri: android.net.Uri,
    rotation: Float,
    onRotate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = uri,
                contentDescription = "page $pageNumber",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayerRotation(rotation)
            )
            
            // Page Number Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF6366F1).copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "$pageNumber",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            
            // Actions
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                IconButton(onClick = onRotate, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(18.dp)) 
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp)) 
                }
            }
        }
    }
}

private fun Modifier.graphicsLayerRotation(rotation: Float): Modifier = this.rotate(rotation)

@Composable
private fun OptionsCard(
    format: PageFormat, onFormatChange: (PageFormat) -> Unit,
    margin: MarginStyle, onMarginChange: (MarginStyle) -> Unit,
    quality: Int, onQualityChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Page Format", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PageFormat.entries.filter { it != PageFormat.FOUR_BY_SIX }.forEach { fmt ->
                    FilterChip(
                        selected = format == fmt,
                        onClick = { onFormatChange(fmt) },
                        label = { Text(fmt.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEEF2FF),
                            selectedLabelColor = Color(0xFF4F46E5)
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text("Margin", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarginStyle.entries.forEach { m ->
                    FilterChip(
                        selected = margin == m, 
                        onClick = { onMarginChange(m) }, 
                        label = { Text(m.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEEF2FF),
                            selectedLabelColor = Color(0xFF4F46E5)
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Quality", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                Text("$quality%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6366F1))
            }
            Slider(
                value = quality.toFloat(), 
                onValueChange = { onQualityChange(it.toInt()) }, 
                valueRange = 30f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6366F1),
                    activeTrackColor = Color(0xFF6366F1)
                )
            )
        }
    }
}
