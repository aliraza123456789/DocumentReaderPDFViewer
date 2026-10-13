package com.pdf.alldocument.office.pdfreaderapp.presentation.home

import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder

// MVI Contract for Home Screen
sealed interface HomeEvent {
    data class TabChanged(val type: DocumentType) : HomeEvent
    data class SortChanged(val order: SortOrder) : HomeEvent
    data class SearchQueryChanged(val query: String) : HomeEvent
    data class DocumentClicked(val document: DocumentItem) : HomeEvent
    data class FavoriteToggled(val document: DocumentItem) : HomeEvent
    data class DocumentDeleted(val document: DocumentItem) : HomeEvent
    data class DocumentRenamed(val document: DocumentItem, val newName: String) : HomeEvent
    data object RefreshRequested : HomeEvent
    data object SearchDismissed : HomeEvent
    data object SortSheetDismissed : HomeEvent
    data object SortSheetOpened : HomeEvent
    data object GridViewToggled : HomeEvent
}

data class HomeState(
    val allDocuments: List<DocumentItem> = emptyList(),
    val filteredDocuments: List<DocumentItem> = emptyList(),
    val selectedTab: DocumentType = DocumentType.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isGridView: Boolean = false,
    val showSortSheet: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

sealed interface HomeEffect {
    data class NavigateToPdfViewer(val document: DocumentItem) : HomeEffect
    data class NavigateToOfficeViewer(val document: DocumentItem) : HomeEffect
    data class ShowError(val message: String) : HomeEffect
    data class ShowSnackbar(val message: String) : HomeEffect
}
