# Smart Attendance — Build Progress

## All 8 Phases Completed
1. **Phase 1**: Project setup, Gradle catalog, Material 3 white/blue theme, 11 Room entities (`exportSchema = true`), `HomeScreen`, and GitHub Actions CI.
2. **Phase 2**: 6-step Setup Wizard with draft persistence, natural roll sorting, Excel/CSV import + sample templates, and `ManageCatalogScreen`.
3. **Phase 3**: Programs → Semesters → Subjects → Date/Lecture → `TakeAttendanceScreen` with drafts, Mark All Present, Undo, jump-to-unmarked validation, and duplicate session editing.
4. **Phase 4**: `AttendanceSheetsScreen` (date-grouped, multi-criteria filters) and `SheetDetailScreen`.
5. **Phase 5**: Excel, PDF, and PNG export (Mode A & Mode B) via SAF + `FileProvider` sharing.
6. **Phase 6**: `ReportsScreen` (all 6 report types, low-attendance highlighting, Leave rules, Export History).
7. **Phase 7**: Backup & Restore (`.zip` with safety backup & rollback), 14-day backup reminder, App Lock (PBKDF2 PIN + Biometric + cooldown), Copy to Next Semester, and Settings.
8. **Phase 8**: Added `PerformanceScaleTest` (500 students × 100 lectures), `SmartAttendanceDatabaseTest` (Room constraints & archiving), `HomeScreenUiTest`, and comprehensive `README.md` with release keystore & GitHub Secrets guide.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this Windows machine, so Gradle compilation, unit tests, instrumented tests, Android Lint, and R8 release builds were not executed locally yet and must be verified via GitHub Actions on push.
