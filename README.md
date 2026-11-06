# FINITE

FINITE is an original Android MVP that helps a user set an intentional Instagram Reels limit. It uses an Android `AccessibilityService` to observe accessibility metadata from `com.instagram.android`, count likely Reel transitions during an active session, and show a `TYPE_ACCESSIBILITY_OVERLAY` reminder at the configured limit.

## Build

Requirements: JDK 17+ and Android SDK 35.

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Run

1. Install and open FINITE.
2. Tap **Enable** and turn on **FINITE Reel Counter** in Android Accessibility settings.
3. Return to FINITE, select a limit, and tap **Start Session**.
4. Open the Reels viewer in the real Instagram app and scroll normally.

The service is scoped to accessibility events from the Instagram package and only counts while a FINITE session is active. Session state is stored in private `SharedPreferences`; no network or backend is used.

## Detection and diagnostics

Instagram provides no official API for per-Reel transitions and can change its internal accessibility tree without notice. FINITE therefore uses conservative, best-effort signals:

- Reels/`clips` viewer resource IDs or a combination of a visible Reels label and viewer actions.
- Vertical scroll events from a Reels viewer pager as the primary transition signal.
- A twice-confirmed change in visible Reel identity metadata as a fallback.
- Separate debounce windows to suppress duplicated accessibility events.

The app never substitutes timer-based or manual simulated counts. To inspect the metadata classifier while testing on a device:

```powershell
adb logcat -s FINITE
```

Look for event type, viewer evidence, action count, fingerprint, and the reason attached to each increment. If Instagram changes its tree, these logs are the starting point for updating the classifier in `ReelDetector.kt`.

## Verification status

The Gradle build can be verified locally. The complete five-Reel acceptance test requires a physical Android device with the real Instagram app and must not be considered passed until that test has been performed.

