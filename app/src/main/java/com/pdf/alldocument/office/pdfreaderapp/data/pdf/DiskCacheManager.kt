package com.pdf.alldocument.office.pdfreaderapp.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiskCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheFolderName = "___pdf_page_cache___"

    fun getCacheDir(): File {
        val dir = File(context.cacheDir, cacheFolderName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, cacheFolderName)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()
    }

    fun getCachedPageFile(docKey: String, pageIndex: Int): File {
        val dir = getCacheDir()
        return File(dir, "${docKey}_p${pageIndex}.jpg")
    }

    suspend fun getOrRenderPage(
        renderer: PdfRenderer,
        docKey: String,
        pageIndex: Int,
        targetWidth: Int = 1080,
        renderScale: Float = 1.2f
    ): com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData? = withContext(Dispatchers.IO) {
        val cachedFile = getCachedPageFile(docKey, pageIndex)
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            return@withContext com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData(
                pageIndex = pageIndex,
                cachedFilePath = cachedFile.absolutePath,
                width = targetWidth,
                height = (targetWidth * 1.4f).toInt()
            )
        }

        try {
            val page = renderer.openPage(pageIndex)
            try {
                val baseWidth = page.width.coerceAtLeast(100)
                val baseHeight = page.height.coerceAtLeast(100)

                // Cap max dimensions to 2048 to avoid OutOfMemory on huge posters or scans
                val maxDim = 2048
                val scale = ((targetWidth.toFloat() / baseWidth.toFloat()).coerceIn(0.5f, 1.8f)) * renderScale
                var finalWidth = (baseWidth * scale).toInt().coerceIn(100, maxDim)
                var finalHeight = (baseHeight * scale).toInt().coerceIn(100, maxDim)

                var bitmap: Bitmap? = null
                try {
                    bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    finalWidth /= 2
                    finalHeight /= 2
                    bitmap = Bitmap.createBitmap(finalWidth.coerceAtLeast(100), finalHeight.coerceAtLeast(100), Bitmap.Config.ARGB_8888)
                }

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val tempFile = File(cachedFile.parentFile, "${cachedFile.name}.tmp")
                java.io.BufferedOutputStream(FileOutputStream(tempFile), 32 * 1024).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                }
                bitmap.recycle()

                if (cachedFile.exists()) {
                    cachedFile.delete()
                }
                if (!tempFile.renameTo(cachedFile)) {
                    tempFile.copyTo(cachedFile, overwrite = true)
                    tempFile.delete()
                }

                com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData(
                    pageIndex = pageIndex,
                    cachedFilePath = cachedFile.absolutePath,
                    width = finalWidth,
                    height = finalHeight
                )
            } finally {
                page.close()
            }
        } catch (t: Throwable) {
            android.util.Log.e("DiskCacheManager", "Error rendering native page $pageIndex", t)
            null
        }
    }

    suspend fun getOrRenderPageWithPdfBox(
        pdfBoxRenderer: com.tom_roush.pdfbox.rendering.PDFRenderer,
        docKey: String,
        pageIndex: Int,
        targetWidth: Int = 1080,
        renderScale: Float = 1.2f
    ): com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData? = withContext(Dispatchers.IO) {
        val cachedFile = getCachedPageFile(docKey, pageIndex)
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            return@withContext com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData(
                pageIndex = pageIndex,
                cachedFilePath = cachedFile.absolutePath,
                width = targetWidth,
                height = (targetWidth * 1.4f).toInt()
            )
        }

        try {
            val scale = renderScale.coerceIn(0.8f, 1.8f)
            var bitmap: Bitmap? = null
            try {
                bitmap = pdfBoxRenderer.renderImage(pageIndex, scale, com.tom_roush.pdfbox.rendering.ImageType.RGB)
            } catch (oom: OutOfMemoryError) {
                System.gc()
                bitmap = pdfBoxRenderer.renderImage(pageIndex, 1.0f, com.tom_roush.pdfbox.rendering.ImageType.RGB)
            }

            if (bitmap != null) {
                val finalWidth = bitmap.width
                val finalHeight = bitmap.height

                val tempFile = File(cachedFile.parentFile, "${cachedFile.name}.tmp")
                java.io.BufferedOutputStream(FileOutputStream(tempFile), 32 * 1024).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                }
                bitmap.recycle()

                if (cachedFile.exists()) {
                    cachedFile.delete()
                }
                if (!tempFile.renameTo(cachedFile)) {
                    tempFile.copyTo(cachedFile, overwrite = true)
                    tempFile.delete()
                }

                com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData(
                    pageIndex = pageIndex,
                    cachedFilePath = cachedFile.absolutePath,
                    width = finalWidth,
                    height = finalHeight
                )
            } else {
                null
            }
        } catch (t: Throwable) {
            android.util.Log.e("DiskCacheManager", "Error rendering PDFBox page $pageIndex", t)
            null
        }
    }
}
