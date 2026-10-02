package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.organize

import android.graphics.Bitmap
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

data class OrganizePageItem(
    val originalIndex: Int,
    val bitmap: Bitmap,
    val isSelected: Boolean = true
)

data class OrganizePdfState(
    val filePath: String = "",
    val title: String = "",
    val pages: List<OrganizePageItem> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class OrganizePdfViewModel @Inject constructor(
    private val pdfToolsRepository: PdfToolsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrganizePdfState())
    val state: StateFlow<OrganizePdfState> = _state.asStateFlow()

    fun loadPdf(filePath: String, title: String) {
        if (filePath.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(filePath = filePath, title = title, isLoading = true) }
            val bitmaps = pdfToolsRepository.renderAllPagesToBitmaps(filePath)
            val items = bitmaps.mapIndexed { index, bitmap ->
                OrganizePageItem(originalIndex = index, bitmap = bitmap, isSelected = true)
            }
            _state.update { it.copy(pages = items, isLoading = false) }
        }
    }

    fun togglePageSelection(index: Int) {
        _state.update { current ->
            val list = current.pages.toMutableList()
            if (index in list.indices) {
                val item = list[index]
                list[index] = item.copy(isSelected = !item.isSelected)
            }
            current.copy(pages = list)
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val list = current.pages.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            current.copy(pages = list)
        }
    }

    fun deletePage(index: Int) {
        _state.update { current ->
            val list = current.pages.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            current.copy(pages = list)
        }
    }

    fun exportOrganizedPdf(outputName: String) {
        val selected = _state.value.pages.filter { it.isSelected }
        if (selected.isEmpty()) {
            _state.update { it.copy(errorMessage = "Please select at least 1 page to export") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, errorMessage = null) }
            val selectedIndices = selected.map { it.originalIndex }
            pdfToolsRepository.organizeAndExportPdf(_state.value.filePath, selectedIndices, outputName)
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
