package com.pdf.alldocument.viewer.pdfreaderapp.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.viewer.pdfreaderapp.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val selectedType: DocumentType = DocumentType.ALL,
    val results: List<DocumentItem> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        performSearch(query, _state.value.selectedType)
    }

    fun onTypeChange(type: DocumentType) {
        _state.update { it.copy(selectedType = type) }
        performSearch(_state.value.query, type)
    }

    private fun performSearch(query: String, type: DocumentType) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            delay(250) // debounce
            searchDocumentsUseCase(query, type)
                .catch { _state.update { it.copy(isSearching = false) } }
                .collect { list ->
                    _state.update { it.copy(results = list, isSearching = false) }
                }
        }
    }
}
