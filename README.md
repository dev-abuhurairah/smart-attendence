# Smart Attendance

An offline-first native Android application (`com.smartattendance.app`) built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, and Hilt for managing classroom attendance on a single device without internet access, ads, or tracking.

## Features
- **Offline-Only Architecture**: No `INTERNET` permission; all data lives locally in Room SQLite and Preferences DataStore.
- **6-Step Setup Wizard**: Program, Semester, Section, Session, natural-sorted Roll Numbers (`2` before `10`, `CS-02` before `CS-10`), multi-line bulk paste, Excel (`.xlsx`) & CSV import with preview & sample templates, Teachers, Subjects, and single-transaction save with persistent drafts.
- **Catalog Management**: Add, edit, archive/restore, and safely remove programs, semesters, students, teachers, and subjects without breaking historical attendance.
- **Take Attendance**: Programs → Semesters → Subjects → Date (`YYYY-MM-DD`, future dates blocked) & Lecture # → Marking screen with live counts (`P`, `A`, `L`, `Left`), search, **Mark All Present**, **Undo**, auto-saved drafts, and jump-to-unmarked validation.
- **Attendance Sheets**: Date-grouped list (newest first) with filters (program, semester, section, subject, teacher, date range), last-edited timestamp, editing, and deletion.
- **Excel, PDF & PNG Exports**:
  - **Mode A (Single Lecture)** & **Mode B (Full Register)** rebuilt fresh from Room.
  - Excel formula-injection escaping (`=`, `+`, `-`, `@`, tab, newline), frozen headers & roll/name columns.
  - Multi-page Portrait/Landscape A4 PDF with repeated headers and `Page X of Y`.
  - Multi-page PNG export with immediate bitmap recycling and stale-page cleanup in SAF folders.
  - SAF **Update existing file** / **Save as new copy** and `FileProvider` sharing.
- **Reports & Export History**: Daily, Monthly, Custom Range, Subject-wise, Student-wise, and Full Semester reports with configurable low-attendance threshold (default `75%`), Leave formula toggle (`P/(P+A+L)` vs `P/(P+A)`), `N/A` on zero denominator, and Export History management.
- **Backup, Security & Semester Copy**:
  - Versioned `.zip` backup & restore with integrity validation, automatic local safety backup, and atomic rollback.
  - 14-day in-app backup reminder.
  - Optional App Lock (4–6 digit PIN with salted `PBKDF2WithHmacSHA256` + 30s cooldown after 5 wrong tries + `BiometricPrompt` fingerprint unlock).
  - Copy selected students to next semester while keeping old records untouched.

---

## Building Locally
1. Install **JDK 17** and **Android SDK** (Platform `android-37`, Build Tools `37.0.0`).
2. Run Gradle (`9.6.0`):
   ```bash
   gradle testDebugUnitTest
   gradle lintDebug
   gradle assembleDebug
   ```

---

## GitHub Actions & Release Signing Setup
`.github/workflows/android.yml` runs unit tests, Android Lint, and builds a debug APK on every push and pull request.
A tagged release (`v*`) or manual `workflow_dispatch` (with `build_release = true`) builds a **signed, R8-minified release APK**.

### Step 1: Generate a Release Keystore (Once)
```bash
keytool -genkeypair -v -keystore smart-attendance-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias smartattendance
```
**Back up `smart-attendance-release.jks` and your passwords in a secure password manager/vault.** Never commit the `.jks` file to Git.

### Step 2: Encode the Keystore to Base64
- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("smart-attendance-release.jks")) | Set-Clipboard
  ```
- **macOS / Linux**:
  ```bash
  base64 -w 0 smart-attendance-release.jks
  ```

### Step 3: Add GitHub Repository Secrets
In your GitHub repository: **Settings → Secrets and variables → Actions → New repository secret**:
- `KEYSTORE_BASE64`: Base64 string from Step 2
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: `smartattendance` (or your chosen alias)
- `KEY_PASSWORD`: Key password

### Step 4: Trigger a Signed Release Build
Increment `versionCode` and `versionName` in `app/build.gradle.kts`, then tag and push:
```bash
git tag v1.0.0
git push origin v1.0.0
```
*(If any secret is missing, the release job intentionally fails rather than falling back to a debug key.)*

---

## Backup & App Lock Notes
- Backup `.zip` files contain private student names, roll numbers, and attendance records—store them securely.
- Because the app is 100% offline, a forgotten PIN **cannot** be recovered. Unlock with fingerprint (if enabled) or clear app data and restore from a `.zip` backup (PINs are excluded from backups).

## Known Limits
- Local JDK/Android SDK were not present in the authoring environment; CI verification on GitHub Actions should be checked after pushing.
