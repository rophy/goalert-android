# TODO / Known Issues

## Remaining

- **Release signing config** (`app/build.gradle.kts`) — no `signingConfig` is defined, so
  `assembleRelease` produces an *unsigned* APK. Add one backed by a keystore (gradle
  properties / env vars) for distributable builds.

- **Full-screen intent for critical alerts** (`NotificationHelper`) — `CATEGORY_ALARM` is set,
  but a true heads-up/full-screen alert (to wake the screen) needs the restricted
  `USE_FULL_SCREEN_INTENT` permission (special approval on Android 14+). Evaluate before adding.

## Notes / to verify

- The `builtin-fcm-push` dest type and push data-payload keys (`type`, `service_name`,
  `summary`, `alert_id`, `instance_url`, `code`, `count`) are a contract with the custom
  server-side GoAlert FCM provider. Keep them in sync with the server.
