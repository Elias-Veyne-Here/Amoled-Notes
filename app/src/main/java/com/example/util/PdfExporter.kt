package com.example.util

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportToPdf(context: Context, note: Note): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 points (72 dpi)
        val pageHeight = 842
        val margin = 50f
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 22f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val metaPaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val bodyPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val dividerPaint = Paint().apply {
            color = AndroidColor.LTGRAY
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var y = margin + 30f

        // Draw Title
        val titleText = note.displayTitle
        canvas.drawText(titleText, margin, y, titlePaint)
        y += 24f

        // Draw Metadata (Folder, Date, Words)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        val metaString = "Folder: ${note.folder} | Words: ${note.wordCount} | ${dateFormat.format(Date(note.updatedAt))}"
        canvas.drawText(metaString, margin, y, metaPaint)
        y += 16f

        // Divider
        canvas.drawLine(margin, y, margin + contentWidth, y, dividerPaint)
        y += 24f

        // Draw Body Lines with word-wrapping and multi-page pagination
        val paragraphs = note.content.split("\n")
        val lineHeight = 18f

        for (paragraph in paragraphs) {
            val words = paragraph.split(" ")
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val measuredWidth = bodyPaint.measureText(testLine)

                if (measuredWidth > contentWidth) {
                    // Check if new page is needed
                    if (y + lineHeight > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = margin + 20f
                    }

                    canvas.drawText(currentLine, margin, y, bodyPaint)
                    y += lineHeight
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }

            if (currentLine.isNotEmpty()) {
                if (y + lineHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }
                canvas.drawText(currentLine, margin, y, bodyPaint)
                y += lineHeight
            }

            // Paragraph spacing
            y += 8f
        }

        pdfDocument.finishPage(page)

        // Save file
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = note.displayTitle.take(30).replace(Regex("[^a-zA-Z0-9_]"), "_")
        val pdfFile = File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    fun getFileUri(context: Context, file: File): android.net.Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
