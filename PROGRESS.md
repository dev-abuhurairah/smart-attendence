# Smart Attendance — Build Progress

## Phase 1–4 (Completed)
- Phase 1: Project setup, Gradle catalog, Material 3 white/blue theme, 11 Room entities (`exportSchema = true`), `HomeScreen`, and GitHub Actions CI.
- Phase 2: 6-step Setup Wizard with draft persistence, natural roll sorting, Excel/CSV import + sample templates, and `ManageCatalogScreen`.
- Phase 3: Programs → Semesters → Subjects → Date/Lecture setup → `TakeAttendanceScreen` with live counts, search, Mark All Present, Undo, auto-drafts, jump-to-unmarked validation, and duplicate session editing.
- Phase 4: `AttendanceSheetsScreen` (date-grouped, multi-criteria filters) and `SheetDetailScreen` (view, edit, delete).

## Phase 5: Excel, PDF & PNG Export (Mode A & Mode B) (Completed)
### What Was Done
- Added `core/AttendanceMath.kt`: formula-injection cell escaping (`=`, `+`, `-`, `@`, tab, newline), filename sanitization, and attendance percentage calculation (both Leave rules + `N/A` for 0 denominator).
- Built `ExcelExporter` (`export/ExcelExporter.kt`): FastExcel `.xlsx` generation with frozen header & roll/name columns, clean column widths, text roll numbers, and formula-injection escaping.
- Built `PdfExporter` (`export/PdfExporter.kt`): Portrait A4 for Single Lecture and Landscape A4 for Full Register, splitting wide date columns and long student lists across pages with repeated headers, roll numbers, names, and `Page X of Y`.
- Built `PngExporter` (`export/PngExporter.kt`): Generates numbered `.png` pages one at a time and recycles each `Bitmap` immediately in `finally`.
- Built `ExportRepository` (`data/repository/ExportRepository.kt`): Rebuilds Mode A & Mode B fresh from Room (showing `-` for earlier lectures before a student joined), verifies output in app temp storage first, writes via SAF (with persistable permissions, lost-permission recovery, and stale `page_*.png` cleanup in PNG folders), records `export_history`, and shares via `FileProvider`.
- Built `ExportOptionsDialog` & `ExportViewModel` and wired them into `TakeAttendanceScreen` and `SheetDetailScreen`.
- Added unit tests `ExportMathAndExcelTest`.

### Key Decisions
- Used `DocumentsContract` directly on tree URIs for PNG multi-page folders so stale `page_*.png` files from previous exports are cleaned up atomically without extra dependencies.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this machine, so Gradle compilation, unit tests, lint, and R8 release build verification of FastExcel/Woodstox were not executed locally yet (will run in GitHub Actions on push).
