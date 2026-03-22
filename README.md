# People Lineage Android

## About

People Lineage Android is an offline-first family lineage and people records app for Android. It is designed for storing large family datasets, linking people through father, mother, spouse, and child relationships, and viewing a readable family hierarchy for any person.

This repository contains the `v1.0.0` Android app built with Kotlin, Room, WorkManager, Hilt, and Jetpack Compose migration foundations.

## Current Features

- Add, edit, and delete people records
- Store name, gender, age, phone, village, P.S, P.O, district, state, and notes
- Link father, mother, spouse, and child relationships
- Support multiple spouses and complex family structures
- Village-wise landing page with drill-down people view
- Search people within a village
- View family graph for any person
- Share person details
- Export family graph as PDF
- Call or open WhatsApp from saved phone number
- Local storage mode
- Cloud sync mode using a linked cloud file
- Drive backup mode with scheduled backup

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Room
- Hilt
- WorkManager
- Coroutines + Flow

## Release

- App version: `1.0.0`
- App label: `People Lineage v1.0.0`
- Signed release APK is published through GitHub Releases

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Roadmap

- Full Compose migration for edit and graph flows
- Stronger cloud-backed sync for multi-device usage
- Web viewer and permission-based family sharing
- Record validation, voting, and confidence scoring
