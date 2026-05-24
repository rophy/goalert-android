package dev.goalert.android

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.isNotEmpty()) {
            NotificationHelper.showNotification(this, message.data)
        }
    }

    override fun onNewToken(token: String) {
        TokenManager.onTokenRefresh(this, token)
    }
}
