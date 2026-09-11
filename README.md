# MedMe

A fully offline Android medication reminder app. Track patients and their medications,
get notified at the right times, log doses as taken (even before the reminder fires),
watch stock levels, and see how consistent dosing has been over time — all stored in a
single JSON file on the device, with no network access and no cloud account.

Built with Java for Android Studio.

## Features

- **Patients & medications** — organize multiple patients, each with their own medications.
- **Flexible schedules** — recurring (ongoing) or a fixed duration expressed in days,
  weeks, or months (converted to an exact day count from the start date).
- **Multiple doses per day** — set as many reminder times per medication as needed.
- **Reminders** — a local notification per dose time, with a "Mark Taken" action and
  swipe-to-dismiss both logged as taken (see [Notifications](#notifications) below).
- **Early dosing** — mark a dose taken from within the app before its reminder fires.
- **Inventory tracking** — log how much medication was acquired; get notified when
  stock drops to (or below) a configurable threshold.
- **Analysis tab** — a delay/earliness chart (MPAndroidChart) plus adherence stats,
  filterable by patient and medication.
- **Export / import** — back up or restore all data as a single `.json` file via the
  system file picker (Storage Access Framework). No cloud, no account.

## Data & privacy

All data lives in one JSON file in the app's private storage. There is no `INTERNET`
permission and nothing is ever transmitted anywhere — export/import is the only way
data leaves or enters the app, and that's entirely user-initiated.

## Notifications

Reminders use `AlarmManager.setAndAllowWhileIdle` (inexact alarms) rather than exact
alarms. This is an intentional tradeoff: it avoids the `SCHEDULE_EXACT_ALARM`
permission (and the Play Console justification step that comes with it) at the cost of
reminders occasionally arriving a few minutes late under Doze. This is disclosed to the
user on the About screen.

Swiping a reminder notification away is treated the same as tapping "Mark Taken" — it
logs the dose as taken at that moment, which feeds the delay/earliness analysis.

## Building

Requires Android Studio (or the Gradle wrapper + Android SDK on the command line).

```
./gradlew assembleDebug
```

- **minSdk**: 26 (Android 8.0 — required for notification channels)
- **targetSdk / compileSdk**: 35
- Gradle 9.7.1 / AGP 9.4.0

## Project structure

```
app/src/main/java/com/adnaan525/medme/
  model/          Plain data classes (Patient, Medication, DoseLog, ...), Gson-serialized
  data/           JsonStorage (file I/O) and DataRepository (single in-memory source of truth)
  notifications/  Notification channels, AlarmManager scheduling, broadcast receivers
  ui/patients/    Patient list, medication list, add/edit medication, medication detail
  ui/analysis/    Delay/earliness chart and adherence stats
  ui/about/       About screen, export/import
  util/           Date/time formatting, dose-schedule math, window-inset helpers
```

## Author

Created by Muntasir Adnan (adnaan525@gmail.com).
