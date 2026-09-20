# Privacy Policy for BetterTube

**Last Updated: September 2026**

BetterTube ("we", "our", or "the app") is committed to respecting and protecting user privacy. BetterTube is an open-source, client-only media management and download tool for Android.

## 1. Zero Data Collection & Telemetry
BetterTube does **NOT** collect, transmit, store, or sell any personal data, usage analytics, device identifiers, or telemetry.
- No Firebase, Google Analytics, Crashlytics, or Sentry SDKs are integrated.
- No advertising networks or tracking SDKs are present.
- All media downloads, extraction operations, encrypted vault files, and crash logs remain exclusively on your local device storage.

## 2. Local Crash Logs
If an unexpected crash occurs, crash diagnostics (stack trace, device model, and Android OS version) are saved locally to private application storage (`/data/data/com.bettertube.app/files/crash_logs/`). These logs are never transmitted over the network and can be viewed or deleted by the user at any time in **Settings > Diagnostics > View crash logs**.

## 3. Encrypted Vault Security
Items placed inside the Vault are encrypted using hardware-backed Android Keystore keys and industry-standard AES-256-GCM cipher algorithms. Master keys are bound to user biometric authentication (BiometricPrompt) or PIN codes and never leave the device.

## 4. Permissions Used & Purpose
BetterTube requests only permissions strictly necessary for its core functionality:
- `INTERNET`: To fetch media metadata, download files, and communicate with user-configured proxies or aria2 JSON-RPC daemon.
- `POST_NOTIFICATIONS`: To show active download progress notifications, speed metrics, and completion alerts (Android 13+).
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC`: To ensure large downloads continue uninterrupted in the background when navigating away from the app.
- `VIBRATE`: To provide tactile haptic feedback during key user actions (unlocking vault, starting downloads, changing tabs).
- `WAKE_LOCK`: To prevent the device CPU from sleeping while high-speed file transfers or media merging operations are active.
- `ACCESS_NETWORK_STATE`: To detect WiFi connectivity when "WiFi-only downloads" is enabled by the user.
- `USE_BIOMETRIC`: To enable biometric authentication (Fingerprint/Face Unlock) for the Encrypted Vault.

## 5. Contact & Source Code
BetterTube is free and open-source software. For questions or security inquiries, please open an issue in the project repository.
