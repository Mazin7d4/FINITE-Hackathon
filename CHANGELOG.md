# Changelog

## [0.1.7] — 2026-11-07

- Kept legacy timestamp/repeat alarms separate from video-count milestones during upgrade.
- Reconciled native reminders on profile restore while preserving once-per-day acknowledgements.
- Added rollback when reminder persistence fails, including edits and deletes.
- Changed reminders from scheduled notifications into once-daily video-count check-ins.
- Added watched and remaining counts to reminder popups.
- Redesigned the reminder and hard-limit overlays with premium cards, progress details, and entrance/exit motion.
- Fixed the next Short playing after Continue with intention.
- Android versionCode 13, versionName 0.1.7.

## [0.1.6] — 2026-11-07

- Added **Skip for now** on the first screen for account-free setup.
- Added persistent guest setup, settings, and reminders.
- Kept optional local account creation, preserving setup when an account is created.
- Android versionCode 12, versionName 0.1.6.

## [0.1.5] — 2026-11-07

- Made daily goal, selected apps, schedule, and reminders editable from the dashboard.
- Added optional scheduling, off by default.
- Added persistent device-local profiles with salted password verification, sign-in, logout, and reminder restoration.
- Synchronized dashboard edits, native session settings, and the shared counter.
- Preserved the Simran visual direction and shared Reels + Shorts detector.

## [0.1.4] — 2026-11-07

- Switched session launch to Instagram first when selected and installed.
- Made counting defaults fail open when app selection is unavailable.
- Updated the limit notification styling.

## [0.1.3] — 2026-11-07

- Added real reminders, scheduled app policy, usage history, and the themed limit overlay.

## [0.1.2] — 2026-11-07

- Made Simran's UI the main flow.

## [0.1.1] — 2026-11-07

- Integrated the team UI and improved fast-swipe counting while the next video loads.

## [0.1.0] — 2026-11-07

- Initial FINITE Android MVP: React Native flow, native accessibility service, shared Reels/Shorts counter, and device-local session state.
