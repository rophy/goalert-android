package dev.goalert.android

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return
        when (data["type"]) {
            // Actionable alerts ring with a full-screen, lock-screen UI unless the user opted out.
            "alert", "alert_bundle" ->
                if (TokenManager.isRingEnabled(this)) {
                    NotificationHelper.showRingingAlert(this, data)
                } else {
                    NotificationHelper.showNotification(this, data)
                }
            else -> NotificationHelper.showNotification(this, data)
        }
    }

    override fun onNewToken(token: String) {
        TokenManager.onTokenRefresh(this, token)
    }
}
