package dev.goalert.android

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
        val instanceUrl = TokenManager.getInstanceUrl(this) ?: return
        val deepLink = safeDeepLink(intent.getStringExtra("deep_link_url"), instanceUrl)
        if (deepLink != null && ::webView.isInitialized) {
            webView.loadUrl(deepLink)
        }
    }

    /**
     * Returns [deepLink] only if it is an absolute URL whose scheme, host and port match the
     * configured GoAlert instance. Since [MainActivity] is exported, any installed app can launch
     * it with a `deep_link_url` extra; without this check a malicious app could load an arbitrary
     * page — or a `javascript:` URL that runs in the authenticated GoAlert origin — into the WebView.
     */
    private fun safeDeepLink(deepLink: String?, instanceUrl: String): String? {
        if (deepLink == null) return null
        return try {
            val link = Uri.parse(deepLink)
            val base = Uri.parse(instanceUrl)
            val sameOrigin = link.scheme?.equals(base.scheme, ignoreCase = true) == true &&
                link.host?.equals(base.host, ignoreCase = true) == true &&
                link.port == base.port
            if (sameOrigin) deepLink else null
        } catch (e: Exception) {
            null
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
                // Retry FCM registration now that a session cookie likely exists. The first
                // attempt in setupWebView() runs before login; TokenManager de-dupes once the
                // contact method is created, so this is a no-op after it succeeds.
                registerFCMToken()
            }
        }

        val deepLink = safeDeepLink(intent.getStringExtra("deep_link_url"), instanceUrl)
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
