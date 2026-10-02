package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.scantoexcel

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

/**
 * Converts ML Kit text elements (with bounding boxes) into a 2D table:
 *  1. Collect all words (elements) with their boxes.
 *  2. Cluster into rows by vertical centre proximity.
 *  3. Inside each row, sort by x and start a new cell when the horizontal gap
 *     is larger than a threshold (relative to median word height).
 *  4. Align cells to global column anchors so ragged rows line up.
 */
object TableParserEngine {

    private data class Word(val text: String, val box: Rect) {
        val cy get() = box.exactCenterY()
        val h get() = box.height()
    }

    fun parse(result: Text): List<List<String>> {
        val words = result.textBlocks.flatMap { b -> b.lines.flatMap { l -> l.elements } }
            .mapNotNull { e -> e.boundingBox?.let { Word(e.text, it) } }
        if (words.isEmpty()) return emptyList()

        val medianH = words.map { it.h }.sorted()[words.size / 2].coerceAtLeast(1)

        // 1) rows
        val rows = mutableListOf<MutableList<Word>>()
        for (w in words.sortedBy { it.cy }) {
            val row = rows.lastOrNull()
            if (row != null && kotlin.math.abs(row.map { it.cy }.average() - w.cy) < medianH * 0.6) row += w
            else rows += mutableListOf(w)
        }

        // 2) cells per row
        val gapThreshold = medianH * 1.2f
        val cellRows = rows.map { row ->
            val sorted = row.sortedBy { it.box.left }
            val cells = mutableListOf<Pair<Int, StringBuilder>>() // (leftX, text)
            var lastRight = Int.MIN_VALUE
            for (w in sorted) {
                if (cells.isEmpty() || w.box.left - lastRight > gapThreshold) {
                    cells += w.box.left to StringBuilder(w.text)
                } else cells.last().second.append(' ').append(w.text)
                lastRight = w.box.right
            }
            cells.map { it.first to it.second.toString() }
        }

        // 3) global column anchors: cluster all cell left-x values
        val anchors = mutableListOf<Int>()
        for (x in cellRows.flatten().map { it.first }.sorted()) {
            if (anchors.isEmpty() || x - anchors.last() > medianH * 2) anchors += x
        }
        fun colIndex(x: Int) = anchors.indices.minByOrNull { kotlin.math.abs(anchors[it] - x) } ?: 0

        return cellRows.map { cells ->
            val out = MutableList(anchors.size) { "" }
            for ((x, t) in cells) {
                val i = colIndex(x)
                out[i] = if (out[i].isEmpty()) t else out[i] + " " + t
            }
            out
        }
    }

    fun writeCsv(file: File, table: List<List<String>>) {
        file.bufferedWriter().use { w ->
            CSVPrinter(w, CSVFormat.DEFAULT).use { p -> table.forEach { p.printRecord(it) } }
        }
    }

    /** Minimal, dependency-free .xlsx writer (inline strings). Opens in Excel, Sheets, LibreOffice. */
    fun writeXlsx(file: File, table: List<List<String>>) {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        fun colName(i: Int): String {
            var n = i; var s = ""
            do { s = ('A' + n % 26) + s; n = n / 26 - 1 } while (n >= 0)
            return s
        }
        val sheet = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            table.forEachIndexed { r, row ->
                append("""<row r="${r + 1}">""")
                row.forEachIndexed { c, v ->
                    if (v.isNotEmpty()) append("""<c r="${colName(c)}${r + 1}" t="inlineStr"><is><t xml:space="preserve">${esc(v)}</t></is></c>""")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        ZipOutputStream(file.outputStream()).use { z ->
            fun put(name: String, content: String) {
                z.putNextEntry(ZipEntry(name)); z.write(content.toByteArray()); z.closeEntry()
            }
            put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            put("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            put("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            put("xl/worksheets/sheet1.xml", sheet)
        }
    }
}
