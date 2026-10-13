package com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.UserPrefsRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val documentRepository: DocumentRepository,
    private val userPrefsRepository: UserPrefsRepository,
    val interstitialAdHelper: com.pdf.alldocument.office.pdfreaderapp.core.ads.InterstitialAdHelper
) : ViewModel() {

    private val _state = MutableStateFlow(FileManagerState())
    val state = _state.asStateFlow()

    private val _effects = Channel<FileManagerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            var first = true
            userPrefsRepository.sortOrder.distinctUntilChanged().collect { order ->
                val shouldLoad = first || _state.value.sortOrder != order
                _state.update { it.copy(sortOrder = order) }
                first = false
                if (shouldLoad) load()
            }
        }
        viewModelScope.launch {
            userPrefsRepository.isGridView.collect { grid ->
                _state.update { it.copy(isGridView = grid) }
            }
        }
    }

    fun onEvent(event: FileManagerEvent) {
        when (event) {
            is FileManagerEvent.FilterChanged -> {
                if (_state.value.filter != event.filter) {
                    _state.update { it.copy(filter = event.filter) }
                    load()
                }
            }

            is FileManagerEvent.SortChanged -> {
                _state.update { it.copy(showSortSheet = false) }
                viewModelScope.launch { userPrefsRepository.setSortOrder(event.order) }
            }

            is FileManagerEvent.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                if (event.query.isBlank()) load() else search(event.query)
            }

            is FileManagerEvent.DocumentClicked -> open(event.document)
            is FileManagerEvent.FavoriteToggled -> toggleFavorite(event.document)
            is FileManagerEvent.DeleteRequested -> delete(event.document)
            is FileManagerEvent.RenameRequested -> rename(event.document, event.newName)

            is FileManagerEvent.SourceChanged -> {
                if (_state.value.source != event.source) {
                    _state.update { it.copy(source = event.source, query = "") }
                    load()
                }
            }

            FileManagerEvent.RefreshRequested -> load(forceRefresh = true)
            FileManagerEvent.GridViewToggled -> {
                viewModelScope.launch {
                    val next = !_state.value.isGridView
                    userPrefsRepository.setGridView(next)
                    _state.update { it.copy(isGridView = next) }
                }
            }

            FileManagerEvent.SortSheetOpened -> _state.update { it.copy(showSortSheet = true) }
            FileManagerEvent.SortSheetDismissed -> _state.update { it.copy(showSortSheet = false) }
            FileManagerEvent.MenuDismissed -> _state.update { it.copy(menuDocument = null) }
        }
    }

    /* ---- 3-dot menu sheet ---- */

    fun onMenuClick(document: DocumentItem) {
        _state.update { it.copy(menuDocument = document) }
    }

    fun onMenuFavorite() {
        val document = _state.value.menuDocument
        _state.update { it.copy(menuDocument = null) }
        document?.let(::toggleFavorite)
    }

    fun onMenuRename() {
        _state.update { it.copy(pendingRename = it.menuDocument, menuDocument = null) }
    }

    fun onMenuDelete() {
        _state.update { it.copy(pendingDelete = it.menuDocument, menuDocument = null) }
    }

    fun onDeleteDialogDismiss() = _state.update { it.copy(pendingDelete = null) }

    fun onRenameDialogDismiss() = _state.update { it.copy(pendingRename = null) }

    /* ---- data ---- */

    private fun currentFlow(forceRefresh: Boolean): Flow<List<DocumentItem>> =
        when (_state.value.source) {
            DocSource.RECENT -> documentRepository.getRecentDocuments()
            DocSource.BROWSER -> getDocumentsUseCase(
                type = _state.value.filter.documentType,
                sortOrder = _state.value.sortOrder,
                forceRefresh = forceRefresh
            )
        }

    private fun load(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.allDocuments.isEmpty()) }
            currentFlow(forceRefresh)
                .catch { e ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(
                        FileManagerEffect.ShowError(e.message ?: "Could not read files")
                    )
                }
                .collect { list ->
                    _state.update { it.copy(allDocuments = list, isLoading = false) }
                }
        }
    }

    private fun search(query: String) {
        if (_state.value.source == DocSource.RECENT) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            delay(150)
            searchDocumentsUseCase(query, _state.value.filter.documentType)
                .catch { /* keep showing the previous list */ }
                .collect { list ->
                    _state.update { it.copy(allDocuments = list, isLoading = false) }
                }
        }
    }

    /* ---- actions ---- */

    private fun open(document: DocumentItem) {
        viewModelScope.launch {
            // Send effect immediately
            _effects.send(
                if (document.extension.equals("pdf", ignoreCase = true)) {
                    FileManagerEffect.NavigateToPdfViewer(document)
                } else {
                    FileManagerEffect.NavigateToOfficeViewer(document)
                }
            )
            documentRepository.recordDocumentOpened(document.path, document.lastReadPage)
        }
    }

    private fun toggleFavorite(document: DocumentItem) {
        viewModelScope.launch {
            val next = !document.isFavorite
            _state.update { state ->
                state.copy(
                    allDocuments = state.allDocuments.map {
                        if (it.path == document.path) it.copy(isFavorite = next) else it
                    }
                )
            }
            documentRepository.toggleFavorite(document.path, next)
            _effects.send(
                FileManagerEffect.ShowMessage(
                    if (next) "Added to favourites" else "Removed from favourites"
                )
            )
        }
    }

    private fun rename(document: DocumentItem, newName: String) {
        _state.update { it.copy(pendingRename = null) }
        viewModelScope.launch {
            documentRepository.renameDocument(document.path, newName)
                .onSuccess {
                    _effects.send(FileManagerEffect.ShowMessage("Renamed successfully"))
                    load(forceRefresh = true)
                }
                .onFailure { e ->
                    _effects.send(FileManagerEffect.ShowError(e.message ?: "Rename failed"))
                }
        }
    }

    private fun delete(document: DocumentItem) {
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val ok = documentRepository.deleteDocument(document.path)
            if (ok) {
                _effects.send(FileManagerEffect.ShowMessage("Deleted ${document.name}"))
                load(forceRefresh = true)
            } else {
                _effects.send(FileManagerEffect.ShowError("Could not delete ${document.name}"))
            }
        }
    }
}
