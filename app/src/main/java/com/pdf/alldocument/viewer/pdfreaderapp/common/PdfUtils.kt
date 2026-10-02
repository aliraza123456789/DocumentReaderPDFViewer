package com.pdf.alldocument.viewer.pdfreaderapp.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

enum class PageFormat(val widthPt: Float, val heightPt: Float) {
    A4(595f, 842f),
    US_LETTER(612f, 792f),
    FOUR_BY_SIX(288f, 432f),
    AUTO_FIT(0f, 0f) // page sized to the image itself
}

enum class MarginStyle(val pt: Float) {
    NONE(0f),
    THIN(16f),
    WIDE(40f)
}

object MimeTypes {
    const val PDF = "application/pdf"
    const val IMAGES = "image/*"
}

/**
 * Native android.graphics.pdf.PdfDocument based multi-page PDF builder.
 * Used by: Image to PDF, Scan to PDF, ID Card Scanner, ID Photo Maker,
 * and as the backing engine for Office-to-PDF rendering.
 */
object PdfBuilder {

    fun build(
        outputFile: File,
        bitmaps: List<Bitmap>,
        format: PageFormat = PageFormat.A4,
        margin: MarginStyle = MarginStyle.THIN,
        quality: Int = 90 // JPEG re-encode quality
    ) {
        val document = PdfDocument()
        bitmaps.forEachIndexed { index, original ->
            val bitmap = if (quality < 100) recompress(original, quality) else original
            val (pageW, pageH) = when (format) {
                PageFormat.AUTO_FIT -> bitmap.width.toFloat() to bitmap.height.toFloat()
                else -> format.widthPt to format.heightPt
            }
            val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), index).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            drawFitted(canvas, bitmap, pageW, pageH, margin.pt)
            document.finishPage(page)
        }
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
    }

    private fun recompress(src: Bitmap, quality: Int): Bitmap {
        val bos = java.io.ByteArrayOutputStream()
        src.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), bos)
        val bytes = bos.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun drawFitted(canvas: Canvas, bitmap: Bitmap, pageW: Float, pageH: Float, margin: Float) {
        val availW = pageW - margin * 2
        val availH = pageH - margin * 2
        val scale = minOf(availW / bitmap.width, availH / bitmap.height)
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = (pageW - drawW) / 2f
        val top = (pageH - drawH) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(left, top)
        }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun buildTwoUp(outputFile: File, front: Bitmap, back: Bitmap, margin: MarginStyle = MarginStyle.THIN) {
        val pageW = PageFormat.A4.widthPt
        val pageH = PageFormat.A4.heightPt
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 0).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val halfH = (pageH - margin.pt * 3) / 2f
        val topRect = RectF(margin.pt, margin.pt, pageW - margin.pt, margin.pt + halfH)
        val bottomRect = RectF(margin.pt, margin.pt * 2 + halfH, pageW - margin.pt, margin.pt * 2 + halfH * 2)

        drawInRect(canvas, front, topRect)
        drawInRect(canvas, back, bottomRect)

        document.finishPage(page)
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
    }

    private fun drawInRect(canvas: Canvas, bitmap: Bitmap, rect: RectF) {
        val scale = minOf(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = rect.left + (rect.width() - drawW) / 2f
        val top = rect.top + (rect.height() - drawH) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(left, top)
        }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    fun buildPrintSheet(
        outputFile: File,
        photo: Bitmap,
        copies: Int,
        cellWidthPt: Float,
        cellHeightPt: Float,
        format: PageFormat = PageFormat.US_LETTER
    ) {
        val pageW = format.widthPt
        val pageH = format.heightPt
        val cols = (pageW / cellWidthPt).toInt().coerceAtLeast(1)
        val rows = (pageH / cellHeightPt).toInt().coerceAtLeast(1)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 0).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var drawn = 0
        outer@ for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (drawn >= copies) break@outer
                val rect = RectF(
                    c * cellWidthPt, r * cellHeightPt,
                    c * cellWidthPt + cellWidthPt, r * cellHeightPt + cellHeightPt
                )
                drawInRect(canvas, photo, rect)
                canvas.drawRect(rect, android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 0.5f
                    color = android.graphics.Color.LTGRAY
                })
                drawn++
            }
        }
        document.finishPage(page)
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
    }
}
