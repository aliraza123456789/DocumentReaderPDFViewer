package com.pdf.alldocument.office.pdfreaderapp.core.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.pdf.alldocument.office.pdfreaderapp.data.pdf.PdfPrintDocumentAdapter
import java.io.File

object ShareUtils {

    fun shareFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = when (file.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "doc", "docx" -> "application/msword"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                "txt" -> "text/plain"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Document"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun printPdf(context: Context, filePath: String, jobName: String = "Document Print") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val printAdapter = PdfPrintDocumentAdapter(filePath)
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf_print", "PDF Print", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager?.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
