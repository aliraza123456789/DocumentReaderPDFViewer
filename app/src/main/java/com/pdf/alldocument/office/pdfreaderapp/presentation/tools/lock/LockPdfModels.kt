package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.lock

import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem

enum class LockFlowStep {
    SELECT_FILE,
    SUCCESS
}

data class LockPdfUiState(
    val step: LockFlowStep = LockFlowStep.SELECT_FILE,
    val availablePdfs: List<DocumentItem> = emptyList(),
    val filteredPdfs: List<DocumentItem> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedDoc: DocumentItem? = null,
    val showPasswordDialog: Boolean = false,
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isLocking: Boolean = false,
    val lockedResultPath: String? = null,
    val lockedResultName: String? = null,
    val lockedResultSize: String? = null,
    val errorMessage: String? = null
)
