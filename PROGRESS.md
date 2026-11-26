# Smart Attendance — Build Progress

## Phase 1: Project Setup, Theme, Navigation, Room DB & CI (Completed)
### What Was Done
- Created Android project at `d:\SmartAttendance` with `applicationId = "com.smartattendance.app"`, `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`.
- Configured Gradle Kotlin DSL and `gradle/libs.versions.toml` with stable versions (AGP `9.5.0`, Gradle `9.6.0`, Kotlin `2.2.10`, KSP `2.3.14`, Compose BOM `2026.11.01`, Room `2.8.5`, Hilt `2.60.1`, Navigation Compose `2.10.2`, DataStore `1.3.0`, Biometric `1.1.0`, FastExcel `0.20.2` + Woodstox `6.5.1`).
- Built banking-inspired white & blue Material 3 theme (`#0B4EA2`, `#073572`, `#EAF2FC`, `#FFFFFF`) with dark-blue dark mode and status colors (`P` green, `A` red, `L` orange).
- Designed and wired all 11 Room entities (`programs`, `semesters`, `students`, `teachers`, `subjects`, `attendance_sessions`, `attendance_records`, `attendance_drafts`, `attendance_draft_records`, `setup_drafts`, `export_history`) with foreign keys, indexes, unique constraints, and `exportSchema = true` (`app/schemas/`).
- Implemented `SettingsDataStore`, `HomeRepository`, `HomeViewModel`, `HomeScreen` (adaptive for phones/tablets/foldables), and `SmartAttendanceNavGraph` with edge-to-edge and predictive back enabled.
- Added `.github/workflows/android.yml` for automated unit tests, Android Lint, debug APK builds, and tag/manual signed release APK builds.

### Key Decisions
- Used isolated `attendance_drafts` and `attendance_draft_records` tables so in-progress attendance never leaks into reports or violates the unique `(subject_id, date, lecture_number)` constraint on `attendance_sessions`.
- Stored `teacher_name_snapshot` on `attendance_sessions` so future teacher reassignments never rewrite historical attendance records.
- Used `gradle/actions/setup-gradle@v4` with `gradle-version: "9.6.0"` in GitHub Actions so CI runs reliably even without a pre-generated `gradle-wrapper.jar`.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this Windows machine, so `testDebugUnitTest`, `lintDebug`, and `assembleDebug` were not executed locally yet and must be verified via GitHub Actions.
