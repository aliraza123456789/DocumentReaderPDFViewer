package com.pdf.alldocument.viewer.pdfreaderapp.presentation.pdftools.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ToolUiState {
    data object Idle : ToolUiState
    data object Loading : ToolUiState
    data class Success(val output: File) : ToolUiState
    data class Error(val message: String) : ToolUiState
}

@HiltViewModel
class TextToPdfViewModel @Inject constructor(
    private val repo: PdfRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ToolUiState>(ToolUiState.Idle)
    val state: StateFlow<ToolUiState> = _state.asStateFlow()

    fun convert(text: String, fontSize: Float, outFile: File) {
        viewModelScope.launch {
            _state.value = ToolUiState.Loading
            runCatching { repo.textToPdf(text, fontSize, outFile) }
                .onSuccess { _state.value = ToolUiState.Success(outFile) }
                .onFailure { _state.value = ToolUiState.Error(it.message ?: "Error") }
        }
    }

    fun reset() { _state.value = ToolUiState.Idle }
}

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    private val repo: PdfRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ToolUiState>(ToolUiState.Idle)
    val state: StateFlow<ToolUiState> = _state.asStateFlow()

    fun merge(sources: List<Uri>, outFile: File) {
        viewModelScope.launch {
            _state.value = ToolUiState.Loading
            runCatching { repo.mergePdfs(sources, outFile) }
                .onSuccess { _state.value = ToolUiState.Success(outFile) }
                .onFailure { _state.value = ToolUiState.Error(it.message ?: "Merge failed") }
        }
    }
}

@HiltViewModel
class SinglePdfToolViewModel @Inject constructor(
    private val repo: PdfRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ToolUiState>(ToolUiState.Idle)
    val state: StateFlow<ToolUiState> = _state.asStateFlow()

    fun pageCount(uri: Uri): Int = repo.pageCount(uri)

    fun run(outFile: File, block: suspend PdfRepository.(Uri) -> Unit, source: Uri) {
        viewModelScope.launch {
            _state.value = ToolUiState.Loading
            runCatching { repo.block(source) }
                .onSuccess { _state.value = ToolUiState.Success(outFile) }
                .onFailure { _state.value = ToolUiState.Error(it.message ?: "Error") }
        }
    }

    fun reset() { _state.value = ToolUiState.Idle }
}
