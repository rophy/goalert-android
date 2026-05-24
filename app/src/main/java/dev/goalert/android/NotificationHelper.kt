package dev.goalert.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_CRITICAL = "alerts_critical"
    const val CHANNEL_STATUS = "alerts_status"
    const val CHANNEL_OTHER = "alerts_other"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val critical = NotificationChannel(
            CHANNEL_CRITICAL,
            context.getString(R.string.channel_alerts_critical),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setBypassDnd(true)
        }

        val status = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(R.string.channel_alerts_status),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val other = NotificationChannel(
            CHANNEL_OTHER,
            context.getString(R.string.channel_alerts_other),
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannels(listOf(critical, status, other))
    }

    fun showNotification(context: Context, data: Map<String, String>) {
        val type = data["type"] ?: return
        // Prefer the URL from the push, but fall back to the configured instance so deep
        // links still work when the server omits instance_url.
        val instanceUrl = data["instance_url"]?.takeIf { it.isNotEmpty() }
            ?: TokenManager.getInstanceUrl(context)
            ?: ""

        val (channel, title, body) = when (type) {
            "alert" -> Triple(
                CHANNEL_CRITICAL,
                data["service_name"] ?: "Alert",
                data["summary"] ?: "New alert"
            )
            "alert_bundle" -> Triple(
                CHANNEL_CRITICAL,
                data["service_name"] ?: "Alerts",
                "${data["count"] ?: "Multiple"} unacknowledged alerts"
            )
            "alert_status" -> Triple(
                CHANNEL_STATUS,
                "Alert Update",
                data["summary"] ?: "Alert status changed"
            )
            "verification" -> Triple(
                CHANNEL_CRITICAL,
                "Verification Code",
                data["code"]?.takeIf { it.isNotEmpty() }
                    ?.let { "Your verification code is: $it" }
                    ?: "Verification code received"
            )
            "test" -> Triple(
                CHANNEL_OTHER,
                "Test Notification",
                "GoAlert push notifications are working"
            )
            else -> return
        }

        val deepLinkUrl = when (type) {
            "alert", "alert_status" -> "$instanceUrl/alerts/${data["alert_id"]}"
            else -> instanceUrl
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link_url", deepLinkUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(
                if (channel == CHANNEL_CRITICAL) NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_STATUS
            )
            .setPriority(
                if (channel == CHANNEL_CRITICAL) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        val notificationId = (data["alert_id"]?.hashCode() ?: System.currentTimeMillis().toInt())
        manager.notify(notificationId, notification)
    }
}
