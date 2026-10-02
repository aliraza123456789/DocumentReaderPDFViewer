package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.idcardscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.viewer.pdfreaderapp.common.PdfBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class IdStep { FRONT, BACK, PREVIEW }
enum class IdFilter { COLOR, BLACK_WHITE }

data class IdCardUiState(
    val step: IdStep = IdStep.FRONT,
    val front: Bitmap? = null,
    val back: Bitmap? = null,
    val filter: IdFilter = IdFilter.COLOR,
    val isExporting: Boolean = false,
    val pdf: File? = null,
    val error: String? = null
)

@HiltViewModel
class IdCardScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdCardUiState())
    val uiState: StateFlow<IdCardUiState> = _uiState

    fun onCaptured(bitmap: Bitmap) {
        _uiState.update {
            when (it.step) {
                IdStep.FRONT -> it.copy(front = bitmap, step = IdStep.BACK)
                IdStep.BACK -> it.copy(back = bitmap, step = IdStep.PREVIEW)
                IdStep.PREVIEW -> it
            }
        }
    }

    fun retake(step: IdStep) = _uiState.update {
        when (step) {
            IdStep.FRONT -> it.copy(front = null, step = IdStep.FRONT, pdf = null)
            IdStep.BACK -> it.copy(back = null, step = IdStep.BACK, pdf = null)
            else -> it
        }
    }

    fun setFilter(filter: IdFilter) = _uiState.update { it.copy(filter = filter, pdf = null) }

    fun applyFilter(src: Bitmap, filter: IdFilter): Bitmap {
        if (filter == IdFilter.COLOR) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
                // boost contrast for a cleaner scanned look
                val c = 1.4f; val t = (-0.5f * c + 0.5f) * 255f
                postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f)))
            })
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun exportPdf() {
        val s = _uiState.value
        val f = s.front ?: return
        val b = s.back ?: return
        _uiState.update { it.copy(isExporting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val out = FileUtils.newOutputFile(context, "IdCard", "pdf")
                PdfBuilder.buildTwoUp(out, applyFilter(f, s.filter), applyFilter(b, s.filter))
                _uiState.update { it.copy(isExporting = false, pdf = out) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }

    fun reset() { _uiState.value = IdCardUiState() }
}
