package com.pdf.alldocument.office.pdfreaderapp.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object UriUtils {
    private const val TAG = "UriUtils"

    data class ParsedFileInfo(
        val filePath: String,
        val displayName: String
    )

    /**
     * Resolves an incoming Uri (content:// or file://) into an accessible local file path and display name.
     * If the Uri points to a content provider (e.g., WhatsApp, Gmail, Telegram),
     * the stream is copied into the app's cache directory so PDF renderers can access it directly.
     */
    fun resolveUriToFile(context: Context, uri: Uri): ParsedFileInfo? {
        try {
            val scheme = uri.scheme
            if (scheme.equals("file", ignoreCase = true)) {
                val path = uri.path ?: return null
                val file = File(path)
                return ParsedFileInfo(
                    filePath = file.absolutePath,
                    displayName = file.name.ifEmpty { "Document.pdf" }
                )
            }

            // Otherwise handle content:// URIs
            val displayName = queryFileName(context, uri) ?: "External_${System.currentTimeMillis()}.pdf"
            
            // Create a clean destination in app's cache directory
            val cacheFolder = File(context.cacheDir, "opened_documents").apply {
                if (!exists()) mkdirs()
            }
            
            // Ensure safe and collision-proof file name
            val uriHash = Math.abs(uri.toString().hashCode()).toString(16)
            val sanitized = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val safeName = "${uriHash}_$sanitized"
            val targetFile = File(cacheFolder, safeName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return null
            }

            return ParsedFileInfo(
                filePath = targetFile.absolutePath,
                displayName = displayName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URI $uri", e)
            return null
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query display name from content resolver", e)
        }
        return name ?: uri.lastPathSegment
    }
}
