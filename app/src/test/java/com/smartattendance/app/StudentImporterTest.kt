package com.smartattendance.app

import com.smartattendance.app.export.StudentImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class StudentImporterTest {

    @Test
    fun parseCsv_detectsErrorsAndDuplicates() {
        val csv = """
            Roll Number,Student Name
            CS-01,Ayesha Khan
            CS-02,Bilal Ahmed
            cs-02,Duplicate In File
            ,Missing Roll
            CS-03,
            CS-00,Existing Student
        """.trimIndent()

        val result = StudentImporter.parseCsv(
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            existingRolls = setOf("cs-00")
        )

        assertEquals(2, result.validCount)
        assertEquals(4, result.errorCount)
        assertTrue(result.rows[0].isValid)
        assertTrue(result.rows[1].isValid)
        assertFalse(result.rows[2].isValid)
        assertFalse(result.rows[3].isValid)
        assertFalse(result.rows[4].isValid)
        assertFalse(result.rows[5].isValid)
    }

    @Test
    fun sampleXlsxTemplate_roundTripsThroughParser() {
        val out = ByteArrayOutputStream()
        StudentImporter.writeSampleXlsxTemplate(out)

        val result = StudentImporter.parseXlsx(
            inputStream = ByteArrayInputStream(out.toByteArray()),
            existingRolls = emptySet()
        )

        assertEquals(3, result.validCount)
        assertEquals(0, result.errorCount)
        assertEquals("CS-01", result.rows[0].rollNumber)
        assertEquals("Ayesha Khan", result.rows[0].name)
    }
}
