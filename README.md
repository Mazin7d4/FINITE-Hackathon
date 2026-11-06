# FINITE

FINITE is an original React Native Android MVP for setting an intentional Instagram Reels limit. The interface and session controls are TypeScript/React Native; the Android integration remains native Kotlin because Reel detection and accessibility overlays require Android system APIs.

## Architecture

- **React Native 0.88 / React 19.3** — dark session UI, limit input, permission setup, and live counter.
- **Kotlin native module** — exposes session state, accessibility status, Instagram launch, and state events to JavaScript.
- **Android `AccessibilityService`** — observes accessibility metadata only from `com.instagram.android` during an active session.
- **Native accessibility overlay** — displays the limit reminder over Instagram with **I'm Done** and **Continue Anyway** actions.
- **SharedPreferences** — keeps the limit, count, active state, and acknowledgement entirely on-device.

No fake Instagram interface, backend, authentication, analytics, or simulated counting is used.

## Requirements

- Node.js 22.11 or newer
- JDK 17 or newer
- Android SDK 36
- Android NDK `27.1.12297006` and CMake 3.22.1 (Gradle can install these after license acceptance)

## Install dependencies

```powershell
npm install
```

## Build a self-contained APK

The release build embeds the JavaScript bundle and does not require Metro:

```powershell
cd android
.\gradlew.bat assembleRelease
```

Output:

```text
android/app/build/outputs/apk/release/app-release.apk
```

The hackathon release currently uses the standard debug signing key. Replace it with a private release keystore before production distribution.

For local React Native development:

```powershell
npm start
npm run android
```

## Run

1. Install and open FINITE.
2. Tap **Enable** and turn on **FINITE Reel Counter** in Android Accessibility settings.
3. Return to FINITE, select a limit, and tap **Start Session**.
4. Open the Reels viewer in the real Instagram app and scroll normally.

## Detection and diagnostics

Instagram provides no official per-Reel transition API and can change its internal accessibility tree. FINITE uses conservative best-effort signals:

- Reels/`clips` viewer resource IDs or a combination of a visible Reels label and viewer actions.
- Vertical scroll events from a Reels viewer pager as the primary transition signal.
- A twice-confirmed change in visible Reel identity metadata as a fallback.
- Separate debounce windows to suppress duplicate accessibility events.

Inspect classifier behavior on a connected device with:

```powershell
adb logcat -s FINITE
```

The log records event types, viewer evidence, fingerprints, and the reason attached to every increment. The five-Reel acceptance test must be performed on a physical Android device with the installed Instagram version before claiming exact detection reliability.
