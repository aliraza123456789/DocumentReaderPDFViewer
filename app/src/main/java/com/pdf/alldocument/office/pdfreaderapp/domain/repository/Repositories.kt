package com.pdf.alldocument.office.pdfreaderapp.domain.repository

import android.graphics.Bitmap
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getDocuments(
        type: DocumentType,
        sortOrder: SortOrder,
        forceRefresh: Boolean = false
    ): Flow<List<DocumentItem>>
    fun getFavoriteDocuments(): Flow<List<DocumentItem>>
    fun getRecentDocuments(): Flow<List<DocumentItem>>
    fun searchDocuments(query: String, type: DocumentType): Flow<List<DocumentItem>>
    suspend fun toggleFavorite(path: String, isFavorite: Boolean)
    suspend fun recordDocumentOpened(path: String, page: Int = 0)
    suspend fun updateLastReadPage(path: String, page: Int)
    suspend fun renameDocument(oldPath: String, newName: String): Result<DocumentItem>
    suspend fun deleteDocument(path: String): Boolean
    fun streamPdfPages(path: String, initialPage: Int = 0): Flow<PageRenderData>
    suspend fun getPdfPageCount(path: String): Int
}

interface PdfToolsRepository {
    suspend fun mergePdfs(pdfPaths: List<String>, outputFileName: String): Result<String>
    suspend fun saveSignedPdf(originalPdfPath: String, signedPages: List<Bitmap>, outputFileName: String): Result<String>
    suspend fun organizeAndExportPdf(originalPdfPath: String, selectedPages: List<Int>, outputFileName: String): Result<String>
    suspend fun renderAllPagesToBitmaps(filePath: String): List<Bitmap>
    suspend fun lockPdf(originalPdfPath: String, password: String, outputFileName: String): Result<String>
}

interface UserPrefsRepository {
    val themeMode: Flow<String>
    val sortOrder: Flow<SortOrder>
    val languageCode: Flow<String>
    val isNightMode: Flow<Boolean>
    val keepScreenOn: Flow<Boolean>
    val isHorizontalScroll: Flow<Boolean>
    val isOnboardingCompleted: Flow<Boolean>
    val isGridView: Flow<Boolean>
    val isPrivacyPolicyAccepted: Flow<Boolean>

    suspend fun setThemeMode(mode: String)
    suspend fun setSortOrder(order: SortOrder)
    suspend fun setLanguageCode(code: String)
    suspend fun setNightMode(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setHorizontalScroll(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setGridView(isGrid: Boolean)
    suspend fun setPrivacyPolicyAccepted(accepted: Boolean)
}
