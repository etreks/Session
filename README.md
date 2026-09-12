# ⏱️ Session

> **An indie Android time-tracking app designed for focused sessions.**  
> Built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**.  
> *Inspired by Google Clock's minimalist stopwatch and Google Calendar's color-coded time blocking.*

---

## 📲 Download & Install on Your Phone

You and your friends can install **Session** directly onto any Android device (Android 8.0+ / API 26+).

### Option 1: Scan QR Code with Your Phone

Scan this QR code with your phone camera to download the APK directly:

<p align="center">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=https://github.com/etreks/Session/releases/latest" alt="Scan QR code to download Session APK" width="240" height="240" />
</p>
<p align="center">
  <a href="https://github.com/etreks/Session/releases/latest"><b>👉 Direct Download Link (Latest Release APK)</b></a>
</p>

---

### Option 2: Step-by-Step Installation Guide

1. **Download the APK**:
   - Open the [Latest Release](https://github.com/etreks/Session/releases/latest) on your Android phone's browser (Chrome, Brave, etc.) and tap `Session-v1.0.apk` or `app-debug.apk` to download.
2. **Enable Unknown Apps**:
   - When the download completes, tap the file in your notification bar or **Downloads** app.
   - If Android shows **"For your security, your phone is not allowed to install unknown apps from this source"**:
     - Tap **Settings** in the popup.
     - Turn on **Allow from this source**.
     - Go back and tap **Install**.
3. **Open & Enjoy**:
   - Tap **Open** to launch Session!

---

## ✨ Features

- **⏱️ Minimalist Stopwatch**:
  - Clean, bold MD3 interface inspired by Google Clock.
  - Large hour:minute:second display with responsive controls.
  - Optional activity label for individual sessions (e.g., "Exam Paper 2024", "Leg Day", "Chapter 3").

- **🎨 Color-Coded Activity Buckets**:
  - Organize sessions under customizable buckets with Google Calendar-inspired color palettes:
    - 🟢 **Study** (`#4CAF50`)
    - 🔵 **Exercise** (`#2196F3`)
    - 🟡 **Reading** (`#FFC107`)
    - 🟠 **Work** (`#FF9800`)
  - Create your own custom buckets with any name and color.

- **🛡️ Smart 5-Minute Auto-Pause (Anti-Distraction)**:
  - Uses an Android Foreground Service so the timer survives screen lock and backgrounding.
  - **Screen-Off Trusted Mode**: When the phone screen is turned off, the session continues indefinitely (you're focused in real life!).
  - **Screen-On Background Detection**: If you leave the app while the phone screen remains on (e.g. switching to social media or games):
    - **2 Minutes**: Friendly notification reminder — *"Come back! Session pausing soon"*.
    - **4 Minutes**: Urgent warning notification — *"⚠️ Last minute! Session auto-stopping in 60s"*.
    - **5 Minutes**: Automatically saves the session silently and stops the timer.

- **📅 24-Hour Calendar Timeline**:
  - Visual 24-hour day grid showing exactly when your sessions occurred.
  - Blocks rendered with bucket colors, session labels, and duration badges.
  - Date navigation: quick arrows + "Today" pill button.

- **📊 Aggregated Statistics**:
  - Summary cards grouped by activity bucket.
  - Total focused time for **Today** and **This Week**.
  - Visual progress indicators showing percentage breakdown of your day.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose + Material Design 3
- **Local Persistence**: Room SQLite Database (Entities: `ActivityBucket`, `Session`)
- **Background Execution**: Android Foreground Service + Notification Channels
- **Lifecycle Awareness**: `ProcessLifecycleOwner` + `DisplayManager` for screen-state tracking
- **Min SDK**: API 26 (Android 8.0 Oreo) | **Target SDK**: API 35 (Android 15)

---

## 💻 Developing & Building from Source

```bash
# Clone the repository
git clone https://github.com/etreks/Session.git
cd Session

# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

<p align="center">
  Crafted with passion by <b>Satyam</b> 🚀
</p>
