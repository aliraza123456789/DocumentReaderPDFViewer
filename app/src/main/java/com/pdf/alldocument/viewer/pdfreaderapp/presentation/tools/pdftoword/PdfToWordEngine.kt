package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.pdftoword

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Extracts text page-by-page with PDFTextStripper and writes a valid .docx
 * (WordprocessingML) with one paragraph per line-group and a page break between PDF pages.
 * Dependency-free DOCX writing (works on Android, unlike Apache POI).
 */
class PdfToWordEngine {

    fun pageCount(input: InputStream): Int = input.use { PDDocument.load(it).use { d -> d.numberOfPages } }

    fun convert(
        input: InputStream,
        output: File,
        fromPage: Int,
        toPage: Int,
        onProgress: (Int) -> Unit
    ) {
        val body = StringBuilder()
        PDDocument.load(input).use { doc ->
            val last = toPage.coerceAtMost(doc.numberOfPages)
            val total = (last - fromPage + 1).coerceAtLeast(1)
            val stripper = PDFTextStripper()
            for (p in fromPage..last) {
                stripper.startPage = p; stripper.endPage = p
                val text = stripper.getText(doc)
                text.split(Regex("\n\\s*\n")).forEach { para ->
                    val clean = para.lines().joinToString(" ") { it.trim() }.trim()
                    if (clean.isNotEmpty()) body.append(paragraph(clean))
                }
                if (p < last) body.append("""<w:p><w:r><w:br w:type="page"/></w:r></w:p>""")
                onProgress((p - fromPage + 1) * 100 / total)
            }
        }
        writeDocx(output, body.toString())
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun paragraph(text: String) =
        """<w:p><w:r><w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p>"""

    private fun writeDocx(file: File, bodyXml: String) {
        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$bodyXml<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr></w:body></w:document>"""
        ZipOutputStream(file.outputStream()).use { z ->
            fun put(name: String, content: String) {
                z.putNextEntry(ZipEntry(name)); z.write(content.toByteArray(Charsets.UTF_8)); z.closeEntry()
            }
            put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            put("word/document.xml", document)
        }
    }
}
