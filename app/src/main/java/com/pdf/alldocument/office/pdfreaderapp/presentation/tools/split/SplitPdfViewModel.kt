package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.PdfToolsRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SplitPdfViewModel @Inject constructor(
    private val pdfToolsRepository: PdfToolsRepository,
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SplitPdfUiState())
    val state: StateFlow<SplitPdfUiState> = _state.asStateFlow()

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
            current.copy(
                isSearching = open,
                searchQuery = if (!open) "" else current.searchQuery,
                filteredPdfs = if (!open) current.availablePdfs else current.filteredPdfs
            )
        }
    }

    fun selectPdf(doc: DocumentItem) {
        loadPdfDocument(doc.path, doc.name, doc)
    }

    fun selectCustomFile(filePath: String, fileName: String) {
        val file = File(filePath)
        val size = file.length()
        val fakeDoc = DocumentItem(
            path = filePath,
            name = fileName,
            size = size,
            formattedSize = FileUtils.formatFileSize(size),
            dateModified = file.lastModified(),
            formattedDate = FileUtils.formatDate(file.lastModified()),
            extension = FileUtils.getFileExtension(fileName),
            type = DocumentType.PDF
        )
        loadPdfDocument(filePath, fileName, fakeDoc)
    }

    private fun loadPdfDocument(filePath: String, fileName: String, doc: DocumentItem) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedPdfDoc = doc,
                    step = SplitFlowStep.SELECT_PAGES,
                    isLoadingPages = true,
                    errorMessage = null,
                    splitParts = emptyList(),
                    editingPartId = null
                )
            }
            try {
                val bitmaps = pdfToolsRepository.renderAllPagesToBitmaps(filePath)
                val pageItems = bitmaps.mapIndexed { index, bitmap ->
                    PdfPageItem(
                        pageIndex = index,
                        pageNumber = index + 1,
                        bitmap = bitmap,
                        isSelected = false
                    )
                }
                _state.update {
                    it.copy(
                        pdfPages = pageItems,
                        isLoadingPages = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingPages = false,
                        errorMessage = "Failed to load PDF pages: ${e.message}"
                    )
                }
            }
        }
    }

    fun togglePageSelection(pageIndex: Int) {
        _state.update { current ->
            val updated = current.pdfPages.map { page ->
                if (page.pageIndex == pageIndex) page.copy(isSelected = !page.isSelected)
                else page
            }
            current.copy(pdfPages = updated)
        }
    }

    fun toggleSelectAll() {
        _state.update { current ->
            val allSelected = current.pdfPages.all { it.isSelected }
            val updated = current.pdfPages.map { it.copy(isSelected = !allSelected) }
            current.copy(pdfPages = updated)
        }
    }

    fun confirmPageSelection() {
        val current = _state.value
        val selectedPages = current.pdfPages.filter { it.isSelected }.map { it.pageIndex }
        if (selectedPages.isEmpty()) return

        val docName = current.selectedPdfDoc?.name ?: "Business Plan.pdf"
        val rangeText = formatPageRanges(selectedPages)

        if (current.editingPartId != null) {
            // Update existing part
            val updatedParts = current.splitParts.map { part ->
                if (part.id == current.editingPartId) {
                    part.copy(
                        pageIndices = selectedPages,
                        pageRangeText = rangeText
                    )
                } else part
            }
            _state.update {
                it.copy(
                    splitParts = updatedParts,
                    editingPartId = null,
                    step = SplitFlowStep.SPLIT_LIST
                )
            }
        } else {
            // Add new part
            val newPart = SplitPart(
                id = UUID.randomUUID().toString(),
                name = docName,
                pageIndices = selectedPages,
                pageRangeText = rangeText
            )
            _state.update {
                it.copy(
                    splitParts = it.splitParts + newPart,
                    step = SplitFlowStep.SPLIT_LIST
                )
            }
        }
    }

    fun startAddMorePages() {
        _state.update { current ->
            // Reset selection for adding another part
            val unselectedPages = current.pdfPages.map { it.copy(isSelected = false) }
            current.copy(
                pdfPages = unselectedPages,
                editingPartId = null,
                step = SplitFlowStep.SELECT_PAGES
            )
        }
    }

    fun startEditPart(partId: String) {
        val current = _state.value
        val targetPart = current.splitParts.find { it.id == partId } ?: return

        val adjustedPages = current.pdfPages.map { page ->
            page.copy(isSelected = page.pageIndex in targetPart.pageIndices)
        }

        _state.update {
            it.copy(
                pdfPages = adjustedPages,
                editingPartId = partId,
                step = SplitFlowStep.SELECT_PAGES
            )
        }
    }

    fun removePart(partId: String) {
        _state.update { current ->
            val updated = current.splitParts.filterNot { it.id == partId }
            current.copy(
                splitParts = updated,
                step = if (updated.isEmpty()) SplitFlowStep.SELECT_PAGES else SplitFlowStep.SPLIT_LIST
            )
        }
    }

    fun executeSplit() {
        val current = _state.value
        val doc = current.selectedPdfDoc ?: return
        if (current.splitParts.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isSplitting = true, splitProgress = 5, errorMessage = null) }

            val results = mutableListOf<SplitResultItem>()
            val baseName = doc.name.substringBeforeLast('.', "Business Plan")
            val totalParts = current.splitParts.size

            val minDisplayTimeMs = 4500L
            val startTime = System.currentTimeMillis()

            try {
                for ((index, part) in current.splitParts.withIndex()) {
                    // Gradual visual progress
                    val targetProgress = ((index + 1) * 85) / totalParts
                    for (p in (_state.value.splitProgress + 5)..targetProgress step 5) {
                        _state.update { it.copy(splitProgress = p.coerceIn(5, 90)) }
                        delay(50)
                    }

                    val partFileName = if (totalParts == 1) {
                        "${baseName}_split.pdf"
                    } else {
                        "${baseName}_Part_${index + 1}.pdf"
                    }

                    val res = pdfToolsRepository.organizeAndExportPdf(
                        originalPdfPath = doc.path,
                        selectedPages = part.pageIndices,
                        outputFileName = partFileName
                    )

                    res.onSuccess { path ->
                        val generatedFile = File(path)
                        val sizeStr = FileUtils.formatFileSize(generatedFile.length())
                        results.add(
                            SplitResultItem(
                                fileName = generatedFile.name,
                                filePath = path,
                                fileSize = sizeStr,
                                pageCount = part.pageIndices.size
                            )
                        )
                    }.onFailure { err ->
                        throw err
                    }
                }

                // Hold dialog so user can see Native Ad for fixed duration
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < minDisplayTimeMs) {
                    delay(minDisplayTimeMs - elapsed)
                }

                // Final stretch to 100%
                for (p in _state.value.splitProgress..100 step 2) {
                    _state.update { it.copy(splitProgress = p.coerceAtMost(100)) }
                    delay(20)
                }
                delay(400)

                _state.update {
                    it.copy(
                        isSplitting = false,
                        splitProgress = 100,
                        splitResults = results,
                        step = SplitFlowStep.SPLIT_SUCCESS
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isSplitting = false,
                        errorMessage = "Splitting failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun handleBackPress(): Boolean {
        // Return true if handled internally, false if parent should pop navigation
        val current = _state.value
        return when (current.step) {
            SplitFlowStep.SELECT_FILE -> false
            SplitFlowStep.SELECT_PAGES -> {
                if (current.splitParts.isNotEmpty()) {
                    _state.update { it.copy(step = SplitFlowStep.SPLIT_LIST, editingPartId = null) }
                    true
                } else {
                    _state.update { it.copy(step = SplitFlowStep.SELECT_FILE, selectedPdfDoc = null) }
                    true
                }
            }
            SplitFlowStep.SPLIT_LIST -> {
                _state.update { it.copy(step = SplitFlowStep.SELECT_FILE, selectedPdfDoc = null, splitParts = emptyList()) }
                true
            }
            SplitFlowStep.SPLIT_SUCCESS -> {
                _state.update {
                    it.copy(
                        step = SplitFlowStep.SELECT_FILE,
                        selectedPdfDoc = null,
                        splitParts = emptyList(),
                        splitResults = emptyList()
                    )
                }
                true
            }
        }
    }

    fun resetToSelectFile() {
        _state.update {
            it.copy(
                step = SplitFlowStep.SELECT_FILE,
                selectedPdfDoc = null,
                pdfPages = emptyList(),
                splitParts = emptyList(),
                editingPartId = null,
                isSplitting = false,
                splitProgress = 0,
                splitResults = emptyList()
            )
        }
    }
}
