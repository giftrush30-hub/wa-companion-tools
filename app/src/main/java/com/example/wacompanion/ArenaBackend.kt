package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Live Hot Seat API. Uses DiscoverBackend's secure session and refresh handling. */
class ArenaBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)

    data class Poll(
        val id: String,
        val question: String,
        val options: List<Option>,
        val myVoteOptionId: String? = null,
        val topic: String = "General",
        val pollType: String = "poll"
    )
    data class Option(val id: String, val text: String)
    data class Result(val optionId: String, val count: Long)

    fun activePoll(excludePollId: String? = null): Poll? {
        val polls = discover.request(
            "/rest/v1/arena_hot_seat_polls?select=id,question,topic,poll_type,starts_at,ends_at&status=eq.published&order=created_at.desc&limit=100",
            "GET", null, null
        ) as JSONArray
        if (polls.length() == 0) return null
        val session = discover.currentSession()
        val votedIds = if (session == null) emptySet() else {
            val votes = discover.request(
                "/rest/v1/arena_hot_seat_votes?select=poll_id&user_id=eq.${session.userId}&limit=500",
                "GET", null, session.accessToken
            ) as JSONArray
            (0 until votes.length()).map { votes.getJSONObject(it).getString("poll_id") }.toSet()
        }
        val candidates = (0 until polls.length()).map { polls.getJSONObject(it) }
            .filter { row ->
                val id = row.getString("id")
                val starts = row.optString("starts_at")
                val ends = row.optString("ends_at")
                (starts.isBlank() || starts == "null" || starts.replace(" ", "T") <= java.time.Instant.now().toString()) &&
                    (ends.isBlank() || ends == "null" || ends.replace(" ", "T") > java.time.Instant.now().toString()) &&
                    id !in votedIds && id != excludePollId
            }
        val fallback = (0 until polls.length()).map { polls.getJSONObject(it) }
            .filter { it.getString("id") != excludePollId }
        val selected = (if (candidates.isNotEmpty()) candidates else fallback).shuffled().firstOrNull()
            ?: return null
        val id = selected.getString("id")
        val optionsJson = discover.request(
            "/rest/v1/arena_hot_seat_options?select=id,option_text&poll_id=eq.$id&order=position.asc",
            "GET", null, null
        ) as JSONArray
        val options = (0 until optionsJson.length()).map {
            val item = optionsJson.getJSONObject(it)
            Option(item.getString("id"), item.getString("option_text"))
        }
        val myVote = if (session == null) null else {
            val votes = discover.request(
                "/rest/v1/arena_hot_seat_votes?select=option_id&poll_id=eq.$id&user_id=eq.${session.userId}&limit=1",
                "GET", null, session.accessToken
            ) as JSONArray
            if (votes.length() == 0) null else votes.getJSONObject(0).getString("option_id")
        }
        return Poll(id, selected.getString("question"), options, myVote,
            selected.optString("topic", "General"), selected.optString("poll_type", "poll"))
    }

    fun vote(pollId: String, optionId: String) {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to vote. Open Discover, sign in, then return to Daily Arena.")
        val payload = JSONObject().put("poll_id", pollId).put("option_id", optionId).put("user_id", session.userId)
        discover.request("/rest/v1/arena_hot_seat_votes", "POST", payload, session.accessToken)
    }

    fun reportPoll(pollId: String, reason: String, details: String? = null) {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to report a question.")
        val payload = JSONObject()
            .put("reporter_id", session.userId)
            .put("target_type", "hot_seat_poll")
            .put("target_id", pollId)
            .put("reason", reason)
        if (!details.isNullOrBlank()) payload.put("details", details.trim().take(1000))
        discover.request("/rest/v1/arena_reports", "POST", payload, session.accessToken)
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
