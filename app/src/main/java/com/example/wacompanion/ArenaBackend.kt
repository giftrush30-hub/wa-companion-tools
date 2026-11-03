package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Live Hot Seat API. Uses DiscoverBackend's secure session and refresh handling. */
class ArenaBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)

    data class Poll(val id: String, val question: String, val options: List<Option>)
    data class Option(val id: String, val text: String)
    data class Result(val optionId: String, val count: Long)

    fun activePoll(): Poll? {
        val polls = discover.request(
            "/rest/v1/arena_hot_seat_polls?select=id,question&status=eq.published&order=created_at.desc&limit=1",
            "GET", null, null
        ) as JSONArray
        if (polls.length() == 0) return null
        val row = polls.getJSONObject(0)
        val id = row.getString("id")
        val optionsJson = discover.request(
            "/rest/v1/arena_hot_seat_options?select=id,option_text&poll_id=eq.$id&order=position.asc",
            "GET", null, null
        ) as JSONArray
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
        discover.request("/rest/v1/arena_hot_seat_votes", "POST", payload, session.accessToken)
    }

    fun results(pollId: String): List<Result> {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in to view poll results.")
        val rows = discover.request(
            "/rest/v1/rpc/arena_hot_seat_results",
            "POST", JSONObject().put("p_poll_id", pollId), session.accessToken
        ) as JSONArray
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            Result(row.getString("option_id"), row.optLong("vote_count"))
        }
    }
}
