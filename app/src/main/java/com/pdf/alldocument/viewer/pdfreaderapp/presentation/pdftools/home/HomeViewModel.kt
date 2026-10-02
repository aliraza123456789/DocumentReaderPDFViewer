package com.pdf.alldocument.viewer.pdfreaderapp.presentation.pdftools.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.local.PdfFileEntity
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentFile(
    val id: Long = 0,
    val name: String,
    val size: String,
    val uriString: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    val recentFiles: StateFlow<List<RecentFile>> = repository.observeRecentFiles()
        .map { list ->
            list.map { entity ->
                RecentFile(
                    id = entity.id,
                    name = entity.name,
                    size = formatFileSize(entity.sizeBytes),
                    uriString = entity.uriString
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                var fileName = "document_${System.currentTimeMillis()}.pdf"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
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

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createFolder(name.trim())
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "${bytes / 1024} KB"
            bytes > 0 -> "$bytes B"
            else -> "PDF"
        }
    }
}
