package com.pdf.alldocument.office.pdfreaderapp.data.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdf.alldocument.office.pdfreaderapp.core.util.UriUtils
import com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class PdfLoadResult {
    data class Success(val pageCount: Int, val isNative: Boolean, val resolvedPath: String) : PdfLoadResult()
    data class PasswordRequired(val resolvedPath: String) : PdfLoadResult()
    data class Error(val message: String) : PdfLoadResult()
}

@Singleton
class PdfRendererEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diskCacheManager: DiskCacheManager
) {

    init {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resolves a file path or URI into an accessible, readable local File.
     */
    fun resolveToFile(filePath: String): File? {
        if (filePath.isBlank()) return null
        val directFile = File(filePath)
        if (directFile.exists() && directFile.canRead() && directFile.length() > 0L) {
            return directFile
        }

        // Try parsing as URI if it looks like content:// or file:// or is not directly accessible
        try {
            val uri = Uri.parse(filePath)
            if (uri != null && (uri.scheme.equals("content", ignoreCase = true) || uri.scheme.equals("file", ignoreCase = true))) {
                val parsed = UriUtils.resolveUriToFile(context, uri)
                if (parsed != null) {
                    val resolvedFile = File(parsed.filePath)
                    if (resolvedFile.exists() && resolvedFile.length() > 0L) {
                        return resolvedFile
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (directFile.exists()) directFile else null
    }

    /**
     * Inspects a document comprehensively using both native Android PdfRenderer and PDFBox fallback.
     * Accurately detects encryption (including AES-256) and returns precise load results.
     */
    suspend fun inspectDocument(filePath: String): PdfLoadResult = withContext(Dispatchers.IO) {
        val file = resolveToFile(filePath)
            ?: return@withContext PdfLoadResult.Error("Unable to locate document file.")

        if (file.length() <= 0L) {
            return@withContext PdfLoadResult.Error("Document file is empty (0 bytes).")
        }

        // 1. Check encryption first
        if (isEncrypted(file.absolutePath)) {
            return@withContext PdfLoadResult.PasswordRequired(file.absolutePath)
        }

        // 2. Try native Android PdfRenderer
        var nativePfd: ParcelFileDescriptor? = null
        var nativeRenderer: PdfRenderer? = null
        try {
            nativePfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            nativeRenderer = PdfRenderer(nativePfd)
            val count = nativeRenderer.pageCount
            if (count > 0) {
                return@withContext PdfLoadResult.Success(count, isNative = true, resolvedPath = file.absolutePath)
            }
        } catch (e: SecurityException) {
            return@withContext PdfLoadResult.PasswordRequired(file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.w("PdfRendererEngine", "Native PdfRenderer failed, trying PDFBox fallback: ${e.message}")
        } finally {
            try { nativeRenderer?.close() } catch (_: Exception) {}
            try { nativePfd?.close() } catch (_: Exception) {}
        }

        // 3. Fallback to PDFBox (handles non-standard XRef, repaired PDFs, linearized streams)
        try {
            PDFBoxResourceLoader.init(context)
            val doc = PDDocument.load(file)
            if (doc.isEncrypted) {
                doc.close()
                return@withContext PdfLoadResult.PasswordRequired(file.absolutePath)
            }
            val count = doc.numberOfPages
            doc.close()
            if (count > 0) {
                return@withContext PdfLoadResult.Success(count, isNative = false, resolvedPath = file.absolutePath)
            }
        } catch (e: InvalidPasswordException) {
            return@withContext PdfLoadResult.PasswordRequired(file.absolutePath)
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypt") || msg.contains("crypt")) {
                return@withContext PdfLoadResult.PasswordRequired(file.absolutePath)
            }
            android.util.Log.e("PdfRendererEngine", "PDFBox fallback also failed: ${e.message}")
        }

        PdfLoadResult.Error("Unable to open or read document. The file may be corrupted.")
    }

    suspend fun getPageCount(filePath: String): Int = withContext(Dispatchers.IO) {
        when (val result = inspectDocument(filePath)) {
            is PdfLoadResult.Success -> result.pageCount
            else -> 0
        }
    }

    /**
     * Streams rendered pages on-demand.
     * Uses native PdfRenderer when possible for speed, or PDFBox as reliable fallback.
     */
    fun streamRenderPages(
        filePath: String,
        initialPage: Int = 0,
        isNative: Boolean = true
    ): Flow<PageRenderData> = flow {
        val file = resolveToFile(filePath) ?: return@flow
        val docKey = "${file.absolutePath.hashCode()}_${file.length()}_${file.lastModified()}"

        if (isNative) {
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                val total = renderer.pageCount
                val pageOrder = buildPageOrder(total, initialPage)

                for (i in pageOrder) {
                    val cachedFile = diskCacheManager.getCachedPageFile(docKey, i)
                    if (cachedFile.exists() && cachedFile.length() > 0L) {
                        emit(PageRenderData(pageIndex = i, cachedFilePath = cachedFile.absolutePath, width = 1080, height = 1500))
                        kotlinx.coroutines.yield()
                        continue
                    }

                    val renderData = diskCacheManager.getOrRenderPage(renderer, docKey, i)
                    if (renderData != null) {
                        emit(renderData)
                    }
                    kotlinx.coroutines.yield()
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfRendererEngine", "Error during native streamRenderPages: ${e.message}")
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        } else {
            // PDFBox rendering fallback
            var doc: PDDocument? = null
            try {
                PDFBoxResourceLoader.init(context)
                doc = PDDocument.load(file)
                val pdfBoxRenderer = com.tom_roush.pdfbox.rendering.PDFRenderer(doc)
                val total = doc.numberOfPages
                val pageOrder = buildPageOrder(total, initialPage)

                for (i in pageOrder) {
                    val cachedFile = diskCacheManager.getCachedPageFile(docKey, i)
                    if (cachedFile.exists() && cachedFile.length() > 0L) {
                        emit(PageRenderData(pageIndex = i, cachedFilePath = cachedFile.absolutePath, width = 1080, height = 1500))
                        kotlinx.coroutines.yield()
                        continue
                    }

                    val renderData = diskCacheManager.getOrRenderPageWithPdfBox(pdfBoxRenderer, docKey, i)
                    if (renderData != null) {
                        emit(renderData)
                    }
                    kotlinx.coroutines.yield()
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfRendererEngine", "Error during PDFBox streamRenderPages: ${e.message}")
            } finally {
                try { doc?.close() } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPageOrder(total: Int, initialPage: Int): List<Int> {
        return buildList(total) {
            val clampedInitial = initialPage.coerceIn(0, (total - 1).coerceAtLeast(0))
            add(clampedInitial)
            var offset = 1
            while (size < total) {
                val next = clampedInitial + offset
                if (next < total) add(next)
                val prev = clampedInitial - offset
                if (prev >= 0) add(prev)
                offset++
            }
        }
    }

    /**
     * Multi-engine encryption detector. Accurately identifies AES-128, AES-256, and password locks.
     */
    fun isEncrypted(filePath: String): Boolean {
        val file = resolveToFile(filePath) ?: return false
        if (!file.exists() || file.length() <= 0L) return false

        // 1. Android Native PdfRenderer test
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        // Successfully opened natively without password -> not encrypted
                        return false
                    }
                }
            }
        } catch (e: SecurityException) {
            // Android throws SecurityException exclusively for encrypted/password protected PDFs
            return true
        } catch (_: Exception) {
            // Continue to secondary verification
        }

        // 2. PDFBox test (handles AES-128 and AES-256 standard and public-key encryption)
        try {
            PDFBoxResourceLoader.init(context)
            PDDocument.load(file).use { doc ->
                if (doc.isEncrypted) return true
            }
        } catch (e: InvalidPasswordException) {
            return true
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypted") || msg.contains("crypt")) {
                return true
            }
        }

        // 3. iText 5 test
        try {
            val reader = com.itextpdf.text.pdf.PdfReader(file.absolutePath)
            val enc = reader.isEncrypted
            reader.close()
            if (enc) return true
        } catch (e: com.itextpdf.text.exceptions.BadPasswordException) {
            return true
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypted") || msg.contains("bad user password")) {
                return true
            }
        }

        return false
    }

    /**
     * Unlocks an encrypted PDF using the supplied password and writes a temporary decrypted copy.
     */
    suspend fun unlockToTemp(filePath: String, password: String): File? = withContext(Dispatchers.IO) {
        val file = resolveToFile(filePath) ?: return@withContext null

        // 1. First try PDFBox (supports modern AES-256 and StandardProtectionPolicy)
        try {
            PDFBoxResourceLoader.init(context)
            val doc = PDDocument.load(file, password)
            doc.isAllSecurityToBeRemoved = true
            val temp = File(context.cacheDir, "unlocked_preview_${System.currentTimeMillis()}.pdf")
            doc.save(temp)
            doc.close()
            if (temp.exists() && temp.length() > 0L) {
                return@withContext temp
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfRendererEngine", "PDFBox unlock failed, trying iText: ${e.message}")
        }

        // 2. Fallback to iText 5
        try {
            val reader = com.itextpdf.text.pdf.PdfReader(file.absolutePath, password.toByteArray(Charsets.UTF_8))
            val temp = File(context.cacheDir, "unlocked_preview_${System.currentTimeMillis()}.pdf")
            val stamper = com.itextpdf.text.pdf.PdfStamper(reader, FileOutputStream(temp))
            stamper.close()
            reader.close()
            if (temp.exists() && temp.length() > 0L) {
                return@withContext temp
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererEngine", "iText unlock failed: ${e.message}")
        }

        null
    }
}
