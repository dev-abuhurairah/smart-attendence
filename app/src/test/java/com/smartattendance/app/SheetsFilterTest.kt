package com.smartattendance.app

import com.smartattendance.app.data.local.db.dao.SavedSheetSummaryTuple
import com.smartattendance.app.data.repository.SheetsFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetsFilterTest {

    private val sampleSheet = SavedSheetSummaryTuple(
        sessionId = 1L,
        subjectId = 10L,
        programId = 2L,
        programName = "BS Computer Science",
        semesterId = 3L,
        semesterNumber = 3,
        section = "A",
        sessionName = "2024-2028",
        subjectName = "Data Structures",
        subjectCode = "CS-201",
        teacherNameSnapshot = "Dr. Sara",
        date = "2026-11-26",
        lectureNumber = 1,
        updatedAt = 1000L,
        presentCount = 25,
        absentCount = 3,
        leaveCount = 2
    )

    @Test
    fun sheetsFilter_matchesAllCriteriaAndDateRange() {
        val matching = SheetsFilter(
            programName = "BS Computer Science",
            semesterNumber = 3,
            section = "A",
            subjectName = "Data Structures",
            teacherName = "Dr. Sara",
            startDate = "2026-11-01",
            endDate = "2026-11-30"
        )
        assertTrue(matching.matches(sampleSheet))
        assertTrue(matching.isActive)

        val outOfRange = matching.copy(endDate = "2026-11-25")
        assertFalse(outOfRange.matches(sampleSheet))

        val wrongTeacher = matching.copy(teacherName = "Prof. Ali")
        assertFalse(wrongTeacher.matches(sampleSheet))
    }
}
