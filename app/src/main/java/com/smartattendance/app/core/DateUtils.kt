package com.smartattendance.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

    fun todayIso(): String = LocalDate.now().toString()

    fun isFutureIsoDate(isoDate: String): Boolean {
        val parsed = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return true
        return parsed.isAfter(LocalDate.now())
    }

    fun formatDisplayDate(isoDate: String): String {
        val parsed = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
        return parsed.format(displayDateFormatter)
    }

    fun formatDateTime(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(displayDateTimeFormatter)
    }
}
