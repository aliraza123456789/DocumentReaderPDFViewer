package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.imagetopdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.common.MarginStyle
import com.pdf.alldocument.office.pdfreaderapp.common.MimeTypes
import com.pdf.alldocument.office.pdfreaderapp.common.PageFormat
import com.pdf.alldocument.office.pdfreaderapp.common.PdfBuilder
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

data class ImagePage(
    val id: String,
    val uri: Uri,
    val rotationDegrees: Float = 0f
)

data class ImageToPdfUiState(
    val pages: List<ImagePage> = emptyList(),
    val format: PageFormat = PageFormat.A4,
    val margin: MarginStyle = MarginStyle.THIN,
    val quality: Int = 90,
    val isConverting: Boolean = false,
    val resultFile: File? = null,
    val error: String? = null
)

@HiltViewModel
class ImageToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState

    fun addImages(uris: List<Uri>) {
        val newPages = uris.map { ImagePage(id = it.toString() + System.nanoTime(), uri = it) }
        _uiState.update { it.copy(pages = it.pages + newPages, resultFile = null) }
    }

    fun removePage(id: String) {
        _uiState.update { it.copy(pages = it.pages.filterNot { p -> p.id == id }) }
    }

    fun rotatePage(id: String) {
        _uiState.update { state ->
            state.copy(pages = state.pages.map { p ->
                if (p.id == id) p.copy(rotationDegrees = (p.rotationDegrees + 90f) % 360f) else p
            })
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.pages.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(pages = list)
        }
    }

    fun setFormat(format: PageFormat) = _uiState.update { it.copy(format = format) }
    fun setMargin(margin: MarginStyle) = _uiState.update { it.copy(margin = margin) }
    fun setQuality(quality: Int) = _uiState.update { it.copy(quality = quality) }

    fun convertToPdf() {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        _uiState.update { it.copy(isConverting = true, error = null) }

        viewModelScope.launch {
            try {
                val outputFile = withContext(Dispatchers.IO) {
                    val bitmaps = state.pages.map { page -> loadAndRotate(page) }
                    val out = FileUtils.newOutputFile(context, "ImageToPdf", "pdf")
                    PdfBuilder.build(
                        outputFile = out,
                        bitmaps = bitmaps,
                        format = state.format,
                        margin = state.margin,
                        quality = state.quality
                    )
                    bitmaps.forEach { it.recycle() }
                    scanner.notifyDocumentCreated(out)
                    out
                }
                _uiState.update { it.copy(isConverting = false, resultFile = outputFile) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isConverting = false, error = e.message ?: "Conversion failed") }
            }
        }
    }

    private fun loadAndRotate(page: ImagePage): Bitmap {
        val input = context.contentResolver.openInputStream(page.uri)
        val bitmap = BitmapFactory.decodeStream(input)
        input?.close()
        return if (page.rotationDegrees != 0f) {
            com.pdf.alldocument.office.pdfreaderapp.common.PdfBuilder.rotate(bitmap, page.rotationDegrees)
        } else bitmap
    }

    fun shareResultIntent(context: Context) =
        uiState.value.resultFile?.let { FileUtils.shareIntent(context, it, MimeTypes.PDF) }

    fun openResultIntent(context: Context) =
        uiState.value.resultFile?.let { FileUtils.openIntent(context, it, MimeTypes.PDF) }
}
