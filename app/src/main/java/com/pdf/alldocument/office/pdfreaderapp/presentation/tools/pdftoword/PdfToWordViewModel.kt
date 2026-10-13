package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.pdftoword

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PdfToWordUiState(
    val uri: Uri? = null,
    val fileName: String? = null,
    val pageCount: Int = 0,
    val fromPage: Int = 1,
    val toPage: Int = 1,
    val progress: Int = 0,
    val isConverting: Boolean = false,
    val result: File? = null,
    val error: String? = null
)

@HiltViewModel
class PdfToWordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val engine = PdfToWordEngine()
    private val _uiState = MutableStateFlow(PdfToWordUiState())
    val uiState: StateFlow<PdfToWordUiState> = _uiState

    init { PDFBoxResourceLoader.init(context) }

    fun onPdfPicked(uri: Uri, name: String) {
        _uiState.value = PdfToWordUiState(uri = uri, fileName = name)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val n = engine.pageCount(context.contentResolver.openInputStream(uri)!!)
                _uiState.update { it.copy(pageCount = n, fromPage = 1, toPage = n) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Cannot read PDF: ${e.message}") }
            }
        }
    }

    fun setRange(from: Int, to: Int) = _uiState.update {
        val f = from.coerceIn(1, it.pageCount.coerceAtLeast(1))
        it.copy(fromPage = f, toPage = to.coerceIn(f, it.pageCount.coerceAtLeast(1)), result = null)
    }

    fun convert() {
        val s = _uiState.value
        val uri = s.uri ?: return
        _uiState.update { it.copy(isConverting = true, progress = 0, error = null, result = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = FileUtils.newOutputFile(context, (s.fileName ?: "document").substringBeforeLast('.'), "docx")
                engine.convert(
                    input = context.contentResolver.openInputStream(uri)!!,
                    output = out, fromPage = s.fromPage, toPage = s.toPage,
                    onProgress = { p -> _uiState.update { it.copy(progress = p) } }
                )
                scanner.notifyDocumentCreated(out)
                _uiState.update { it.copy(isConverting = false, result = out) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isConverting = false, error = e.message ?: "Conversion failed") }
            }
        }
    }
}
