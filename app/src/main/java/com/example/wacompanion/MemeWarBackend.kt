package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Meme War: caption challenges, moderated entries, one vote per player, and safe aggregate counts. */
class MemeWarBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)

    data class Challenge(
        val id: String,
        val title: String,
        val instructions: String,
        val templateImageUrl: String?,
        val endsAt: String?
    )
    data class Submission(
        val id: String,
        val caption: String,
        val createdAt: String,
        val votes: Long,
        val votedByMe: Boolean
    )

    fun activeChallenge(): Challenge? {
        val rows = discover.request(
            "/rest/v1/arena_meme_challenges?select=id,title,instructions,template_image_url,ends_at&status=eq.published&order=created_at.desc&limit=1",
            "GET", null, null
        ) as JSONArray
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        return Challenge(
            row.getString("id"),
            row.getString("title"),
            row.optString("instructions"),
            row.optString("template_image_url").takeIf { it.isNotBlank() && it != "null" },
            row.optString("ends_at").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    fun mySubmission(challengeId: String): JSONObject? {
        val session = requireSession()
        val rows = discover.request(
            "/rest/v1/arena_meme_submissions?select=id,caption,status,created_at&challenge_id=eq.$challengeId&user_id=eq.${session.userId}&limit=1",
            "GET", null, session.accessToken
        ) as JSONArray
        return if (rows.length() == 0) null else rows.getJSONObject(0)
    }

    fun submitCaption(challengeId: String, caption: String) {
        val session = requireSession()
        val clean = caption.trim()
        if (clean.length !in 1..500) throw IllegalArgumentException("Your caption must be between 1 and 500 characters.")
        discover.request(
            "/rest/v1/arena_meme_submissions",
            "POST",
            JSONObject().put("challenge_id", challengeId)
                .put("user_id", session.userId)
                .put("caption", clean)
                .put("status", "pending"),
            session.accessToken
        )
    }

    fun approvedSubmissions(challengeId: String): List<Submission> {
        val session = requireSession()
        val rows = discover.request(
            "/rest/v1/arena_meme_submissions?select=id,caption,created_at&challenge_id=eq.$challengeId&status=eq.approved&order=created_at.desc&limit=100",
            "GET", null, session.accessToken
        ) as JSONArray
        val countsJson = discover.request(
            "/rest/v1/rpc/arena_meme_vote_counts",
            "POST", JSONObject().put("p_challenge_id", challengeId), session.accessToken
        ) as JSONArray
        val counts = (0 until countsJson.length()).associate {
            val item = countsJson.getJSONObject(it)
            item.getString("submission_id") to item.optLong("vote_count")
        }
        val votesJson = discover.request(
            "/rest/v1/arena_meme_votes?select=submission_id&user_id=eq.${session.userId}",
            "GET", null, session.accessToken
        ) as JSONArray
        val myVotes = (0 until votesJson.length()).map { votesJson.getJSONObject(it).getString("submission_id") }.toSet()
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            val id = row.getString("id")
            Submission(id, row.getString("caption"), row.optString("created_at"),
                counts[id] ?: 0L, id in myVotes)
        }
    }

    fun vote(submissionId: String) {
        val session = requireSession()
        discover.request(
            "/rest/v1/arena_meme_votes",
            "POST",
            JSONObject().put("submission_id", submissionId).put("user_id", session.userId),
            session.accessToken
        )
    }

    fun report(submissionId: String, reason: String, details: String?) {
        val session = requireSession()
        val payload = JSONObject()
            .put("reporter_id", session.userId)
            .put("target_type", "meme_submission")
            .put("target_id", submissionId)
            .put("reason", reason)
        if (!details.isNullOrBlank()) payload.put("details", details.trim().take(1000))
        discover.request("/rest/v1/arena_reports", "POST", payload, session.accessToken)
    }

    private fun requireSession() = discover.currentSession()
        ?: throw IllegalStateException("Sign in first. Open Discover, sign in, then return to Meme War.")
}
