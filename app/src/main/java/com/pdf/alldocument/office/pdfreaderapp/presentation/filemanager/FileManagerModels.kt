package com.pdf.alldocument.office.pdfreaderapp.presentation.filemanager

import androidx.compose.ui.graphics.Color
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType

/** Document kinds the file manager can render an icon for. */
enum class FileType(
    val label: String,
    val extColor: Color,
    val gradient: Pair<Color, Color>
) {
    PDF(
        label = "PDF",
        extColor = Color(0xFFFF2D55),
        gradient = Color(0xFFFF5E7A) to Color(0xFFFF2D55)
    ),
    WORD(
        label = "DOC",
        extColor = Color(0xFF2979FF),
        gradient = Color(0xFF38B6FF) to Color(0xFF2979FF)
    ),
    TXT(
        label = "TXT",
        extColor = Color(0xFF7B61FF),
        gradient = Color(0xFFA259FF) to Color(0xFF7B61FF)
    ),
    PPT(
        label = "PPT",
        extColor = Color(0xFFFF7A00),
        gradient = Color(0xFFFFA726) to Color(0xFFFF7A00)
    ),

    /** Spreadsheets have no tab of their own in the spec's filter row, so they are All-only. */
    XLS(
        label = "XLS",
        extColor = Color(0xFF2E7D32),
        gradient = Color(0xFF4CAF50) to Color(0xFF2E7D32)
    ),

    /** Anything unrecognised. */
    FILE(
        label = "FILE",
        extColor = Color(0xFF546E7A),
        gradient = Color(0xFF90A4AE) to Color(0xFF546E7A)
    );

    /** Only PDF shows the small scissors glyph on its icon, per the design spec. */
    val showScissors: Boolean get() = this == PDF
}

/** A single row in the file list. */
data class FileItem(
    val name: String,
    val ext: String,
    val size: String,
    val date: String,
    val iconType: FileType
) {
    /** `ext` is stored without the leading dot, so normalise defensively. */
    val extWithDot: String get() = if (ext.startsWith(".")) ext else ".$ext"
}

/** Pill-shaped filter row above the list. */
enum class FileFilter(val label: String, val iconType: FileType) {
    ALL("All", FileType.FILE),
    PDF("PDF", FileType.PDF),
    WORD("WORD", FileType.WORD),
    PPT("PPT", FileType.PPT),
    TEXT("TEXT", FileType.TXT);

    fun matches(item: FileItem): Boolean = matches(item.iconType)

    /** Fast path for list filtering — avoids building a [FileItem] just to compare. */
    fun matches(type: FileType): Boolean = when (this) {
        ALL -> true
        PDF -> type == FileType.PDF
        WORD -> type == FileType.WORD
        PPT -> type == FileType.PPT
        TEXT -> type == FileType.TXT
    }

    /** The domain filter this tab maps onto when querying the repository. */
    val documentType: DocumentType
        get() = when (this) {
            ALL -> DocumentType.ALL
            PDF -> DocumentType.PDF
            WORD -> DocumentType.WORD
            PPT -> DocumentType.PPT
            TEXT -> DocumentType.TXT
        }
}

/** Bottom navigation destinations. */
enum class NavTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", FmIcons.Home),
    RECENT("Recent", FmIcons.Recent),
    FAVOURITE("Favourite", FmIcons.Favourite),
    TOOLS("Tools", FmIcons.Tools)
}

/**
 * One entry in the list: either a document or an inline ad slot.
 * Keeps the ad injection logic out of the composable.
 *
 * Holds the real [DocumentItem] (not the display model) so clicks, renames and
 * deletes all carry the path straight through to the repository.
 */
sealed interface ListEntry {
    data class Document(val document: DocumentItem) : ListEntry
    data object Ad : ListEntry
}

/**
 * Splits [documents] into list entries, dropping an ad after every
 * [adAfterEvery] files. A trailing ad is never appended because it would
 * never be reached.
 */
fun buildListEntries(
    documents: List<DocumentItem>,
    adAfterEvery: Int = 4
): List<ListEntry> {
    if (adAfterEvery <= 0) return documents.map(ListEntry::Document)
    val entries = mutableListOf<ListEntry>()
    documents.forEachIndexed { index, document ->
        entries += ListEntry.Document(document)
        val isLast = index == documents.lastIndex
        if (!isLast && (index + 1) % adAfterEvery == 0) entries += ListEntry.Ad
    }
    return entries
}

/** Fake data matching the reference screenshot. */
val sampleFiles: List<FileItem> = listOf(
    FileItem("Business Plan.", "pdf", "257.1 KB", "03/24/2024", FileType.PDF),
    FileItem("Marketing Strategy", "doc", "1.2 MB", "03/22/2024", FileType.WORD),
    FileItem("Meeting Notes", "text", "48.3 KB", "03/20/2024", FileType.TXT),
    FileItem("Quarterly Report", "ppt", "3.8 MB", "03/18/2024", FileType.PPT),
    FileItem("Annual Budget", "pdf", "890.4 KB", "03/15/2024", FileType.PDF),
    FileItem("Project Proposal", "docx", "624.0 KB", "03/12/2024", FileType.WORD),
    FileItem("Readme First", "txt", "12.8 KB", "03/10/2024", FileType.TXT),
    FileItem("Product Launch", "pptx", "5.1 MB", "03/08/2024", FileType.PPT),
    FileItem("Design Portfolio", "pdf", "1.8 MB", "03/05/2024", FileType.PDF),
    FileItem("Expense Summary", "xlsx", "142.6 KB", "03/02/2024", FileType.XLS),
    FileItem("Release Notes", "txt", "22.4 KB", "02/28/2024", FileType.TXT),
    FileItem("Investor Deck", "pptx", "7.3 MB", "02/25/2024", FileType.PPT)
)

/* ------------------------------------------------------------------ */
/* Domain <-> display mapping                                          */
/* ------------------------------------------------------------------ */

/** Repository type -> icon type. */
fun DocumentType.toFileType(): FileType = when (this) {
    DocumentType.PDF -> FileType.PDF
    DocumentType.WORD -> FileType.WORD
    DocumentType.EXCEL -> FileType.XLS
    DocumentType.PPT -> FileType.PPT
    DocumentType.TXT -> FileType.TXT
    DocumentType.ALL -> FileType.FILE
}

/**
 * Splits `"Business Plan.pdf"` into `("Business Plan", "pdf")`.
 * Tolerant of a name that already omits its extension.
 */
private fun splitName(fullName: String, ext: String): Pair<String, String> {
    val bareExt = ext.removePrefix(".")
    val suffix = ".$bareExt"
    val base = when {
        fullName.equals(suffix, ignoreCase = true) -> ""
        fullName.endsWith(suffix, ignoreCase = true) -> fullName.dropLast(suffix.length)
        else -> fullName.substringBeforeLast('.', fullName)
    }
    return base to bareExt
}

/** Projects a real document into the display model the UI renders. */
fun DocumentItem.toFileItem(): FileItem {
    val (base, bareExt) = splitName(name, extension)
    return FileItem(
        name = base,
        ext = bareExt,
        size = formattedSize,
        date = formattedDate,
        iconType = type.toFileType()
    )
}
