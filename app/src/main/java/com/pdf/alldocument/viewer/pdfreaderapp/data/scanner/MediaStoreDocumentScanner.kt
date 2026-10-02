package com.pdf.alldocument.viewer.pdfreaderapp.data.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.pdf.alldocument.viewer.pdfreaderapp.core.database.DocumentDao
import com.pdf.alldocument.viewer.pdfreaderapp.core.database.DocumentEntity
import com.pdf.alldocument.viewer.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.DocumentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentDao: DocumentDao
) {

    private val supportedExtensions = setOf(
        "pdf",
        "doc", "docx", "dot", "dotx", "docm",
        "xls", "xlsx", "xlsm", "xlt", "xltx", "csv",
        "ppt", "pptx", "pptm", "pps", "ppsx", "pot", "potx",
        "txt", "log", "json", "xml", "java", "html", "htm"
    )

    private val scanMutex = Mutex()

    @Volatile
    private var cachedDocuments: List<DocumentItem>? = null

    @Volatile
    private var cacheTimestamp: Long = 0L

    fun scanDeviceDocuments(forceRefresh: Boolean = false): Flow<List<DocumentItem>> = flow {
        emit(getDocuments(forceRefresh))
    }.flowOn(Dispatchers.IO)

    fun updateCachedDocument(path: String, transform: (DocumentItem) -> DocumentItem) {
        val current = cachedDocuments ?: return
        var documentFound = false
        val updated = current.map { document ->
            if (document.path == path) {
                documentFound = true
                transform(document)
            } else {
                document
            }
        }
        if (documentFound) {
            cachedDocuments = updated
        }
    }

    fun replaceCachedDocument(oldPath: String, newDocument: DocumentItem) {
        val current = cachedDocuments ?: return
        val index = current.indexOfFirst { it.path == oldPath }
        if (index == -1) return

        cachedDocuments = current.toMutableList().apply {
            this[index] = newDocument
        }
    }

    fun removeCachedDocument(path: String) {
        val current = cachedDocuments ?: return
        cachedDocuments = current.filterNot { it.path == path }
    }

    fun invalidateCache() {
        cachedDocuments = null
        cacheTimestamp = 0L
    }

    fun getCachedDocumentsSnapshot(): List<DocumentItem>? = cachedDocuments

    private suspend fun getDocuments(forceRefresh: Boolean): List<DocumentItem> = scanMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        val cached = cachedDocuments
        if (!forceRefresh && cached != null && now - cacheTimestamp < CACHE_VALIDITY_MS) {
            return@withLock cached
        }
        if (!hasStorageAccess()) return@withLock emptyList()

        // Read saved metadata once instead of issuing one Room query for every file.
        val storedMetadata = try {
            documentDao.getAllDocumentsSnapshot().associateBy(DocumentEntity::path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not read saved document metadata", e)
            emptyMap()
        }

        val documents = queryMediaStore(storedMetadata)
        cacheTimestamp = SystemClock.elapsedRealtime()
        cachedDocuments = documents
        documents
    }

    private fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager() ||
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun queryMediaStore(storedMetadata: Map<String, DocumentEntity>): List<DocumentItem> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val uri = MediaStore.Files.getContentUri("external")
        val documents = mutableListOf<DocumentItem>()

        val extensionConditions = supportedExtensions.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.$it'"
        }
        val selection = "${MediaStore.Files.FileColumns.SIZE} > 0 AND ($extensionConditions)"

        return try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            ) ?: return fallbackFileScan(storedMetadata)

            cursor.use {
                val dataIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (it.moveToNext()) {
                    val path = it.getString(dataIndex)?.takeIf(String::isNotBlank) ?: continue
                    val size = it.getLong(sizeIndex)
                    if (size <= 0L) continue

                    val name = it.getString(nameIndex)?.takeIf(String::isNotBlank)
                        ?: File(path).name
                    val extension = FileUtils.getFileExtension(name)
                    if (extension !in supportedExtensions) continue

                    val saved = storedMetadata[path]
                    val dateModified = it.getLong(dateIndex) * 1000L
                    documents += DocumentItem(
                        path = path,
                        name = name,
                        size = size,
                        formattedSize = FileUtils.formatFileSize(size),
                        dateModified = dateModified,
                        formattedDate = FileUtils.formatDate(dateModified),
                        extension = extension,
                        type = DocumentType.fromExtension(extension),
                        isFavorite = saved?.isFavorite ?: false,
                        isRecent = saved?.isRecent ?: false,
                        lastReadTime = saved?.lastReadTime ?: 0L,
                        lastReadPage = saved?.lastReadPage ?: 0,
                        totalPages = saved?.totalPages ?: 0
                    )
                }
            }
            documents
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore scan failed; using targeted filesystem fallback", e)
            fallbackFileScan(storedMetadata)
        }
    }

    private fun fallbackFileScan(storedMetadata: Map<String, DocumentEntity>): List<DocumentItem> {
        val documents = mutableListOf<DocumentItem>()
        try {
            val roots = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                File(Environment.getExternalStorageDirectory(), "Documents"),
                File(Environment.getExternalStorageDirectory(), "Download"),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Documents"),
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents")
            ).filter { it.exists() && it.isDirectory }

            for (dir in roots) {
                scanDirectory(dir, documents, storedMetadata, depth = 0, maxDepth = 3)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Filesystem fallback scan failed", e)
        }
        return documents.distinctBy { it.path }.sortedByDescending(DocumentItem::dateModified)
    }

    private val ignoredDirectories = setOf(
        "android", "dcim", "pictures", "movies", "music", "podcasts",
        "ringtones", "alarms", "notifications", "cache", ".thumbnails"
    )

    private fun scanDirectory(
        directory: File,
        documents: MutableList<DocumentItem>,
        storedMetadata: Map<String, DocumentEntity>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth || !directory.exists() || !directory.canRead()) return
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                val dirName = file.name.lowercase()
                if (!dirName.startsWith(".") && dirName !in ignoredDirectories) {
                    scanDirectory(file, documents, storedMetadata, depth + 1, maxDepth)
                }
                continue
            }

            val extension = file.extension.lowercase()
            if (extension !in supportedExtensions) continue

            val size = file.length()
            if (size <= 0L) continue

            val dateModified = file.lastModified()
            val saved = storedMetadata[file.absolutePath]
            documents += DocumentItem(
                path = file.absolutePath,
                name = file.name,
                size = size,
                formattedSize = FileUtils.formatFileSize(size),
                dateModified = dateModified,
                formattedDate = FileUtils.formatDate(dateModified),
                extension = extension,
                type = DocumentType.fromExtension(extension),
                isFavorite = saved?.isFavorite ?: false,
                isRecent = saved?.isRecent ?: false,
                lastReadTime = saved?.lastReadTime ?: 0L,
                lastReadPage = saved?.lastReadPage ?: 0,
                totalPages = saved?.totalPages ?: 0
            )
        }
    }

    private companion object {
        const val TAG = "DocumentScanner"
        const val CACHE_VALIDITY_MS = 300_000L // 5 minutes cache to prevent aggressive rescans
    }
}
