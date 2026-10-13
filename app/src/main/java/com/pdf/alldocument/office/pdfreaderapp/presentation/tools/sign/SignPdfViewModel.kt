package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.sign

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.PdfToolsRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
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
    val availablePdfs: List<DocumentItem> = emptyList(),
    val filteredPdfs: List<DocumentItem> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val pageBitmaps: List<Bitmap> = emptyList(),
    val currentPageIndex: Int = 0,
    val signatureBitmap: Bitmap? = null,
    val savedSignatures: List<Bitmap> = emptyList(),
    // Normalized signature bounds (0f..1f relative to displayed page width and height)
    val sigNormX: Float = 0.35f,
    val sigNormY: Float = 0.65f,
    val sigNormWidth: Float = 0.38f,
    val isFlippedHorizontally: Boolean = false,
    val isFitToWidth: Boolean = false,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SignPdfViewModel @Inject constructor(
    private val pdfToolsRepository: PdfToolsRepository,
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SignPdfState())
    val state: StateFlow<SignPdfState> = _state.asStateFlow()

    init {
        loadDevicePdfs()
    }

    private fun loadDevicePdfs() {
        viewModelScope.launch {
            getDocumentsUseCase(DocumentType.PDF, SortOrder.DATE_DESC).collect { docs ->
                _state.update { current ->
                    val filtered = if (current.searchQuery.isBlank()) docs
                    else docs.filter { it.name.contains(current.searchQuery, ignoreCase = true) }
                    current.copy(availablePdfs = docs, filteredPdfs = filtered)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            val filtered = if (query.isBlank()) current.availablePdfs
            else current.availablePdfs.filter { it.name.contains(query, ignoreCase = true) }
            current.copy(searchQuery = query, filteredPdfs = filtered)
        }
    }

    fun toggleSearch(open: Boolean) {
        _state.update { current ->
            itResetSearch(current, open)
        }
    }

    private fun itResetSearch(current: SignPdfState, open: Boolean): SignPdfState {
        return current.copy(
            isSearching = open,
            searchQuery = if (!open) "" else current.searchQuery,
            filteredPdfs = if (!open) current.availablePdfs else current.filteredPdfs
        )
    }

    fun loadPdf(filePath: String, title: String) {
        if (filePath.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(filePath = filePath, title = title, isLoading = true, errorMessage = null) }
            val bitmaps = pdfToolsRepository.renderAllPagesToBitmaps(filePath)
            _state.update {
                it.copy(
                    pageBitmaps = bitmaps,
                    currentPageIndex = 0,
                    isLoading = false,
                    errorMessage = if (bitmaps.isEmpty()) "Failed to load PDF pages" else null
                )
            }
        }
    }

    fun setSignature(bitmap: Bitmap) {
        _state.update { current ->
            val exists = current.savedSignatures.any { it == bitmap }
            val updatedSaved = if (exists) current.savedSignatures else (listOf(bitmap) + current.savedSignatures).take(10)
            current.copy(
                signatureBitmap = bitmap,
                savedSignatures = updatedSaved,
                sigNormX = 0.35f,
                sigNormY = 0.65f,
                sigNormWidth = 0.38f,
                isFlippedHorizontally = false
            )
        }
    }

    fun selectSavedSignature(bitmap: Bitmap) {
        _state.update { current ->
            current.copy(
                signatureBitmap = bitmap,
                sigNormX = 0.35f,
                sigNormY = 0.65f,
                sigNormWidth = 0.38f,
                isFlippedHorizontally = false
            )
        }
    }

    fun removeCurrentSignature() {
        _state.update { it.copy(signatureBitmap = null) }
    }

    fun deleteSavedSignature(bitmap: Bitmap) {
        _state.update { current ->
            val updated = current.savedSignatures.filter { it != bitmap }
            val newCurrentSig = if (current.signatureBitmap == bitmap) null else current.signatureBitmap
            current.copy(savedSignatures = updated, signatureBitmap = newCurrentSig)
        }
    }

    fun updateSigPosition(normDeltaX: Float, normDeltaY: Float) {
        _state.update { current ->
            val newX = (current.sigNormX + normDeltaX).coerceIn(0.01f, 0.90f)
            val newY = (current.sigNormY + normDeltaY).coerceIn(0.01f, 0.90f)
            current.copy(sigNormX = newX, sigNormY = newY)
        }
    }

    fun updateSigWidth(normDeltaWidth: Float) {
        _state.update { current ->
            val newWidth = (current.sigNormWidth + normDeltaWidth).coerceIn(0.12f, 0.85f)
            current.copy(sigNormWidth = newWidth)
        }
    }

    fun toggleFlipHorizontal() {
        _state.update { it.copy(isFlippedHorizontally = !it.isFlippedHorizontally) }
    }

    fun toggleFitToWidth() {
        _state.update { it.copy(isFitToWidth = !it.isFitToWidth) }
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

            // Overlay signature onto the target page bitmap using exact normalized coordinates
            val finalPages = pages.mapIndexed { index, bmp ->
                if (index == currentState.currentPageIndex && sig != null) {
                    val combined = bmp.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(combined)

                    val sigPixelWidth = (combined.width * currentState.sigNormWidth).toInt().coerceAtLeast(10)
                    val sigAspect = sig.height.toFloat() / sig.width.toFloat()
                    val sigPixelHeight = (sigPixelWidth * sigAspect).toInt().coerceAtLeast(10)

                    val sigPixelX = combined.width * currentState.sigNormX
                    val sigPixelY = combined.height * currentState.sigNormY

                    var scaledSig = Bitmap.createScaledBitmap(sig, sigPixelWidth, sigPixelHeight, true)
                    if (currentState.isFlippedHorizontally) {
                        val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
                        scaledSig = Bitmap.createBitmap(scaledSig, 0, 0, scaledSig.width, scaledSig.height, matrix, true)
                    }
                    canvas.drawBitmap(scaledSig, sigPixelX, sigPixelY, Paint(Paint.ANTI_ALIAS_FLAG))
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
