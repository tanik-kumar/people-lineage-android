# People Lineage Android

Offline-first Android app for village-wise family lineage and people records.

## About

People Lineage Android helps store and manage large family records with father, mother, spouse, and child relationships. It is designed for gradual long-term data collection, local-first usage, and future expansion toward cloud sync and web access.

Current public release: [`v1.0.0`](https://github.com/tanik-kumar/people-lineage-android/releases/tag/v1.0.0)

Signed APK download:
[`people-lineage-v1.0.0-release.apk`](https://github.com/tanik-kumar/people-lineage-android/releases/download/v1.0.0/people-lineage-v1.0.0-release.apk)

## Screenshots

Screenshots are tracked under [docs/screenshots/README.md](docs/screenshots/README.md). Add real device screenshots there for the next repo polish pass.

## Core Features

- Add, edit, and delete person records
- Store full name, gender, age, phone, village, P.S, P.O, district, state, and notes
- Link father, mother, spouse, and child relationships
- Support multiple spouses and complex family structures
- Village-wise landing page with drill-down people listing
- Search people inside a selected village
- View family hierarchy graph for any person
- Share person details
- Export family graph as PDF
- Call and WhatsApp quick actions from saved phone numbers
- Local storage mode
- Cloud sync mode using a linked cloud file
- Drive backup mode with periodic backup support

## Storage Modes

### Local

All core data stays on the device and works offline.

### Cloud Sync

Sync uses a linked cloud file through the Android document provider flow.

### Drive Backup

Backup uses a linked file with scheduled refresh and manual restore support.

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Room
- Hilt
- WorkManager
- Coroutines + Flow
- Navigation Compose

## Installation

1. Open the latest release:
   [`v1.0.0`](https://github.com/tanik-kumar/people-lineage-android/releases/tag/v1.0.0)
2. Download the signed APK:
   [`people-lineage-v1.0.0-release.apk`](https://github.com/tanik-kumar/people-lineage-android/releases/download/v1.0.0/people-lineage-v1.0.0-release.apk)
3. Install it on an Android device that allows sideloaded APKs.

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Release

- App version: `1.0.0`
- App label: `People Lineage v1.0.0`
- Signed with release certificate:
  `CN=People Lineage Release, OU=Mobile, O=Tanik, L=Kolkata, ST=West Bengal, C=IN`

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Security

See [SECURITY.md](SECURITY.md).

## Roadmap

- Full Compose migration for edit and graph flows
- Stronger cloud-backed sync for multi-device usage
- Web viewer and permission-based family access
- Record validation, voting, and confidence scoring
