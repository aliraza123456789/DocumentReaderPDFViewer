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
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.ValidatePdfUseCase
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
    private val validatePdfUseCase: ValidatePdfUseCase,
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
        val isAlreadySelected = _state.value.selectedFiles.any { it.path == file.path }
        if (isAlreadySelected) {
            _state.update { current ->
                current.copy(selectedFiles = current.selectedFiles.filter { it.path != file.path })
            }
            return
        }

        viewModelScope.launch {
            val validation = validatePdfUseCase(file.path)
            if (validation.isFailure) {
                val errorMsg = validation.exceptionOrNull()?.message
                    ?: "Cannot select \"${file.name}\": The file is corrupted or not a valid PDF."
                _state.update { it.copy(errorMessage = errorMsg) }
                return@launch
            }

            _state.update { current ->
                if (current.selectedFiles.any { it.path == file.path }) current
                else current.copy(selectedFiles = current.selectedFiles + file)
            }
        }
    }

    fun addSelectedUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val added = mutableListOf<DocumentItem>()
            var corruptCount = 0
            for (uri in uris) {
                val rawSegment = uri.lastPathSegment ?: "file"
                val cleanSegment = rawSegment
                    .substringAfterLast('/')
                    .substringAfterLast(':')
                    .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    .removeSuffix(".pdf")
                val safeTargetName = "merge_${System.currentTimeMillis()}_${cleanSegment.ifBlank { "file" }}.pdf"
                val tempFile = FileUtils.copyUriToTempFile(
                    context,
                    uri,
                    safeTargetName
                )
                if (tempFile != null && tempFile.exists()) {
                    val validation = validatePdfUseCase(tempFile.absolutePath)
                    if (validation.isSuccess) {
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
                    } else {
                        corruptCount++
                        runCatching { tempFile.delete() }
                    }
                }
            }
            _state.update { current ->
                val newAvailable = (added + current.availableFiles).distinctBy { it.path }
                val newSelected = (current.selectedFiles + added).distinctBy { it.path }
                val errorMsg = if (corruptCount > 0) {
                    "$corruptCount file(s) could not be selected because they are corrupted or invalid."
                } else current.errorMessage
                current.copy(
                    availableFiles = newAvailable,
                    filteredFiles = filterDocs(newAvailable, current.searchQuery),
                    selectedFiles = newSelected,
                    errorMessage = errorMsg
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

            val minDisplayTimeMs = 4500L
            val startTime = System.currentTimeMillis()

            // Smooth progress counting from 0% up to 90% over ~4.5 seconds
            progressJob?.cancel()
            progressJob = launch {
                for (p in 1..90) {
                    delay(50)
                    _state.update { it.copy(mergeProgress = p) }
                }
            }

            val paths = files.map { it.path }
            val result = mergePdfUseCase(paths, cleanedName)

            // Hold dialog so user can see Native Ad for fixed duration
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < minDisplayTimeMs) {
                delay(minDisplayTimeMs - elapsed)
            }
            progressJob?.cancel()

            result.onSuccess { path ->
                // Smoothly finish progress to 100%
                val currentP = _state.value.mergeProgress
                for (p in currentP..100) {
                    _state.update { it.copy(mergeProgress = p) }
                    delay(15)
                }
                delay(400) // Brief pause to see 100%
                _state.update {
                    it.copy(
                        isMerging = false,
                        step = MergeFlowStep.SUCCESS,
                        mergedFilePath = path,
                        mergedFileName = File(path).name
                    )
                }
            }.onFailure { error ->
                val rawMsg = error.message.orEmpty()
                val errorMsg = when {
                    rawMsg.contains("root object specification", ignoreCase = true) ->
                        "One or more selected files are corrupted (missing root trailer) and cannot be merged."
                    rawMsg.contains("header signature", ignoreCase = true) ->
                        "One or more selected files are corrupted or not valid PDF documents."
                    rawMsg.isNotBlank() -> rawMsg
                    else -> "Merge failed. Please try again."
                }
                _state.update {
                    it.copy(
                        isMerging = false,
                        errorMessage = errorMsg
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
