# BetterTube

[![Android CI](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square&logo=android)](https://github.com/)
[![Platform](https://img.shields.io/badge/Platform-Android_14_--_15_(API_24+)-3DDC84?style=flat-square&logo=android)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean_%2B_MVVM-blueviolet?style=flat-square)](#architecture)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)

A native, privacy-first Android media downloader and encrypted vault. Powered by `yt-dlp`, an embedded native `aria2` daemon over local JSON-RPC, and hardware-backed AES-256-GCM encryption.

---

## Highlights

| Capability | Technical Implementation |
|---|---|
| **Multi-Source Extraction** | Powered by `youtubedl-android` (yt-dlp wrapper) with embedded FFmpeg for on-device muxing, format conversion (MP4, MKV, MP3, M4A, FLAC, WAV), and subtitle extraction. |
| **Power Downloads (aria2 Daemon)** | Embedded `aria2c` Unix binary executing locally, controlled over Unix sockets / localhost JSON-RPC. Supports BitTorrent, Magnet links, Metalink, and multi-connection segment acceleration. |
| **Encrypted Vault** | Hardware-backed `AES-256-GCM` via Android KeyStore (`VaultCipher`). Protected by Android `BiometricPrompt` (fingerprint/face) and PBKDF2 PIN authentication. |
| **Foreground Service & Worker** | Sticky `DownloadForegroundService` with dynamic notification actions (Pause/Resume/Cancel) and `WorkManager` (`ScheduleEnforcerWorker`) for off-peak scheduling windows. |
| **Queue & Traffic Controls** | Reorderable download queue, global and per-task bandwidth throttles, and strict WiFi-only network enforcement via `NetworkMonitor`. |
| **100% Client-Side Privacy** | Zero third-party trackers, zero advertising SDKs, and zero external telemetry. All metadata, credentials, and vault items remain isolated on-device. |

---

## Architecture Overview

BetterTube follows strict **Clean Architecture** and **MVVM** principles with unidirectional data flow:

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (Screens, M3 Components, BetterTubeNavGraph, Theme)  │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                    ViewModel Layer                     │
│    (HomeViewModel, DownloadsViewModel, FilesViewModel) │
└───────────────────────────▲────────────────────────────┘
                            │ Domain UseCases
┌───────────────────────────┴────────────────────────────┐
│                      Domain Layer                      │
│ (Pure Kotlin: Models, Repository Contracts, UseCases)  │
└──────┬────────────────────┬────────────────────┬───────┘
       │                    │                    │
┌──────▼───────┐    ┌───────▼────────┐   ┌───────▼───────┐
│ YtDlpEngine  │    │ Aria2Process   │   │  VaultCipher  │
│ (youtubedl)  │    │ & JSON-RPC     │   │ (AES-256-GCM) │
└──────┬───────┘    └───────┬────────┘   └───────┬───────┘
       │                    │                    │
┌──────▼───────┐    ┌───────▼────────┐   ┌───────▼───────┐
│ Python/FFmpeg│    │ native aria2c  │   │AndroidKeyStore│
│ binary engine│    │ daemon process │   │ & Biometrics  │
└──────────────┘    └────────────────┘   └───────────────┘
```

### Module Structure

```
app/src/main/java/com/bettertube/app/
├── data/
│   ├── aria2/           # Native binary executor & local JSON-RPC socket client
│   ├── engine/          # yt-dlp core wrapper & FFmpeg process bindings
│   ├── repository/      # Implementations of domain repository interfaces
│   ├── schedule/        # WorkManager configurations and schedule windows
│   ├── vault/           # AES-256-GCM cipher and PBKDF2/Biometric PIN security
│   ├── clipboard/       # Auto-detect copied video URLs
│   └── share/           # Android Send/Share sheet intent processor
├── domain/
│   ├── model/           # Framework-free data structures (DownloadTask, VaultItem)
│   ├── repository/      # Repository interface definitions
│   └── usecase/         # Decoupled business actions (StartDownload, FetchSubtitles)
├── service/
│   ├── DownloadForegroundService.kt  # Ongoing foreground task notification controller
│   └── ScheduleEnforcerWorker.kt     # Periodic background schedule enforcer
├── ui/
│   ├── navigation/      # Navigation Compose graph & typed routes
│   ├── screens/         # Home, Downloads, Files, Music, Browser, Settings
│   ├── components/      # Material 3 UI widgets, Shimmer, PressScale
│   └── theme/           # Color palette, Typography, M3 Theme setup
└── utils/               # NetworkMonitor, UrlValidator, CrashLogger, FormatUtils
```

---

## Tech Stack

- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Language:** Kotlin 2.0.x with JVM 17 target
- **Dependency Injection:** [Dagger Hilt](https://dagger.dev/hilt/)
- **Asynchronous & Streams:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Media Engine:** [`youtubedl-android`](https://github.com/yausername/youtubedl-android) (`yt-dlp` + FFmpeg + `aria2c`)
- **Networking & RPC:** OkHttp, Retrofit, Moshi JSON adapter
- **Security & Storage:** Android KeyStore, `androidx.security:security-crypto`, AndroidX Biometric
- **Background Tasks:** AndroidX WorkManager, Foreground Services
- **Testing:** JUnit 4, Robolectric, Roborazzi screenshot verification

---

## Getting Started

### Prerequisites
- **JDK 17** (Temurin, Azul Zulu, or OpenJDK)
- **Android SDK:** Compile SDK 35 / Min SDK 24
- **Android Studio:** Ladybug (2024.2.1+) or newer recommended

### Building from Source

Clone the repository and compile the debug APK:

```bash
git clone https://github.com/bettertube/bettertube.git
cd bettertube

# Build debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Tests

Execute the unit test and Robolectric suite:

```bash
# Run unit & Robolectric tests
./gradlew :app:testDebugUnitTest

# Run Roborazzi screenshot verification (if configured)
./gradlew :app:verifyRoborazziDebug
```

---

## Release Configuration

To sign release builds, create `keystore.properties` in the project root:

```properties
storeFile=/path/to/your/release.keystore
storePassword=your_keystore_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Build the signed release artifacts:

```bash
# Build signed APK
./gradlew assembleRelease

# Build signed Android App Bundle (AAB)
./gradlew bundleRelease
```

---

## Security & Privacy Model

1. **Hardware-Backed KeyStore:** Vault encryption keys are generated directly inside the Android KeyStore provider (`AndroidKeyStore`) and never touch RAM as plaintext.
2. **Zero In-Memory Plaintext Retention:** Passwords and PIN buffers are wiped after use.
3. **No Network Leaks:** All external network connections occur strictly during metadata fetch or media downloading. No diagnostic telemetry or third-party ad requests are generated.

---

## Contributing & Agent Guidelines

We welcome contributions from human developers and AI assistants alike!
- For AI coding agents and automated contributors, please review **[AGENTS.md](AGENTS.md)** / **[agents.md](agents.md)** for our strict architectural boundaries, touch target rules, and environment guidelines.
- Open an issue or discussion before making substantial architecture alterations.

---

## License

BetterTube is licensed under the [GNU General Public License v3.0](LICENSE).
