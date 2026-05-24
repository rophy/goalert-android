package dev.goalert.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannels(this)

        val instanceUrl = TokenManager.getInstanceUrl(this)
        if (instanceUrl == null) {
            showInstanceUrlDialog()
        } else {
            setupWebView(instanceUrl)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val deepLink = intent.getStringExtra("deep_link_url")
        if (deepLink != null && ::webView.isInitialized) {
            webView.loadUrl(deepLink)
        }
    }

    private fun showInstanceUrlDialog() {
        val input = EditText(this).apply {
            hint = "https://goalert.example.com"
            setPadding(48, 32, 48, 32)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("GoAlert Instance URL")
            .setMessage("Enter the URL of your GoAlert instance:")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Connect") { _, _ ->
                var url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    if (!url.startsWith("http")) url = "https://$url"
                    url = url.trimEnd('/')
                    TokenManager.setInstanceUrl(this, url)
                    setupWebView(url)
                }
            }
            .show()
    }

    private fun setupWebView(instanceUrl: String) {
        webView = WebView(this)
        setContentView(webView)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
            }
        }

        val deepLink = intent.getStringExtra("deep_link_url")
        webView.loadUrl(deepLink ?: instanceUrl)

        requestNotificationPermission()
        registerFCMToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                )
            }
        }
    }

    private fun registerFCMToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            TokenManager.registerToken(this, token)
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
