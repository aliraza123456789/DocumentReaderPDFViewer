package com.pdf.alldocument.office.pdfreaderapp.presentation.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentState(
    val documents: List<DocumentItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface RecentEffect {
    data class NavigateToPdfViewer(val document: DocumentItem) : RecentEffect
    data class NavigateToOfficeViewer(val document: DocumentItem) : RecentEffect
    data class ShowSnackbar(val message: String) : RecentEffect
    data class ShowError(val message: String) : RecentEffect
}

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecentState())
    val state: StateFlow<RecentState> = _state.asStateFlow()

    private val _effects = Channel<RecentEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.documents.isEmpty(), errorMessage = null) }

            documentRepository.getRecentDocuments()
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load recent files"
                        )
                    }
                }
                .collect { documents ->
                    _state.update {
                        it.copy(
                            documents = documents,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun openDocument(document: DocumentItem) {
        viewModelScope.launch {
            val effect = if (document.extension.equals("pdf", ignoreCase = true)) {
                RecentEffect.NavigateToPdfViewer(document)
            } else {
                RecentEffect.NavigateToOfficeViewer(document)
            }
            _effects.send(effect)
            documentRepository.recordDocumentOpened(document.path, document.lastReadPage)
        }
    }

    fun toggleFavorite(document: DocumentItem) {
        viewModelScope.launch {
            val newFav = !document.isFavorite
            _state.update { state ->
                state.copy(
                    documents = state.documents.map {
                        if (it.path == document.path) it.copy(isFavorite = newFav) else it
                    }
                )
            }
            documentRepository.toggleFavorite(document.path, newFav)
            _effects.send(
                RecentEffect.ShowSnackbar(
                    if (newFav) "Added to favourites" else "Removed from favourites"
                )
            )
        }
    }

    fun deleteDocument(document: DocumentItem) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(documents = state.documents.filterNot { it.path == document.path })
            }
            if (documentRepository.deleteDocument(document.path)) {
                _effects.send(RecentEffect.ShowSnackbar("'${document.name}' deleted"))
            } else {
                _effects.send(RecentEffect.ShowError("Failed to delete file"))
                refresh()
            }
        }
    }

    fun renameDocument(document: DocumentItem, newName: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(document.path, newName)
                .onSuccess { renamedDocument ->
                    _state.update { state ->
                        state.copy(
                            documents = state.documents.map {
                                if (it.path == document.path) renamedDocument else it
                            }
                        )
                    }
                    _effects.send(RecentEffect.ShowSnackbar("Renamed successfully"))
                }
                .onFailure { error ->
                    _effects.send(
                        RecentEffect.ShowError(error.message ?: "Rename failed")
                    )
                }
        }
    }
}
