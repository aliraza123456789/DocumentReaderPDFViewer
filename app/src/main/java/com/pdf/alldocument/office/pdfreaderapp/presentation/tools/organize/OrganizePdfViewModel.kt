package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.organize

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.PdfToolsRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.OrganizePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class OrganizeFlowStep {
    SELECT_FILE,
    REORDER_PAGES,
    SUCCESS
}

data class OrganizePageItem(
    val id: String = UUID.randomUUID().toString(),
    val originalIndex: Int,
    val bitmap: Bitmap
)

data class OrganizePdfState(
    val step: OrganizeFlowStep = OrganizeFlowStep.SELECT_FILE,
    val initialFilePath: String = "",
    val filePath: String = "",
    val fileName: String = "",
    val availablePdfs: List<DocumentItem> = emptyList(),
    val filteredPdfs: List<DocumentItem> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoadingFiles: Boolean = true,
    val pages: List<OrganizePageItem> = emptyList(),
    val isLoadingPages: Boolean = false,
    val isPreparing: Boolean = false,
    val prepareProgress: Int = 0,
    val outputFilePath: String? = null,
    val outputFileName: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class OrganizePdfViewModel @Inject constructor(
    private val pdfToolsRepository: PdfToolsRepository,
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val documentRepository: DocumentRepository,
    private val organizePdfUseCase: OrganizePdfUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OrganizePdfState())
    val state: StateFlow<OrganizePdfState> = _state.asStateFlow()

    private var progressJob: Job? = null

    init {
        loadAvailablePdfs()
    }

    fun initInitialFile(filePath: String, title: String) {
        if (filePath.isBlank()) return
        if (_state.value.initialFilePath == filePath) return

        _state.update { it.copy(initialFilePath = filePath) }
        selectAndLoadPdf(filePath, title.ifBlank { File(filePath).name })
    }

    private fun loadAvailablePdfs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFiles = true) }
            getDocumentsUseCase(DocumentType.PDF, SortOrder.DATE_DESC).collect { docs ->
                _state.update { current ->
                    val filtered = if (current.searchQuery.isBlank()) docs
                    else docs.filter { it.name.contains(current.searchQuery, ignoreCase = true) }
                    current.copy(
                        availablePdfs = docs,
                        filteredPdfs = filtered,
                        isLoadingFiles = false
                    )
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

    fun toggleSearch(active: Boolean? = null) {
        _state.update { current ->
            val newActive = active ?: !current.isSearchActive
            current.copy(
                isSearchActive = newActive,
                searchQuery = if (!newActive) "" else current.searchQuery,
                filteredPdfs = if (!newActive) current.availablePdfs else current.filteredPdfs
            )
        }
    }

    fun selectAndLoadPdf(filePath: String, fileName: String) {
        if (filePath.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    filePath = filePath,
                    fileName = fileName,
                    isLoadingPages = true,
                    errorMessage = null
                )
            }
            try {
                val bitmaps = pdfToolsRepository.renderAllPagesToBitmaps(filePath)
                if (bitmaps.isEmpty()) {
                    _state.update {
                        it.copy(
                            isLoadingPages = false,
                            errorMessage = "No pages found in this PDF"
                        )
                    }
                    return@launch
                }
                val items = bitmaps.mapIndexed { index, bitmap ->
                    OrganizePageItem(originalIndex = index, bitmap = bitmap)
                }
                _state.update {
                    it.copy(
                        pages = items,
                        isLoadingPages = false,
                        step = OrganizeFlowStep.REORDER_PAGES
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingPages = false,
                        errorMessage = e.message ?: "Failed to read PDF pages"
                    )
                }
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val list = current.pages.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            current.copy(pages = list)
        }
    }

    fun deletePage(index: Int) {
        val currentPages = _state.value.pages
        if (currentPages.size <= 1) {
            _state.update { it.copy(errorMessage = "At least 1 page is required") }
            return
        }
        _state.update { current ->
            val list = current.pages.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            current.copy(pages = list)
        }
    }

    fun prepareOrganizedPdf() {
        val pages = _state.value.pages
        if (pages.isEmpty()) {
            _state.update { it.copy(errorMessage = "No pages to export") }
            return
        }

        val baseName = _state.value.fileName.substringBeforeLast('.', _state.value.fileName)
            .ifBlank { "Business Plan" }
        val outputName = "${baseName}.pdf"

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isPreparing = true,
                    prepareProgress = 0,
                    errorMessage = null
                )
            }

            // Smooth progress counting from 0% up to 88% while export executes
            progressJob?.cancel()
            progressJob = launch {
                for (p in 1..88) {
                    delay(35)
                    _state.update { it.copy(prepareProgress = p) }
                }
            }

            val selectedIndices = pages.map { it.originalIndex }
            val result = organizePdfUseCase(_state.value.filePath, selectedIndices, outputName)

            progressJob?.cancel()

            result.onSuccess { path ->
                // Smoothly finish progress to 100%
                val currentP = _state.value.prepareProgress
                for (p in currentP..100) {
                    _state.update { it.copy(prepareProgress = p) }
                    delay(12)
                }
                delay(350) // Brief pause to see 100%
                _state.update {
                    it.copy(
                        isPreparing = false,
                        step = OrganizeFlowStep.SUCCESS,
                        outputFilePath = path,
                        outputFileName = File(path).name
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isPreparing = false,
                        errorMessage = error.message ?: "Failed to export PDF"
                    )
                }
            }
        }
    }

    fun renameOutputFile(newName: String) {
        val currentPath = _state.value.outputFilePath ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            val cleaned = newName.trim().removeSuffix(".pdf").removeSuffix(".PDF")
            documentRepository.renameDocument(currentPath, cleaned)
                .onSuccess { renamed ->
                    _state.update {
                        it.copy(
                            outputFilePath = renamed.path,
                            outputFileName = renamed.name
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(errorMessage = error.message ?: "Could not rename file") }
                }
        }
    }

    fun backToSelectFile() {
        _state.update {
            it.copy(
                step = OrganizeFlowStep.SELECT_FILE,
                pages = emptyList(),
                filePath = "",
                fileName = "",
                errorMessage = null
            )
        }
    }

    fun resetFlow() {
        _state.update {
            it.copy(
                step = OrganizeFlowStep.SELECT_FILE,
                pages = emptyList(),
                filePath = "",
                fileName = "",
                outputFilePath = null,
                outputFileName = null,
                errorMessage = null,
                isPreparing = false,
                prepareProgress = 0
            )
        }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }
}
