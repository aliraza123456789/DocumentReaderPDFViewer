package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.officetopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class OfficeToPdfUiState(
    val fileName: String? = null,
    val progress: Int = 0,
    val isConverting: Boolean = false,
    val result: File? = null,
    val error: String? = null
)

@HiltViewModel
class OfficeToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val engine = OfficeToPdfEngine()
    private val _uiState = MutableStateFlow(OfficeToPdfUiState())
    val uiState: StateFlow<OfficeToPdfUiState> = _uiState

    fun convert(uri: Uri, displayName: String) {
        val kind = engine.kindOf(displayName)
        if (kind == null) {
            _uiState.value = OfficeToPdfUiState(
                fileName = displayName,
                error = "Unsupported format. Supported: .docx, .xlsx, .pptx (legacy .doc/.xls/.ppt need to be re-saved in the new format)."
            )
            return
        }
        _uiState.value = OfficeToPdfUiState(fileName = displayName, isConverting = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = FileUtils.newOutputFile(context, displayName.substringBeforeLast('.'), "pdf")
                engine.convert(
                    input = { context.contentResolver.openInputStream(uri)!! },
                    kind = kind, output = out,
                    onProgress = { p -> _uiState.update { it.copy(progress = p) } }
                )
                scanner.notifyDocumentCreated(out)
                _uiState.update { it.copy(isConverting = false, result = out, progress = 100) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isConverting = false, error = e.message ?: "Conversion failed") }
            }
        }
    }
}
