package com.smartattendance.app.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.smartattendance.app.domain.model.AttendanceStatus
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    private const val PORTRAIT_W = 595
    private const val PORTRAIT_H = 842
    private const val LANDSCAPE_W = 842
    private const val LANDSCAPE_H = 595
    private const val MARGIN = 28f

    fun exportSingleLecture(data: SingleLectureExportData, targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        val rowsPerPage = 28
        val chunks = if (data.rows.isEmpty()) listOf(emptyList()) else data.rows.chunked(rowsPerPage)
        val totalPages = chunks.size

        val doc = PdfDocument()
        try {
            chunks.forEachIndexed { pageIndex, pageRows ->
                val pageInfo = PdfDocument.PageInfo.Builder(PORTRAIT_W, PORTRAIT_H, pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                drawSingleLecturePage(
                    canvas = page.canvas,
                    width = PORTRAIT_W.toFloat(),
                    height = PORTRAIT_H.toFloat(),
                    data = data,
                    rows = pageRows,
                    pageNumber = pageIndex + 1,
                    totalPages = totalPages
                )
                doc.finishPage(page)
            }
            FileOutputStream(targetFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        check(targetFile.exists() && targetFile.length() > 0L) { "Generated PDF is empty." }
        return targetFile
    }

    fun exportFullRegister(data: FullRegisterExportData, targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        val rowsPerPage = 18
        val colsPerPage = 10

        val rowChunks = if (data.rows.isEmpty()) listOf(emptyList()) else data.rows.chunked(rowsPerPage)
        val colSlices = buildColumnSlices(data.columns.size, colsPerPage)
        val totalPages = rowChunks.size * colSlices.size

        val doc = PdfDocument()
        try {
            var pageCounter = 0
            rowChunks.forEach { pageRows ->
                colSlices.forEachIndexed { sliceIdx, colRange ->
                    pageCounter++
                    val includeSummary = sliceIdx == colSlices.lastIndex
                    val pageInfo = PdfDocument.PageInfo.Builder(LANDSCAPE_W, LANDSCAPE_H, pageCounter).create()
                    val page = doc.startPage(pageInfo)
                    drawRegisterPage(
                        canvas = page.canvas,
                        width = LANDSCAPE_W.toFloat(),
                        height = LANDSCAPE_H.toFloat(),
                        data = data,
                        rows = pageRows,
                        colRange = colRange,
                        includeSummaryCols = includeSummary,
                        pageNumber = pageCounter,
                        totalPages = totalPages
                    )
                    doc.finishPage(page)
                }
            }
            FileOutputStream(targetFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        check(targetFile.exists() && targetFile.length() > 0L) { "Generated PDF is empty." }
        return targetFile
    }

    internal fun buildColumnSlices(totalCols: Int, colsPerPage: Int): List<IntRange> {
        if (totalCols <= 0) return listOf(IntRange.EMPTY)
        val slices = mutableListOf<IntRange>()
        var start = 0
        while (start < totalCols) {
            val end = minOf(start + colsPerPage - 1, totalCols - 1)
            slices.add(start..end)
            start += colsPerPage
        }
        return slices
    }

    internal fun drawSingleLecturePage(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: SingleLectureExportData,
        rows: List<SingleLectureRow>,
        pageNumber: Int,
        totalPages: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val startY = drawReportHeader(
            canvas = canvas,
            width = width,
            meta = data.metadata,
            subtitle = "Single Lecture #${data.lectureNumber} • P: ${data.presentCount}  A: ${data.absentCount}  L: ${data.leaveCount}"
        )

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B4EA2") }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E1B2E")
            textSize = 10f
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DCE6F5")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val tableW = width - MARGIN * 2
        val colRollW = 100f
        val colStatusW = 80f
        val colNameW = tableW - colRollW - colStatusW
        val rowH = 22f

        var y = startY
        canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, headerPaint)
        canvas.drawText("Roll No", MARGIN + 6f, y + 15f, headerTextPaint)
        canvas.drawText("Student Name", MARGIN + colRollW + 6f, y + 15f, headerTextPaint)
        canvas.drawText("Status", MARGIN + colRollW + colNameW + 6f, y + 15f, headerTextPaint)
        y += rowH

        rows.forEachIndexed { idx, row ->
            if (idx % 2 == 1) {
                val alt = Paint().apply { color = Color.parseColor("#F6F9FE") }
                canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, alt)
            }
            canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, borderPaint)
            canvas.drawText(row.rollNumber.take(16), MARGIN + 6f, y + 15f, rowTextPaint)
            canvas.drawText(row.studentName.take(38), MARGIN + colRollW + 6f, y + 15f, rowTextPaint)
            val statusPaint = Paint(rowTextPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = statusColor(row.status)
            }
            canvas.drawText("${row.status.code} (${row.status.label})", MARGIN + colRollW + colNameW + 6f, y + 15f, statusPaint)
            y += rowH
        }

        drawPageFooter(canvas, width, height, pageNumber, totalPages)
    }

    internal fun drawRegisterPage(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: FullRegisterExportData,
        rows: List<RegisterStudentRow>,
        colRange: IntRange,
        includeSummaryCols: Boolean,
        pageNumber: Int,
        totalPages: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val startY = drawReportHeader(
            canvas = canvas,
            width = width,
            meta = data.metadata,
            subtitle = "Full Attendance Register"
        )

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B4EA2") }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E1B2E")
            textSize = 9f
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DCE6F5")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val colRollW = 72f
        val colNameW = 140f
        val dateCols = if (colRange.isEmpty()) emptyList() else colRange.toList()
        val summaryCount = if (includeSummaryCols) 4 else 0
        val remainingW = (width - MARGIN * 2 - colRollW - colNameW).coerceAtLeast(100f)
        val dynamicColCount = (dateCols.size + summaryCount).coerceAtLeast(1)
        val colW = (remainingW / dynamicColCount).coerceAtMost(54f)
        val rowH = 22f

        var y = startY
        val totalUsedW = colRollW + colNameW + colW * (dateCols.size + summaryCount)
        canvas.drawRect(MARGIN, y, MARGIN + totalUsedW, y + rowH, headerPaint)
        canvas.drawText("Roll No", MARGIN + 4f, y + 14f, headerTextPaint)
        canvas.drawText("Student Name", MARGIN + colRollW + 4f, y + 14f, headerTextPaint)

        var x = MARGIN + colRollW + colNameW
        dateCols.forEach { idx ->
            canvas.drawText(data.columns[idx].shortLabel, x + 3f, y + 14f, headerTextPaint)
            x += colW
        }
        if (includeSummaryCols) {
            listOf("P", "A", "L", "%").forEach { lbl ->
                canvas.drawText(lbl, x + 6f, y + 14f, headerTextPaint)
                x += colW
            }
        }
        y += rowH

        rows.forEachIndexed { rIdx, row ->
            if (rIdx % 2 == 1) {
                val alt = Paint().apply { color = Color.parseColor("#F6F9FE") }
                canvas.drawRect(MARGIN, y, MARGIN + totalUsedW, y + rowH, alt)
            }
            canvas.drawRect(MARGIN, y, MARGIN + totalUsedW, y + rowH, borderPaint)
            canvas.drawText(row.rollNumber.take(12), MARGIN + 4f, y + 14f, cellPaint)
            val name = if (row.isArchived) "${row.studentName}*" else row.studentName
            canvas.drawText(name.take(22), MARGIN + colRollW + 4f, y + 14f, cellPaint)

            var cx = MARGIN + colRollW + colNameW
            dateCols.forEach { colIdx ->
                val st = row.statuses.getOrNull(colIdx)
                val p = Paint(cellPaint).apply {
                    color = if (st != null) statusColor(st) else Color.GRAY
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(st?.code ?: "-", cx + 8f, y + 14f, p)
                cx += colW
            }
            if (includeSummaryCols) {
                canvas.drawText(row.presentCount.toString(), cx + 6f, y + 14f, cellPaint)
                canvas.drawText(row.absentCount.toString(), cx + colW + 6f, y + 14f, cellPaint)
                canvas.drawText(row.leaveCount.toString(), cx + colW * 2 + 6f, y + 14f, cellPaint)
                canvas.drawText(row.percentageLabel, cx + colW * 3 + 3f, y + 14f, cellPaint)
            }
            y += rowH
        }

        drawPageFooter(canvas, width, height, pageNumber, totalPages)
    }

    private fun drawReportHeader(
        canvas: Canvas,
        width: Float,
        meta: ExportMetadata,
        subtitle: String
    ): Float {
        var textStartX = MARGIN
        val logoFile = meta.logoPath?.let { File(it) }?.takeIf { it.exists() }
        if (logoFile != null) {
            val logoBmp: Bitmap? = runCatching { BitmapFactory.decodeFile(logoFile.absolutePath) }.getOrNull()
            if (logoBmp != null) {
                try {
                    val dest = Rect(MARGIN.toInt(), MARGIN.toInt(), (MARGIN + 42).toInt(), (MARGIN + 42).toInt())
                    canvas.drawBitmap(logoBmp, null, dest, null)
                    textStartX = MARGIN + 52f
                } finally {
                    logoBmp.recycle()
                }
            }
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#073572")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52647D")
            textSize = 10f
        }

        val sec = if (meta.section.isBlank()) "" else " (${meta.section})"
        canvas.drawText(meta.instituteName, textStartX, MARGIN + 14f, titlePaint)
        canvas.drawText("$subtitle • ${meta.programName} • ${meta.semesterLabel}$sec", textStartX, MARGIN + 29f, subPaint)
        canvas.drawText("Subject: ${meta.subjectName} • Teacher: ${meta.teacherName} • ${meta.dateRangeLabel}", textStartX, MARGIN + 43f, subPaint)

        val linePaint = Paint().apply {
            color = Color.parseColor("#0B4EA2")
            strokeWidth = 1.5f
        }
        val lineY = MARGIN + 52f
        canvas.drawLine(MARGIN, lineY, width - MARGIN, lineY, linePaint)
        return lineY + 10f
    }

    private fun drawPageFooter(
        canvas: Canvas,
        width: Float,
        height: Float,
        pageNumber: Int,
        totalPages: Int
    ) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52647D")
            textSize = 9.5f
        }
        canvas.drawText("Page $pageNumber of $totalPages", width - MARGIN - 70f, height - 14f, footerPaint)
        canvas.drawText("Generated by Smart Attendance", MARGIN, height - 14f, footerPaint)
    }

    private fun statusColor(status: AttendanceStatus): Int = when (status) {
        AttendanceStatus.PRESENT -> Color.parseColor("#1B873F")
        AttendanceStatus.ABSENT -> Color.parseColor("#C62828")
        AttendanceStatus.LEAVE -> Color.parseColor("#E65100")
    }

    fun exportReport(data: ReportExportData, targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        val rowsPerPage = 26
        val chunks = if (data.rows.isEmpty()) listOf(emptyList()) else data.rows.chunked(rowsPerPage)
        val totalPages = chunks.size

        val doc = PdfDocument()
        try {
            chunks.forEachIndexed { idx, pageRows ->
                val pageInfo = PdfDocument.PageInfo.Builder(PORTRAIT_W, PORTRAIT_H, idx + 1).create()
                val page = doc.startPage(pageInfo)
                drawReportPage(page.canvas, PORTRAIT_W.toFloat(), PORTRAIT_H.toFloat(), data, pageRows, idx + 1, totalPages)
                doc.finishPage(page)
            }
            FileOutputStream(targetFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        check(targetFile.exists() && targetFile.length() > 0L) { "Generated report PDF is empty." }
        return targetFile
    }

    internal fun drawReportPage(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: ReportExportData,
        rows: List<ReportExportRow>,
        pageNumber: Int,
        totalPages: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val meta = ExportMetadata(
            instituteName = data.instituteName,
            logoPath = data.logoPath,
            programName = data.reportTitle,
            semesterLabel = data.activeRuleLabel,
            section = "",
            subjectName = "Low < ${data.threshold}%",
            teacherName = "Overall: P=${data.totalPresent} A=${data.totalAbsent} L=${data.totalLeave} (${data.overallPercentageLabel})",
            dateRangeLabel = data.dateRangeLabel,
            leaveRule = com.smartattendance.app.domain.model.LeaveRule.INCLUDE_LEAVE,
            generatedAtLabel = ""
        )
        val startY = drawReportHeader(canvas, width, meta, data.reportTitle)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B4EA2") }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E1B2E")
            textSize = 9.5f
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DCE6F5")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val tableW = width - MARGIN * 2
        val col1W = 140f
        val col2W = 180f
        val numW = (tableW - col1W - col2W) / 4f
        val rowH = 22f
        var y = startY

        canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, headerPaint)
        canvas.drawText("Item", MARGIN + 4f, y + 14f, headerTextPaint)
        canvas.drawText("Details", MARGIN + col1W + 4f, y + 14f, headerTextPaint)
        canvas.drawText("P", MARGIN + col1W + col2W + 6f, y + 14f, headerTextPaint)
        canvas.drawText("A", MARGIN + col1W + col2W + numW + 6f, y + 14f, headerTextPaint)
        canvas.drawText("L", MARGIN + col1W + col2W + numW * 2 + 6f, y + 14f, headerTextPaint)
        canvas.drawText("%", MARGIN + col1W + col2W + numW * 3 + 6f, y + 14f, headerTextPaint)
        y += rowH

        rows.forEachIndexed { rIdx, row ->
            if (row.isLowAttendance) {
                val lowBg = Paint().apply { color = Color.parseColor("#FCE8E6") }
                canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, lowBg)
            } else if (rIdx % 2 == 1) {
                val alt = Paint().apply { color = Color.parseColor("#F6F9FE") }
                canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, alt)
            }
            canvas.drawRect(MARGIN, y, MARGIN + tableW, y + rowH, borderPaint)
            canvas.drawText(row.primaryLabel.take(22), MARGIN + 4f, y + 14f, cellPaint)
            canvas.drawText(row.secondaryLabel.take(28), MARGIN + col1W + 4f, y + 14f, cellPaint)
            canvas.drawText(row.presentCount.toString(), MARGIN + col1W + col2W + 6f, y + 14f, cellPaint)
            canvas.drawText(row.absentCount.toString(), MARGIN + col1W + col2W + numW + 6f, y + 14f, cellPaint)
            canvas.drawText(row.leaveCount.toString(), MARGIN + col1W + col2W + numW * 2 + 6f, y + 14f, cellPaint)
            val pctPaint = Paint(cellPaint).apply {
                color = if (row.isLowAttendance) Color.parseColor("#C62828") else Color.parseColor("#0E1B2E")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(row.percentageLabel, MARGIN + col1W + col2W + numW * 3 + 4f, y + 14f, pctPaint)
            y += rowH
        }
        drawPageFooter(canvas, width, height, pageNumber, totalPages)
    }
}
