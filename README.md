# AMOLED Notes

A minimalist, premium notepad application for Android with unlimited text capacity, rich text editing, and true-black AMOLED styling (#000000).

## Highlights & Features

- **Unlimited Text Capacity**: No artificial limits on document size. Room SQLite stores full text without truncating. Notes with 100,000+ words are rendered efficiently through lazy segmented chunking to maintain steady 60fps scrolling performance.
- **Rich Text Markdown Engine**: Live formatting toolbar for Bold (`**`), Italic (`*`), Underline (`<u>`), Strikethrough (`~~`), H1/H2/H3 (`#`), Bullet lists (`-`), Numbered lists (`1.`), Interactive Checklists (`[ ]` / `[x]`), Quotes (`>`), and Horizontal Dividers (`---`).
- **Interactive Checkboxes**: Tap checklist checkboxes directly in Read Mode to toggle state effortlessly.
- **Debounced Auto-Save & Version Snapshots**: Notes automatically persist to Room DB 2 seconds after typing ceases and immediately on exit. Incremental snapshot revisions are recorded every 5 minutes and on note open, with one-tap restoration.
- **Organization & Instant Search**: Folders, multi-tag categorization, and real-time indexed search highlighting matched tokens in titles and snippets.
- **Note Pinning & Archive**: Pin up to 3 priority notes to the top of your list. Archive completed thoughts without clutter.
- **Biometric & 4-Digit Passcode Security**: Lock sensitive notes using fingerprint / face biometrics or a custom 4-digit PIN.
- **Multi-Format Export & Offline Backup**:
  - Export notes to Markdown (`.md`), Plain Text (`.txt`), or Printable PDF (`.pdf`) using native Android `PdfDocument`.
  - Share via Android System Share Sheet.
  - Complete JSON backup and restore with full note, version, folder, and tag schema preservation.
- **True-Black AMOLED Aesthetic**:
  - Background: Pure black `#000000` (100% OLED pixel shutoff).
  - Surfaces: Subtle white-alpha tiers (8% and 16% luminance) with 1px hairline wireframes.
  - Typography: Clean sans-serif hierarchy with Small, Medium, and Large scaling.
  - Theme override: AMOLED Black, Light, or System Default.

## Architecture

- **Architecture Pattern**: MVVM (Model-View-ViewModel) + Repository Pattern
- **UI Framework**: Jetpack Compose (Material 3) with Single-Activity Navigation
- **Local Persistence**: Room SQLite with KSP, Coroutines, and StateFlow
- **Preferences**: Jetpack DataStore Preferences
- **Security**: AndroidX Biometric (`BiometricPrompt`) with SHA-256 hashed PIN fallback
- **Export Engine**: Android native `android.graphics.pdf.PdfDocument` and Android FileProvider

## Build & Run Instructions

To compile and assemble the debug APK:

```bash
./gradlew assembleDebug
```

To run unit tests:

```bash
./gradlew testDebugUnitTest
```
