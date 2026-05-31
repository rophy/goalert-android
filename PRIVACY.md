# Privacy Policy — GoAlert Android

_Last updated: 2026-05-31_

GoAlert Android ("the app") is a thin native wrapper around your own
[GoAlert](https://github.com/target/goalert) instance. The app has **no backend of its own**:
it talks only to the GoAlert server you point it at and to Google's Firebase Cloud Messaging
(FCM) for delivering push notifications.

## In plain English

- We don't run a server. We don't collect or store your data anywhere we control.
- Everything you enter (instance URL, login credentials, session) stays on **your device** or
  goes to **your own GoAlert server**.
- The only third party involved is **Firebase Cloud Messaging (Google)**, which is used solely
  as the transport for push notifications, the same way iMessage and WhatsApp use APNs/FCM.
- No analytics, no crash reporting, no advertising, no tracking SDKs.

## Data the app handles

| Data | Where it's stored | Where it's sent | Why |
|---|---|---|---|
| **GoAlert instance URL** you enter on first launch | Local app preferences on your device | Your GoAlert server (when the app loads its pages) | Lets the app know which server to connect to |
| **GoAlert login credentials** | Typed directly into the GoAlert web UI inside the WebView; never read by the app | Your GoAlert server only | Logging in to your GoAlert instance |
| **GoAlert session cookies** | Local WebView storage on your device | Your GoAlert server only | Keeps you logged in |
| **FCM device token** (a random identifier issued by Google to your device) | Local app preferences on your device | Google (Firebase) for push delivery; your GoAlert server, so it knows where to send your alerts | Receiving push notifications |
| **GoAlert contact method ID** (assigned by your GoAlert server when the device is registered) | Local app preferences on your device | Your GoAlert server only | Letting the server identify this device when alerts are sent |
| **Notification payloads** (service name, alert summary, alert ID, etc.) | Displayed transiently as notifications; not retained by the app | Sent by your GoAlert server through FCM | Showing you the alert |

## Data the app does **not** collect

- No personal identifiers (name, email, phone)
- No location data
- No contacts, photos, files, microphone, or camera
- No usage analytics, crash reporting, or device fingerprinting
- No advertising identifiers
- No third-party SDKs beyond Firebase Messaging

## Permissions the app requests

| Permission | Why |
|---|---|
| `INTERNET` | Reach your GoAlert server and Firebase |
| `POST_NOTIFICATIONS` | Show alert notifications (Android 13+) |
| `USE_FULL_SCREEN_INTENT` | Show critical-alert rings over the lock screen |
| `SYSTEM_ALERT_WINDOW` | Show critical-alert rings over other apps |
| `VIBRATE` | Vibrate during critical-alert rings |

## Where your data goes

- **Your GoAlert server** — whichever instance URL you configure. Its operator (you, in a
  self-hosted setup) controls everything you send through it. Refer to that server's own
  policy.
- **Firebase Cloud Messaging (Google LLC)** — used as the push notification transport.
  Google receives only what's required to deliver pushes: your FCM token and the notification
  payload your GoAlert server sends. See
  [Google's privacy policy](https://policies.google.com/privacy).

The app does **not** send your data to any other party.

## Data retention and deletion

- All locally stored data (instance URL, FCM token, contact method ID, cookies) lives only
  on your device. **Uninstalling the app removes it.**
- To stop receiving push notifications from a given device, delete the corresponding
  "Android Device" contact method in your GoAlert profile, which removes its registration on
  the server.
- The FCM token itself can be invalidated at any time by uninstalling the app or by signing
  out of Google Play Services.

## Children

GoAlert Android is an on-call alerting tool intended for adults in operational/IT roles. It
is not directed at children under 13 and does not knowingly collect data from them.

## Open source

The full source of GoAlert Android is published under the Apache License 2.0:
<https://github.com/rophy/goalert-android>. You can audit what the app does at any time.

## Changes to this policy

If this policy changes, the updated version will be committed to the repository above with a
new "Last updated" date. Significant changes will be noted in the release notes.

## Contact

Questions or requests about this policy:

- Open an issue: <https://github.com/rophy/goalert-android/issues>
