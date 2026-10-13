package com.pdf.alldocument.office.pdfreaderapp.presentation.pdftools.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.data.pdftools.local.FolderEntity
import com.pdf.alldocument.office.pdfreaderapp.data.pdftools.local.PdfFileEntity
import com.pdf.alldocument.office.pdfreaderapp.data.pdftools.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    val files: StateFlow<List<PdfFileEntity>> = repository.observeRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinFiles: StateFlow<List<PdfFileEntity>> = repository.observeRecycleBin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createFolder(name.trim())
            }
        }
    }

    fun deleteFile(id: Long) {
        viewModelScope.launch {
            repository.moveToRecycleBin(id)
        }
    }

    fun restoreFile(id: Long) {
        viewModelScope.launch {
            repository.restoreFile(id)
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            repository.deletePermanently(id)
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
        }
    }

    fun importPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                var fileName = "imported_${System.currentTimeMillis()}.pdf"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }
                repository.insertRecentFile(
                    PdfFileEntity(
                        name = fileName,
                        uriString = uri.toString(),
                        sizeBytes = fileSize,
                        lastOpenedAt = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
