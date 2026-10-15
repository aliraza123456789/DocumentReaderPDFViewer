package com.pdf.alldocument.office.pdfreaderapp.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdf.alldocument.office.pdfreaderapp.core.designsystem.EmptyStateComposable
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.home.DocTypeIcon
import com.pdf.alldocument.office.pdfreaderapp.presentation.home.DocumentListItem
import com.pdf.alldocument.office.pdfreaderapp.presentation.home.DocumentTabs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    initialType: String = "ALL",
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onDocumentClick: (DocumentItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Search documents by name...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DocumentTabs(
                selectedTab = state.selectedType,
                onTabSelected = { viewModel.onTypeChange(it) }
            )

            if (state.isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.query.isNotBlank() && state.results.isEmpty()) {
                EmptyStateComposable(
                    icon = Icons.Default.Search,
                    title = "No results for \"${state.query}\"",
                    subtitle = "Check your spelling or try searching another keyword"
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.path }) { doc ->
                        DocumentListItem(
                            document = doc,
                            onClick = { onDocumentClick(doc) },
                            onFavoriteClick = {},
                            onDeleteClick = {},
                            onRenameClick = {}
                        )
                    }
                }
            }
        }
    }
}
