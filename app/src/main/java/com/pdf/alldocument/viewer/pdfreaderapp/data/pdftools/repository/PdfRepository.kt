package com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.local.PdfDao
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.local.PdfFileEntity
import com.pdf.alldocument.viewer.pdfreaderapp.di.pdftools.IoDispatcher
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.Flow
import com.pdf.alldocument.viewer.pdfreaderapp.data.pdftools.local.FolderEntity

interface PdfRepository {
    suspend fun recentFiles(): List<PdfFileEntity>
    fun observeRecentFiles(): Flow<List<PdfFileEntity>>
    suspend fun insertRecentFile(entity: PdfFileEntity): Long
    fun observeFolders(): Flow<List<FolderEntity>>
    suspend fun createFolder(name: String): Long
    fun observeRecycleBin(): Flow<List<PdfFileEntity>>
    suspend fun restoreFile(id: Long)
    suspend fun deletePermanently(id: Long)
    suspend fun moveToRecycleBin(id: Long)
    suspend fun emptyRecycleBin()
    suspend fun textToPdf(text: String, fontSize: Float, output: File)
    suspend fun mergePdfs(sources: List<Uri>, output: File)
    suspend fun pdfToLongImage(source: Uri, output: File, quality: Int)
    suspend fun pdfToImages(source: Uri, outDir: File, format: Bitmap.CompressFormat, dpiScale: Float): List<File>
    suspend fun compressPdf(source: Uri, output: File, quality: Int)
    suspend fun lockPdf(source: Uri, output: File, password: String)
    suspend fun unlockPdf(source: Uri, output: File, password: String)
    suspend fun splitPdf(source: Uri, pages: List<Int>, outDir: File): List<File>
    fun pageCount(uri: Uri): Int
}

@Singleton
class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PdfDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : PdfRepository {

    override fun observeRecentFiles(): Flow<List<PdfFileEntity>> = dao.observeRecentFiles()

    override suspend fun insertRecentFile(entity: PdfFileEntity): Long = withContext(io) {
        dao.insertFile(entity)
    }

    override fun observeFolders(): Flow<List<FolderEntity>> = dao.observeFolders()

    override suspend fun createFolder(name: String): Long = withContext(io) {
        dao.insertFolder(FolderEntity(name = name))
    }

    override fun observeRecycleBin(): Flow<List<PdfFileEntity>> = dao.observeRecycleBin()

    override suspend fun restoreFile(id: Long) = withContext(io) {
        dao.restore(id)
    }

    override suspend fun deletePermanently(id: Long) = withContext(io) {
        dao.deletePermanently(id)
    }

    override suspend fun moveToRecycleBin(id: Long) = withContext(io) {
        dao.moveToRecycleBin(id, System.currentTimeMillis())
    }

    override suspend fun emptyRecycleBin() = withContext(io) {
        dao.emptyRecycleBin()
    }

    override suspend fun recentFiles(): List<PdfFileEntity> = withContext(io) {
        emptyList()
    }

