package com.smartattendance.app.export

import com.smartattendance.app.core.normalizeRollNumber
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets

data class ImportedStudentRow(
    val rowNumber: Int,
    val rollNumber: String,
    val name: String,
    val error: String? = null
) {
    val isValid: Boolean get() = error == null
}

data class ImportPreviewResult(
    val rows: List<ImportedStudentRow>,
    val validCount: Int,
    val errorCount: Int
)

object StudentImporter {

    init {
        // Ensure Woodstox StAX provider is registered on Android
        if (System.getProperty("javax.xml.stream.XMLInputFactory").isNullOrBlank()) {
            System.setProperty(
                "javax.xml.stream.XMLInputFactory",
                "com.fasterxml.woodstox.stax.WstxInputFactory"
            )
        }
        if (System.getProperty("javax.xml.stream.XMLOutputFactory").isNullOrBlank()) {
            System.setProperty(
                "javax.xml.stream.XMLOutputFactory",
                "com.fasterxml.woodstox.stax.WstxOutputFactory"
            )
        }
    }

    fun parseCsv(
        inputStream: InputStream,
        existingRolls: Set<String>
    ): ImportPreviewResult {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val rawRows = mutableListOf<List<String>>()
        reader.forEachLine { line ->
            val cleaned = line.removePrefix("\uFEFF")
            if (cleaned.isNotBlank()) {
                rawRows.add(parseCsvLine(cleaned))
            }
        }
        return validateRows(rawRows, existingRolls)
    }

    fun parseXlsx(
        inputStream: InputStream,
        existingRolls: Set<String>
    ): ImportPreviewResult {
        val rawRows = mutableListOf<List<String>>()
        ReadableWorkbook(inputStream).use { workbook ->
            val sheet = workbook.firstSheet
            sheet.openStream().use { rows ->
                rows.forEach { row ->
                    val cell0 = row.getCellText(0)?.trim().orEmpty()
                    val cell1 = row.getCellText(1)?.trim().orEmpty()
                    if (cell0.isNotEmpty() || cell1.isNotEmpty()) {
                        rawRows.add(listOf(cell0, cell1))
                    }
                }
            }
        }
        return validateRows(rawRows, existingRolls)
    }

    private fun validateRows(
        rawRows: List<List<String>>,
        existingRolls: Set<String>
    ): ImportPreviewResult {
        if (rawRows.isEmpty()) {
            return ImportPreviewResult(emptyList(), 0, 0)
        }

        val firstRow = rawRows.first()
        val hasHeader = firstRow.size >= 2 &&
            (firstRow[0].contains("roll", ignoreCase = true) ||
                firstRow[1].contains("name", ignoreCase = true))

        val dataRows = if (hasHeader) rawRows.drop(1) else rawRows
        val seenInFile = mutableSetOf<String>()
        val result = mutableListOf<ImportedStudentRow>()

        dataRows.forEachIndexed { index, cols ->
            val displayRowNum = if (hasHeader) index + 2 else index + 1
            val roll = cols.getOrNull(0)?.trim().orEmpty()
            val name = cols.getOrNull(1)?.trim().orEmpty()
            val normRoll = normalizeRollNumber(roll)

            val error = when {
                roll.isEmpty() -> "Missing roll number"
                name.isEmpty() -> "Missing student name"
                normRoll in existingRolls -> "Already exists in list"
                !seenInFile.add(normRoll) -> "Duplicate roll number in file"
                else -> null
            }
            result.add(
                ImportedStudentRow(
                    rowNumber = displayRowNum,
                    rollNumber = roll,
                    name = name,
                    error = error
                )
            )
        }

        val validCount = result.count { it.isValid }
        return ImportPreviewResult(
            rows = result,
            validCount = validCount,
            errorCount = result.size - validCount
        )
    }

    fun writeSampleCsvTemplate(outputStream: OutputStream) {
        outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write("Roll Number,Student Name\n")
            writer.write("CS-01,Ayesha Khan\n")
            writer.write("CS-02,Bilal Ahmed\n")
            writer.write("CS-10,Zara Malik\n")
        }
    }

    fun writeSampleXlsxTemplate(outputStream: OutputStream) {
        Workbook(outputStream, "SmartAttendance", "1.0").use { wb ->
            val ws = wb.newWorksheet("Students")
            ws.value(0, 0, "Roll Number")
            ws.value(0, 1, "Student Name")
            ws.value(1, 0, "CS-01")
            ws.value(1, 1, "Ayesha Khan")
            ws.value(2, 0, "CS-02")
            ws.value(2, 1, "Bilal Ahmed")
            ws.value(3, 0, "CS-10")
            ws.value(3, 1, "Zara Malik")
            ws.width(0, 18.0)
            ws.width(1, 28.0)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
