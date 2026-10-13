package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.merge

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.MergePdfUseCase
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
import javax.inject.Inject

enum class MergeFlowStep {
    SELECT_FILES,     // Images 48.png & 49.png
    REORDER_FILES,    // Image 50.png
    SUCCESS           // Image 53.png
}

data class MergePdfState(
    val step: MergeFlowStep = MergeFlowStep.SELECT_FILES,
    val availableFiles: List<DocumentItem> = emptyList(),
    val filteredFiles: List<DocumentItem> = emptyList(),
    val selectedFiles: List<DocumentItem> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoadingFiles: Boolean = true,
    val showFileNameDialog: Boolean = false, // Image 51.png
    val proposedFileName: String = "",
    val isMerging: Boolean = false,          // Image 52.png
    val mergeProgress: Int = 0,
    val mergedFilePath: String? = null,      // Image 53.png
    val mergedFileName: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    private val mergePdfUseCase: MergePdfUseCase,
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MergePdfState())
    val state: StateFlow<MergePdfState> = _state.asStateFlow()

    private var progressJob: Job? = null

    init {
        loadDevicePdfs()
    }

    private fun loadDevicePdfs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFiles = true) }
            getDocumentsUseCase(DocumentType.PDF, SortOrder.DATE_DESC).collect { docs ->
                _state.update { current ->
                    val filtered = filterDocs(docs, current.searchQuery)
                    current.copy(
                        availableFiles = docs,
                        filteredFiles = filtered,
                        isLoadingFiles = false
                    )
                }
            }
        }
    }

    private fun filterDocs(docs: List<DocumentItem>, query: String): List<DocumentItem> {
        return if (query.isBlank()) docs
        else docs.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun toggleSearch(active: Boolean? = null) {
        _state.update { current ->
            val nextActive = active ?: !current.isSearchActive
            val nextQuery = if (!nextActive) "" else current.searchQuery
            val filtered = filterDocs(current.availableFiles, nextQuery)
            current.copy(
                isSearchActive = nextActive,
                searchQuery = nextQuery,
                filteredFiles = filtered
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            val filtered = filterDocs(current.availableFiles, query)
            current.copy(searchQuery = query, filteredFiles = filtered)
        }
    }

    fun toggleFileSelection(file: DocumentItem) {
        _state.update { current ->
            val isAlreadySelected = current.selectedFiles.any { it.path == file.path }
            val updated = if (isAlreadySelected) {
                current.selectedFiles.filter { it.path != file.path }
            } else {
                current.selectedFiles + file
            }
            current.copy(selectedFiles = updated)
        }
    }

    fun addSelectedUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val added = mutableListOf<DocumentItem>()
            for (uri in uris) {
                val tempFile = FileUtils.copyUriToTempFile(
                    context,
                    uri,
                    "merge_${System.currentTimeMillis()}_${uri.lastPathSegment ?: "file"}.pdf"
                )
                if (tempFile != null && tempFile.exists()) {
                    val item = DocumentItem(
                        path = tempFile.absolutePath,
                        name = tempFile.name,
                        size = tempFile.length(),
                        formattedSize = FileUtils.formatFileSize(tempFile.length()),
                        dateModified = tempFile.lastModified(),
                        formattedDate = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(tempFile.lastModified())),
                        extension = "pdf",
                        type = DocumentType.PDF
                    )
                    added.add(item)
                }
            }
            _state.update { current ->
                val newAvailable = current.availableFiles + added
                val newSelected = current.selectedFiles + added
                current.copy(
                    availableFiles = newAvailable,
                    filteredFiles = filterDocs(newAvailable, current.searchQuery),
                    selectedFiles = newSelected
                )
            }
        }
    }

    fun proceedToReorder(): Boolean {
        val count = _state.value.selectedFiles.size
        if (count < 2) {
            _state.update { it.copy(errorMessage = "Please select at least 2 PDF files to merge") }
            return false
        }
        val defaultName = _state.value.selectedFiles.firstOrNull()?.name?.let {
            if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
        } ?: "Merged_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"

        _state.update { it.copy(step = MergeFlowStep.REORDER_FILES, proposedFileName = defaultName) }
        return true
    }

    fun backToSelection() {
        _state.update { it.copy(step = MergeFlowStep.SELECT_FILES) }
    }

    fun removeSelectedFile(file: DocumentItem) {
        _state.update { current ->
            current.copy(selectedFiles = current.selectedFiles.filter { it.path != file.path })
        }
    }

    fun moveFile(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val list = current.selectedFiles.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            current.copy(selectedFiles = list)
        }
    }

    fun openFileNameDialog() {
        val defaultName = _state.value.selectedFiles.firstOrNull()?.name?.let {
            if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
        } ?: "Merged_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"

        _state.update { it.copy(showFileNameDialog = true, proposedFileName = defaultName) }
    }

    fun dismissFileNameDialog() {
        _state.update { it.copy(showFileNameDialog = false) }
    }

    fun mergePdfs(outputName: String) {
        val files = _state.value.selectedFiles
        if (files.size < 2) {
            _state.update { it.copy(errorMessage = "Select at least 2 PDF files to merge") }
            return
        }

        val cleanedName = outputName.trim().let {
            if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    showFileNameDialog = false,
                    isMerging = true,
                    mergeProgress = 0,
                    errorMessage = null
                )
            }

            // Smooth progress counting from 0% up to 88% while merge executes
            progressJob?.cancel()
            progressJob = launch {
                for (p in 1..88) {
                    delay(35)
                    _state.update { it.copy(mergeProgress = p) }
                }
            }

            val paths = files.map { it.path }
            val result = mergePdfUseCase(paths, cleanedName)

            progressJob?.cancel()

            result.onSuccess { path ->
                // Smoothly finish progress to 100%
                val currentP = _state.value.mergeProgress
                for (p in currentP..100) {
                    _state.update { it.copy(mergeProgress = p) }
                    delay(12)
                }
                delay(350) // Brief pause to see 100%
                _state.update {
                    it.copy(
                        isMerging = false,
                        step = MergeFlowStep.SUCCESS,
                        mergedFilePath = path,
                        mergedFileName = File(path).name
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isMerging = false,
                        errorMessage = error.message ?: "Merge failed. Please try again."
                    )
                }
            }
        }
    }

    fun renameMergedFile(newName: String) {
        val currentPath = _state.value.mergedFilePath ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            val cleaned = newName.trim().removeSuffix(".pdf").removeSuffix(".PDF")
            documentRepository.renameDocument(currentPath, cleaned)
                .onSuccess { renamed ->
                    _state.update {
                        it.copy(
                            mergedFilePath = renamed.path,
                            mergedFileName = renamed.name
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(errorMessage = error.message ?: "Could not rename file") }
                }
        }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun resetFlow() {
        _state.update {
            it.copy(
                step = MergeFlowStep.SELECT_FILES,
                selectedFiles = emptyList(),
                isMerging = false,
                mergeProgress = 0,
                mergedFilePath = null,
                mergedFileName = null,
                errorMessage = null
            )
        }
    }
}
