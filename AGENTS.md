# Agent Guidelines & Engineering Protocol: BetterTube

This document defines the architectural rules, coding standards, build conventions, and operational constraints for any AI agent or software engineer working on the **BetterTube** codebase.

---

## 1. Project Overview & Core Philosophy

**BetterTube** is a native, privacy-first Android media extraction and encrypted vault application. It bridges native Unix media utilities (`yt-dlp`, `aria2c`, and `ffmpeg`) into a modern, fluid Android experience built with Jetpack Compose (Material Design 3).

### Key Architectural Pillars
- **Clean Architecture + MVVM**: Strict separation between `data`, `domain`, `ui`, `service`, and `utils`.
- **Hilt Dependency Injection**: Standardized injection using `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, and modular `@Provides` / `@Binds` modules in `di/`.
- **Reactive State Flow**: UI observes `StateFlow` from ViewModels using `collectAsStateWithLifecycle()` or `collectAsState()`.
- **Hardware-Backed Cryptography**: The encrypted vault relies on Android KeyStore AES-256-GCM (`VaultCipher`) and biometric/PIN authentication (`VaultPinManager`).
- **Daemon-Driven Downloads**: Multi-connection and torrent downloads communicate over a local JSON-RPC socket to an embedded `aria2c` process managed by `Aria2ProcessManager`.

---

## 2. Immutable Environment Constraints (Never Violate)

1. **NO Physical Emulator or ADB in Environment**:
   - You do **NOT** have access to `adb`, connected devices, or interactive Android emulators.
   - **NEVER** attempt to run `adb` commands or write instrumented tests that require a live emulator (`app/src/androidTest/` relying on real devices).
   - All tests must be fast local JVM unit tests or **Robolectric** tests (`app/src/test/`).

2. **Keystore Integrity**:
   - **NEVER** edit, delete, or recreate `debug.keystore` or `debug.keystore.base64`.
   - **NEVER** change the `signingConfigs` in `app/build.gradle.kts`.

3. **Application ID & Namespace**:
   - `namespace` is `com.bettertube.app`.
   - `applicationId` in `app/build.gradle.kts` is fixed to `com.bettertube.app`. Do **not** rename or generate new IDs.

4. **Platform Metadata Sync (`metadata.json`)**:
   - The platform relies on `metadata.json` for sidebar indexing and project discovery.
   - If `app_name` in `res/values/strings.xml` is modified, `name` in `metadata.json` must be updated in sync.
   - **CRITICAL**: Never remove `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` from `majorCapabilities` in `metadata.json`.

5. **Secrets & API Keys**:
   - Never hardcode API keys or secrets in source files or Gradle files.
   - The project uses the **Secrets Gradle Plugin** reading from `.env` and `.env.example`. Secrets are exposed via `BuildConfig`.
   - Never create or rely on `local.properties`.

6. **No Mock Fallbacks in Production Code**:
   - Do not replace real download pipelines or aria2 RPC calls with fake timers/progress generators.
   - BetterTube is designed to invoke the real `YtDlpEngine` and `Aria2ProcessManager` on device. Keep business logic and error handling authentic.

---

## 3. Directory Layout & Layer Boundaries

```
app/src/main/java/com/bettertube/app/
├── BetterTubeApp.kt              # @HiltAndroidApp application class & engine lifecycle
├── MainActivity.kt               # Single activity, edge-to-edge Compose root
├── di/                           # Dependency Injection (Hilt)
│   ├── AppModule.kt              # Engine, scheduler, and context bindings
│   └── RepositoryModule.kt       # Interface-to-implementation repository bindings
├── domain/                       # Pure Kotlin domain layer (NO Android framework dependencies)
│   ├── model/                    # Immutable business models (DownloadTask, VaultItem, etc.)
│   ├── repository/               # Repository interfaces (DownloadRepository, VaultRepository, etc.)
│   └── usecase/                  # Single-responsibility UseCases (FetchMetadata, StartDownload, etc.)
├── data/                         # Data layer (Android-specific implementations)
│   ├── aria2/                    # aria2c binary lifecycle & JSON-RPC client
│   │   ├── Aria2BinaryProvider.kt
│   │   ├── Aria2ProcessManager.kt
│   │   └── rpc/ (Aria2RpcClient.kt, JsonRpcModels.kt)
│   ├── engine/                   # youtubedl-android wrapper (YtDlpEngine.kt)
│   ├── repository/               # Implementations of domain repository interfaces
│   ├── schedule/                 # WorkManager config store and scheduler helpers
│   ├── vault/                    # AES-256-GCM crypto engine & PIN manager
│   ├── clipboard/                # Clipboard URL detection manager
│   └── share/                    # Intent sharing and deep-link extractor
├── service/                      # Background execution
│   ├── DownloadForegroundService.kt  # Sticky foreground service with notification updates
│   └── ScheduleEnforcerWorker.kt     # WorkManager worker for scheduled download windows
├── ui/                           # Jetpack Compose UI (Material 3)
│   ├── components/               # Shared components (Shimmer, PressScale)
│   ├── navigation/               # Routes.kt and BetterTubeNavGraph.kt
│   ├── screens/                  # Feature screens (home, downloads, files, music, browser, settings, onboarding)
│   ├── theme/                    # Color, Typography, Shapes, Theme.kt
│   └── utils/                    # Haptic feedback and UI extensions
└── utils/                        # System utilities (FormatUtils, NetworkMonitor, UrlValidator, CrashLogger)
```

### Layer Dependency Rules
- **Domain layer**: Must remain pure Kotlin. Never import `android.*` packages into `domain/model/` or `domain/usecase/` (except when strictly passing android-agnostic wrappers).
- **Data layer**: Implements domain interfaces. Exposes `Flow<T>` or `Result<T>` to domain use cases.
- **UI layer**: Composables must only communicate with `ViewModel`. Do not instantiate repositories or use cases directly in Composables.

---

## 4. UI & Jetpack Compose Standards

1. **Material Design 3 (M3)**:
   - Always use `androidx.compose.material3.*` components (`Scaffold`, `TopAppBar`, `NavigationBar`, `Card`, `Button`, `IconButton`, `Text`).
   - Use dynamic or theme colors via `MaterialTheme.colorScheme`. Avoid hardcoded hex colors in Composables.

2. **Automated Testing Tags (`testTag`)**:
   - Every primary interactive element (buttons, cards, search inputs, tabs) **MUST** include `Modifier.testTag("snake_case_id")`.
   - Examples:
     ```kotlin
     Modifier.testTag("download_button")
     Modifier.testTag("search_input_field")
     Modifier.testTag("vault_unlock_pin_input")
     ```

3. **Touch Targets & Accessibility**:
   - Interactive targets must satisfy the minimum touch target size of **48dp x 48dp**.
   - Always supply meaningful `contentDescription` for icons and images (or `null` if explicitly decorative).

4. **Edge-to-Edge & Insets**:
   - The app runs in edge-to-edge mode (`enableEdgeToEdge()`).
   - Respect window insets in root layouts (`Scaffold(contentWindowInsets = WindowInsets.systemBars)` or safe padding).

5. **File Size Limit**:
   - Keep Composable files under 500 lines. Break large screens into dedicated sub-components under `ui/screens/<feature>/components/`.

---

## 5. Coroutines, Concurrency & Networking

1. **Dispatchers**:
   - Long-running disk/network/RPC operations must run on `Dispatchers.IO`.
   - Cryptographic and heavy hashing tasks must run on `Dispatchers.Default`.
   - UI state updates and navigation must remain on `Dispatchers.Main`.

2. **Cancellation & Lifecycle**:
   - ViewModels launch coroutines within `viewModelScope`.
   - Daemons and services listen for `Job.isCancelled` and process teardown signals (`SIGTERM` before `SIGKILL`).
   - Clean up sockets, processes, and temporary extraction buffers in `finally` blocks.

3. **Network Constraints**:
   - Observe `NetworkMonitor` to honor the "WiFi-only" setting (`DownloadRepository.setWifiOnly(true)`).
   - Pause or prevent task initialization when network restrictions are not satisfied.

---

## 6. Testing Protocol

1. **Robolectric JVM Unit Tests**:
   - Location: `app/src/test/java/com/bettertube/app/`
   - Use `@RunWith(RobolectricTestRunner::class)` for tests requiring Android context, SharedPreferences, or Resources.
   - Run tests via shell:
     ```bash
     gradle :app:testDebugUnitTest
     ```

2. **Compose Screenshot & UI Tests**:
   - Use Roborazzi for snapshot verification (`RoborazziRule`).
   - Test UI states with Compose UI Test framework (`createComposeRule()` / `runComposeUiTest`).

---

## 7. Dependency & Build Configuration Guidelines

1. **Version Catalog (`gradle/libs.versions.toml`)**:
   - All external library versions and aliases reside in `gradle/libs.versions.toml`.
   - When referencing a library in `app/build.gradle.kts`, convert kebab-case to dot-notation (e.g., `androidx-room-runtime` $\to$ `libs.androidx.room.runtime`).
   - Keep unused dependencies commented out to prevent binary bloat.

2. **Compilation Verification**:
   - After completing code changes across files, verify the build using `compile_applet` (or `gradle :app:assembleDebug`).
   - Do not invoke `gradle clean` unless resolving a corrupted build cache, as it slows incremental compilation.

---

## 8. Summary Checklist for Agents Before Submitting Work

- [ ] Does the project compile cleanly with `compile_applet`?
- [ ] Are all new interactive UI elements assigned a `Modifier.testTag("...")`?
- [ ] Did you keep touch targets $\ge 48\text{dp}$ and provide accessibility content descriptions?
- [ ] Are all domain models and use cases kept independent of Android framework logic?
- [ ] Is `applicationId` unchanged?
- [ ] Are keystore files intact?
- [ ] Did you avoid introducing hardcoded secrets or mock replacement data?
- [ ] Are any new strings placed in `res/values/strings.xml` with descriptive IDs?
