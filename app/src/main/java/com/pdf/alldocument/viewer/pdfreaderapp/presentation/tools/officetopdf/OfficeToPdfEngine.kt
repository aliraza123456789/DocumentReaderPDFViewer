package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.officetopdf

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Xml
import com.pdf.alldocument.viewer.pdfreaderapp.common.PageFormat
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Self-contained Office -> PDF engine for OOXML formats (.docx, .xlsx, .pptx).
 * It extracts text/tables/slide text and lays them out with PdfDocument.
 */
class OfficeToPdfEngine {

    enum class Kind { DOCX, XLSX, PPTX }

    data class LogicalPage(val lines: List<String>, val landscape: Boolean = false, val title: String? = null, val mono: Boolean = false)

    fun kindOf(name: String): Kind? = when (name.substringAfterLast('.').lowercase()) {
        "docx" -> Kind.DOCX
        "xlsx" -> Kind.XLSX
        "pptx" -> Kind.PPTX
        else -> null
    }

    fun convert(
        input: () -> InputStream,
        kind: Kind,
        output: File,
        onProgress: (Int) -> Unit
    ) {
        onProgress(5)
        val pages = when (kind) {
            Kind.DOCX -> readDocx(input())
            Kind.XLSX -> readXlsx(input)
            Kind.PPTX -> readPptx(input())
        }
        onProgress(30)
        require(pages.isNotEmpty()) { "Document has no readable content" }
        writePdf(pages, output, onProgress)
        onProgress(100)
    }

    private fun zipEntries(stream: InputStream, wanted: (String) -> Boolean): Map<String, ByteArray> {
        val out = linkedMapOf<String, ByteArray>()
        ZipInputStream(stream).use { z ->
            var e = z.nextEntry
            while (e != null) {
                if (wanted(e.name)) out[e.name] = z.readBytes()
                e = z.nextEntry
            }
        }
        return out
    }

