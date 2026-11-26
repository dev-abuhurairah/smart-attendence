package com.smartattendance.app

import com.smartattendance.app.domain.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase1SanityTest {
    @Test
    fun attendanceStatus_hasExpectedCodes() {
        assertEquals("P", AttendanceStatus.PRESENT.code)
        assertEquals("A", AttendanceStatus.ABSENT.code)
        assertEquals("L", AttendanceStatus.LEAVE.code)
    }
}
