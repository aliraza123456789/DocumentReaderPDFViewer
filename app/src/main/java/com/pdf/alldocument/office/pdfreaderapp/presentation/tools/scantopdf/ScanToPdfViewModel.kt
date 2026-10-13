package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.scantopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.core.util.MimeTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ScanToPdfUiState(
    val isLoading: Boolean = false,
    val recentScans: List<File> = emptyList(),
    val lastScan: File? = null,
    val error: String? = null
)

@HiltViewModel
class ScanToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanToPdfUiState())
    val uiState: StateFlow<ScanToPdfUiState> = _uiState

    init { loadRecent() }

    fun loadRecent() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = FileUtils.outputDir(context)
                .listFiles { f -> f.name.startsWith("Scan_") && f.extension == "pdf" }
                ?.sortedByDescending { it.lastModified() }
                ?.take(10) ?: emptyList()
            _uiState.update { it.copy(recentScans = files) }
        }
    }

    /** Called with the PDF uri returned by GmsDocumentScanner. */
    fun onScanResult(pdfUri: Uri?) {
        if (pdfUri == null) {
            _uiState.update { it.copy(error = "Scan returned no PDF") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val f = FileUtils.copyToOutput(context, pdfUri, "Scan", "pdf")
                    scanner.notifyDocumentCreated(f)
                    f
                }
                _uiState.update { it.copy(isLoading = false, lastScan = file) }
                loadRecent()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onScanCancelledOrFailed(message: String? = null) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    fun shareIntent(file: File) = FileUtils.shareIntent(context, file, MimeTypes.PDF)
    fun openIntent(file: File) = FileUtils.openIntent(context, file, MimeTypes.PDF)
}
