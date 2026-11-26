# Smart Attendance — Build Progress

## Phase 1: Project Setup, Theme, Navigation, Room DB & CI (Completed)
- Created Android project at `d:\SmartAttendance` (`com.smartattendance.app`, `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`), white & blue Material 3 theme, all 11 Room entities, `SettingsDataStore`, `HomeScreen`, and `.github/workflows/android.yml`.

## Phase 2: Setup Wizard, Excel/CSV Import & Catalog Management (Completed)
- Implemented `NaturalRollComparator`, `StudentImporter` (`.xlsx` + `.csv` + sample templates), `SetupRepository` (single-transaction save, draft persistence, history-aware archive/delete), 6-step `SetupWizardScreen`, `ManageCatalogScreen`, and unit tests.

## Phase 3: Programs Flow & Attendance Marking (Completed)
### What Was Done
- Added `core/DateUtils.kt` with `YYYY-MM-DD` calendar-date handling and future-date blocking.
- Expanded `Daos.kt` and built `AttendanceRepository` (`observeActivePrograms`, `observeSemesters` with active/archived toggle, `observeSubjectsWithTeacher`, `loadMarkingContext`, `saveDraft`, and `saveAttendanceSession`).
- Built `ProgramsScreen`, `SemestersScreen` (active/archived filter), and `SubjectsScreen` (with `SessionSetupDialog` blocking future dates, defaulting lecture to `1`, and detecting existing sessions for edit).
- Built `TakeAttendanceScreen` & `TakeAttendanceViewModel`: header info, live counts (`Total`, `P`, `A`, `L`, `Left`), search by name/roll, **Mark All Present**, **Undo** stack, auto-saved drafts, unmarked-student highlight + jump-to-first-unmarked on save, summary confirmation dialog, and duplicate-session editing.
- Wired all Phase 3 routes in `NavGraph.kt` and added `DateUtilsTest`.

### Key Decisions
- Used `LocalDate` (`YYYY-MM-DD`) strings for session dates so timezone changes never shift an attendance date.
- Kept `attendance_drafts` auto-saving only for new unsaved sessions; editing an existing saved session updates in-place on confirmation.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this machine, so Gradle build, unit tests, and lint checks were not executed locally yet (will run in GitHub Actions on push).
