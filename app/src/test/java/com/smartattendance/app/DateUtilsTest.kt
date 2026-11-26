package com.smartattendance.app

import com.smartattendance.app.core.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun isFutureIsoDate_blocksFutureAndAllowsTodayAndPast() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        assertFalse(DateUtils.isFutureIsoDate(today.toString()))
        assertFalse(DateUtils.isFutureIsoDate(yesterday.toString()))
        assertTrue(DateUtils.isFutureIsoDate(tomorrow.toString()))
    }

    @Test
    fun formatDisplayDate_formatsIsoDateCleanly() {
        assertEquals("26 Nov 2026", DateUtils.formatDisplayDate("2026-11-26"))
    }
}
