package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Live Hot Seat API. Vote identities are never downloaded by the app. */
class ArenaBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)
    private val baseUrl = "https://coodsyfdhjpqfxanqhwm.supabase.co"
    private val publishableKey = "sb_publishable_JqE_4yBgTnkp1n6I2j-l2A_tKxKAC9H"

    data class Poll(val id: String, val question: String, val options: List<Option>)
    data class Option(val id: String, val text: String)
    data class Result(val optionId: String, val count: Long)

    fun activePoll(): Poll? {
        val polls = get("/rest/v1/arena_hot_seat_polls?select=id,question&status=eq.published&order=created_at.desc&limit=1", null) as JSONArray
        if (polls.length() == 0) return null
        val row = polls.getJSONObject(0)
        val id = row.getString("id")
        val optionsJson = get("/rest/v1/arena_hot_seat_options?select=id,option_text&poll_id=eq.$id&order=position.asc", null) as JSONArray
        val options = (0 until optionsJson.length()).map {
            val item = optionsJson.getJSONObject(it)
            Option(item.getString("id"), item.getString("option_text"))
        }
        return Poll(id, row.getString("question"), options)
    }

    fun vote(pollId: String, optionId: String) {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to vote. Open Discover, sign in, then return to Daily Arena.")
        val payload = JSONObject().put("poll_id", pollId).put("option_id", optionId).put("user_id", session.userId)
        post("/rest/v1/arena_hot_seat_votes", payload, session.accessToken)
    }

    fun results(pollId: String): List<Result> {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in to view poll results.")
        val rows = post("/rest/v1/rpc/arena_hot_seat_results", JSONObject().put("p_poll_id", pollId), session.accessToken) as JSONArray
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            Result(row.getString("option_id"), row.optLong("vote_count"))
        }
    }

    private fun get(path: String, token: String?): Any = request(path, "GET", null, token)
    private fun post(path: String, body: JSONObject, token: String): Any = request(path, "POST", body, token)

    private fun request(path: String, method: String, body: JSONObject?, token: String?): Any {
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
                setRequestProperty("Prefer", "return=minimal")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try {
                    JSONObject(response).optString("message").ifBlank { JSONObject(response).optString("details") }
                } catch (_: Exception) { response }
                throw IllegalStateException(message.ifBlank { "Arena request failed ($code)." })
            }
            if (response.isBlank()) return JSONObject()
            return if (response.trimStart().startsWith("[")) JSONArray(response) else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}
