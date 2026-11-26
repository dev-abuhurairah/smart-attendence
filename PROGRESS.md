# Smart Attendance — Build Progress

## Phase 1: Project Setup, Theme, Navigation, Room DB & CI (Completed)
- Created Android project at `d:\SmartAttendance` (`com.smartattendance.app`, `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`).
- Configured `libs.versions.toml`, Material 3 white & blue theme (`#0B4EA2`, `#073572`, `#EAF2FC`, `#FFFFFF`) + dark-blue dark mode, all 11 Room entities (`exportSchema = true`), `SettingsDataStore`, `HomeScreen`, `NavGraph`, and `.github/workflows/android.yml`.

## Phase 2: Setup Wizard, Excel/CSV Import & Catalog Management (Completed)
### What Was Done
- Implemented `NaturalRollComparator` (`2` before `10`, `CS-02` before `CS-10`) and case-insensitive/whitespace-collapsed normalization helpers in `core/NaturalSort.kt`.
- Implemented `StudentImporter` (`export/StudentImporter.kt`) supporting `.xlsx` (FastExcel + Woodstox StAX) and `.csv` parsing, header auto-detection, row-level error & duplicate highlighting, and downloadable `.xlsx` / `.csv` sample templates via SAF.
- Expanded `Daos.kt` and built `SetupRepository` with single-transaction `saveSetupWizard()`, program name deduplication, duplicate semester+section+session prevention, roll number uniqueness, and persistent JSON draft saving (`setup_drafts`).
- Built the 6-step `SetupWizardScreen` & `SetupWizardViewModel` (Program, Students with rapid add & bulk multi-line paste, Import preview, Teachers with quick-pick saved teachers, Subjects, Review & Save).
- Built `ManageCatalogScreen` & `ManageCatalogViewModel` for editing, archiving/restoring, and deleting programs, semesters, students, teachers, and subjects with history-aware archiving and a 2-step permanent delete warning.
- Added unit tests `NaturalSortTest` and `StudentImporterTest`.

### Key Decisions
- Used Android's built-in `org.json` for `setup_drafts` serialization to avoid adding an extra JSON serialization library/plugin.
- Configured Woodstox StAX properties inside `StudentImporter` so `fastexcel-reader` runs cleanly on Android without desktop `javax.xml.stream` runtime gaps.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this Windows machine, so Gradle compilation, unit tests (`NaturalSortTest`, `StudentImporterTest`), and lint checks were not executed locally yet (will run in GitHub Actions on push).
