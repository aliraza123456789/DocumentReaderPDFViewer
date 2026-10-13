package com.pdf.alldocument.office.pdfreaderapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.UserPrefsRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val documentRepository: DocumentRepository,
    private val userPrefsRepository: UserPrefsRepository,
    val interstitialAdHelper: com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            var isFirstSortEmission = true
            userPrefsRepository.sortOrder
                .distinctUntilChanged()
                .collect { order ->
                    val isChanged = _state.value.sortOrder != order
                    val shouldLoad = isFirstSortEmission || isChanged
                    isFirstSortEmission = false
                    if (shouldLoad) {
                        if (_state.value.allDocuments.isNotEmpty()) {
                            _state.update { current ->
                                val sorted = sortDocuments(current.allDocuments, order)
                                val filtered = filterDocs(sorted, current.selectedTab, current.searchQuery)
                                current.copy(
                                    sortOrder = order,
                                    allDocuments = sorted,
                                    filteredDocuments = filtered
                                )
                            }
                        } else {
                            _state.update { it.copy(sortOrder = order) }
                            loadDocuments()
                        }
                    }
                }
        }
        viewModelScope.launch {
            userPrefsRepository.isGridView.collect { isGrid ->
                _state.update { it.copy(isGridView = isGrid) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.TabChanged -> {
                if (_state.value.selectedTab != event.type) {
                    val current = _state.value
                    val filtered = filterDocs(current.allDocuments, event.type, current.searchQuery)
                    _state.update { it.copy(selectedTab = event.type, filteredDocuments = filtered) }
                    if (_state.value.allDocuments.isEmpty() && loadJob?.isActive != true) {
                        loadDocuments()
                    }
                }
            }
            is HomeEvent.SortChanged -> {
                _state.update { it.copy(showSortSheet = false, sortOrder = event.order) }
                viewModelScope.launch(Dispatchers.Default) {
                    val current = _state.value
                    val sorted = sortDocuments(current.allDocuments, event.order)
                    val filtered = filterDocs(sorted, current.selectedTab, current.searchQuery)
                    _state.update { it.copy(allDocuments = sorted, filteredDocuments = filtered) }
                    userPrefsRepository.setSortOrder(event.order)
                }
            }
            is HomeEvent.SearchQueryChanged -> {
                val query = event.query
                _state.update {
                    it.copy(
                        searchQuery = query,
                        filteredDocuments = filterDocs(it.allDocuments, it.selectedTab, query)
                    )
                }
            }
            is HomeEvent.SearchDismissed -> {
                _state.update {
                    it.copy(
                        searchQuery = "",
                        filteredDocuments = filterDocs(it.allDocuments, it.selectedTab, "")
                    )
                }
            }
            is HomeEvent.DocumentClicked -> openDocument(event.document)
            is HomeEvent.FavoriteToggled -> toggleFavorite(event.document)
            is HomeEvent.DocumentDeleted -> deleteDocument(event.document)
            is HomeEvent.DocumentRenamed -> renameDocument(event.document, event.newName)
            HomeEvent.RefreshRequested -> loadDocuments(forceRefresh = true)
            HomeEvent.SortSheetDismissed -> _state.update { it.copy(showSortSheet = false) }
            HomeEvent.SortSheetOpened -> _state.update { it.copy(showSortSheet = true) }
            HomeEvent.GridViewToggled -> {
                viewModelScope.launch {
                    val newVal = !_state.value.isGridView
                    userPrefsRepository.setGridView(newVal)
                    _state.update { it.copy(isGridView = newVal) }
                }
            }
        }
    }

    private fun filterDocs(
        docs: List<DocumentItem>,
        type: DocumentType,
        query: String
    ): List<DocumentItem> {
        val typeFiltered = if (type == DocumentType.ALL) docs else docs.filter { it.type == type }
        return if (query.isBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    private fun sortDocuments(list: List<DocumentItem>, sortOrder: SortOrder): List<DocumentItem> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.dateModified }
            SortOrder.DATE_ASC -> list.sortedBy { it.dateModified }
            SortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
            SortOrder.SIZE_ASC -> list.sortedBy { it.size }
        }
    }

    private fun loadDocuments(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.allDocuments.isEmpty()) }
            val currentSort = _state.value.sortOrder
            getDocumentsUseCase(DocumentType.ALL, currentSort, forceRefresh)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { list ->
                    _state.update {
                        val filtered = filterDocs(list, it.selectedTab, it.searchQuery)
                        it.copy(
                            allDocuments = list,
                            filteredDocuments = filtered,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun openDocument(document: DocumentItem) {
        viewModelScope.launch {
            // Immediate navigation effect for instant responsiveness
            val effect = when (document.extension.lowercase()) {
                "pdf" -> HomeEffect.NavigateToPdfViewer(document)
                else -> HomeEffect.NavigateToOfficeViewer(document)
            }
            _effects.send(effect)

            // Asynchronously record document opened in the database
            documentRepository.recordDocumentOpened(document.path, document.lastReadPage)
        }
    }

    private fun toggleFavorite(document: DocumentItem) {
        viewModelScope.launch {
            val newFavState = !document.isFavorite
            _state.update { state ->
                state.copy(
                    allDocuments = state.allDocuments.map {
                        if (it.path == document.path) it.copy(isFavorite = newFavState) else it
                    },
                    filteredDocuments = state.filteredDocuments.map {
                        if (it.path == document.path) it.copy(isFavorite = newFavState) else it
                    }
                )
            }
            documentRepository.toggleFavorite(document.path, newFavState)
            val msg = if (newFavState) "Added to favorites" else "Removed from favorites"
            _effects.send(HomeEffect.ShowSnackbar(msg))
        }
    }

    private fun deleteDocument(document: DocumentItem) {
        viewModelScope.launch {
            val success = documentRepository.deleteDocument(document.path)
            if (success) {
                _effects.send(HomeEffect.ShowSnackbar("'${document.name}' deleted"))
                loadDocuments()
            } else {
                _effects.send(HomeEffect.ShowError("Failed to delete file"))
            }
        }
    }

    private fun renameDocument(document: DocumentItem, newName: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(document.path, newName)
                .onSuccess {
                    _effects.send(HomeEffect.ShowSnackbar("Renamed successfully"))
                    loadDocuments()
                }
                .onFailure { e ->
                    _effects.send(HomeEffect.ShowError(e.message ?: "Rename failed"))
                }
        }
    }
}
