package com.pdf.alldocument.office.pdfreaderapp.presentation.recent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.AppTopBar
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.ConfirmDeleteDialog
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.EmptyStateComposable
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.RenameDialog
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.home.DocumentList
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RecentScreen(
    viewModel: RecentViewModel = hiltViewModel(),
    onNavigateToPdfViewer: (DocumentItem) -> Unit,
    onNavigateToOfficeViewer: (DocumentItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var documentToDelete by remember { mutableStateOf<DocumentItem?>(null) }
    var documentToRename by remember { mutableStateOf<DocumentItem?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is RecentEffect.NavigateToPdfViewer -> {
                    onNavigateToPdfViewer(effect.document)
                }
                is RecentEffect.NavigateToOfficeViewer -> {
                    onNavigateToOfficeViewer(effect.document)
                }
                is RecentEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is RecentEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(title = "Recent Files")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.documents.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null && state.documents.isEmpty() -> {
                    EmptyStateComposable(
                        icon = Icons.Outlined.Schedule,
                        title = "Unable to load recent files",
                        subtitle = state.errorMessage ?: "Please try again",
                        modifier = Modifier.clickable(onClick = viewModel::refresh)
                    )
                }

                state.documents.isEmpty() -> {
                    EmptyStateComposable(
                        icon = Icons.Outlined.Schedule,
                        title = "No recent files",
                        subtitle = "Files you open will show up here"
                    )
                }

                else -> {
                    DocumentList(
                        documents = state.documents,
                        isGridView = false,
                        onDocumentClick = viewModel::openDocument,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onDeleteClick = { documentToDelete = it },
                        onRenameClick = { documentToRename = it }
                    )
                }
            }
        }
    }

    documentToDelete?.let { document ->
        ConfirmDeleteDialog(
            fileName = document.name,
            onDismiss = { documentToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(document)
                documentToDelete = null
            }
        )
    }

    documentToRename?.let { document ->
        RenameDialog(
            initialName = document.name.substringBeforeLast('.'),
            onDismiss = { documentToRename = null },
            onConfirm = { newName ->
                viewModel.renameDocument(document, newName)
                documentToRename = null
            }
        )
    }
}
