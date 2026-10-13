package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.wordtopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import com.pdf.alldocument.office.pdfreaderapp.presentation.tools.officetopdf.OfficeToPdfEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

@HiltViewModel
class WordToPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val engine = OfficeToPdfEngine()
    private val _state = MutableStateFlow(WordToPdfUiState())
    val state: StateFlow<WordToPdfUiState> = _state.asStateFlow()

    init {
        loadWordDocuments()
    }

    private fun loadWordDocuments() {
        viewModelScope.launch {
            getDocumentsUseCase(DocumentType.WORD, SortOrder.DATE_DESC).collect { docs ->
                _state.update { current ->
                    val filtered = if (current.searchQuery.isBlank()) docs
                    else docs.filter { it.name.contains(current.searchQuery, ignoreCase = true) }
                    current.copy(availableDocs = docs, filteredDocs = filtered)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            val filtered = if (query.isBlank()) current.availableDocs
            else current.availableDocs.filter { it.name.contains(query, ignoreCase = true) }
            current.copy(searchQuery = query, filteredDocs = filtered)
        }
    }

    fun toggleSearch(open: Boolean) {
        _state.update { current ->
            current.copy(
                isSearching = open,
                searchQuery = if (!open) "" else current.searchQuery,
                filteredDocs = if (!open) current.availableDocs else current.filteredDocs
            )
        }
    }

    fun selectDocument(doc: DocumentItem) {
        _state.update { current ->
            if (current.selectedDoc?.path == doc.path) {
                // Deselect if already selected
                current.copy(selectedDoc = null)
            } else {
                current.copy(selectedDoc = doc)
            }
        }
    }

    fun selectCustomDocument(uri: Uri, displayName: String) {
        val temp = FileUtils.copyUriToTempFile(context, uri, displayName) ?: return
        val doc = DocumentItem(
            path = temp.absolutePath,
            name = displayName,
            size = temp.length(),
            formattedSize = FileUtils.formatFileSize(temp.length()),
            dateModified = temp.lastModified(),
            formattedDate = FileUtils.formatDate(temp.lastModified()),
            extension = FileUtils.getFileExtension(displayName),
            type = DocumentType.WORD
        )
        _state.update { current ->
            val updatedList = listOf(doc) + current.availableDocs.filter { it.path != doc.path }
            current.copy(
                availableDocs = updatedList,
                filteredDocs = updatedList,
                selectedDoc = doc
            )
        }
    }

    fun onConvertClicked() {
        val selected = _state.value.selectedDoc ?: return
        val baseName = selected.name.substringBeforeLast('.')
        _state.update {
            it.copy(
                isRenaming = true,
                customFileName = baseName
            )
        }
    }

    fun updateCustomFileName(name: String) {
        _state.update { it.copy(customFileName = name) }
    }

    fun dismissRenameDialog() {
        _state.update { it.copy(isRenaming = false) }
    }

    fun startConversion() {
        val selected = _state.value.selectedDoc ?: return
        val targetName = _state.value.customFileName.trim().ifBlank {
            selected.name.substringBeforeLast('.')
        }

        _state.update {
            it.copy(
                isRenaming = false,
                isConverting = true,
                conversionProgress = 0,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outFile = FileUtils.newOutputFile(context, targetName, "pdf")
                val kind = engine.kindOf(selected.name) ?: OfficeToPdfEngine.Kind.DOCX

                engine.convert(
                    input = {
                        val file = File(selected.path)
                        if (file.exists()) {
                            FileInputStream(file)
                        } else {
                            val uri = Uri.parse(selected.path)
                            context.contentResolver.openInputStream(uri)
                                ?: FileInputStream(file)
                        }
                    },
                    kind = kind,
                    output = outFile,
                    onProgress = { p ->
                        _state.update { it.copy(conversionProgress = p) }
                    }
                )

                _state.update {
                    it.copy(
                        isConverting = false,
                        conversionProgress = 100,
                        convertedFile = outFile,
                        convertedFilePath = outFile.absolutePath,
                        convertedFileName = outFile.name,
                        step = WordToPdfStep.SUCCESS
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConverting = false,
                        errorMessage = e.message ?: "Failed to convert document"
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
                step = WordToPdfStep.SELECT_FILE,
                selectedDoc = null,
                isRenaming = false,
                isConverting = false,
                conversionProgress = 0,
                convertedFile = null,
                convertedFilePath = null,
                convertedFileName = null,
                errorMessage = null
            )
        }
    }

    fun handleBackPress(): Boolean {
        if (_state.value.isRenaming) {
            _state.update { it.copy(isRenaming = false) }
            return true
        }
        if (_state.value.isSearching) {
            toggleSearch(false)
            return true
        }
        if (_state.value.selectedDoc != null && _state.value.step == WordToPdfStep.SELECT_FILE) {
            _state.update { it.copy(selectedDoc = null) }
            return true
        }
        if (_state.value.step == WordToPdfStep.SUCCESS) {
            resetFlow()
            return true
        }
        return false
    }
}
