# TODO / Known Issues

Most review findings have been fixed (see git history). Remaining items are below.

## Fixed

- **Deep link fallback** — `NotificationHelper` now falls back to the stored instance URL
  when a push omits `instance_url`.
- **WebView lifecycle** — WebView is destroyed in `onDestroy`, `onPause`/`onResume` are
  forwarded, and `android:configChanges` avoids reload on rotation.
- **Back navigation** — migrated from deprecated `onBackPressed()` to `OnBackPressedDispatcher`.
- **Notification icon** — added a monochrome `ic_notification` vector (no more white blob).
- **Release hardening** — R8 minify + `shrinkResources` enabled, `proguard-rules.pro` added.
- **Critical alerts** — set `CATEGORY_ALARM` on the critical channel.
- **Backup of sensitive prefs** — `backup_rules.xml` / `data_extraction_rules.xml` exclude
  the shared prefs (instance URL, FCM token, contact-method id) from backup/transfer.
- **Cleartext** — README documents the HTTPS-only requirement.
- **Verification notification** — no longer shows "code is: null" when the code is absent.

## Remaining

- **Release signing config** (`app/build.gradle.kts`) — still no `signingConfig`, so
  `assembleRelease` produces an *unsigned* APK. Add one backed by a keystore (gradle
  properties / env vars) for distributable builds. Needs a keystore, so left undone.

- **Full-screen intent for critical alerts** (`NotificationHelper`) — `CATEGORY_ALARM` is set,
  but a true heads-up/full-screen alert (to wake the screen) needs the restricted
  `USE_FULL_SCREEN_INTENT` permission (special approval on Android 14+). Evaluate before adding.

## Notes / to verify

- The `builtin-fcm-push` dest type and push data-payload keys (`type`, `service_name`,
  `summary`, `alert_id`, `instance_url`, `code`, `count`) are a contract with the custom
  server-side GoAlert FCM provider. Keep them in sync with the server.
