package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.split

import android.graphics.Bitmap
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem

enum class SplitFlowStep {
    SELECT_FILE,      // Screen 1: Pick PDF from device (Image 3)
    SELECT_PAGES,     // Screen 2: Select pages from PDF grid (Images 4 & 5)
    SPLIT_LIST,       // Screen 3: Review split parts queue (Image 1)
    SPLIT_SUCCESS     // Screen 5: Split results view (Missing screen)
}

data class SplitPart(
    val id: String,
    val name: String,
    val pageIndices: List<Int>, // 0-based page indices
    val pageRangeText: String   // e.g. "Pages 1-4"
)

data class PdfPageItem(
    val pageIndex: Int,
    val pageNumber: Int,
    val bitmap: Bitmap,
    val isSelected: Boolean = false
)

data class SplitResultItem(
    val fileName: String,
    val filePath: String,
    val fileSize: String,
    val pageCount: Int
)

data class SplitPdfUiState(
    val step: SplitFlowStep = SplitFlowStep.SELECT_FILE,
    val availablePdfs: List<DocumentItem> = emptyList(),
    val filteredPdfs: List<DocumentItem> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedPdfDoc: DocumentItem? = null,
    val pdfPages: List<PdfPageItem> = emptyList(),
    val isLoadingPages: Boolean = false,
    val splitParts: List<SplitPart> = emptyList(),
    val editingPartId: String? = null,
    val isSplitting: Boolean = false,
    val splitProgress: Int = 0,
    val splitResults: List<SplitResultItem> = emptyList(),
    val errorMessage: String? = null
)

fun formatPageRanges(pageIndices: List<Int>): String {
    if (pageIndices.isEmpty()) return "0 Pages"
    val sorted = pageIndices.map { it + 1 }.distinct().sorted()
    if (sorted.size == 1) return "Page ${sorted.first()}"

    val ranges = mutableListOf<String>()
    var start = sorted.first()
    var prev = start

    for (i in 1 until sorted.size) {
        val curr = sorted[i]
        if (curr == prev + 1) {
            prev = curr
        } else {
            if (start == prev) ranges.add("$start")
            else ranges.add("$start-$prev")
            start = curr
            prev = curr
        }
    }
    if (start == prev) ranges.add("$start")
    else ranges.add("$start-$prev")

    return "Pages " + ranges.joinToString(", ")
}
