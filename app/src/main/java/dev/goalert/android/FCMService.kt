package dev.goalert.android

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return
        when (data["type"]) {
            // Actionable alerts ring unless the user opted out.
            "alert", "alert_bundle" -> ringOrNotify(data)
            else -> NotificationHelper.showNotification(this, data)
        }
    }

    private fun ringOrNotify(data: Map<String, String>) {
        if (!TokenManager.isRingEnabled(this)) {
            NotificationHelper.showNotification(this, data)
            return
        }
        // With overlay permission we can launch the ring immediately, even while the screen is
        // on/unlocked. Otherwise fall back to a full-screen-intent notification (rings when
        // locked, heads-up when unlocked).
        val ringIntent = NotificationHelper.ringActivityIntent(this, data)
        if (ringIntent != null && NotificationHelper.canDrawOverlays(this)) {
            startActivity(ringIntent)
        } else {
            NotificationHelper.showRingingAlert(this, data)
        }
    }

    override fun onNewToken(token: String) {
        TokenManager.onTokenRefresh(this, token)
    }
}
