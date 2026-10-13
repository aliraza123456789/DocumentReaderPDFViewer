package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.imagetotext

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.common.PageFormat
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

sealed interface OcrState {
    data object Idle : OcrState
    data object Scanning : OcrState
    data class Success(val extractedText: String) : OcrState
    data class Error(val msg: String) : OcrState
}

data class ImageToTextUiState(
    val imageUri: Uri? = null,
    val ocr: OcrState = OcrState.Idle,
    val editedText: String = "",
    val exportedFile: File? = null
)

@HiltViewModel
class ImageToTextViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageToTextUiState())
    val uiState: StateFlow<ImageToTextUiState> = _uiState

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri, ocr = OcrState.Scanning, exportedFile = null) }
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val image = InputImage.fromFilePath(context, uri)
                    recognizer.process(image).await().text
                }
                _uiState.update {
                    it.copy(
                        ocr = if (text.isBlank()) OcrState.Error("No text found in image")
                        else OcrState.Success(text),
                        editedText = text
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(ocr = OcrState.Error(e.message ?: "OCR failed")) }
            }
        }
    }

    fun onTextEdited(text: String) = _uiState.update { it.copy(editedText = text) }

    fun exportTxt() {
        viewModelScope.launch(Dispatchers.IO) {
            val f = FileUtils.newOutputFile(context, "OCR", "txt")
            f.writeText(_uiState.value.editedText)
            scanner.notifyDocumentCreated(f)
            _uiState.update { it.copy(exportedFile = f) }
        }
    }

    fun exportPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val f = FileUtils.newOutputFile(context, "OCR", "pdf")
            writeTextPdf(f, _uiState.value.editedText)
            scanner.notifyDocumentCreated(f)
            _uiState.update { it.copy(exportedFile = f) }
        }
    }

    /** Simple multi-page text -> PDF using native PdfDocument with line wrapping. */
    private fun writeTextPdf(file: File, text: String) {
        val doc = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint().apply { textSize = 12f; isAntiAlias = true }
        val w = PageFormat.A4.widthPt.toInt()
        val h = PageFormat.A4.heightPt.toInt()
        val margin = 40f
        val lineH = 16f
        var pageNo = 1
        var page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, pageNo).create())
        var y = margin

        fun wrap(line: String): List<String> {
            if (line.isEmpty()) return listOf("")
            val out = mutableListOf<String>()
            var cur = StringBuilder()
            for (word in line.split(" ")) {
                val test = if (cur.isEmpty()) word else "$cur $word"
                if (paint.measureText(test) > w - margin * 2) {
                    out += cur.toString(); cur = StringBuilder(word)
                } else cur = StringBuilder(test)
            }
            out += cur.toString()
            return out
        }

        for (raw in text.lines()) for (line in wrap(raw)) {
            if (y > h - margin) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, pageNo).create())
                y = margin
            }
            page.canvas.drawText(line, margin, y, paint)
            y += lineH
        }
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    override fun onCleared() {
        recognizer.close()
    }
}
