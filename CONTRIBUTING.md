# Contributing to FINITE

Thanks for helping make FINITE calmer and more useful. Keep changes focused, explain the user outcome, and preserve the native/React Native boundary.

## Before opening a pull request

```bash
npm ci
npx tsc --noEmit
npm test -- --runInBand
cd android && ./gradlew testDebugUnitTest
```

For UI changes, include a short before/after description and updated screenshots when the visual result changes. For detector or bridge changes, add or update focused tests and explain how state crosses the native bridge.

Please do not add credentials, user data, generated build outputs, APKs, or tool caches. Open an issue first for large product changes.
