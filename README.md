# 🧠 Forgot App - Personal Memory & Reminder Assistant

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Forgot** (`com.forgot.app`) is an offline-first personal memory retention, reminder, and document tracking Android application built with **Kotlin** and **Jetpack Compose**. It helps users securely store notes, credentials, vehicle parking spots, prescription medicine schedules, financial transactions, document expiry dates, and gift ideas.

---

## ✨ Features & Highlights

### 📁 1. 9 Specialized Memory Categories
- **General Notes**: Rich text, audio note recordings, photo attachments, reminder dates.
- **Credentials & Logins**: Usernames, passwords, account names, and security notes.
- **Vehicle & Parking**: Parking level/spot numbers, vehicle details, and parking photos.
- **Health & Medicine**: Prescription photos, dosage info, and Morning/Afternoon/Night time slot pickers.
- **Finance & Money**: Transaction records, amounts, income/expense tags, and receipt photos.
- **Real Estate & Places**: Addresses, location notes, and property photos.
- **Documents & Cards**: ID numbers, issue/expiry dates, and document photos with automated expiry alerts.
- **Contacts & People**: Important contacts, phone numbers, addresses, and personal notes.
- **Wishlist & Gifts**: Gift ideas, recipient names, prices, and item web links.

---

### ⏰ 2. Smart Reminders & 7-Column Calendar Grid
- **Dual View Modes**: Switch seamlessly between **List View** and a **7-Column Calendar View**.
- **Interactive Calendar Grid**: Monthly dates grid with today highlights, selected day tinting, and event dot markers.
- **Medicine Time Slots**: Dedicated Morning (8:00 AM), Afternoon (1:00 PM), and Night (9:00 PM) dose pickers.
- **Reboot Persistence**: `ReminderReceiver` and `BootReceiver` update timestamps in Room DB to reschedule alarms across phone reboots.

---

### 🔔 3. Compact Updates & Top Notification Banner
- **Compact Settings Card**: Clean single-row update item matching the Trash Bin card style.
- **Red Notification Badge**: Small red dot on the update icon whenever a newer version is available on GitHub.
- **Mini Top Update Panel**: Non-intrusive banner overlay on primary screens with **[Update]** action and **[X]** session close button.
- **Semantic Versioning**: Numerical version comparison (`1.10.0` > `1.9.0`) with deduplicated network calls and offline fail-safe handling.

---

### 🎨 4. Theme Engine & Responsive Scaling
- **6 Theme Presets**: **Dark Vibe**, **Light Vibe**, **AMOLED Black**, **Cyber Blue**, **Mint Green**, and **Neon Velvet**.
- **Dynamic Metrics**: Continuous scaling (`LocalResponsiveMetrics`) for 100% clean layouts on all mobile screen heights (10cm to 15cm+).

---

### 🔒 5. Offline-First & SAF Backup/Restore
- **Privacy First**: 100% offline data storage—no analytics or unexpected cloud uploads.
- **Full Media Preservation**: JSON backup export/import encodes photo attachments into Base64 strings to prevent data loss.

---

## 🛠️ Architecture & Tech Stack

```text
com.forgot.app
├── data/
│   ├── database/       # Room DB (AppDatabase, MemoryDao, 9 Entity tables)
│   ├── model/          # Memory & category detail data models
│   └── repository/     # ReminderScheduler, ReminderReceiver, BootReceiver, UpdateRepository
├── ui/
│   ├── components/     # Reusable Compose cards, buttons, dialogs
│   ├── detail/         # MemoryDetailScreen (full media viewer & gallery downloader)
│   ├── home/           # HomeScreen (expiry alerts, quick add carousel, statistics)
│   ├── profile/        # SettingsScreen (themes, archive, trash, updates)
│   ├── remember/       # RememberScreen (form field hierarchy, category forms)
│   ├── reminders/      # RemindersScreen (list & 7-column calendar grid)
│   ├── search/         # SearchScreen (reactive search, category pills filter)
│   ├── update/         # AppVersionScreen (OTA updater, rich markdown renderer)
│   └── utils/          # ReleaseNotesFormatter, LanguageUtils, AdaptiveUI
└── MainActivity.kt     # Single Activity entry point & Navigation Compose NavHost
```

- **Architecture**: Single Activity + Jetpack Compose + Navigation Compose
- **State Management**: `AndroidViewModel` (`MemoryViewModel`, `UpdateViewModel`) + `StateFlow`
- **Database**: Room Database v2 (1:1 relation tables with `CASCADE` delete)
- **Networking**: Retrofit 2 + Moshi + OkHttp3
- **Alarms & Background**: `AlarmManager` (`RTC_WAKEUP`) + BroadcastReceivers
- **Media**: `MediaRecorder`, `MediaPlayer`, `AsyncImage` (Coil), `FileProvider`

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio**: Ladybug / Koala or newer
- **JDK**: 17 or higher
- **Target SDK**: 36 (Min SDK: 24)

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Run Unit Tests
./gradlew test
```

---

## 📄 Documentation

For full detailed project documentation, screen-by-screen feature breakdowns, and resolved bug fix logs:
- 🔍 **[Project Documentation & Analysis Report](.system_generated/../project_analysis_report.md)**
- 🎨 **[Walkthrough & Verification Log](.system_generated/../walkthrough.md)**

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
