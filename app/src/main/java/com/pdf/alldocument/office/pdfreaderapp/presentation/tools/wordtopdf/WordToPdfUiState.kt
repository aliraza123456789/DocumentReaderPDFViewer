package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.wordtopdf

import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import java.io.File

enum class WordToPdfStep {
    SELECT_FILE,
    SUCCESS
}

data class WordToPdfUiState(
    val step: WordToPdfStep = WordToPdfStep.SELECT_FILE,
    val availableDocs: List<DocumentItem> = emptyList(),
    val filteredDocs: List<DocumentItem> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedDoc: DocumentItem? = null,
    val isRenaming: Boolean = false,
    val customFileName: String = "",
    val isConverting: Boolean = false,
    val conversionProgress: Int = 0,
    val convertedFile: File? = null,
    val convertedFilePath: String? = null,
    val convertedFileName: String? = null,
    val errorMessage: String? = null
)
