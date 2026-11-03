package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Live Hot Seat API. Uses DiscoverBackend's secure session and refresh handling. */
class ArenaBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)
    private val rotationPrefs = context.applicationContext.getSharedPreferences("hot_seat_rotation", Context.MODE_PRIVATE)

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
    data class Suggestion(
        val id: String, val question: String, val topic: String,
        val options: List<String>, val votes: Long, val votedByMe: Boolean
    )

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
        val recentIds = rotationPrefs.getStringSet("recent_poll_ids", emptySet()) ?: emptySet()
        val eligible = (0 until polls.length()).map { polls.getJSONObject(it) }
            .filter { row ->
                val id = row.getString("id")
                val starts = row.optString("starts_at")
                val ends = row.optString("ends_at")
                (starts.isBlank() || starts == "null" || starts.replace(" ", "T") <= java.time.Instant.now().toString()) &&
                    (ends.isBlank() || ends == "null" || ends.replace(" ", "T") > java.time.Instant.now().toString()) &&
                    id !in votedIds && id != excludePollId
            }
        val fresh = eligible.filter { it.getString("id") !in recentIds }
        val fallback = (0 until polls.length()).map { polls.getJSONObject(it) }
            .filter { it.getString("id") != excludePollId }
        val selected = (if (fresh.isNotEmpty()) fresh else if (eligible.isNotEmpty()) eligible else fallback)
            .shuffled().firstOrNull() ?: return null
        val updatedRecent = (recentIds + selected.getString("id")).takeLast(12).toSet()
        rotationPrefs.edit().putStringSet("recent_poll_ids", updatedRecent).apply()
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

    fun submitSuggestion(question: String, topic: String, options: List<String>) {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to suggest a Hot Seat question.")
        val cleanQuestion = question.trim()
        val cleanOptions = options.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanQuestion.length !in 12..180) throw IllegalArgumentException("Question must be 12–180 characters.")
        if (cleanOptions.size !in 2..4) throw IllegalArgumentException("Add between 2 and 4 different answer options.")
        val payload = JSONObject()
            .put("user_id", session.userId)
            .put("question", cleanQuestion)
            .put("topic", topic.trim().ifBlank { "General" }.take(40))
            .put("options", JSONArray(cleanOptions))
            .put("status", "pending")
        discover.request("/rest/v1/arena_hot_seat_suggestions", "POST", payload, session.accessToken)
    }

    fun suggestions(): List<Suggestion> {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in to explore community questions.")
        val rows = discover.request(
            "/rest/v1/arena_hot_seat_suggestions?select=id,question,topic,options&status=eq.approved&order=created_at.desc&limit=40",
            "GET", null, session.accessToken
        ) as JSONArray
        if (rows.length() == 0) return emptyList()
        val ids = JSONArray().apply {
            for (i in 0 until rows.length()) put(rows.getJSONObject(i).getString("id"))
        }
        val countsJson = discover.request(
            "/rest/v1/rpc/arena_hot_seat_suggestion_vote_counts",
            "POST", JSONObject().put("p_ids", ids), session.accessToken
        ) as JSONArray
        val counts = (0 until countsJson.length()).associate {
            val row = countsJson.getJSONObject(it)
            row.getString("suggestion_id") to row.optLong("vote_count")
        }
        val mineJson = discover.request(
            "/rest/v1/arena_hot_seat_suggestion_votes?select=suggestion_id&user_id=eq.${session.userId}&limit=500",
            "GET", null, session.accessToken
        ) as JSONArray
        val mine = (0 until mineJson.length()).map { mineJson.getJSONObject(it).getString("suggestion_id") }.toSet()
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            val options = row.optJSONArray("options") ?: JSONArray()
            Suggestion(
                row.getString("id"), row.getString("question"), row.optString("topic"),
                (0 until options.length()).map { index -> options.getString(index) },
                counts[row.getString("id")] ?: 0L, row.getString("id") in mine
            )
        }.sortedByDescending { it.votes }
    }

    fun voteSuggestion(suggestionId: String) {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in to vote for community questions.")
        discover.request(
            "/rest/v1/arena_hot_seat_suggestion_votes", "POST",
            JSONObject().put("suggestion_id", suggestionId).put("user_id", session.userId),
            session.accessToken
        )
    }

    fun pendingSuggestions(): List<Suggestion> {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first.")
        if (!isAdmin()) throw IllegalStateException("Moderator access required.")
        val rows = discover.request(
            "/rest/v1/arena_hot_seat_suggestions?select=id,question,topic,options,status&status=eq.pending&order=created_at.asc&limit=100",
            "GET", null, session.accessToken
        ) as JSONArray
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            val options = row.optJSONArray("options") ?: JSONArray()
            Suggestion(row.getString("id"), row.getString("question"), row.optString("topic"),
                (0 until options.length()).map { index -> options.getString(index) }, 0L, false)
        }
    }

    fun moderateSuggestion(id: String, approved: Boolean) {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first.")
        if (!isAdmin()) throw IllegalStateException("Moderator access required.")
        discover.request(
            "/rest/v1/arena_hot_seat_suggestions?id=eq.$id", "PATCH",
            JSONObject().put("status", if (approved) "approved" else "rejected"),
            session.accessToken
        )
    }

    private fun isAdmin(): Boolean {
        val token = discover.currentSession()?.accessToken ?: return false
        return try {
            val payload = token.split(".").getOrNull(1) ?: return false
            val decoded = android.util.Base64.decode(
                payload.replace('-', '+').replace('_', '/') +
                    "=".repeat((4 - payload.length % 4) % 4), android.util.Base64.DEFAULT
            )
            JSONObject(String(decoded, Charsets.UTF_8)).optJSONObject("app_metadata")?.optString("role") == "admin"
        } catch (_: Exception) { false }
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
