package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LockPdfViewModel @Inject constructor(
    private val pdfToolsRepository: PdfToolsRepository,
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LockPdfUiState())
    val state: StateFlow<LockPdfUiState> = _state.asStateFlow()

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
        _state.update {
            it.copy(
                selectedDoc = doc,
                showPasswordDialog = true,
                passwordInput = "",
                errorMessage = null
            )
        }
    }

    fun selectCustomFile(filePath: String, fileName: String) {
        val file = File(filePath)
        val size = file.length()
        val customDoc = DocumentItem(
            path = filePath,
            name = fileName,
            size = size,
            formattedSize = FileUtils.formatFileSize(size),
            dateModified = file.lastModified(),
            formattedDate = FileUtils.formatDate(file.lastModified()),
            extension = FileUtils.getFileExtension(fileName),
            type = DocumentType.PDF
        )
        selectPdf(customDoc)
    }

    fun setPassword(password: String) {
        _state.update { it.copy(passwordInput = password) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun dismissPasswordDialog() {
        _state.update {
            it.copy(
                selectedDoc = null,
                showPasswordDialog = false,
                passwordInput = "",
                isPasswordVisible = false
            )
        }
    }

    fun lockSelectedPdf() {
        val current = _state.value
        val doc = current.selectedDoc ?: return
        val password = current.passwordInput
        if (password.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter a password") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLocking = true, errorMessage = null) }

            val baseName = doc.name.substringBeforeLast('.', "Document")
            val outputFileName = "${baseName}_locked.pdf"

            val result = pdfToolsRepository.lockPdf(
                originalPdfPath = doc.path,
                password = password,
                outputFileName = outputFileName
            )

            result.onSuccess { outputPath ->
                val outFile = File(outputPath)
                _state.update {
                    it.copy(
                        isLocking = false,
                        showPasswordDialog = false,
                        step = LockFlowStep.SUCCESS,
                        lockedResultPath = outputPath,
                        lockedResultName = outFile.name,
                        lockedResultSize = FileUtils.formatFileSize(outFile.length())
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLocking = false,
                        errorMessage = error.localizedMessage ?: "Failed to lock PDF"
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun resetFlow() {
        _state.update {
            it.copy(
                step = LockFlowStep.SELECT_FILE,
                selectedDoc = null,
                showPasswordDialog = false,
                passwordInput = "",
                isPasswordVisible = false,
                isLocking = false,
                lockedResultPath = null,
                lockedResultName = null,
                lockedResultSize = null,
                errorMessage = null
            )
        }
    }

    fun handleBackPress(): Boolean {
        val current = _state.value
        return when {
            current.showPasswordDialog -> {
                dismissPasswordDialog()
                true
            }
            current.isSearching -> {
                toggleSearch(false)
                true
            }
            current.step == LockFlowStep.SUCCESS -> {
                resetFlow()
                false
            }
            else -> false
        }
    }
}
