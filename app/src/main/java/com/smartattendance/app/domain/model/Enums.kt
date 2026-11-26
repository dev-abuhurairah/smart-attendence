package com.smartattendance.app.domain.model

enum class AttendanceStatus(val code: String, val label: String) {
    PRESENT("P", "Present"),
    ABSENT("A", "Absent"),
    LEAVE("L", "Leave")
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class LeaveRule {
    INCLUDE_LEAVE,
    EXCLUDE_LEAVE
}
