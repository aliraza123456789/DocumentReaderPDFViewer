package com.pdf.alldocument.viewer.pdfreaderapp.presentation.viewer.pdf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdf.PdfRendererEngine
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.PageRenderData
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfViewerState(
    val filePath: String = "",
    val title: String = "",
    val pages: List<PageRenderData> = emptyList(),
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val isNightMode: Boolean = false,
    val isHorizontalScroll: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val errorMessage: String? = null
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val pdfRendererEngine: PdfRendererEngine,
    private val documentRepository: DocumentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    fun initialize(filePath: String, title: String, lastReadPage: Int = 0) {
        _state.update { it.copy(filePath = filePath, title = title, currentPage = lastReadPage, isLoading = true) }
        loadPdf(filePath, lastReadPage)
        viewModelScope.launch {
            documentRepository.recordDocumentOpened(filePath, lastReadPage)
        }
    }

    private fun loadPdf(filePath: String, initialPage: Int) {
        viewModelScope.launch {
            val totalPages = pdfRendererEngine.getPageCount(filePath)
            val placeholderPages = List(totalPages) { PageRenderData(it, "", 0, 0) }
            _state.update { it.copy(totalPages = totalPages, pages = placeholderPages) }

            pdfRendererEngine.streamRenderPages(filePath, initialPage)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message) }
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
