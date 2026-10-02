package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.sign

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.viewer.pdfreaderapp.core.designsystem.LottieLoadingIndicator
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.tactileClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    filePath: String,
    title: String,
    viewModel: SignPdfViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSignaturePad by remember { mutableStateOf(false) }
    var showFileNameDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val temp = FileUtils.copyUriToTempFile(context, it, "sign_${System.currentTimeMillis()}.pdf")
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
                title = { Text("Sign PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.pageBitmaps.isNotEmpty()) {
                        IconButton(onClick = { showSignaturePad = true }) {
                            Icon(Icons.Default.Draw, contentDescription = "Add Signature", tint = Color(0xFFF59E0B))
                        }
                        IconButton(onClick = { showFileNameDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Signed PDF", tint = Color(0xFF10B981))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            if (state.pageBitmaps.size > 1) {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setPageIndex(state.currentPageIndex - 1) },
                            enabled = state.currentPageIndex > 0,
                            modifier = Modifier.background(if(state.currentPageIndex > 0) Color(0xFFF1F5F9) else Color.Transparent, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                        }
                        Text(
                            text = "Page ${state.currentPageIndex + 1} of ${state.pageBitmaps.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        IconButton(
                            onClick = { viewModel.setPageIndex(state.currentPageIndex + 1) },
                            enabled = state.currentPageIndex < state.pageBitmaps.size - 1,
                            modifier = Modifier.background(if(state.currentPageIndex < state.pageBitmaps.size - 1) Color(0xFFF1F5F9) else Color.Transparent, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }
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
                    message = if (state.isExporting) "Saving signed document..." else "Rendering PDF pages...",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.pageBitmaps.isEmpty()) {
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
                                            listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Orange for signature
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Draw, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Select a PDF to sign",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Draw your digital signature and position it anywhere on the document.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // PDF Page Canvas Preview with draggable signature overlay
                val currentPageBitmap = state.pageBitmaps.getOrNull(state.currentPageIndex)
                if (currentPageBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .aspectRatio(currentPageBitmap.width.toFloat() / currentPageBitmap.height.toFloat()),
                            shape = RoundedCornerShape(4.dp),
                            shadowElevation = 8.dp,
                            color = Color.White
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = currentPageBitmap.asImageBitmap(),
                                    contentDescription = "PDF Page",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )

                                // Signature Overlay
                                state.signatureBitmap?.let { sigBmp ->
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(state.sigPosX.toInt(), state.sigPosY.toInt()) }
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    viewModel.updateSigPosition(dragAmount.x, dragAmount.y)
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, _, zoom, _ ->
                                                    viewModel.updateSigScale(zoom)
                                                }
                                            }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.85f))
                                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(4.dp)
                                    ) {
                                        Image(
                                            bitmap = sigBmp.asImageBitmap(),
                                            contentDescription = "Signature",
                                            modifier = Modifier.size(
                                                (140 * state.sigScale).dp,
                                                (70 * state.sigScale).dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (state.signatureBitmap == null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Draw, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Tap the draw icon above to create your signature.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Draw Signature Dialog
        if (showSignaturePad) {
            SignatureDrawingDialog(
                onDismiss = { showSignaturePad = false },
                onSignatureCaptured = { bmp ->
                    viewModel.setSignature(bmp)
                    showSignaturePad = false
                }
            )
        }

        // Save Dialog
        if (showFileNameDialog) {
            var fileName by remember { mutableStateOf("Signed_${System.currentTimeMillis()}") }
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                title = { Text("Save Signed Document", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                            viewModel.exportSignedPdf(fileName)
                        },
                        enabled = fileName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Export")
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

        // Success Dialog
        state.exportedFilePath?.let { path ->
            AlertDialog(
                onDismissRequest = { viewModel.clearResult() },
                title = { Text("Signature Applied! ✍️", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = { Text("Your signed document is ready.", color = Color(0xFF475569)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearResult()
                            onNavigateToViewer(path, "Signed Document")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Open Document")
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

@Composable
fun SignatureDrawingDialog(
    onDismiss: () -> Unit,
    onSignatureCaptured: (Bitmap) -> Unit
) {
    val points = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(android.graphics.Color.BLACK) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Draw Your Signature", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> points.add(offset) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    points.add(change.position)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (points.size > 1) {
                            val path = Path()
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                path.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = Color(selectedColor),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                    
                    if (points.isEmpty()) {
                        Text(
                            "Sign here",
                            color = Color(0xFFCBD5E1),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Clear, Color toggle, Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { points.clear() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Clear", fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(2.dp, if (selectedColor == android.graphics.Color.BLACK) Color(0xFFF59E0B) else Color.Transparent, CircleShape)
                                .tactileClick { selectedColor = android.graphics.Color.BLACK }
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)) // Blue
                                .border(2.dp, if (selectedColor == android.graphics.Color.BLUE) Color(0xFFF59E0B) else Color.Transparent, CircleShape)
                                .tactileClick { selectedColor = android.graphics.Color.BLUE }
                        )
                    }

                    Button(
                        onClick = {
                            if (points.isNotEmpty()) {
                                val bmp = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = selectedColor
                                    strokeWidth = 6f
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                }
                                for (i in 0 until points.size - 1) {
                                    canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, paint)
                                }
                                onSignatureCaptured(bmp)
                            }
                        },
                        enabled = points.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
