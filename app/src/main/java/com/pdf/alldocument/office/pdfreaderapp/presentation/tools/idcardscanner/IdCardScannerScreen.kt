package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.idcardscanner

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.common.MimeTypes
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools.VibrantActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCardScannerScreen(onBack: () -> Unit, viewModel: IdCardScannerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (state.step) {
                        IdStep.FRONT -> "Scan FRONT side"
                        IdStep.BACK -> "Scan BACK side"
                        IdStep.PREVIEW -> "Preview & Export"
                    }, fontWeight = FontWeight.Bold)
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (state.step == IdStep.PREVIEW) Color.White else Color.Black, titleContentColor = if (state.step == IdStep.PREVIEW) Color.Black else Color.White, navigationIconContentColor = if (state.step == IdStep.PREVIEW) Color.Black else Color.White)
            )
        },
        containerColor = if (state.step == IdStep.PREVIEW) Color(0xFFF8FAFC) else Color.Black
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.step) {
                IdStep.FRONT, IdStep.BACK -> {
                    if (hasCamera) {
                        CameraCapture(
                            step = state.step,
                            onCaptured = viewModel::onCaptured
                        )
                    } else {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Camera permission required", color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant Permission") }
                        }
                    }
                }
                IdStep.PREVIEW -> PreviewStep(state, viewModel, onShare = {
                    state.pdf?.let { context.startActivity(FileUtils.shareIntent(context, it, MimeTypes.PDF)) }
                }, onOpen = {
                    state.pdf?.let { context.startActivity(FileUtils.openIntent(context, it, MimeTypes.PDF)) }
                })
            }
        }
    }
}

@Composable
private fun CameraCapture(step: IdStep, onCaptured: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )
        IdCardOverlay()
        
        // Step Pill Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                if (step == IdStep.FRONT) "Position the FRONT of card in frame" else "Position the BACK of card in frame",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        // Capture Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.White)
                .tactileClick {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bmp = image.toRotatedBitmap()
                                image.close()
                                onCaptured(bmp.cropToCardRatio())
                            }
                            override fun onError(exception: ImageCaptureException) {}
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(54.dp).clip(CircleShape).border(2.dp, Color.Black, CircleShape))
        }
    }
}

private fun ImageProxy.toRotatedBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val m = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

/** Center-crops to the same 90%-width, 1.586 ratio window shown by the overlay. */
private fun Bitmap.cropToCardRatio(): Bitmap {
    val w = (width * 0.9f).toInt()
    val h = (w / 1.586f).toInt().coerceAtMost(height)
    val x = (width - w) / 2
    val y = (height - h) / 2
    return Bitmap.createBitmap(this, x, y, w, h)
}

@Composable
private fun PreviewStep(state: IdCardUiState, vm: IdCardScannerViewModel, onShare: () -> Unit, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        
        // Filter Chips
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(12.dp)) {
                FilterChip(
                    selected = state.filter == IdFilter.COLOR, 
                    onClick = { vm.setFilter(IdFilter.COLOR) }, 
                    label = { Text("Original Color") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEFF6FF), selectedLabelColor = Color(0xFF2563EB)),
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = state.filter == IdFilter.BLACK_WHITE, 
                    onClick = { vm.setFilter(IdFilter.BLACK_WHITE) }, 
                    label = { Text("B&W Scan") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEFF6FF), selectedLabelColor = Color(0xFF2563EB))
                )
            }
        }
        
        // White A4-like canvas preview
        Surface(
            color = Color.White, 
            shadowElevation = 8.dp, 
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                state.front?.let { 
                    Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                        Image(vm.applyFilter(it, state.filter).asImageBitmap(), "Front", Modifier.fillMaxSize()) 
                        IconButton(
                            onClick = { vm.retake(IdStep.FRONT) },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, "Retake Front", tint = Color(0xFF64748B))
                        }
                    }
                }
                
                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 32.dp))
                
                state.back?.let { 
                    Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                        Image(vm.applyFilter(it, state.filter).asImageBitmap(), "Back", Modifier.fillMaxSize()) 
                        IconButton(
                            onClick = { vm.retake(IdStep.BACK) },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, "Retake Back", tint = Color(0xFF64748B))
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        state.error?.let { 
            Text(it, color = Color(0xFFEF4444), modifier = Modifier.padding(bottom = 8.dp)) 
        }
        
        if (state.pdf == null) {
            VibrantActionButton(
                text = if (state.isExporting) "Exporting..." else "Export as A4 PDF",
                icon = Icons.Default.PictureAsPdf,
                onClick = vm::exportPdf,
                enabled = !state.isExporting,
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)) // Purple
            )
        } else {
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
                        Text("PDF Exported!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF065F46))
                        Text(state.pdf!!.name, fontSize = 11.sp, color = Color(0xFF047857))
                    }
                    IconButton(onClick = onOpen) { Icon(Icons.Default.Visibility, "View", tint = Color(0xFF059669)) }
                    IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share", tint = Color(0xFF059669)) }
                }
            }
        }
    }
}
