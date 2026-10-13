package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.home

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.components.FeatureCard
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.components.FeatureSection
import com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.navigation.Screen
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    onOpenTab: (Screen) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val recent by viewModel.recentFiles.collectAsStateWithLifecycle()
    var showCreateFolder by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importPdf(context, it)
            Toast.makeText(context, "File imported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val printLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            try {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder("document_${System.currentTimeMillis()}.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            context.contentResolver.openInputStream(fileUri)?.use { input ->
                                FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager?.print("Document", printAdapter, null)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open print service: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { onOpenTab(Screen.Settings) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenTab(Screen.Files) },
                    icon = { Icon(Icons.Filled.Folder, null) },
                    label = { Text("Files") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenTab(Screen.Tools) },
                    icon = { Icon(Icons.Filled.Search, null) },
                    label = { Text("Tools") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenTab(Screen.Settings) },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ---- Recent files ----
            Text("Recent Files", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (recent.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "No recent files. Open or import a PDF to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recent.forEach { file ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(),
                            modifier = Modifier.clickable {
                                if (file.uriString.isNotBlank()) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(android.net.Uri.parse(file.uriString), "application/pdf")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, file.name, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Column(Modifier.padding(12.dp).width(140.dp)) {
                                Icon(Icons.Filled.PictureAsPdf, null, tint = Color(0xFFEF4444))
                                Spacer(Modifier.height(6.dp))
                                Text(file.name, maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(file.size, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Filled.UploadFile, null); Spacer(Modifier.width(6.dp)); Text("Open PDF") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Import File") }
            }

            Spacer(Modifier.height(20.dp))

            val onFeatureClick: (Feature) -> Unit = { feature ->
                when (feature.title) {
                    "Import Files" -> importLauncher.launch(arrayOf("application/pdf"))
                    "Create Folder" -> showCreateFolder = true
                    "Print" -> printLauncher.launch(arrayOf("application/pdf"))
                    else -> feature.route?.let(onNavigate)
                }
            }

            FeatureSection("📄 Convert", convertFeatures, onFeatureClick)
            FeatureSection("✏️ Edit", editFeatures, onFeatureClick)
            FeatureSection("🗂️ Manage", manageFeatures, onFeatureClick)
            FeatureSection("⚙️ Other", otherFeatures, onFeatureClick)

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showCreateFolder) {
        com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.components.CreateFolderDialog(
            onDismiss = { showCreateFolder = false },
            onCreate = { name ->
                viewModel.createFolder(name)
                Toast.makeText(context, "Folder created: $name", Toast.LENGTH_SHORT).show()
                showCreateFolder = false
            }
        )
    }

    if (showSearchSheet) {
        ModalBottomSheet(onDismissRequest = {
            showSearchSheet = false
            searchQuery = ""
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Search Tools & Features", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by tool name (e.g. merge, compress, print)...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                val filtered = remember(searchQuery) {
                    if (searchQuery.isBlank()) allFeatures
                    else allFeatures.filter { it.title.contains(searchQuery, ignoreCase = true) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filtered.forEach { feature ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSearchSheet = false
                                    when (feature.title) {
                                        "Import Files" -> importLauncher.launch(arrayOf("application/pdf"))
                                        "Create Folder" -> showCreateFolder = true
                                        "Print" -> printLauncher.launch(arrayOf("application/pdf"))
                                        else -> feature.route?.let(onNavigate)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(feature.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(feature.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
