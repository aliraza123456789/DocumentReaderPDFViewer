package com.pdf.alldocument.office.pdfreaderapp.presentation.favorites

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

data class FavoritesState(
    val documents: List<DocumentItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface FavoritesEffect {
    data class NavigateToPdfViewer(val document: DocumentItem) : FavoritesEffect
    data class NavigateToOfficeViewer(val document: DocumentItem) : FavoritesEffect
    data class ShowSnackbar(val message: String) : FavoritesEffect
    data class ShowError(val message: String) : FavoritesEffect
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    private val _effects = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.documents.isEmpty(), errorMessage = null) }

            documentRepository.getFavoriteDocuments()
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load favourites"
                        )
                    }
                }
                .collect { documents ->
                    _state.update {
                        it.copy(
                            documents = documents.sortedByDescending(DocumentItem::dateModified),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun openDocument(document: DocumentItem) {
        viewModelScope.launch {
            // Instant navigation
            val effect = if (document.extension.equals("pdf", ignoreCase = true)) {
                FavoritesEffect.NavigateToPdfViewer(document)
            } else {
                FavoritesEffect.NavigateToOfficeViewer(document)
            }
            _effects.send(effect)
            documentRepository.recordDocumentOpened(document.path, document.lastReadPage)
        }
    }

    fun removeFavorite(document: DocumentItem) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(documents = state.documents.filterNot { it.path == document.path })
            }
            documentRepository.toggleFavorite(document.path, false)
            _effects.send(FavoritesEffect.ShowSnackbar("Removed from favourites"))
        }
    }

    fun deleteDocument(document: DocumentItem) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(documents = state.documents.filterNot { it.path == document.path })
            }
            if (documentRepository.deleteDocument(document.path)) {
                _effects.send(FavoritesEffect.ShowSnackbar("'${document.name}' deleted"))
            } else {
                _effects.send(FavoritesEffect.ShowError("Failed to delete file"))
                refresh()
            }
        }
    }

    fun renameDocument(document: DocumentItem, newName: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(document.path, newName)
                .onSuccess { renamedDocument ->
                    val updatedDocument = renamedDocument.copy(isFavorite = true)
                    _state.update { state ->
                        state.copy(
                            documents = state.documents.map {
                                if (it.path == document.path) updatedDocument else it
                            }
                        )
                    }
                    _effects.send(FavoritesEffect.ShowSnackbar("Renamed successfully"))
                }
                .onFailure { error ->
                    _effects.send(
                        FavoritesEffect.ShowError(error.message ?: "Rename failed")
                    )
                }
        }
    }
}
