package dev.goalert.android

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

/**
 * Full-screen "ringing" alert shown over the lock screen. Loops an alarm-stream sound and
 * vibration until the user opens the alert (which silences it). The user is expected to
 * acknowledge in the GoAlert web UI to stop further alerts.
 */
class AlertRingActivity : AppCompatActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContentView(R.layout.activity_alert_ring)

        bindContent(intent)
        findViewById<Button>(R.id.ring_open).setOnClickListener { openAlert() }
        startRinging()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A newer alert arrived while ringing; refresh the UI and restart the ring.
        setIntent(intent)
        bindContent(intent)
        stopRinging()
        startRinging()
    }

    private fun bindContent(intent: Intent) {
        findViewById<TextView>(R.id.ring_title).text =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.ring_heading)
        findViewById<TextView>(R.id.ring_body).text = intent.getStringExtra(EXTRA_BODY).orEmpty()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startRinging() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlertRingActivity, uri)
            isLooping = true
            prepare()
            start()
        }

        vibrator = currentVibrator().also {
            val pattern = longArrayOf(0, 700, 700)
            it.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun stopRinging() {
        player?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun openAlert() {
        stopRinging()
        NotificationManagerCompat.from(this).cancel(NotificationHelper.RING_NOTIFICATION_ID)

        val deepLink = intent.getStringExtra(EXTRA_DEEP_LINK)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (deepLink != null) putExtra("deep_link_url", deepLink)
            }
        )
        finish()
    }

    private fun currentVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "ring_title"
        const val EXTRA_BODY = "ring_body"
        const val EXTRA_DEEP_LINK = "ring_deep_link"
    }
}
