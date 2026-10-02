package com.pdf.alldocument.viewer.pdfreaderapp.domain.model

enum class DocumentType(val title: String) {
    ALL("All"),
    PDF("PDF"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PPT"),
    TXT("TXT");

    companion object {
        fun fromExtension(ext: String): DocumentType {
            return when (ext.lowercase()) {
                "pdf" -> PDF
                "doc", "docx", "dot", "dotx", "docm" -> WORD
                "xls", "xlsx", "xlsm", "xlt", "xltx", "csv" -> EXCEL
                "ppt", "pptx", "pptm", "pps", "ppsx", "pot", "potx" -> PPT
                "txt", "log", "json", "xml", "java", "html", "htm" -> TXT
                else -> ALL
            }
        }
    }
}

enum class SortOrder(val label: String) {
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest first)"),
    SIZE_ASC("Size (Smallest first)")
}

data class DocumentItem(
    val path: String,
    val name: String,
    val size: Long,
    val formattedSize: String,
    val dateModified: Long,
    val formattedDate: String,
    val extension: String,
    val type: DocumentType,
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false,
    val lastReadTime: Long = 0L,
    val lastReadPage: Int = 0,
    val totalPages: Int = 0
)

data class PageRenderData(
    val pageIndex: Int,
    val cachedFilePath: String,
    val width: Int,
    val height: Int
)

enum class ToolOperation {
    MERGE,
    SIGN,
    ORGANIZE_SPLIT,
    IMAGE_TO_PDF,
    COMPRESS,
    EXTRACT_TEXT
}
