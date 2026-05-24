package dev.goalert.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_CRITICAL = "alerts_critical"
    const val CHANNEL_STATUS = "alerts_status"

    // Removed in the two-channel model; deleted from existing installs in createChannels().
    private const val LEGACY_CHANNEL_OTHER = "alerts_other"

    const val RING_NOTIFICATION_ID = 911911

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // Actionable: alerts and verification codes. Requests DND bypass (user must grant it).
        val critical = NotificationChannel(
            CHANNEL_CRITICAL,
            context.getString(R.string.channel_alerts_critical),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setBypassDnd(true)
        }

        // Informational: alert status changes and test messages. Never bypasses DND.
        val status = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(R.string.channel_alerts_status),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        manager.createNotificationChannels(listOf(critical, status))
        manager.deleteNotificationChannel(LEGACY_CHANNEL_OTHER)
    }

    /** True if the user has granted DND override for the critical channel. */
    fun criticalDndBypassEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(CHANNEL_CRITICAL)?.canBypassDnd() == true
    }

    /** Intent that opens the system settings page for the critical channel. */
    fun criticalChannelSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_CRITICAL)
        }
    }

    /** True if the app may launch full-screen intents (always true before Android 14). */
    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else {
            true
        }
    }

    /** Settings page (Android 14+) where the user grants the full-screen-intent permission. */
    fun fullScreenIntentSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Posts a high-priority full-screen-intent notification that launches [AlertRingActivity],
     * producing a ringing, lock-screen alert for actionable alert types.
     */
    fun showRingingAlert(context: Context, data: Map<String, String>) {
        val type = data["type"] ?: return
        val instanceUrl = data["instance_url"]?.takeIf { it.isNotEmpty() }
            ?: TokenManager.getInstanceUrl(context)
            ?: ""

        val (title, body) = when (type) {
            "alert" -> (data["service_name"] ?: "Alert") to (data["summary"] ?: "New alert")
            "alert_bundle" -> (data["service_name"] ?: "Alerts") to
                "${data["count"] ?: "Multiple"} unacknowledged alerts"
            else -> return
        }
        val deepLink = if (type == "alert") "$instanceUrl/alerts/${data["alert_id"]}" else instanceUrl

        val ringIntent = Intent(context, AlertRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlertRingActivity.EXTRA_TITLE, title)
            putExtra(AlertRingActivity.EXTRA_BODY, body)
            putExtra(AlertRingActivity.EXTRA_DEEP_LINK, deepLink)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, RING_NOTIFICATION_ID, ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CRITICAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(RING_NOTIFICATION_ID, notification)
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
                CHANNEL_STATUS,
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
