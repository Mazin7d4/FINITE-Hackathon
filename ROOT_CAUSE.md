# v0.1.4 regression fix

## Confirmed root cause

v0.1.3 added native policy restoration on app startup. The restored JS default schedule was 08:00–22:00, and missing or invalid saved app selections were filtered to an empty set. Native policy enforcement then rejected otherwise valid Reels and Shorts events outside that window or with no selected app. v0.1.2 did not restore either gate.

## Fixes

- Missing, empty, legacy, and invalid app values fall back to Instagram + YouTube; recognized supported package/label values normalize to their canonical app without expanding a valid single selection.
- Missing, invalid, or unconfigured schedules normalize to all days, 00:00–00:00 (always active).
- Explicit valid weekday and overnight schedules remain enforced, including their start-day overnight semantics.
- Existing goals restore the safe both-app default when no valid app selection was saved.
- Session launch uses deterministic Instagram-first ordering when selected and installed, with YouTube as the fallback.
- Accessibility service configuration and native lifecycle cleanup remain unchanged.

## Checks

- Jest policy tests pass (5 tests), covering defaults, app migration, weekdays, and overnight windows.
- TypeScript typecheck passes.
- Android unit coverage added for default/migration behavior, weekdays, overnight windows, package/label normalization, and shared counting.

## Release verification

- TypeScript typecheck, 5 JavaScript policy tests, 61 Kotlin unit tests, and release APK assembly pass.
- Emulator reproduction used a v0.1.2 saved goal, upgraded to v0.1.3, with local time outside its implicit 08:00–22:00 gate: active session stayed at 0/7 despite Shorts detections.
- Installing v0.1.4 over that same saved state restored Shorts counting to 2/7 outside the old gate. A fresh v0.1.4 installation also counted Shorts (1/7).
- Instagram-first launch confirmed with both apps installed.
- Screenshots/logs captured under /workspace/finite-test/v014-*.
