# CampusTask

**COMP3074 – Mobile Application Development I** | Android application prototype

CampusTask is an offline-first Android assignment tracker. It helps students organize courses and assignments with due dates, priorities, notes, completion status, and a dashboard.

## Features

- Dashboard showing upcoming, overdue and completed assignment counts
- Add, edit and delete courses (with confirmation before cascading deletion)
- Add, edit and delete assignments
- Course, due date, priority, notes and completion status
- Mark assignments complete/incomplete from the list or details screen
- Filter by course and completion status
- Required-field validation and duplicate course-code protection
- Local Room/SQLite data storage, surviving app restarts; no account or API key needed
- Android Material UI, RecyclerView lists, date picker and navigation

## Run in Android Studio

1. Install a recent version of **Android Studio** with Android SDK **35** and **JDK 17**.
2. Open the `CampusTask` folder as an Android project (the folder containing `settings.gradle.kts`).
3. Allow Android Studio to sync the Gradle build and install missing SDK components.
4. Run `app` on an Android emulator or device running **Android 8.0 (API 26)** or later.
5. Start by creating a course under **Courses**, then add your assignments.

Android Studio can create the Gradle wrapper for the project if needed; the ZIP intentionally excludes generated binaries and IDE-local configuration. An internet connection is needed for the initial Gradle dependency sync only. The application itself works offline.

## Architecture

- Kotlin + Android XML layouts + Material Components
- AndroidX RecyclerView for tasks
- Room database for persistent `Course` and `Task` entities (one-to-many, cascading removal)
- ViewModel to hold UI list/filter/navigation state across configuration changes
- Background executor for database reads/writes so the main thread stays responsive

## Project structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/campustask/app/
│   ├── MainActivity.kt
│   └── data/Database.kt
└── res/
    ├── layout/activity_main.xml
    ├── layout/item_task.xml
    └── values/colors.xml, styles.xml
```

## GitHub upload

Create an empty GitHub repository named `CampusTask`, then run from the extracted folder:

```bash
git init
git add .
git commit -m "Initial working CampusTask Android prototype"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/CampusTask.git
git push -u origin main
```

## Scope

Implements the core MVP described in the course proposal. Optional reminders, calendar view, and cloud sync are not included. No APK is bundled; build and execute from Android Studio. Automated emulator/build validation has not been performed in the creation environment.
