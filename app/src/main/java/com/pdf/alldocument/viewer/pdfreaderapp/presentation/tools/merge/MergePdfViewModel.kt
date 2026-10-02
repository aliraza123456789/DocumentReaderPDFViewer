package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.merge

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.viewer.pdfreaderapp.domain.usecase.MergePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SelectedPdfFile(
    val id: String,
    val path: String,
    val name: String,
    val formattedSize: String
)

data class MergePdfState(
    val selectedFiles: List<SelectedPdfFile> = emptyList(),
    val isMerging: Boolean = false,
    val mergedFilePath: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    private val mergePdfUseCase: MergePdfUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MergePdfState())
    val state: StateFlow<MergePdfState> = _state.asStateFlow()

    fun addSelectedUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val added = mutableListOf<SelectedPdfFile>()
            for (uri in uris) {
                val tempFile = FileUtils.copyUriToTempFile(
                    context,
                    uri,
                    "merge_${System.currentTimeMillis()}_${uri.lastPathSegment ?: "file"}.pdf"
                )
                if (tempFile != null && tempFile.exists()) {
                    added.add(
                        SelectedPdfFile(
                            id = tempFile.absolutePath,
                            path = tempFile.absolutePath,
                            name = tempFile.name,
                            formattedSize = FileUtils.formatFileSize(tempFile.length())
                        )
                    )
                }
            }
            _state.update { it.copy(selectedFiles = it.selectedFiles + added) }
        }
    }

    fun removeFile(file: SelectedPdfFile) {
        _state.update { current ->
            current.copy(selectedFiles = current.selectedFiles.filter { it.id != file.id })
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

    fun mergePdfs(outputName: String) {
        val files = _state.value.selectedFiles
        if (files.size < 2) {
            _state.update { it.copy(errorMessage = "Select at least 2 PDF files to merge") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isMerging = true, errorMessage = null) }
            val paths = files.map { it.path }
            mergePdfUseCase(paths, outputName)
                .onSuccess { path ->
                    _state.update { it.copy(isMerging = false, mergedFilePath = path) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isMerging = false, errorMessage = error.message ?: "Merge failed") }
                }
        }
    }

    fun clearResult() {
        _state.update { it.copy(mergedFilePath = null, errorMessage = null) }
    }
}
