package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.components.ToolScaffold
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.screens.TabsViewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(onBack: () -> Unit, vm: TabsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val deletedFiles by vm.recycleBinFiles.collectAsStateWithLifecycle()
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedFiles.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirmDialog = true }) {
                            Text("Empty Bin", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (deletedFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recycle Bin is Empty", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Files you delete will appear here and can be restored anytime",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(deletedFiles) { file ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, maxLines = 1)
                                    Text("Deleted item", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    vm.restoreFile(file.id)
                                    Toast.makeText(context, "Restored: ${file.name}", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Outlined.Restore, contentDescription = "Restore", tint = Color(0xFF16A34A))
                                }
                                IconButton(onClick = {
                                    vm.deletePermanently(file.id)
                                    Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = { Text("Empty Recycle Bin") },
            text = { Text("All items in the Recycle Bin will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.emptyRecycleBin()
                        showEmptyConfirmDialog = false
                        Toast.makeText(context, "Recycle Bin emptied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddTextScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var textToAdd by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var textPosition by remember { mutableStateOf("Bottom") }
    var isProcessing by remember { mutableStateOf(false) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedUri = uri
        outputFile = null
        errorMessage = null
    }

    ToolScaffold("Add Text & Watermark", onBack, loading = isProcessing) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AttractiveFileSelector(
                fileName = selectedUri?.lastPathSegment,
                placeholder = "Tap to choose PDF to add text",
                onPickClick = { picker.launch(arrayOf("application/pdf")) }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Text to Insert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = textToAdd,
                        onValueChange = { textToAdd = it },
                        placeholder = { Text("Enter text, note, or watermark...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${fontSize.toInt()} pt", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 10f..40f
                    )

                    Text("Placement", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Top", "Center", "Bottom").forEach { pos ->
                            FilterChip(
                                selected = textPosition == pos,
                                onClick = { textPosition = pos },
                                label = { Text(pos) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            VibrantActionButton(
                text = "Apply Text to PDF",
                icon = Icons.Outlined.TextFields,
                gradient = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                enabled = selectedUri != null && textToAdd.isNotBlank(),
                onClick = {
                    val uri = selectedUri ?: return@VibrantActionButton
                    isProcessing = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val out = withContext(Dispatchers.IO) {
                                val outF = File(context.getExternalFilesDir(null), "text_added_${System.currentTimeMillis()}.pdf")
                                context.contentResolver.openInputStream(uri).use { stream ->
                                    PDDocument.load(stream).use { doc ->
                                        for (page in doc.pages) {
                                            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                                cs.beginText()
                                                cs.setFont(PDType1Font.HELVETICA_BOLD, fontSize)
                                                cs.setNonStrokingColor(40f / 255f, 40f / 255f, 40f / 255f)
                                                val yOffset = when (textPosition) {
                                                    "Top" -> page.mediaBox.height - 40f
                                                    "Center" -> page.mediaBox.height / 2f
                                                    else -> 40f
                                                }
                                                cs.newLineAtOffset(50f, yOffset)
                                                cs.showText(textToAdd.take(80))
                                                cs.endText()
                                            }
                                        }
                                        doc.save(outF)
                                    }
                                }
                                outF
                            }
                            outputFile = out
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to add text to PDF"
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            )

            outputFile?.let { out ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Text successfully added!", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            Text(out.name, fontSize = 11.5.sp, color = Color(0xFF166534))
                        }
                    }
                }
            }

            errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626))
                        Spacer(Modifier.width(10.dp))
                        Text(err, color = Color(0xFFB91C1C), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotateScreen(onBack: () -> Unit) = AddTextScreen(onBack = onBack)

@Composable
fun EditTextScreen(onBack: () -> Unit) {
    ToolScaffold("Edit Text", onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color(0xFF2563EB))
            Text("PDF Vector Text Security", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "Standard PDF files protect existing typographical streams to prevent tampering. To edit or overlay annotations, please use the Add Text & Watermark utility.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go to Add Text Tool")
            }
        }
    }
}
