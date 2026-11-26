package com.smartattendance.app.core

import com.smartattendance.app.domain.model.LeaveRule
import java.util.Locale

object AttendanceMath {

    private val formulaTriggerChars = setOf('=', '+', '-', '@', '\t', '\r', '\n')

    /**
     * Escapes spreadsheet formula-injection characters by prefixing an apostrophe.
     */
    fun sanitizeExcelCell(value: String): String {
        if (value.isEmpty()) return value
        return if (value[0] in formulaTriggerChars) "'$value" else value
    }

    /**
     * Removes unsafe filesystem characters from export file names.
     */
    fun sanitizeFileName(raw: String): String =
        raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifEmpty { "export" }

    fun calculatePercentageValue(
        present: Int,
        absent: Int,
        leave: Int,
        rule: LeaveRule
    ): Double? {
        val denominator = when (rule) {
            LeaveRule.INCLUDE_LEAVE -> present + absent + leave
            LeaveRule.EXCLUDE_LEAVE -> present + absent
        }
        if (denominator <= 0) return null
        return (present.toDouble() / denominator.toDouble()) * 100.0
    }

    fun formatPercentage(
        present: Int,
        absent: Int,
        leave: Int,
        rule: LeaveRule
    ): String {
        val pct = calculatePercentageValue(present, absent, leave, rule) ?: return "N/A"
        return String.format(Locale.US, "%.1f%%", pct)
    }
}
