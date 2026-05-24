package dev.goalert.android

import android.content.Context
import android.webkit.CookieManager
import androidx.preference.PreferenceManager
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object TokenManager {

    private const val PREF_INSTANCE_URL = "instance_url"
    private const val PREF_FCM_TOKEN = "fcm_token"
    private const val PREF_CONTACT_METHOD_ID = "contact_method_id"

    fun getInstanceUrl(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_INSTANCE_URL, null)
    }

    fun setInstanceUrl(context: Context, url: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_INSTANCE_URL, url).apply()
    }

    fun registerToken(context: Context, token: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val oldToken = prefs.getString(PREF_FCM_TOKEN, null)
        if (oldToken == token) return

        prefs.edit().putString(PREF_FCM_TOKEN, token).apply()

        val instanceUrl = getInstanceUrl(context) ?: return
        val existingId = prefs.getString(PREF_CONTACT_METHOD_ID, null)

        thread {
            if (existingId != null) {
                updateContactMethod(context, instanceUrl, existingId, token)
            } else {
                createContactMethod(context, instanceUrl, token)
            }
        }
    }

    fun onTokenRefresh(context: Context, token: String) {
        registerToken(context, token)
    }

    private fun createContactMethod(context: Context, instanceUrl: String, token: String) {
        val query = """
            mutation(${'$'}token: String!) {
              createUserContactMethod(input: {
                name: "Android Device"
                dest: {
                  type: "builtin-fcm-push"
                  args: { device_token: ${'$'}token }
                }
              }) { id }
            }
        """.trimIndent()

        val variables = JSONObject().put("token", token)
        val id = executeGraphQL(instanceUrl, query, variables, "createUserContactMethod")
        if (id != null) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(PREF_CONTACT_METHOD_ID, id).apply()
        }
    }

    private fun updateContactMethod(context: Context, instanceUrl: String, id: String, token: String) {
        val query = """
            mutation(${'$'}id: ID!, ${'$'}token: String!) {
              updateUserContactMethod(input: {
                id: ${'$'}id
                name: "Android Device"
                dest: {
                  type: "builtin-fcm-push"
                  args: { device_token: ${'$'}token }
                }
              }) { id }
            }
        """.trimIndent()

        val variables = JSONObject().put("id", id).put("token", token)
        executeGraphQL(instanceUrl, query, variables, "updateUserContactMethod")
    }

    private fun executeGraphQL(
        instanceUrl: String,
        query: String,
        variables: JSONObject,
        operationPath: String
    ): String? {
        try {
            val url = URL("$instanceUrl/api/graphql")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")

            val cookies = CookieManager.getInstance().getCookie(instanceUrl)
            if (cookies != null) {
                conn.setRequestProperty("Cookie", cookies)
            }

            conn.doOutput = true
            val body = JSONObject()
                .put("query", query)
                .put("variables", variables)
                .toString()
            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                val result = data?.optJSONObject(operationPath)
                return result?.optString("id")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
