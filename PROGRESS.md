# Smart Attendance — Build Progress

## Phase 1–5 (Completed)
- Phase 1: Project setup, Gradle catalog, Material 3 white/blue theme, 11 Room entities (`exportSchema = true`), `HomeScreen`, and GitHub Actions CI.
- Phase 2: 6-step Setup Wizard with draft persistence, natural roll sorting, Excel/CSV import + sample templates, and `ManageCatalogScreen`.
- Phase 3: Programs → Semesters → Subjects → Date/Lecture → `TakeAttendanceScreen` with drafts, Mark All Present, Undo, jump-to-unmarked validation, and duplicate session editing.
- Phase 4: `AttendanceSheetsScreen` (date-grouped, multi-criteria filters) and `SheetDetailScreen`.
- Phase 5: Excel, PDF, and PNG export (Mode A & Mode B) via SAF + `FileProvider` sharing.

## Phase 6: Reports & Export History (Completed)
### What Was Done
- Added `observeAllReportRecords()` SQL join query to `AttendanceDao`.
- Added `exportReport` support to `ExcelExporter`, `PdfExporter`, and `PngExporter`.
- Built `ReportsRepository` supporting all 6 report types (`Daily`, `Monthly`, `Custom Range`, `Subject-wise`, `Student-wise`, `Full Semester`), low-attendance threshold highlighting, both Leave formula rules (`P/(P+A+L)` and `P/(P+A)`), `N/A` on 0 denominator, and Export History URI accessibility checks.
- Built `ReportsScreen` & `ReportsViewModel` with **Analytics** tab (all 6 report types, active rule banner, date pickers, program filters, export to Excel/PDF/PNG) and **Export History** tab (Open, Share, Recreate missing files, Remove from list without deleting attendance data).
- Wired `Routes.REPORTS` in `NavGraph.kt` and added `ReportsCalculationTest`.

### Key Decisions
- Computed all 6 report aggregations from `observeAllReportRecords()` in `ReportsRepository` so reports always reflect saved Room data immediately, even when no files have been exported.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this machine, so Gradle compilation, unit tests, and lint checks were not executed locally yet (will run in GitHub Actions on push).
