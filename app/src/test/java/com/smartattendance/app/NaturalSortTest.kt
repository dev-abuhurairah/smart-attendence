package com.smartattendance.app

import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.core.normalizeProgramName
import com.smartattendance.app.core.normalizeRollNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalSortTest {

    @Test
    fun naturalSort_ordersNumericAndPrefixedRollsCorrectly() {
        val input = listOf("CS-10", "CS-02", "CS-1", "10", "2", "001")
        val sorted = input.sortedWith(NaturalRollComparator)
        assertEquals(listOf("001", "2", "10", "CS-1", "CS-02", "CS-10"), sorted)
    }

    @Test
    fun normalizeProgramName_collapsesSpacesAndLowercases() {
        assertEquals(
            "bs computer science",
            normalizeProgramName("  BS   Computer   Science  ")
        )
    }

    @Test
    fun normalizeRollNumber_trimsAndLowercases() {
        assertEquals("cs-02", normalizeRollNumber("  CS-02 "))
    }
}
