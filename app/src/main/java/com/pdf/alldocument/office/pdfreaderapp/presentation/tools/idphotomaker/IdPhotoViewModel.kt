package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.idphotomaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.common.PageFormat
import com.pdf.alldocument.office.pdfreaderapp.common.PdfBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Physical photo sizes. mm values used for ratio and print layout. */
enum class PhotoSize(val label: String, val wMm: Float, val hMm: Float) {
    SQUARE("1:1", 35f, 35f),
    PASSPORT_35x45("35x45 mm", 35f, 45f),
    US_2x2("2x2 in (US)", 50.8f, 50.8f),
    SIZE_30x40("30x40 mm", 30f, 40f);
    val ratio get() = wMm / hMm
}

enum class PhotoBackground(val label: String, val color: Int) {
    WHITE("White", Color.WHITE),
    LIGHT_BLUE("Light Blue", Color.rgb(173, 216, 230)),
    GREY("Grey", Color.rgb(200, 200, 200)),
    RED("Red", Color.rgb(200, 30, 30))
}

enum class SheetPaper(val label: String, val format: PageFormat) {
    FOUR_BY_SIX("4x6 in", PageFormat.FOUR_BY_SIX),
    A4("A4", PageFormat.A4)
}

data class IdPhotoUiState(
    val source: Bitmap? = null,
    val size: PhotoSize = PhotoSize.PASSPORT_35x45,
    val background: PhotoBackground = PhotoBackground.WHITE,
    val copies: Int = 6,
    val paper: SheetPaper = SheetPaper.A4,
    val zoom: Float = 1f,
    val offsetX: Float = 0f, // -1..1 fraction of free space
    val offsetY: Float = 0f,
    val isExporting: Boolean = false,
    val output: File? = null,
    val error: String? = null
)

@HiltViewModel
class IdPhotoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdPhotoUiState())
    val uiState: StateFlow<IdPhotoUiState> = _uiState

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            _uiState.update { it.copy(source = bmp, output = null, zoom = 1f, offsetX = 0f, offsetY = 0f) }
        }
    }

    fun setSize(s: PhotoSize) = _uiState.update { it.copy(size = s, output = null) }
    fun setBackground(b: PhotoBackground) = _uiState.update { it.copy(background = b, output = null) }
    fun setCopies(n: Int) = _uiState.update { it.copy(copies = n, output = null) }
    fun setPaper(p: SheetPaper) = _uiState.update { it.copy(paper = p, output = null) }
    fun setZoom(z: Float) = _uiState.update { it.copy(zoom = z) }
    fun setOffset(x: Float, y: Float) = _uiState.update { it.copy(offsetX = x, offsetY = y) }

    /** Renders one cropped ID photo (300 dpi) with the chosen background colour. */
    fun renderSinglePhoto(s: IdPhotoUiState = _uiState.value): Bitmap? {
        val src = s.source ?: return null
        val outW = (s.size.wMm / 25.4f * 300f).toInt()
        val outH = (s.size.hMm / 25.4f * 300f).toInt()
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(s.background.color)

        // scale source so it covers the target, then apply user zoom
        val cover = maxOf(outW / src.width.toFloat(), outH / src.height.toFloat())
        val scale = cover * s.zoom
        val drawW = src.width * scale
        val drawH = src.height * scale
        val freeX = (drawW - outW).coerceAtLeast(0f)
        val freeY = (drawH - outH).coerceAtLeast(0f)
        val left = -freeX / 2f - s.offsetX * freeX / 2f
        val top = -freeY / 2f - s.offsetY * freeY / 2f
        val m = android.graphics.Matrix().apply { postScale(scale, scale); postTranslate(left, top) }
        canvas.drawBitmap(src, m, null)
        return out
    }

    fun exportJpeg() = export { photo ->
        val f = FileUtils.newOutputFile(context, "IdPhoto", "jpg")
        f.outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        f
    }

    fun exportSheetPdf() = export { photo ->
        val s = _uiState.value
        val f = FileUtils.newOutputFile(context, "IdPhotoSheet", "pdf")
        // pt = mm / 25.4 * 72
        val cellW = s.size.wMm / 25.4f * 72f + 6f
        val cellH = s.size.hMm / 25.4f * 72f + 6f
        PdfBuilder.buildPrintSheet(f, photo, s.copies, cellW, cellH, s.paper.format)
        f
    }

    private fun export(block: (Bitmap) -> File) {
        val photo = renderSinglePhoto() ?: return
        _uiState.update { it.copy(isExporting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val f = block(photo)
                scanner.notifyDocumentCreated(f)
                _uiState.update { it.copy(isExporting = false, output = f) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }
}
