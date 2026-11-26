package com.smartattendance.app.export

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PngExporter {

    private const val PORTRAIT_W = 1190
    private const val PORTRAIT_H = 1684
    private const val LANDSCAPE_W = 1684
    private const val LANDSCAPE_H = 1190

    fun exportSingleLecturePages(data: SingleLectureExportData, outputDir: File): List<File> {
        outputDir.mkdirs()
        val rowsPerPage = 28
        val chunks = if (data.rows.isEmpty()) listOf(emptyList()) else data.rows.chunked(rowsPerPage)
        val totalPages = chunks.size
        val files = mutableListOf<File>()

        chunks.forEachIndexed { idx, pageRows ->
            val file = File(outputDir, String.format(Locale.US, "page_%02d.png", idx + 1))
            val bitmap = Bitmap.createBitmap(PORTRAIT_W, PORTRAIT_H, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                canvas.scale(2f, 2f)
                PdfExporter.drawSingleLecturePage(
                    canvas = canvas,
                    width = PORTRAIT_W / 2f,
                    height = PORTRAIT_H / 2f,
                    data = data,
                    rows = pageRows,
                    pageNumber = idx + 1,
                    totalPages = totalPages
                )
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } finally {
                bitmap.recycle()
            }
            check(file.exists() && file.length() > 0L) { "Generated PNG page ${idx + 1} is empty." }
            files.add(file)
        }
        return files
    }

    fun exportFullRegisterPages(data: FullRegisterExportData, outputDir: File): List<File> {
        outputDir.mkdirs()
        val rowsPerPage = 18
        val colsPerPage = 10
        val rowChunks = if (data.rows.isEmpty()) listOf(emptyList()) else data.rows.chunked(rowsPerPage)
        val colSlices = PdfExporter.buildColumnSlices(data.columns.size, colsPerPage)
        val totalPages = rowChunks.size * colSlices.size
        val files = mutableListOf<File>()

        var pageCounter = 0
        rowChunks.forEach { pageRows ->
            colSlices.forEachIndexed { sliceIdx, colRange ->
                pageCounter++
                val file = File(outputDir, String.format(Locale.US, "page_%02d.png", pageCounter))
                val bitmap = Bitmap.createBitmap(LANDSCAPE_W, LANDSCAPE_H, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(bitmap)
                    canvas.scale(2f, 2f)
                    PdfExporter.drawRegisterPage(
                        canvas = canvas,
                        width = LANDSCAPE_W / 2f,
                        height = LANDSCAPE_H / 2f,
                        data = data,
                        rows = pageRows,
                        colRange = colRange,
                        includeSummaryCols = sliceIdx == colSlices.lastIndex,
                        pageNumber = pageCounter,
                        totalPages = totalPages
                    )
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } finally {
                    bitmap.recycle()
                }
                check(file.exists() && file.length() > 0L) { "Generated PNG page $pageCounter is empty." }
                files.add(file)
            }
        }
        return files
    }
}
