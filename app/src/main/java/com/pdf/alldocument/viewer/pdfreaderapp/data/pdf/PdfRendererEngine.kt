package com.pdf.alldocument.viewer.pdfreaderapp.data.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.pdf.alldocument.viewer.pdfreaderapp.domain.model.PageRenderData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRendererEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diskCacheManager: DiskCacheManager
) {

    suspend fun getPageCount(filePath: String): Int = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext 0
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun streamRenderPages(filePath: String, initialPage: Int = 0): Flow<PageRenderData> = flow {
        val file = File(filePath)
        if (!file.exists()) return@flow

        val docKey = "${file.name.hashCode()}_${file.lastModified()}"
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val total = renderer.pageCount

        try {
            val pageOrder = buildList(total) {
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

            for (i in pageOrder) {
                // Check if already cached on disk
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
        } finally {
            renderer.close()
            pfd.close()
        }
    }.flowOn(Dispatchers.IO)
}
