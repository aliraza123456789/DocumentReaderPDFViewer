package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.components.ToolScaffold
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.tactileClick
import java.io.File

private fun outFile(context: android.content.Context, name: String) =
    File(com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils.outputDir(context), name)

/* Reusable Attractive Gradient Action Button */
@Composable
fun VibrantActionButton(
    text: String,
    icon: ImageVector? = null,
    gradient: List<Color> = emptyList(),
    gradientColors: List<Color> = gradient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = if (gradientColors.isNotEmpty()) gradientColors else if (gradient.isNotEmpty()) gradient else listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(if (enabled) Modifier.tactileClick(onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (enabled) 4.dp else 0.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (enabled) Brush.linearGradient(colors)
                    else Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/* Reusable File Selector Card */
@Composable
fun AttractiveFileSelector(
    fileName: String?,
    placeholder: String = "Tap to choose PDF",
    onPickClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactileClick(onClick = onPickClick),
        shape = RoundedCornerShape(16.dp),
        color = if (fileName != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        border = BorderStroke(1.5.dp, if (fileName != null) Color(0xFF86EFAC) else Color(0xFFE2E8F0))
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
                        if (fileName != null) Color(0xFFDCFCE7)
                        else Color(0xFFEFF6FF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (fileName != null) Icons.Default.CheckCircle else Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = if (fileName != null) Color(0xFF16A34A) else Color(0xFF2563EB),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName ?: placeholder,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Text(
                    text = if (fileName != null) "File selected • Tap to change" else "Select PDF from device storage",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (fileName != null) Color(0xFF22C55E) else Color(0xFF2563EB)
            ) {
                Text(
                    text = if (fileName != null) "Change" else "Browse",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/* 1. TEXT TO PDF SCREEN */
@Composable
fun TextToPdfScreen(onBack: () -> Unit, vm: TextToPdfViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(14f) }

    ToolScaffold("Text to PDF", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Document Content",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Type or paste document text here...") },
                        minLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${fontSize.toInt()} sp", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 8f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF7C3AED),
                            activeTrackColor = Color(0xFF7C3AED)
                        )
                    )
                }
            }

            VibrantActionButton(
                text = "Generate PDF Document",
                icon = Icons.Outlined.PictureAsPdf,
                gradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                enabled = text.isNotBlank(),
                onClick = { vm.convert(text, fontSize, outFile(context, "text_output.pdf")) }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("PDF generated: ${s.output.name}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/* 2. PDF TO LONG IMAGE SCREEN */
@Composable
fun PdfToLongImageScreen(onBack: () -> Unit, vm: SinglePdfToolViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var quality by remember { mutableFloatStateOf(90f) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source = it }

    ToolScaffold("PDF to Long Image", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = source?.lastPathSegment,
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Image Quality", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${quality.toInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    }
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF06B6D4),
                            activeTrackColor = Color(0xFF06B6D4)
                        )
                    )
                    Text(
                        text = "All pages will be stitched vertically into a single long panoramic image.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            VibrantActionButton(
                text = "Stitch Pages into Long Image",
                icon = Icons.Outlined.ViewAgenda,
                gradient = listOf(Color(0xFF06B6D4), Color(0xFF0284C7)),
                enabled = source != null,
                onClick = {
                    source?.let {
                        vm.run(
                            outFile(context, "long_image.jpg"),
                            { u -> pdfToLongImage(u, outFile(context, "long_image.jpg"), quality.toInt()) },
                            it
                        )
                    }
                }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("Saved successfully: ${s.output.name}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/* 3. PDF TO IMAGE SCREEN */
@Composable
fun PdfToImageScreen(onBack: () -> Unit, vm: SinglePdfToolViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var png by remember { mutableStateOf(false) }
    var dpi by remember { mutableFloatStateOf(2f) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source = it }

    ToolScaffold("PDF to Image", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = source?.lastPathSegment,
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Output Format", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(if (png) "Lossless PNG" else "Standard JPG", fontSize = 11.5.sp, color = Color(0xFF64748B))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("JPG", fontSize = 12.sp, fontWeight = if (!png) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.width(6.dp))
                            Switch(checked = png, onCheckedChange = { png = it })
                            Spacer(Modifier.width(6.dp))
                            Text("PNG", fontSize = 12.sp, fontWeight = if (png) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Resolution Scale", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${"%.1f".format(dpi)}x", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Slider(
                            value = dpi,
                            onValueChange = { dpi = it },
                            valueRange = 1f..3f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF10B981),
                                activeTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }

            VibrantActionButton(
                text = "Extract All Pages to Images",
                icon = Icons.Outlined.Image,
                gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                enabled = source != null,
                onClick = {
                    val dir = File(com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils.outputDir(context), "pdf_images").apply { mkdirs() }
                    source?.let {
                        vm.run(
                            outFile(context, "images"),
                            { u -> pdfToImages(u, dir, if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, dpi) },
                            it
                        )
                    }
                }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("Extracted pages saved to gallery folder", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/* 4. LOCK PDF SCREEN */
@Composable
fun LockPdfScreen(onBack: () -> Unit, vm: SinglePdfToolViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source = it }

    ToolScaffold("Lock PDF", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = source?.lastPathSegment,
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Security Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password (min 4 characters)") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Outlined.LockClock, null) },
                        isError = confirm.isNotBlank() && confirm != pass,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val canEncrypt = source != null && pass.length >= 4 && pass == confirm
            VibrantActionButton(
                text = "Encrypt & Lock Document",
                icon = Icons.Outlined.Lock,
                gradient = listOf(Color(0xFF374151), Color(0xFF111827)),
                enabled = canEncrypt,
                onClick = {
                    source?.let { uri ->
                        vm.run(
                            outFile(context, "locked.pdf"),
                            { u -> lockPdf(u, outFile(context, "locked.pdf"), pass) },
                            uri
                        )
                    }
                }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("Password protected: ${s.output.name}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/* 5. UNLOCK PDF SCREEN */
@Composable
fun UnlockPdfScreen(onBack: () -> Unit, vm: SinglePdfToolViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var pass by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source = it }

    ToolScaffold("Unlock PDF", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = source?.lastPathSegment,
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Existing Password", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Enter current document password") },
                        leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            VibrantActionButton(
                text = "Remove Password Protection",
                icon = Icons.Outlined.LockOpen,
                gradient = listOf(Color(0xFF059669), Color(0xFF047857)),
                enabled = source != null && pass.isNotBlank(),
                onClick = {
                    source?.let { uri ->
                        vm.run(
                            outFile(context, "unlocked.pdf"),
                            { u -> unlockPdf(u, outFile(context, "unlocked.pdf"), pass) },
                            uri
                        )
                    }
                }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("Document unlocked: ${s.output.name}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/* 6. SPLIT PDF SCREEN */
@Composable
fun SplitPdfScreen(onBack: () -> Unit, vm: SinglePdfToolViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var rangeText by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source = it }

    ToolScaffold("Split PDF", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = source?.lastPathSegment,
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pages to Extract", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = rangeText,
                        onValueChange = { rangeText = it },
                        placeholder = { Text("E.g. 1-3, 5, 8-10") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.CallSplit, null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Specify single pages or comma-separated ranges to extract into separate PDF files.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            VibrantActionButton(
                text = "Extract Selected Pages",
                icon = Icons.AutoMirrored.Outlined.CallSplit,
                gradient = listOf(Color(0xFFF43F5E), Color(0xFFBE123C)),
                enabled = source != null && rangeText.isNotBlank(),
                onClick = {
                    val dir = File(com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils.outputDir(context), "split").apply { mkdirs() }
                    try {
                        val pages = rangeText.split(",").flatMap { part ->
                            val clean = part.trim()
                            if ("-" in clean) {
                                val (a, b) = clean.split("-").map { it.trim().toInt() }
                                (a - 1 until b).toList()
                            } else listOf(clean.toInt() - 1)
                        }
                        source?.let { uri ->
                            vm.run(outFile(context, "split"), { u -> splitPdf(u, pages, dir) }, uri)
                        }
                    } catch (_: Exception) {}
                }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("Pages extracted successfully!", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/* 7. COMPRESS SCREEN */
@Composable
fun CompressScreen(onBack: () -> Unit, vm: SinglePdfToolViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<Uri?>(null) }
    var preset by remember { mutableStateOf("Medium") }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { source = it }
    val presets = listOf("Low", "Medium", "High")

    ToolScaffold("Compress PDF", onBack, loading = state is ToolUiState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = source?.lastPathSegment,
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Compression Level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { p ->
                            FilterChip(
                                selected = preset == p,
                                onClick = { preset = p },
                                label = { Text(p, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            VibrantActionButton(
                text = "Compress PDF",
                icon = Icons.Outlined.Compress,
                gradient = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                enabled = source != null,
                onClick = {
                    source?.let { uri ->
                        val quality = when (preset) {
                            "Low" -> 50
                            "High" -> 90
                            else -> 70
                        }
                        vm.run(
                            outFile(context, "compressed.pdf"),
                            { u -> compressPdf(u, outFile(context, "compressed.pdf"), quality) },
                            uri
                        )
                    }
                }
            )

            when (val s = state) {
                is ToolUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                            Spacer(Modifier.width(10.dp))
                            Text("PDF compressed: ${s.output.name}", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
                is ToolUiState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFDC2626))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
