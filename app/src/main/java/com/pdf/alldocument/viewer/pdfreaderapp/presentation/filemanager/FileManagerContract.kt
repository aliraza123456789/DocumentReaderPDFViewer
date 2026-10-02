package com.pdf.alldocument.viewer.pdfreaderapp.presentation.filemanager

import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.SortOrder

/**
 * Which repository feed the list is showing.
 *
 * `BROWSER` is the main filtered/sorted listing; `RECENT` is the
 * last-opened feed. The bottom nav's Home/Recent tabs switch between them.
 */
enum class DocSource { BROWSER, RECENT }

/** MVI contract for the file manager screen. */
sealed interface FileManagerEvent {
    data class FilterChanged(val filter: FileFilter) : FileManagerEvent
    data class SortChanged(val order: SortOrder) : FileManagerEvent
    data class QueryChanged(val query: String) : FileManagerEvent
    data class DocumentClicked(val document: DocumentItem) : FileManagerEvent
    data class FavoriteToggled(val document: DocumentItem) : FileManagerEvent
    data class DeleteRequested(val document: DocumentItem) : FileManagerEvent
    data class RenameRequested(val document: DocumentItem, val newName: String) : FileManagerEvent
    data class SourceChanged(val source: DocSource) : FileManagerEvent
    data object RefreshRequested : FileManagerEvent
    data object GridViewToggled : FileManagerEvent
    data object SortSheetOpened : FileManagerEvent
    data object SortSheetDismissed : FileManagerEvent
    data object MenuDismissed : FileManagerEvent
}

data class FileManagerState(
    val allDocuments: List<DocumentItem> = emptyList(),
    val source: DocSource = DocSource.BROWSER,
    val filter: FileFilter = FileFilter.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val query: String = "",
    val isLoading: Boolean = false,
    val isGridView: Boolean = false,
    val showSortSheet: Boolean = false,
    val menuDocument: DocumentItem? = null,
    val pendingDelete: DocumentItem? = null,
    val pendingRename: DocumentItem? = null
) {
    /** Filter applied on top of the loaded documents. */
    val visibleDocuments: List<DocumentItem>
        get() = allDocuments.filter { filter.matches(it.type.toFileType()) }

    /** Ad-injected entries backing the LazyColumn. */
    val entries: List<ListEntry> get() = buildListEntries(visibleDocuments)
}

sealed interface FileManagerEffect {
    data class NavigateToPdfViewer(val document: DocumentItem) : FileManagerEffect
    data class NavigateToOfficeViewer(val document: DocumentItem) : FileManagerEffect
    data class ShowMessage(val message: String) : FileManagerEffect
    data class ShowError(val message: String) : FileManagerEffect
}
