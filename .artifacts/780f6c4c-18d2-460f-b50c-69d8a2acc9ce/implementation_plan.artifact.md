# Implementation Plan - UI and Backup Functionality Fixes

Fix Bottom Navigation styling and implement full Storage Access Framework (SAF) for Backup/Restore operations.

## User Review Required

> [!IMPORTANT]
> The Bottom Navigation will be updated to follow Material Design 3 guidelines more strictly. The central "Quick Add" button's label size and spacing will be adjusted to ensure alignment with other items.

> [!WARNING]
> Export/Import will now exclusively use the Android File Picker (SAF). The legacy clipboard-based backup logic will be removed.

## Proposed Changes

### UI Components & Navigation

#### [MODIFY] [MainActivity.kt](file:///D:/Vamshi_Apps/Forgot/Forgot_App/app/src/main/java/com/example/MainActivity.kt)
- Adjust `NavigationBar` height to `84.dp` for more breathing room.
- Increase label `fontSize` to `13.sp` and adjust `icon` sizes to `24.dp` (standard M3) for better balance.
- Refine the central "Quick Add" button styling to ensure it doesn't disrupt the baseline alignment of labels.

#### [MODIFY] [SettingsScreen.kt](file:///D:/Vamshi_Apps/Forgot/Forgot_App/app/src/main/java/com/example/ui/profile/SettingsScreen.kt)
- Update `SettingsScreen` signature to include `onExportBackup: () -> Unit` and `onImportBackup: () -> Unit`.
- Update Export/Import buttons to call these callbacks.
- Use `LanguageUtils` for button labels to ensure consistency and easy localization.
- Update `Privacy Policy` text to remove mention of clipboard backups.

### Data & Logic

#### [MODIFY] [MemoryViewModel.kt](file:///D:/Vamshi_Apps/Forgot/Forgot_App/app/src/main/java/com/example/ui/viewmodel/MemoryViewModel.kt)
- Implement `performExportBackup(uri: Uri)` to write JSON data directly to the selected file.
- Implement `performImportBackup(uri: Uri)` to read JSON data from the selected file and restore the database.
- Remove legacy `exportBackupToFile` and `importBackupFromFile` internal logic that was using fallback methods.

#### [MODIFY] [LanguageUtils.kt](file:///D:/Vamshi_Apps/Forgot/Forgot_App/app/src/main/java/com/example/ui/utils/LanguageUtils.kt)
- Remove "(JSON)" suffix from `export_backup` and `import_backup` strings in both English and Telugu.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.

### Manual Verification
- **Bottom Navigation**: Verify alignment and label readability on device.
- **Export**: Tap Export, choose location in file picker, verify success toast and file creation.
- **Import**: Tap Import, pick the exported file, verify data restoration and success toast.
