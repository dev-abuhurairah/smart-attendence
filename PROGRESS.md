# Smart Attendance — Build Progress

## Phase 1–6 (Completed)
- Phase 1: Project setup, Gradle catalog, Material 3 white/blue theme, 11 Room entities (`exportSchema = true`), `HomeScreen`, and GitHub Actions CI.
- Phase 2: 6-step Setup Wizard with draft persistence, natural roll sorting, Excel/CSV import + sample templates, and `ManageCatalogScreen`.
- Phase 3: Programs → Semesters → Subjects → Date/Lecture → `TakeAttendanceScreen` with drafts, Mark All Present, Undo, jump-to-unmarked validation, and duplicate session editing.
- Phase 4: `AttendanceSheetsScreen` (date-grouped, multi-criteria filters) and `SheetDetailScreen`.
- Phase 5: Excel, PDF, and PNG export (Mode A & Mode B) via SAF + `FileProvider` sharing.
- Phase 6: `ReportsScreen` (all 6 report types, low-attendance highlighting, Leave rules, Export History).

## Phase 7: Backup & Restore, App Lock, Copy Semester & Settings (Completed)
### What Was Done
- Implemented `PinHasher` (`security/PinHasher.kt`) using `PBKDF2WithHmacSHA256` (120,000 iterations, random 16-byte salt) and cooldown tracking in `SettingsDataStore` (30s cooldown after 5 wrong tries).
- Built `AppLockScreen` with PIN entry, cooldown timer, and `BiometricPrompt` fingerprint unlock (`MainActivity` updated to `FragmentActivity`).
- Built `BackupManager` (`backup/BackupManager.kt`) creating versioned `.zip` backups (`manifest.json`, `database.json`, `settings.json` excluding PIN, and `logo.png`), validating version & table integrity before restore, creating an automatic safety backup, and rolling back atomically on failure.
- Added non-intrusive 14-day backup reminder banner to `HomeScreen` / `HomeViewModel`.
- Built `CopySemesterRepository`, `CopySemesterScreen`, and `CopySemesterViewModel` to copy selected students into a new semester/session with new subjects & teachers.
- Built `SettingsScreen` & `SettingsViewModel` (institute name, logo, Light/Dark/System theme, low-attendance threshold, Leave formula rule, App Lock PIN + Biometric with offline no-recovery warning, Backup/Restore, and Copy Semester).
- Wired `Routes.SETTINGS` and `Routes.COPY_SEMESTER` in `NavGraph.kt` and added `PinHasherTest`.

### Key Decisions
- Stored all backup tables as versioned JSON inside `smart_attendance_backup.zip` rather than raw SQLite file copying so schema validation and atomic rollback inside Room transactions remain portable across Android versions.

### Known Limits / Untested Locally
- Local JDK and Android SDK are not installed on this machine, so Gradle compilation, unit tests, and lint checks were not executed locally yet (will run in GitHub Actions on push).
