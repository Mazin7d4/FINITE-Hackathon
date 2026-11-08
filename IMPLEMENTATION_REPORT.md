# FINITE v0.1.3 implementation report

## Implemented

- Android `versionCode 9` and `versionName "0.1.3"`.
- Persisted native policy for selected apps, enabled weekdays, start/end time, and daily limit.
- Native counting now rejects unselected platforms and events outside the saved schedule.
- Overnight windows are supported and remain associated with the day on which the window started (for example Friday 22:00–02:00 includes Saturday 01:00).
- Daily history is persisted locally by date, with combined totals and Instagram/YouTube splits. The usage review refreshes from native history and always shows the current local day plus the prior six days.
- Choose App limits tracking to Instagram and YouTube. TikTok and Snapchat remain visible in the preserved layout but show an explicit coming-soon notice.
- Choose Schedule persists the preset days and an editable time window while preserving the existing visual language and colors.
- Android local reminder scheduling uses `AlarmManager`, a notification channel, Android 13 `POST_NOTIFICATIONS` runtime permission, and boot/package-replacement rescheduling. Reminders are persisted in AsyncStorage, listed in the reminder screen, editable with saved values prefilled, independently deletable, and scheduled with independent native alarm IDs. Repeating alarms advance with local `Calendar` arithmetic until the next future wall-clock occurrence, including across DST.
- Reminder date/time selection now uses native Android `DatePickerDialog` followed by `TimePickerDialog` through a UI-thread-safe, cancellation-safe React Native bridge. The reminder screen displays the selected local date and time, preserves the selected time when switching to Today when possible, rejects past saves clearly, and exposes Add new reminder while editing. The footer accurately describes a scheduled-time reminder.
- The usage dashboard has same-theme edit actions for tracked apps and the counting schedule. Dashboard fallback chart values are zero rather than sample usage.
- Overlay display has a final native selected-app and schedule-window check for both Instagram and YouTube event paths. Session starts preserve the current local-day accumulated count while resetting only session acknowledgement state.
- Restyled the native limit overlay to match `src/screens/theme.ts`: exact theme gradient colors, rounded corners, divider, surface/primary actions, and Android sans-serif typography. Existing Done/Continue Anyway handlers, accessibility semantics, permission behavior, and detector implementations are unchanged.
- Detector implementations were not changed. The accessibility service now passes the detected package into the policy-aware store boundary.
- Added deterministic TypeScript policy tests and an Android overnight-window test.

## Passing checks

- TypeScript typecheck: PASS.
- JavaScript policy tests: 2 passed.
- Kotlin unit tests: 52 passed, zero failures/errors.
- Release APK assembly: PASS.
- APK: versionName 0.1.3, versionCode 9, signed with the existing debug key.
- Emulator smoke tests: notification permission prompt, saved reminder list, scheduled notification delivery, persisted usage totals and per-app split, themed native limit overlay, Continue Anyway.

## Installation

Download finite-v0.1.3.apk from GitHub Releases, allow installs from the browser/file manager, install over the previous version, enable FINITE Accessibility, and allow notifications when saving a reminder.
