package com.pdf.alldocument.office.pdfreaderapp.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfToolsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: com.pdf.alldocument.office.pdfreaderapp.data.scanner.MediaStoreDocumentScanner
) {

    suspend fun mergePdfs(pdfPaths: List<String>, outputFileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (pdfPaths.isEmpty()) return@withContext Result.failure(Exception("No PDF files selected"))

            val outputFolder = FileUtils.getOutputDocumentsFolder()
            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(outputFolder, finalName)

            val document = Document()
            val copy = PdfCopy(document, FileOutputStream(outputFile))
            document.open()

            for (path in pdfPaths) {
                val reader = PdfReader(path)
                val n = reader.numberOfPages
                for (page in 1..n) {
                    copy.addPage(copy.getImportedPage(reader, page))
                }
                reader.close()
            }

            document.close()
            scanner.notifyDocumentCreated(outputFile)
            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun saveSignedPdf(
        originalPdfPath: String,
        signedPageBitmaps: List<Bitmap>,
        outputFileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val outputFolder = FileUtils.getOutputDocumentsFolder()
            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(outputFolder, finalName)

            val pdfDocument = PdfDocument()

            for ((index, bitmap) in signedPageBitmaps.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                pdfDocument.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            scanner.notifyDocumentCreated(outputFile)

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun organizeAndExportPdf(
        originalPdfPath: String,
        selectedPageIndices: List<Int>, // 0-based ordered list
        outputFileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val outputFolder = FileUtils.getOutputDocumentsFolder()
            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(outputFolder, finalName)

            val reader = PdfReader(originalPdfPath)
            val document = Document()
            val copy = PdfCopy(document, FileOutputStream(outputFile))
            document.open()

            for (pageIndex in selectedPageIndices) {
                // iText is 1-based
                val pageNumber = pageIndex + 1
                if (pageNumber in 1..reader.numberOfPages) {
                    copy.addPage(copy.getImportedPage(reader, pageNumber))
                }
            }

            document.close()
            reader.close()
            scanner.notifyDocumentCreated(outputFile)

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun lockPdf(
        originalPdfPath: String,
        password: String,
        outputFileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val publicFolder = FileUtils.getOutputDocumentsFolder()
            val outputFolder = if (publicFolder.exists() || publicFolder.mkdirs()) {
                publicFolder
            } else {
                FileUtils.outputDir(context)
            }
            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(outputFolder, finalName)

            val inputStream = if (originalPdfPath.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(originalPdfPath))
                    ?: return@withContext Result.failure(Exception("Cannot open source document"))
            } else {
                val f = File(originalPdfPath)
                if (!f.exists()) return@withContext Result.failure(Exception("Source file does not exist: $originalPdfPath"))
                java.io.FileInputStream(f)
            }

            // In PDF specification (ISO 32000-1) and iText:
            // userPassword: REQUIRED TO OPEN AND READ THE PDF
            // ownerPassword: Controls modification permissions and MUST NOT equal userPassword.
            // If userPassword == ownerPassword, iText treats userPassword as empty!
            val userPassBytes = password.toByteArray(Charsets.UTF_8)
            val ownerPassString = "${password}_${java.util.UUID.randomUUID().toString().take(8)}_owner"
            val ownerPassBytes = ownerPassString.toByteArray(Charsets.UTF_8)

            inputStream.use { stream ->
                val reader = PdfReader(stream)
                val fos = FileOutputStream(outputFile)
                val stamper = com.itextpdf.text.pdf.PdfStamper(reader, fos)
                stamper.setEncryption(
                    userPassBytes,
                    ownerPassBytes,
                    com.itextpdf.text.pdf.PdfWriter.ALLOW_PRINTING or com.itextpdf.text.pdf.PdfWriter.ALLOW_COPY,
                    com.itextpdf.text.pdf.PdfWriter.ENCRYPTION_AES_128
                )
                stamper.close()
                reader.close()
            }

            scanner.notifyDocumentCreated(outputFile)

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)

                val publicFolder = FileUtils.getOutputDocumentsFolder()
                val outputFolder = if (publicFolder.exists() || publicFolder.mkdirs()) publicFolder else FileUtils.outputDir(context)
                val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
                val outputFile = File(outputFolder, finalName)

                val file = File(originalPdfPath)
                val doc = if (file.exists()) {
                    com.tom_roush.pdfbox.pdmodel.PDDocument.load(file)
                } else {
                    val stream = context.contentResolver.openInputStream(android.net.Uri.parse(originalPdfPath))
                    com.tom_roush.pdfbox.pdmodel.PDDocument.load(stream)
                }

                val ownerPassString = "${password}_${java.util.UUID.randomUUID().toString().take(8)}_owner"
                val ap = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
                val spp = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                    ownerPassString, // owner password
                    password,        // user password (required to open)
                    ap
                )
                spp.encryptionKeyLength = 128
                doc.use { d ->
                    d.protect(spp)
                    d.save(outputFile)
                }

                scanner.notifyDocumentCreated(outputFile)

                Result.success(outputFile.absolutePath)
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                Result.failure(fallbackEx)
            }
        }
    }

    suspend fun renderAllPagesToBitmaps(filePath: String): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val file = File(filePath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val total = renderer.pageCount

            for (i in 0 until total) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bmp)
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }
}
