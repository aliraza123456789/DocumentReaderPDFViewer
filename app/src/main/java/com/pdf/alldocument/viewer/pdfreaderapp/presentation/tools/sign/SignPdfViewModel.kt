package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.sign

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.PdfToolsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignPdfState(
    val filePath: String = "",
    val title: String = "",
    val pageBitmaps: List<Bitmap> = emptyList(),
    val currentPageIndex: Int = 0,
    val signatureBitmap: Bitmap? = null,
    val sigPosX: Float = 100f,
    val sigPosY: Float = 100f,
    val sigScale: Float = 1f,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SignPdfViewModel @Inject constructor(
    private val pdfToolsRepository: PdfToolsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SignPdfState())
    val state: StateFlow<SignPdfState> = _state.asStateFlow()

    fun loadPdf(filePath: String, title: String) {
        if (filePath.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(filePath = filePath, title = title, isLoading = true) }
            val bitmaps = pdfToolsRepository.renderAllPagesToBitmaps(filePath)
            _state.update { it.copy(pageBitmaps = bitmaps, isLoading = false) }
        }
    }

    fun setSignature(bitmap: Bitmap) {
        _state.update { it.copy(signatureBitmap = bitmap, sigPosX = 200f, sigPosY = 200f, sigScale = 1f) }
    }

    fun updateSigPosition(deltaX: Float, deltaY: Float) {
        _state.update { current ->
            current.copy(
                sigPosX = current.sigPosX + deltaX,
                sigPosY = current.sigPosY + deltaY
            )
        }
    }

    fun updateSigScale(scaleFactor: Float) {
        _state.update { current ->
            current.copy(sigScale = (current.sigScale * scaleFactor).coerceIn(0.5f, 3.0f))
        }
    }

    fun setPageIndex(index: Int) {
        if (index in _state.value.pageBitmaps.indices) {
            _state.update { it.copy(currentPageIndex = index) }
        }
    }

    fun exportSignedPdf(outputName: String) {
        val currentState = _state.value
        val pages = currentState.pageBitmaps
        val sig = currentState.signatureBitmap

        if (pages.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, errorMessage = null) }

            // Overlay signature onto the target page bitmap
            val finalPages = pages.mapIndexed { index, bmp ->
                if (index == currentState.currentPageIndex && sig != null) {
                    val combined = bmp.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(combined)
                    val scaledSig = Bitmap.createScaledBitmap(
                        sig,
                        (sig.width * currentState.sigScale).toInt().coerceAtLeast(1),
                        (sig.height * currentState.sigScale).toInt().coerceAtLeast(1),
                        true
                    )
                    canvas.drawBitmap(scaledSig, currentState.sigPosX, currentState.sigPosY, Paint(Paint.ANTI_ALIAS_FLAG))
                    combined
                } else {
                    bmp
                }
            }

            pdfToolsRepository.saveSignedPdf(currentState.filePath, finalPages, outputName)
                .onSuccess { path ->
                    _state.update { it.copy(isExporting = false, exportedFilePath = path) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isExporting = false, errorMessage = e.message ?: "Export failed") }
                }
        }
    }

    fun clearResult() {
        _state.update { it.copy(exportedFilePath = null, errorMessage = null) }
    }
}
