package dev.goalert.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var dndStatus: TextView
    private lateinit var dndFix: Button

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

        dndStatus = findViewById(R.id.dnd_status)
        dndFix = findViewById(R.id.dnd_fix)
        dndFix.setOnClickListener {
            startActivity(NotificationHelper.criticalChannelSettingsIntent(this))
        }

        findViewById<Button>(R.id.test_critical).setOnClickListener {
            NotificationHelper.showTestCriticalNotification(this)
            Toast.makeText(this, R.string.settings_test_critical_sent, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDndStatus()
    }

    private fun refreshDndStatus() {
        if (NotificationHelper.criticalDndBypassEnabled(this)) {
            dndStatus.text = getString(R.string.settings_dnd_enabled)
            dndStatus.setTextColor(Color.parseColor("#2E7D32")) // green
            dndFix.visibility = View.GONE
        } else {
            dndStatus.text = getString(R.string.settings_dnd_disabled)
            dndStatus.setTextColor(Color.parseColor("#F9A825")) // amber
            dndFix.visibility = View.VISIBLE
        }
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
