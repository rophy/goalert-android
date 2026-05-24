# TODO / Known Issues

Outstanding findings from the code review. The High-severity security issues and the
FCM registration bug have already been fixed (see git history). The items below remain.

## Medium

- **Deep link breaks when a push omits `instance_url`**
  `NotificationHelper.kt` (`showNotification`, `instanceUrl` defaults to `""`) → builds an
  invalid `/alerts/<id>` URL. `FCMService` has a `Context`, so fall back to
  `TokenManager.getInstanceUrl(this)` instead of trusting the payload to always carry it.

- **WebView lifecycle**
  `MainActivity.kt` (`setupWebView`) — the WebView is never destroyed (leak), `onPause`/
  `onResume` aren't forwarded, and with no `android:configChanges` it's recreated on rotation
  (reloads the page, loses scroll/form state). Forward lifecycle and/or handle config changes.

## Low

- **`onBackPressed()` is deprecated** on `targetSdk 34` (`MainActivity.kt`). Migrate to
  `OnBackPressedDispatcher` / `OnBackPressedCallback`.

- **Notification small icon** uses `android.R.drawable.ic_dialog_alert`
  (`NotificationHelper.kt`) — a full-color system drawable renders as a white blob in the
  status bar. Ship a proper monochrome silhouette icon.

- **Release build hardening** (`app/build.gradle.kts`) — `isMinifyEnabled = false`, no R8/
  shrinking, no signing config. Enable R8 and add a signing config for real release builds.

- **`setBypassDnd(true)`** (`NotificationHelper.kt`) is ignored unless the app is granted
  notification-policy access. For true critical alerts consider `CATEGORY_ALARM` and/or a
  full-screen intent.

- **`allowBackup="true"`** (`AndroidManifest.xml`) backs up the instance URL + FCM token +
  contact-method id. Consider `fullBackupContent` rules or disabling backup for these.

- **Cleartext blocked globally** (`network_security_config.xml`) — self-hosted `http://`
  instances will fail silently, and `http://`-prefixed input in the URL dialog isn't upgraded
  (`MainActivity.kt`). Document this, or handle http hosts explicitly.

- **Verification notification** shows `"...is: null"` if the `code` field is missing
  (`NotificationHelper.kt`).

## Notes / to verify

- The `builtin-fcm-push` dest type and the push data-payload keys (`type`, `service_name`,
  `summary`, `alert_id`, `instance_url`, `code`, `count`) are a contract with the custom
  server-side GoAlert FCM provider. Keep them in sync with the server.
