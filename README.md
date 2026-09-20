# BetterTube - Android Media Downloader & Vault

BetterTube is an advanced, privacy-first Android media downloader built with Kotlin, Jetpack Compose, Material 3, and Clean Architecture.

## Key Features

- **Multi-Source Extraction:** Powered by `youtubedl-android` (yt-dlp engine) and embedded FFmpeg for format conversion.
- **Power Downloads (aria2 Daemon):** Native aria2 binary execution with JSON-RPC controlling BitTorrent, Magnet, Metalink, and multi-segment HTTP/HTTPS downloads.
- **Encrypted Vault:** AES-256-GCM hardware-backed encryption with BiometricPrompt (fingerprint/face) and custom PIN unlock.
- **Modern Jetpack Compose UI:** Material Design 3 theming, shimmer skeleton loading states, smooth animations, and haptic feedback.
- **WorkManager & Background Scheduling:** Time-windowed download scheduling and WiFi-only network constraint enforcement.
- **Zero Telemetry / Ads:** 100% offline-first and client-side operation with local crash logging only.

## Build and Release Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17
- Android SDK 35 (Compile SDK) / Min SDK 24

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK / AAB
1. Copy `keystore.properties.example` to `keystore.properties` (or set environment variables):
   ```properties
   storeFile=/path/to/release.keystore
   storePassword=your_store_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```
2. Run the Gradle release task:
   ```bash
   ./gradlew assembleRelease
   ```
   or bundle:
   ```bash
   ./gradlew bundleRelease
   ```

## Architecture

- **`app/src/main/java/com/bettertube/app`**
  - **`data/`**: Repositories, preferences, aria2 daemon & JSON-RPC client, Room/JSON storage, encryption engines.
  - **`domain/`**: Pure Kotlin models and UseCases.
  - **`service/`**: Foreground download service with NotificationManager lifecycle management.
  - **`ui/`**: Jetpack Compose screens (`home`, `downloads`, `browser`, `vault`, `playlists`, `settings`, `onboarding`), custom components (`Shimmer`, `PressScale`), navigation graph, and M3 themes.
  - **`utils/`**: Format helpers, haptics, crash logging, and device state managers.

## License
GNU General Public License v3.0 (see [LICENSE](LICENSE)).
