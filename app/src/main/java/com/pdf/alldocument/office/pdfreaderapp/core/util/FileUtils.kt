package com.pdf.alldocument.office.pdfreaderapp.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun outputDir(context: Context): File {
        return try {
            val publicFolder = getOutputDocumentsFolder()
            if (publicFolder.exists() || publicFolder.mkdirs()) {
                publicFolder
            } else {
                val dir = File(context.getExternalFilesDir(null), "DocSuite")
                if (!dir.exists()) dir.mkdirs()
                dir
            }
        } catch (e: Exception) {
            val dir = File(context.getExternalFilesDir(null), "DocSuite")
            if (!dir.exists()) dir.mkdirs()
            dir
        }
    }

    fun newOutputFile(context: Context, prefix: String, extension: String): File {
        return File(outputDir(context), "${prefix}_${timestamp()}.$extension")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun shareIntent(context: Context, file: File, mime: String): Intent {
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun openIntent(context: Context, file: File, mime: String): Intent {
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun copyToOutput(context: Context, sourceUri: Uri, prefix: String, extension: String): File {
        val out = newOutputFile(context, prefix, extension)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(size / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun formatDate(timestampMs: Long): String {
        val date = Date(timestampMs)
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    fun getFileExtension(pathOrName: String): String {
        return pathOrName.substringAfterLast('.', "").lowercase()
    }

    fun getOutputDocumentsFolder(): File {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDF Reader")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun copyUriToTempFile(context: Context, uri: Uri, targetName: String): File? {
        return try {
            val tempFile = File(context.cacheDir, targetName)
            tempFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                tempFile
            } else {
                tempFile.delete()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renameFile(sourceFile: File, newNameWithoutExtension: String): File? {
        val ext = sourceFile.extension
        val newFileName = if (ext.isNotEmpty()) "$newNameWithoutExtension.$ext" else newNameWithoutExtension
        val targetFile = File(sourceFile.parentFile, newFileName)
        return if (sourceFile.renameTo(targetFile)) targetFile else null
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) file.delete() else false
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun scanFile(context: Context, file: File, mimeType: String? = null) {
        val mime = mimeType ?: when (file.extension.lowercase()) {
            "pdf" -> MimeTypes.PDF
            "docx", "doc" -> MimeTypes.DOCX
            "xlsx", "xls" -> MimeTypes.XLSX
            "csv" -> MimeTypes.CSV
            "txt" -> MimeTypes.TXT
            "jpg", "jpeg", "png" -> MimeTypes.IMAGE
            else -> "*/*"
        }
        try {
            android.media.MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf(mime),
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

object MimeTypes {
    const val PDF = "application/pdf"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val CSV = "text/csv"
    const val TXT = "text/plain"
    const val IMAGE = "image/*"
}
