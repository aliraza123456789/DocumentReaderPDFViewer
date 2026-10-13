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
            val baseWidth = page.width
            val baseHeight = page.height

            val scale = ((targetWidth.toFloat() / baseWidth.toFloat()).coerceIn(1.0f, 1.8f)) * renderScale
            val finalWidth = (baseWidth * scale).toInt().coerceAtLeast(100)
            val finalHeight = (baseHeight * scale).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val tempFile = File(cachedFile.parentFile, "${cachedFile.name}.tmp")
            java.io.BufferedOutputStream(FileOutputStream(tempFile), 32 * 1024).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.flush()
            }
            bitmap.recycle()
            tempFile.renameTo(cachedFile)
            com.pdf.alldocument.office.pdfreaderapp.domain.model.PageRenderData(
                pageIndex = pageIndex,
                cachedFilePath = cachedFile.absolutePath,
                width = finalWidth,
                height = finalHeight
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
