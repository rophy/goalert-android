# TODO / Known Issues

## Remaining

- **Release signing config** (`app/build.gradle.kts`) — no `signingConfig` is defined, so
  `assembleRelease` produces an *unsigned* APK. Add one backed by a keystore (gradle
  properties / env vars) for distributable builds.

## Play Store prep (only if publishing publicly)

- **App bundle** — produce an `.aab` (`./gradlew bundleRelease`); Play requires AAB for new apps.
- **512×512 Play icon** — high-res icon from the GoAlert logo for the store listing.
- **targetSdk / versioning** — bump `targetSdk` to 35 (Play minimum is rising) and define a real
  `versionName` / `versionCode` scheme (currently `1` / `1.0.0`).
- **Privacy policy** — required by Play (the app collects an FCM token and connects to a server).
- **Policy risk to resolve first** — `USE_FULL_SCREEN_INTENT` is auto-revoked on Android 14+ for
  non-calling apps, and `SYSTEM_ALERT_WINDOW` is heavily scrutinized. Both are central to the ring
  feature, so decide on the distribution channel (public Play vs. sideload / internal) before
  investing in a public listing.

## Notes / to verify

- The `builtin-fcm-push` dest type and push data-payload keys (`type`, `service_name`,
  `summary`, `alert_id`, `instance_url`, `code`, `count`) are a contract with the custom
  server-side GoAlert FCM provider. Keep them in sync with the server.
