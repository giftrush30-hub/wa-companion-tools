package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Supabase REST/Auth client. Only the publishable key is shipped in the app.
 * Network calls must be made off the UI thread.
 */
class DiscoverBackend(context: Context) {
    private val prefs = context.getSharedPreferences("wa_discover_session", Context.MODE_PRIVATE)
    private val baseUrl = "https://coodsyfdhjpqfxanqhwm.supabase.co"
    private val publishableKey = "sb_publishable_JqE_4yBgTnkp1n6I2j-l2A_tKxKAC9H"

    data class SellerSession(val accessToken: String, val userId: String)

    fun currentSession(): SellerSession? {
        val token = prefs.getString("access_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        return SellerSession(token, userId)
    }

    fun signUp(email: String, password: String): JSONObject =
        request("/auth/v1/signup", "POST", JSONObject()
            .put("email", email).put("password", password), null) as JSONObject

    fun signIn(email: String, password: String): SellerSession {
        val result = request("/auth/v1/token?grant_type=password", "POST",
            JSONObject().put("email", email).put("password", password), null) as JSONObject
        val token = result.optString("access_token")
        val userId = result.optJSONObject("user")?.optString("id").orEmpty()
        if (token.isBlank() || userId.isBlank()) {
            throw IllegalStateException("Sign-in did not return a session. Confirm your email if Supabase requests it, then sign in again.")
        }
        prefs.edit().putString("access_token", token)
            .putString("refresh_token", result.optString("refresh_token"))
            .putString("user_id", userId).apply()
        return SellerSession(token, userId)
    }

    fun signOut() {
        val token = prefs.getString("access_token", null)
        if (!token.isNullOrBlank()) {
            try { request("/auth/v1/logout", "POST", JSONObject(), token) } catch (_: Exception) {}
        }
        prefs.edit().clear().apply()
    }

    fun searchBusinesses(term: String = "", state: String = "", city: String = ""): JSONArray {
        val filters = mutableListOf("select=id,name,description,category_id,whatsapp_number,catalogue_url,state,city")
        filters += "approval_status=eq.approved"
        if (term.isNotBlank()) {
            val safe = term.trim().replace(",", " ").replace("%", "").replace("*", "")
            filters += "or=(name.ilike.*$safe*,description.ilike.*$safe*)"
        }
        if (state.isNotBlank()) filters += "state=ilike.*${encode(state.trim())}*"
        if (city.isNotBlank()) filters += "city=ilike.*${encode(city.trim())}*"
        filters += "order=name.asc"
        return request("/rest/v1/discover_businesses?${filters.joinToString("&")}", "GET", null, null)
            .let { JSONArray(it.toString()) }
    }

    fun submitBusiness(session: SellerSession, values: JSONObject): JSONObject {
        val payload = JSONObject(values.toString()).put("owner_id", session.userId)
            .put("approval_status", "pending")
        val result = request("/rest/v1/discover_businesses", "POST", payload, session.accessToken, true)
        return if (result is JSONArray && result.length() > 0) result.getJSONObject(0) else JSONObject()
    }

    fun categories(): JSONArray {
        val result = request("/rest/v1/discover_categories?select=id,name&active=eq.true&order=sort_order.asc",
            "GET", null, null)
        return JSONArray(result.toString())
    }

    private fun request(path: String, method: String, body: JSONObject?, token: String?, returnRepresentation: Boolean = false): Any {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Authorization", "Bearer ${token ?: publishableKey}")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (returnRepresentation) setRequestProperty("Prefer", "return=representation")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try { JSONObject(text).optString("message").ifBlank { JSONObject(text).optString("msg") } } catch (_: Exception) { text }
                throw IllegalStateException(message.ifBlank { "Supabase request failed ($code)." })
            }
            if (text.isBlank()) return JSONObject()
            return if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
}
