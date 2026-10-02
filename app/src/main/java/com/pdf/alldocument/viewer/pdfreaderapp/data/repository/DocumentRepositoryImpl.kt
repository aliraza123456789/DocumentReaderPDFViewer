package com.pdf.alldocument.viewer.pdfreaderapp.data.repository

import com.pdf.alldocument.viewer.pdfreaderapp.core.database.DocumentDao
import com.pdf.alldocument.viewer.pdfreaderapp.core.database.DocumentEntity
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdf.PdfRendererEngine
import com.pdf.alldocument.viewer.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.PageRenderData
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.viewer.pdfreaderapp.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val scanner: MediaStoreDocumentScanner,
    private val documentDao: DocumentDao,
    private val pdfRendererEngine: PdfRendererEngine
) : DocumentRepository {

    override fun getDocuments(
        type: DocumentType,
        sortOrder: SortOrder,
        forceRefresh: Boolean
    ): Flow<List<DocumentItem>> {
        return scanner.scanDeviceDocuments(forceRefresh).map { list ->
            val filtered = if (type == DocumentType.ALL) list else list.filter { it.type == type }
            sortList(filtered, sortOrder)
        }.flowOn(Dispatchers.Default)
    }

    override fun getFavoriteDocuments(): Flow<List<DocumentItem>> {
        return documentDao.getFavoriteDocuments().map { entities ->
            entities.mapNotNull { entity ->
                if (File(entity.path).exists()) entity.toDocumentItem() else null
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRecentDocuments(): Flow<List<DocumentItem>> {
        return documentDao.getRecentDocuments().map { entities ->
            entities.mapNotNull { entity ->
                if (File(entity.path).exists()) entity.toDocumentItem() else null
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun searchDocuments(query: String, type: DocumentType): Flow<List<DocumentItem>> {
        val cached = scanner.getCachedDocumentsSnapshot()
        val baseFlow = if (cached != null) {
            kotlinx.coroutines.flow.flowOf(cached)
        } else {
            scanner.scanDeviceDocuments()
        }
        return baseFlow.map { list ->
            val filtered = if (type == DocumentType.ALL) list else list.filter { it.type == type }
            if (query.isBlank()) filtered else filtered.filter { it.name.contains(query, ignoreCase = true) }
        }.flowOn(Dispatchers.Default)
    }

    private fun DocumentEntity.toDocumentItem(): DocumentItem {
        return DocumentItem(
            path = path,
            name = name,
            size = size,
            formattedSize = FileUtils.formatFileSize(size),
            dateModified = dateModified,
            formattedDate = FileUtils.formatDate(dateModified),
            extension = extension,
            type = DocumentType.fromExtension(extension),
            isFavorite = isFavorite,
            isRecent = isRecent,
            lastReadTime = lastReadTime,
            lastReadPage = lastReadPage,
            totalPages = totalPages
        )
    }

    override suspend fun toggleFavorite(path: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = documentDao.getDocumentByPath(path)
        if (existing != null) {
            documentDao.updateFavorite(path, isFavorite)
        } else {
            val file = File(path)
            if (file.exists()) {
                val entity = DocumentEntity(
                    path = path,
                    name = file.name,
                    size = file.length(),
                    dateModified = file.lastModified(),
                    extension = FileUtils.getFileExtension(path),
                    isFavorite = isFavorite
                )
                documentDao.insertOrUpdate(entity)
            }
        }
        scanner.updateCachedDocument(path) { it.copy(isFavorite = isFavorite) }
    }

    override suspend fun recordDocumentOpened(path: String, page: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = documentDao.getDocumentByPath(path)
        if (existing != null) {
            documentDao.updateRecentAndPage(path, now, page)
        } else {
            val file = File(path)
            if (file.exists()) {
                val entity = DocumentEntity(
                    path = path,
                    name = file.name,
                    size = file.length(),
                    dateModified = file.lastModified(),
                    extension = FileUtils.getFileExtension(path),
                    isRecent = true,
                    lastReadTime = now,
                    lastReadPage = page
                )
                documentDao.insertOrUpdate(entity)
            }
        }
        scanner.updateCachedDocument(path) {
            it.copy(
                isRecent = true,
                lastReadTime = now,
                lastReadPage = page
            )
        }
    }

    override suspend fun updateLastReadPage(path: String, page: Int) = withContext(Dispatchers.IO) {
        documentDao.updateLastReadPage(path, page)
        scanner.updateCachedDocument(path) { it.copy(lastReadPage = page) }
    }

    override suspend fun renameDocument(oldPath: String, newName: String): Result<DocumentItem> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) return@withContext Result.failure(Exception("File does not exist"))

            val newFile = FileUtils.renameFile(oldFile, newName)
                ?: return@withContext Result.failure(Exception("Could not rename file"))

            val savedMetadata = documentDao.getDocumentByPath(oldPath)
            documentDao.deleteByPath(oldPath)

            val ext = newFile.extension
            val size = newFile.length()
            val dateModified = newFile.lastModified()
            val item = DocumentItem(
                path = newFile.absolutePath,
                name = newFile.name,
                size = size,
                formattedSize = FileUtils.formatFileSize(size),
                dateModified = dateModified,
                formattedDate = FileUtils.formatDate(dateModified),
                extension = ext,
                type = DocumentType.fromExtension(ext),
                isFavorite = savedMetadata?.isFavorite ?: false,
                isRecent = savedMetadata?.isRecent ?: false,
                lastReadTime = savedMetadata?.lastReadTime ?: 0L,
                lastReadPage = savedMetadata?.lastReadPage ?: 0,
                totalPages = savedMetadata?.totalPages ?: 0
            )

            if (savedMetadata != null) {
                documentDao.insertOrUpdate(
                    DocumentEntity(
                        path = item.path,
                        name = item.name,
                        size = item.size,
                        dateModified = item.dateModified,
                        extension = item.extension,
                        isFavorite = item.isFavorite,
                        isRecent = item.isRecent,
                        lastReadTime = item.lastReadTime,
                        lastReadPage = item.lastReadPage,
                        totalPages = item.totalPages
                    )
                )
            }
            scanner.replaceCachedDocument(oldPath, item)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDocument(path: String): Boolean = withContext(Dispatchers.IO) {
        documentDao.deleteByPath(path)
        val deleted = FileUtils.deleteFile(path)
        if (deleted) {
            scanner.removeCachedDocument(path)
        }
        deleted
    }

    override fun streamPdfPages(path: String, initialPage: Int): Flow<PageRenderData> {
        return pdfRendererEngine.streamRenderPages(path, initialPage)
    }

    override suspend fun getPdfPageCount(path: String): Int {
        return pdfRendererEngine.getPageCount(path)
    }

    private fun sortList(list: List<DocumentItem>, sortOrder: SortOrder): List<DocumentItem> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.dateModified }
            SortOrder.DATE_ASC -> list.sortedBy { it.dateModified }
            SortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
            SortOrder.SIZE_ASC -> list.sortedBy { it.size }
        }
    }
}
