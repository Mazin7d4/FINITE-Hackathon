# FINITE

FINITE is a React Native Android MVP for setting one shared Instagram Reels and YouTube Shorts limit. The interface and session controls are TypeScript/React Native; detection and overlays use native Kotlin and Android accessibility APIs. Counting is best effort.

## Architecture

- **React Native 0.88 / React 19.3** — dark session UI, limit input, permission setup, and live counter.
- **Kotlin native module** — exposes session state, accessibility status, app launch, and state events to JavaScript. Existing `FiniteNative` methods and state fields are retained.
- **Android `AccessibilityService`** — observes Instagram and YouTube during an active session. Each platform has independent detection state and feeds the same `SessionStore.increment()` method.
- **Native accessibility overlay** — displays the shared limit reminder with **I'm Done** and **Continue Anyway** actions.
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
2. Tap **Enable** and turn on **FINITE Reels and Shorts Counter** in Android Accessibility settings.
3. Return to FINITE, select a limit, and tap **Start Session**.
4. Open Reels in Instagram or Shorts in YouTube. Start Session launches Instagram when installed, otherwise YouTube. You can switch apps manually during the session, and either app can be used on its own.

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

## YouTube Shorts detection

`ShortsDetector` identifies a visible Shorts viewer using the observed `reel_watch_fragment_root`, `reel_watch_player`, and `reel_recycler` IDs together. Ordinary `watch_player` playback is rejected; `reel_time_bar`, Home/search scrolling, and generic content-change events cannot independently count. Visible engagement panels, loading, and end pages suppress pending transitions. The identifiers came from the earlier Galaxy A15 5G / Android 16 / YouTube 21.43.523 diagnostic capture, not an official YouTube API.

`ShortsCounter` emits one accepted increment reason for the first confirmed viewer entry and for each accepted Shorts scroll burst. Only `TYPE_VIEW_SCROLLED` events from `reel_player_underlay` or `reel_recycler` qualify. `sourceScrollable=true` is not required. Events are grouped until 200 ms of quiet, with a 700 ms count debounce. Where available, repeated committed page indices, unchanged cumulative scroll offsets, duplicate timestamps, horizontal movement, and cancelled/tiny movement are rejected. Both directions and revisiting prior videos can count. No YouTube text, account metadata, or captions are read, hashed, logged, or stored.

Temporary viewer uncertainty preserves the entry latch; an outside-viewer classification must persist across two checks for at least 600 ms before another entry can count. App switching cancels pending Shorts checks but preserves the latch. The bridge sends an explicit reset broadcast and waits for the service receiver before starting a fresh session. Session end cancels pending checks, and `SessionStore.increment()` independently rejects inactive sessions. Instagram's existing `ReelDetector` and increment criteria remain unchanged.

This is a hackathon heuristic, not verified one-count-per-video accuracy. Some events expose no stable page index or movement; those use the qualified scroll source plus debounce as a fallback. Rapid swipes can be missed; long partial swipes, delayed duplicate events, ads, service restarts, and future YouTube changes can cause extra or missed counts. New physical-device QA is still required.

## Device QA

Run focused Android tests and build the bundled APK:

```powershell
cd android
.\gradlew.bat testDebugUnitTest assembleRelease
```

Install `android/app/build/outputs/apk/release/app-release.apk` and enable the accessibility service. The release APK embeds JavaScript and does not require Metro.

1. Start with limit 10. Open Shorts: the first unobstructed Short should count as 1. Make four deliberate swipes about one second apart; the total should reach 5. Swipe backward and verify each completed transition adds one.
2. Stay on one Short; like/unlike, pause/resume, open/scroll/close comments. None should add a count. Scroll YouTube Home/search and play a normal video; those should not count.
3. Switch to Instagram Reels and verify its counts add to the same total. Return to the same Short and check that an app switch alone does not add an extra first-video count.
4. Verify the reminder at the shared limit, both reminder actions, End Session, and a fresh session resetting the count to zero. Test with YouTube only installed as well.

Accepted counts are logged under `FINITE`. Debug builds additionally log safe Shorts viewer/source information and rejection/acceptance reasons under `FINITE_SHORTS`; scroll logs bypass content-diagnostic throttling:

```powershell
adb logcat -v threadtime "FINITE_SHORTS:D" "FINITE:I" "*:S"
```
