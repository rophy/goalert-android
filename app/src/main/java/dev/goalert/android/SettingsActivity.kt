package dev.goalert.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var dndStatus: TextView
    private lateinit var dndHint: TextView
    private lateinit var fullScreenStatus: TextView
    private lateinit var fullScreenHint: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var overlayHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setTitle(R.string.settings_title)
            setDisplayHomeAsUpEnabled(true)
        }

        findViewById<TextView>(R.id.instance_url).text =
            TokenManager.getInstanceUrl(this) ?: "—"
        findViewById<Button>(R.id.change_instance).setOnClickListener { changeInstance() }

        findViewById<SwitchCompat>(R.id.ring_switch).apply {
            isChecked = TokenManager.isRingEnabled(this@SettingsActivity)
            setOnCheckedChangeListener { _, checked ->
                TokenManager.setRingEnabled(this@SettingsActivity, checked)
            }
        }

        dndStatus = findViewById(R.id.dnd_status)
        dndHint = findViewById(R.id.dnd_hint)
        findViewById<Button>(R.id.dnd_fix).setOnClickListener {
            startActivity(NotificationHelper.criticalChannelSettingsIntent(this))
        }

        fullScreenStatus = findViewById(R.id.fullscreen_status)
        fullScreenHint = findViewById(R.id.fullscreen_hint)
        findViewById<Button>(R.id.fullscreen_fix).setOnClickListener {
            startActivity(NotificationHelper.fullScreenIntentSettingsIntent(this))
        }

        overlayStatus = findViewById(R.id.overlay_status)
        overlayHint = findViewById(R.id.overlay_hint)
        findViewById<Button>(R.id.overlay_fix).setOnClickListener {
            startActivity(NotificationHelper.overlaySettingsIntent(this))
        }

        findViewById<Button>(R.id.test_critical).setOnClickListener { startTestRing() }
    }

    override fun onResume() {
        super.onResume()
        setStatus(
            dndStatus, dndHint,
            NotificationHelper.criticalDndBypassEnabled(this),
            R.string.settings_dnd_hint_on, R.string.settings_dnd_hint_off
        )
        setStatus(
            fullScreenStatus, fullScreenHint,
            NotificationHelper.canUseFullScreenIntent(this),
            R.string.settings_fullscreen_hint_on, R.string.settings_fullscreen_hint_off
        )
        setStatus(
            overlayStatus, overlayHint,
            NotificationHelper.canDrawOverlays(this),
            R.string.settings_overlay_hint_on, R.string.settings_overlay_hint_off
        )
    }

    private fun setStatus(status: TextView, hint: TextView, enabled: Boolean, hintOn: Int, hintOff: Int) {
        if (enabled) {
            status.setText(R.string.settings_status_enabled)
            status.setTextColor(Color.parseColor("#2E7D32")) // green
            hint.setText(hintOn)
        } else {
            status.setText(R.string.settings_status_disabled)
            status.setTextColor(Color.parseColor("#F9A825")) // amber
            hint.setText(hintOff)
        }
    }

    private fun startTestRing() {
        startActivity(
            Intent(this, AlertRingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlertRingActivity.EXTRA_TITLE, getString(R.string.settings_test_critical))
                putExtra(AlertRingActivity.EXTRA_BODY, "This is a test ring")
                putExtra(
                    AlertRingActivity.EXTRA_DEEP_LINK,
                    TokenManager.getInstanceUrl(this@SettingsActivity)
                )
            }
        )
    }

    private fun changeInstance() {
        TokenManager.clearInstance(this)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
