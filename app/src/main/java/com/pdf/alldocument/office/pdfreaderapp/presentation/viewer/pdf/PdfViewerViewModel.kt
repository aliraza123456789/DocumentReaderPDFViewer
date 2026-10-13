package com.pdf.alldocument.office.pdfreaderapp.presentation.viewer.pdf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.analytics.AnalyticsManager
import com.pdf.alldocument.office.pdfreaderapp.data.pdf.PdfRendererEngine
import com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfViewerState(
    val filePath: String = "",
    val activeRenderPath: String = "",
    val title: String = "",
    val pages: List<PageRenderData> = emptyList(),
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val isNightMode: Boolean = false,
    val isHorizontalScroll: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val isPasswordProtected: Boolean = false,
    val passwordError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val pdfRendererEngine: PdfRendererEngine,
    private val documentRepository: DocumentRepository,
    private val analyticsManager: AnalyticsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    fun initialize(filePath: String, title: String, lastReadPage: Int = 0) {
        analyticsManager.logDocumentOpened("pdf")
        _state.update {
            it.copy(
                filePath = filePath,
                activeRenderPath = filePath,
                title = title,
                currentPage = lastReadPage,
                isLoading = true,
                isPasswordProtected = false,
                passwordError = null
            )
        }

        // Check if file is password protected
        if (pdfRendererEngine.isEncrypted(filePath)) {
            _state.update { it.copy(isPasswordProtected = true, isLoading = false) }
            return
        }

        loadPdf(filePath, lastReadPage)
        viewModelScope.launch {
            documentRepository.recordDocumentOpened(filePath, lastReadPage)
        }
    }

    fun unlockWithPassword(password: String) {
        if (password.isBlank()) {
            _state.update { it.copy(passwordError = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, passwordError = null) }
            val tempFile = pdfRendererEngine.unlockToTemp(_state.value.filePath, password)
            if (tempFile != null && tempFile.exists()) {
                _state.update {
                    it.copy(
                        isPasswordProtected = false,
                        passwordError = null,
                        activeRenderPath = tempFile.absolutePath,
                        isLoading = true
                    )
                }
                loadPdf(tempFile.absolutePath, _state.value.currentPage)
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        passwordError = "Incorrect password. Please try again."
                    )
                }
            }
        }
    }

    private fun loadPdf(renderPath: String, initialPage: Int) {
        viewModelScope.launch {
            val totalPages = pdfRendererEngine.getPageCount(renderPath)
            if (totalPages == 0) {
                if (pdfRendererEngine.isEncrypted(_state.value.filePath)) {
                    _state.update { it.copy(isPasswordProtected = true, isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false, errorMessage = "Unable to open or read document") }
                }
                return@launch
            }

            val placeholderPages = List(totalPages) { PageRenderData(it, "", 0, 0) }
            _state.update { it.copy(totalPages = totalPages, pages = placeholderPages) }

            pdfRendererEngine.streamRenderPages(renderPath, initialPage)
                .catch { e ->
                    if (pdfRendererEngine.isEncrypted(_state.value.filePath)) {
                        _state.update { it.copy(isPasswordProtected = true, isLoading = false) }
                    } else {
                        _state.update { it.copy(isLoading = false, errorMessage = e.message) }
                    }
                }
                .collect { pageData ->
                    _state.update { current ->
                        val newPages = current.pages.toMutableList()
                        if (pageData.pageIndex < newPages.size) {
                            newPages[pageData.pageIndex] = pageData
                        } else {
                            while (newPages.size <= pageData.pageIndex) {
                                newPages.add(PageRenderData(newPages.size, "", 0, 0))
                            }
                            newPages[pageData.pageIndex] = pageData
                        }
                        current.copy(
                            pages = newPages.toList(),
                            isLoading = false
                        )
                    }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private var lastRecordedPage = -1

    fun onPageChanged(pageIndex: Int) {
        if (_state.value.currentPage == pageIndex) return
        _state.update { it.copy(currentPage = pageIndex) }
        if (lastRecordedPage != pageIndex) {
            lastRecordedPage = pageIndex
            viewModelScope.launch(Dispatchers.IO) {
                documentRepository.updateLastReadPage(_state.value.filePath, pageIndex)
            }
        }
    }

    fun toggleNightMode() {
        _state.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun toggleHorizontalScroll() {
        _state.update { it.copy(isHorizontalScroll = !it.isHorizontalScroll) }
    }

    fun jumpToPage(pageIndex: Int) {
        if (pageIndex in 0 until _state.value.totalPages) {
            _state.update { it.copy(currentPage = pageIndex) }
        }
    }
}
