package com.pdf.alldocument.office.pdfreaderapp.presentation.viewer.office

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.reader.officereader.ActivityFilesReader
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AppTopBar
import com.pdf.alldocument.office.pdfreaderapp.presentation.home.DocTypeIcon
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.core.util.ShareUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OfficeViewerViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {
    fun recordOpened(path: String) {
        viewModelScope.launch {
            documentRepository.recordDocumentOpened(path, 0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeViewerScreen(
    filePath: String,
    title: String,
    viewModel: OfficeViewerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }
    val extension = remember(filePath) { FileUtils.getFileExtension(filePath) }

    LaunchedEffect(filePath) {
        viewModel.recordOpened(filePath)
        // Auto-launch the high-performance offline engine
        launchOfficeEngine(context, filePath)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = title.ifEmpty { file.name },
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { ShareUtils.shareFile(context, filePath) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DocTypeIcon(extension = extension, size = 64.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = title.ifEmpty { file.name },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Offline Office Reader Engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { launchOfficeEngine(context, filePath) },
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-open in Office Engine")
                }
            }
        }
    }
}

private fun launchOfficeEngine(context: Context, path: String) {
    try {
        val intent = ActivityFilesReader.launchDocFromPath(context, path)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
