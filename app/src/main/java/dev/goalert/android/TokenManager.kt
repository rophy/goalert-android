package dev.goalert.android

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import androidx.preference.PreferenceManager
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object TokenManager {

    private const val TAG = "TokenManager"
    private const val PREF_INSTANCE_URL = "instance_url"
    private const val PREF_FCM_TOKEN = "fcm_token"
    private const val PREF_CONTACT_METHOD_ID = "contact_method_id"
    private const val PREF_DND_PROMPT_DISMISSED = "dnd_prompt_dismissed"

    fun getInstanceUrl(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_INSTANCE_URL, null)
    }

    fun setInstanceUrl(context: Context, url: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_INSTANCE_URL, url).apply()
    }

    /** Clears the instance URL and its per-instance registration so the user can reconnect. */
    fun clearInstance(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .remove(PREF_INSTANCE_URL)
            .remove(PREF_CONTACT_METHOD_ID)
            .remove(PREF_FCM_TOKEN)
            .apply()
    }

    fun isDndPromptDismissed(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_DND_PROMPT_DISMISSED, false)
    }

    fun setDndPromptDismissed(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putBoolean(PREF_DND_PROMPT_DISMISSED, true).apply()
    }

    fun registerToken(context: Context, token: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val existingId = prefs.getString(PREF_CONTACT_METHOD_ID, null)
        val registeredToken = prefs.getString(PREF_FCM_TOKEN, null)

        // Already registered this exact token with a known contact method — nothing to do.
        if (existingId != null && registeredToken == token) return

        val instanceUrl = getInstanceUrl(context) ?: return

        thread {
            val ok = if (existingId != null) {
                updateContactMethod(instanceUrl, existingId, token)
            } else {
                createContactMethod(context, instanceUrl, token)
            }
            // Only cache the token once the server has actually accepted it. A failed
            // attempt (e.g. fired before login, so no session cookie) leaves the token
            // uncached so the next registerToken() call retries instead of short-circuiting.
            if (ok) {
                prefs.edit().putString(PREF_FCM_TOKEN, token).apply()
            }
        }
    }

    fun onTokenRefresh(context: Context, token: String) {
        registerToken(context, token)
    }

    /** Returns the authenticated user's id, or null if it can't be fetched. */
    private fun fetchUserId(instanceUrl: String): String? {
        val data = executeGraphQL(instanceUrl, "query { user { id } }", JSONObject()) ?: return null
        return data.optJSONObject("user")?.optString("id")?.takeIf { it.isNotEmpty() }
    }

    /** Returns true if the contact method was created (and its id persisted). */
    private fun createContactMethod(context: Context, instanceUrl: String, token: String): Boolean {
        val userId = fetchUserId(instanceUrl) ?: return false
        val query = """
            mutation(${'$'}userID: ID!, ${'$'}token: String!) {
              createUserContactMethod(input: {
                userID: ${'$'}userID
                name: "Android Device"
                dest: {
                  type: "builtin-fcm-push"
                  args: { device_token: ${'$'}token }
                }
              }) { id }
            }
        """.trimIndent()

        val variables = JSONObject().put("userID", userId).put("token", token)
        val data = executeGraphQL(instanceUrl, query, variables) ?: return false
        val id = data.optJSONObject("createUserContactMethod")?.optString("id")
        if (id.isNullOrEmpty()) return false

        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_CONTACT_METHOD_ID, id).apply()
        return true
    }

    /** Returns true if the existing contact method was updated with the new token. */
    private fun updateContactMethod(instanceUrl: String, id: String, token: String): Boolean {
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
        return executeGraphQL(instanceUrl, query, variables) != null
    }

    /** Runs a GraphQL mutation; returns the `data` object on success (HTTP 200, no errors), else null. */
    private fun executeGraphQL(
        instanceUrl: String,
        query: String,
        variables: JSONObject
    ): JSONObject? {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$instanceUrl/api/graphql")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json")
                CookieManager.getInstance().getCookie(instanceUrl)?.let {
                    setRequestProperty("Cookie", it)
                }
                doOutput = true
            }

            val body = JSONObject()
                .put("query", query)
                .put("variables", variables)
                .toString()
            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val code = conn.responseCode
            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                Log.e(TAG, "GraphQL HTTP $code: $err")
                return null
            }
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            if (json.has("errors")) {
                Log.e(TAG, "GraphQL errors: ${json.getJSONArray("errors")}")
                return null
            }
            return json.optJSONObject("data")
        } catch (e: Exception) {
            Log.e(TAG, "GraphQL request failed", e)
            return null
        } finally {
            conn?.disconnect()
        }
    }
}
