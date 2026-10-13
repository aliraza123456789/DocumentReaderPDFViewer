package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.scantoexcel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ScanToExcelUiState(
    val imageUri: Uri? = null,
    val table: List<List<String>> = emptyList(),
    val isProcessing: Boolean = false,
    val exported: File? = null,
    val error: String? = null
)

@HiltViewModel
class ScanToExcelViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanToExcelUiState())
    val uiState: StateFlow<ScanToExcelUiState> = _uiState
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun onImage(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri, isProcessing = true, error = null, exported = null) }
        viewModelScope.launch {
            try {
                val table = withContext(Dispatchers.IO) {
                    val result = recognizer.process(InputImage.fromFilePath(context, uri)).await()
                    TableParserEngine.parse(result)
                }
                _uiState.update {
                    it.copy(isProcessing = false, table = table,
                        error = if (table.isEmpty()) "No table data detected" else null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun editCell(row: Int, col: Int, value: String) = _uiState.update { s ->
        s.copy(table = s.table.mapIndexed { r, cells ->
            if (r == row) cells.mapIndexed { c, v -> if (c == col) value else v } else cells
        }, exported = null)
    }

    fun addRow() = _uiState.update { s ->
        val cols = s.table.maxOfOrNull { it.size } ?: 1
        s.copy(table = s.table + listOf(List(cols) { "" }))
    }

    fun deleteRow(row: Int) = _uiState.update { s -> s.copy(table = s.table.filterIndexed { i, _ -> i != row }) }

    fun exportXlsx() = export("xlsx") { f, t -> TableParserEngine.writeXlsx(f, t) }
    fun exportCsv() = export("csv") { f, t -> TableParserEngine.writeCsv(f, t) }

    private fun export(ext: String, writer: (File, List<List<String>>) -> Unit) {
        val table = _uiState.value.table
        if (table.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val f = FileUtils.newOutputFile(context, "Table", ext)
                writer(f, table)
                scanner.notifyDocumentCreated(f)
                _uiState.update { it.copy(exported = f) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() = recognizer.close()
}
