package com.pdf.alldocument.office.pdfreaderapp.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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

            val publicFolder = FileUtils.getOutputDocumentsFolder()
            val outputFolder = if (publicFolder.exists() || publicFolder.mkdirs()) {
                publicFolder
            } else {
                FileUtils.outputDir(context)
            }
            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(outputFolder, finalName)

            // 1. Try iText with sanitized bytes (strips UTF-8 BOM, spaces or null bytes before %PDF-)
            val itextResult = runCatching {
                mergeWithIText(pdfPaths, outputFile)
            }

            if (itextResult.isSuccess && outputFile.exists() && outputFile.length() > 0L) {
                scanner.notifyDocumentCreated(outputFile)
                return@withContext Result.success(outputFile.absolutePath)
            }

            // 2. If iText failed (e.g., 'PDF header signature not found', complex xref, unsupported stream),
            // fallback directly to PDFBox PDFMergerUtility
            val pdfboxResult = runCatching {
                mergeWithPdfBox(pdfPaths, outputFile)
            }

            if (pdfboxResult.isSuccess && outputFile.exists() && outputFile.length() > 0L) {
                scanner.notifyDocumentCreated(outputFile)
                return@withContext Result.success(outputFile.absolutePath)
            }

            // If both iText and PDFBox failed, check each file to pinpoint any corrupted PDF
            for (path in pdfPaths) {
                val validation = validatePdf(path)
                if (validation.isFailure) {
                    val ex = validation.exceptionOrNull()
                    return@withContext Result.failure(ex ?: Exception("A corrupted PDF file was detected."))
                }
            }

            val err = pdfboxResult.exceptionOrNull() ?: itextResult.exceptionOrNull()
            val message = err?.message
            val errorMsg = when {
                message?.contains("root object specification", ignoreCase = true) == true ->
                    "One or more selected documents are corrupted (missing root trailer) and cannot be merged."
                message?.contains("header signature", ignoreCase = true) == true ->
                    "One or more selected documents are invalid or corrupted PDF files."
                message.isNullOrBlank() -> "Merge failed. Please try again."
                else -> message
            }
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun mergeWithIText(pdfPaths: List<String>, outputFile: File) {
        val document = Document()
        val copy = PdfCopy(document, FileOutputStream(outputFile))
        document.open()

        val readers = mutableListOf<PdfReader>()
        try {
            for (path in pdfPaths) {
                val rawBytes = readPdfBytes(path)
                    ?: throw java.io.IOException("Cannot read document: ${File(path).name}")
                val cleanBytes = sanitizePdfBytes(rawBytes)
                val reader = PdfReader(cleanBytes)
                readers.add(reader)
                val n = reader.numberOfPages
                for (page in 1..n) {
                    copy.addPage(copy.getImportedPage(reader, page))
                }
                copy.freeReader(reader)
            }
        } finally {
            runCatching { document.close() }
            readers.forEach { r -> runCatching { r.close() } }
        }
    }

    private fun mergeWithPdfBox(pdfPaths: List<String>, outputFile: File) {
        PDFBoxResourceLoader.init(context)
        val merger = PDFMergerUtility()
        merger.destinationFileName = outputFile.absolutePath

        val openStreams = mutableListOf<InputStream>()
        try {
            for (path in pdfPaths) {
                val stream = openPdfInputStream(path)
                    ?: throw java.io.IOException("Cannot read document: ${File(path).name}")
                openStreams.add(stream)
                merger.addSource(stream)
            }
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
        } finally {
            openStreams.forEach { runCatching { it.close() } }
        }
    }

    private fun openPdfInputStream(path: String): InputStream? {
        return try {
            if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))
            } else if (path.startsWith("file://")) {
                val parsed = Uri.parse(path)
                val filePath = parsed.path
                if (!filePath.isNullOrBlank() && File(filePath).exists() && File(filePath).canRead()) {
                    FileInputStream(File(filePath))
                } else {
                    context.contentResolver.openInputStream(parsed)
                }
            } else {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(Uri.fromFile(file))
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readPdfBytes(path: String): ByteArray? {
        return try {
            openPdfInputStream(path)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitizePdfBytes(bytes: ByteArray): ByteArray {
        val pdfSig = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // "%PDF-"
        val maxSearch = minOf(bytes.size - pdfSig.size, 2048)
        for (i in 0..maxSearch) {
            var match = true
            for (j in pdfSig.indices) {
                if (bytes[i + j] != pdfSig[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                return if (i == 0) bytes else bytes.copyOfRange(i, bytes.size)
            }
        }
        return bytes
    }

    suspend fun validatePdf(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val displayName = runCatching { File(path).name }.getOrNull()?.ifBlank { "Selected file" } ?: "Selected file"
        try {
            val rawBytes = readPdfBytes(path)
                ?: return@withContext Result.failure(Exception("Cannot read \"$displayName\""))

            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(Exception("\"$displayName\" is empty (0 bytes) and cannot be merged."))
            }

            // Quick check: must contain %PDF- in the header area
            val cleanBytes = sanitizePdfBytes(rawBytes)
            val pdfSig = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // "%PDF-"
            if (cleanBytes.size < 5 || !cleanBytes.take(5).toByteArray().contentEquals(pdfSig)) {
                return@withContext Result.failure(Exception("\"$displayName\" is not a valid PDF file."))
            }

            // 1. Try iText validation
            var itextValid = false
            try {
                val reader = PdfReader(cleanBytes)
                val count = reader.numberOfPages
                reader.close()
                if (count > 0) {
                    itextValid = true
                }
            } catch (_: Exception) {
                // iText may fail on some valid modern PDFs; fallback to PDFBox below
            }

            if (itextValid) {
                return@withContext Result.success(Unit)
            }

            // 2. Try PDFBox validation
            var pdfboxValid = false
            try {
                PDFBoxResourceLoader.init(context)
                openPdfInputStream(path)?.use { stream ->
                    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(stream)
                    val count = doc.numberOfPages
                    doc.close()
                    if (count > 0) {
                        pdfboxValid = true
                    }
                }
            } catch (_: Exception) {
            }

            if (pdfboxValid) {
                return@withContext Result.success(Unit)
            }

            Result.failure(Exception("\"$displayName\" is corrupted or incomplete and cannot be merged."))
        } catch (e: Exception) {
            Result.failure(Exception("\"$displayName\" is corrupted and cannot be merged."))
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
            val publicFolder = FileUtils.getOutputDocumentsFolder()
            val outputFolder = if (publicFolder.exists() || publicFolder.mkdirs()) {
                publicFolder
            } else {
                FileUtils.outputDir(context)
            }
            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
            val outputFile = File(outputFolder, finalName)

            val rawBytes = readPdfBytes(originalPdfPath)
                ?: return@withContext Result.failure(Exception("Cannot read document: ${File(originalPdfPath).name}"))
            val cleanBytes = sanitizePdfBytes(rawBytes)
            val reader = PdfReader(cleanBytes)
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