    override suspend fun textToPdf(text: String, fontSize: Float, output: File) = withContext(io) {
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA, fontSize)
                cs.newLineAtOffset(50f, page.mediaBox.height - 50f)
                val leading = fontSize * 1.4f
                cs.setLeading(leading)
                text.lineSequence().forEach { line ->
                    cs.showText(line.take(90)) // TODO: proper line wrapping
                    cs.newLineAtOffset(0f, -leading)
                }
                cs.endText()
            }
            doc.save(output)
        }
    }

    override suspend fun mergePdfs(sources: List<Uri>, output: File) = withContext(io) {
        val merger = PDFMergerUtility()
        merger.destinationFileName = output.absolutePath
        sources.forEach { merger.addSource(context.contentResolver.openInputStream(it)) }
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
    }

    override suspend fun pdfToLongImage(source: Uri, output: File, quality: Int) = withContext(io) {
        val pages = renderPages(source)
        val totalHeight = pages.sumOf { it.height }
        val maxWidth = pages.maxOf { it.width }
        val longBmp = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(longBmp)
        var top = 0
        pages.forEach { canvas.drawBitmap(it, 0f, top.toFloat(), null); top += it.height; it.recycle() }
        FileOutputStream(output).use { longBmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        longBmp.recycle()
    }

    override suspend fun pdfToImages(
        source: Uri, outDir: File, format: Bitmap.CompressFormat, dpiScale: Float
    ): List<File> = withContext(io) {
        renderPages(source, dpiScale).mapIndexed { index, bmp ->
            File(outDir, "page_${index + 1}.${if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"}").also { f ->
                FileOutputStream(f).use { bmp.compress(format, 95, it) }
                bmp.recycle()
            }
        }
    }

    override suspend fun compressPdf(source: Uri, output: File, quality: Int) = withContext(io) {
        val inputStream = context.contentResolver.openInputStream(source) ?: return@withContext
        PDDocument.load(inputStream).use { doc ->
            val targetQuality = quality.coerceIn(20, 95)
            var imageCompressed = false

            for (page in doc.pages) {
                val resources = page.resources ?: continue
                val xObjectNames = resources.xObjectNames ?: continue
                for (name in xObjectNames) {
                    if (resources.isImageXObject(name)) {
                        try {
                            val xImage = resources.getXObject(name) as? com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject ?: continue
                            val bmp = xImage.image ?: continue
                            val maxDim = when {
                                targetQuality <= 50 -> 1080
                                targetQuality <= 70 -> 1440
                                else -> 1920
                            }
                            val scale = if (bmp.width > maxDim || bmp.height > maxDim) {
                                val s = maxDim.toFloat() / maxOf(bmp.width, bmp.height).toFloat()
                                s.coerceAtMost(1.0f)
                            } else {
                                1.0f
                            }
                            val workingBmp = if (scale < 1.0f) {
                                val w = (bmp.width * scale).toInt().coerceAtLeast(1)
                                val h = (bmp.height * scale).toInt().coerceAtLeast(1)
                                Bitmap.createScaledBitmap(bmp, w, h, true)
                            } else {
                                bmp
                            }

                            val stream = java.io.ByteArrayOutputStream()
                            workingBmp.compress(Bitmap.CompressFormat.JPEG, targetQuality, stream)
                            if (workingBmp != bmp) workingBmp.recycle()
                            bmp.recycle()

                            val compressedImage = JPEGFactory.createFromStream(
                                doc,
                                java.io.ByteArrayInputStream(stream.toByteArray())
                            )
                            resources.put(name, compressedImage)
                            imageCompressed = true
                        } catch (_: Exception) {
                            // Continue with other objects if one image fails
                        }
                    }
                }
            }

            if (!imageCompressed && targetQuality < 85 && doc.numberOfPages in 1..25) {
                PDDocument().use { compressedDoc ->
                    val scale = if (targetQuality <= 50) 1.2f else 1.5f
                    val pageBitmaps = renderPages(source, scale)
                    pageBitmaps.forEach { pageBmp ->
                        val p = PDPage(PDRectangle(pageBmp.width.toFloat(), pageBmp.height.toFloat()))
                        compressedDoc.addPage(p)
                        val stream = java.io.ByteArrayOutputStream()
                        pageBmp.compress(Bitmap.CompressFormat.JPEG, targetQuality, stream)
                        val pdImg = JPEGFactory.createFromStream(compressedDoc, java.io.ByteArrayInputStream(stream.toByteArray()))
                        pageBmp.recycle()
                        PDPageContentStream(compressedDoc, p).use { cs ->
                            cs.drawImage(pdImg, 0f, 0f, p.mediaBox.width, p.mediaBox.height)
                        }
                    }
                    compressedDoc.save(output)
                    return@withContext
                }
            }

            doc.save(output)
        }
    }

    override suspend fun lockPdf(source: Uri, output: File, password: String) = withContext(io) {
        PDDocument.load(context.contentResolver.openInputStream(source)).use { doc ->
            doc.protect(com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                password, password, com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
            ))
            doc.save(output)
        }
    }

    override suspend fun unlockPdf(source: Uri, output: File, password: String) = withContext(io) {
        PDDocument.load(context.contentResolver.openInputStream(source), password).use { doc ->
            doc.isAllSecurityToBeRemoved = true
            doc.save(output)
        }
    }

    override suspend fun splitPdf(source: Uri, pages: List<Int>, outDir: File): List<File> =
        withContext(io) {
            PDDocument.load(context.contentResolver.openInputStream(source)).use { doc ->
                pages.mapIndexed { i, pageIndex ->
                    PDDocument().use { single ->
                        single.addPage(doc.getPage(pageIndex))
                        File(outDir, "split_${i + 1}.pdf").also { single.save(it) }
                    }
                }
            }
        }

    override fun pageCount(uri: Uri): Int {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        return PdfRenderer(pfd).use { it.pageCount }
    }

    private fun renderPages(uri: Uri, scale: Float = 2f): List<Bitmap> {
        val pfd: ParcelFileDescriptor =
            context.contentResolver.openFileDescriptor(uri, "r")!!
        return PdfRenderer(pfd).use { renderer ->
            (0 until renderer.pageCount).map { i ->
                renderer.openPage(i).use { page ->
                    val bmp = Bitmap.createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    }
}