    private fun parser(bytes: ByteArray): XmlPullParser =
        Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }

    private fun readDocx(stream: InputStream): List<LogicalPage> {
        val xml = zipEntries(stream) { it == "word/document.xml" }["word/document.xml"] ?: error("Invalid DOCX")
        val p = parser(xml)
        val pages = mutableListOf<LogicalPage>()
        var current = mutableListOf<String>()
        val para = StringBuilder()
        var inText = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "t" -> inText = true
                    "tab" -> para.append("    ")
                    "br" -> if (p.getAttributeValue(null, "type") == "page" ||
                        p.getAttributeValue("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "type") == "page") {
                        pages += LogicalPage(current); current = mutableListOf()
                    }
                }
                XmlPullParser.TEXT -> if (inText) para.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inText = false
                    "p" -> { current += para.toString(); para.setLength(0) }
                }
            }
            ev = p.next()
        }
        if (current.isNotEmpty()) pages += LogicalPage(current)
        return pages
    }

    private fun readXlsx(input: () -> InputStream): List<LogicalPage> {
        val files = zipEntries(input()) { it == "xl/sharedStrings.xml" || it.startsWith("xl/worksheets/sheet") }
        val shared = mutableListOf<String>()
        files["xl/sharedStrings.xml"]?.let { bytes ->
            val p = parser(bytes)
            val sb = StringBuilder(); var inT = false; var inSi = false
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> if (p.name == "si") { inSi = true; sb.setLength(0) } else if (p.name == "t") inT = true
                    XmlPullParser.TEXT -> if (inT && inSi) sb.append(p.text)
                    XmlPullParser.END_TAG -> if (p.name == "t") inT = false else if (p.name == "si") { shared += sb.toString(); inSi = false }
                }
                ev = p.next()
            }
        }
        val pages = mutableListOf<LogicalPage>()
        files.filterKeys { it.startsWith("xl/worksheets/sheet") }.toSortedMap().forEach { (name, bytes) ->
            val p = parser(bytes)
            val rows = mutableListOf<List<String>>()
            var row = mutableListOf<String>()
            var type: String? = null; var inV = false; var inIs = false; var value = StringBuilder()
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "row" -> row = mutableListOf()
                        "c" -> { type = p.getAttributeValue(null, "t"); value = StringBuilder() }
                        "v" -> inV = true
                        "is" -> inIs = true
                        "t" -> if (inIs) inV = true
                    }
                    XmlPullParser.TEXT -> if (inV) value.append(p.text)
                    XmlPullParser.END_TAG -> when (p.name) {
                        "v", "t" -> inV = false
                        "is" -> inIs = false
                        "c" -> row += if (type == "s") shared.getOrElse(value.toString().toIntOrNull() ?: -1) { "" } else value.toString()
                        "row" -> rows += row
                    }
                }
                ev = p.next()
            }
            val cols = rows.maxOfOrNull { it.size } ?: 0
            val widths = List(cols) { c -> rows.maxOf { r -> r.getOrNull(c)?.length ?: 0 }.coerceIn(3, 24) }
            val lines = rows.map { r -> r.mapIndexed { i, v -> v.take(widths[i]).padEnd(widths[i]) }.joinToString(" | ") }
            lines.chunked(60).forEachIndexed { i, chunk ->
                pages += LogicalPage(chunk, landscape = true, title = if (i == 0) name.substringAfterLast('/').removeSuffix(".xml") else null, mono = true)
            }
        }
        return pages
    }

    private fun readPptx(stream: InputStream): List<LogicalPage> {
        val slides = zipEntries(stream) { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
            .toSortedMap(compareBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 })
        return slides.values.mapIndexed { idx, bytes ->
            val p = parser(bytes)
            val lines = mutableListOf<String>(); val sb = StringBuilder(); var inT = false
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> if (p.name == "t") inT = true
                    XmlPullParser.TEXT -> if (inT) sb.append(p.text)
                    XmlPullParser.END_TAG -> when (p.name) {
                        "t" -> inT = false
                        "p" -> { if (sb.isNotBlank()) lines += sb.toString(); sb.setLength(0) }
                    }
                }
                ev = p.next()
            }
            LogicalPage(lines, landscape = true, title = "Slide ${idx + 1}")
        }
    }

    private fun writePdf(pages: List<LogicalPage>, output: File, onProgress: (Int) -> Unit) {
        val doc = PdfDocument()
        val bodyText = Paint().apply { textSize = 11f; isAntiAlias = true; typeface = Typeface.DEFAULT }
        val bodyMono = Paint().apply { textSize = 9f; isAntiAlias = true; typeface = Typeface.MONOSPACE }
        val head = Paint().apply { textSize = 16f; isAntiAlias = true; isFakeBoldText = true }
        var pageNo = 1
        val margin = 40f; val lineH = 15f

        pages.forEachIndexed { index, lp ->
            val (w, h) = if (lp.landscape) PageFormat.A4.heightPt.toInt() to PageFormat.A4.widthPt.toInt()
                         else PageFormat.A4.widthPt.toInt() to PageFormat.A4.heightPt.toInt()
            val body = if (lp.mono) bodyMono else bodyText
            var page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo++).create())
            var y = margin
            lp.title?.let { page.canvas.drawText(it, margin, y, head); y += 26f }

            fun wrap(s: String): List<String> {
                if (s.isEmpty() || body.measureText(s) <= w - margin * 2) return listOf(s)
                val out = mutableListOf<String>(); var cur = ""
                for (word in s.split(" ")) {
                    val t = if (cur.isEmpty()) word else "$cur $word"
                    if (body.measureText(t) > w - margin * 2) { out += cur; cur = word } else cur = t
                }
                return out + cur
            }
            for (line in lp.lines.flatMap(::wrap)) {
                if (y > h - margin) {
                    doc.finishPage(page)
                    page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo++).create())
                    y = margin
                }
                page.canvas.drawText(line, margin, y, body); y += lineH
            }
            doc.finishPage(page)
            onProgress(30 + ((index + 1) * 65 / pages.size))
        }
        output.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}
