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
        val templateCredit: String?,
        val endsAt: String?,
        val category: String = "Everyday Naija"
    )
    data class Submission(
        val id: String,
        val caption: String,
        val imageUrl: String?,
        val createdAt: String,
        val votes: Long,
        val votedByMe: Boolean
    )
    data class PendingSubmission(val id: String, val caption: String, val challengeTitle: String, val createdAt: String)
    data class ArenaReport(
        val id: String,
        val targetType: String,
        val targetId: String,
        val reason: String,
        val details: String?,
        val status: String,
        val createdAt: String
    )

    fun pendingReports(): List<ArenaReport> {
        val session = requireSession()
        val rows = discover.request(
            "/rest/v1/arena_reports?select=id,target_type,target_id,reason,details,status,created_at&status=in.(open,reviewing)&order=created_at.asc&limit=100",
            "GET", null, session.accessToken
        ) as JSONArray
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            ArenaReport(
                row.getString("id"), row.getString("target_type"), row.getString("target_id"),
                row.getString("reason"), row.optString("details").takeIf { it.isNotBlank() && it != "null" },
                row.getString("status"), row.optString("created_at")
            )
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        val session = requireSession()
        if (!isAdmin()) throw IllegalStateException("This review area is for authorised moderators only.")
        if (status !in setOf("reviewing", "resolved", "dismissed")) {
            throw IllegalArgumentException("Invalid moderation status.")
        }
        discover.request(
            "/rest/v1/arena_reports?id=eq.$reportId",
            "PATCH", JSONObject().put("status", status), session.accessToken
        )
    }

    fun isAdmin(): Boolean {
        val token = discover.currentSession()?.accessToken ?: return false
        return try {
            val payload = token.split(".").getOrNull(1) ?: return false
            val decoded = android.util.Base64.decode(
                payload.replace('-', '+').replace('_', '/') +
                    "=".repeat((4 - payload.length % 4) % 4),
                android.util.Base64.DEFAULT
            )
            JSONObject(String(decoded, Charsets.UTF_8))
                .optJSONObject("app_metadata")
                ?.optString("role") == "admin"
        } catch (_: Exception) { false }
    }

    fun pendingSubmissions(): List<PendingSubmission> {
        val session = requireSession()
        val rows = discover.request(
            "/rest/v1/arena_meme_submissions?select=id,caption,created_at,challenge_id&status=eq.pending&order=created_at.asc&limit=100",
            "GET", null, session.accessToken
        ) as JSONArray
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val challengeRows = discover.request(
                "/rest/v1/arena_meme_challenges?select=title&id=eq.${row.getString("challenge_id")}&limit=1",
                "GET", null, session.accessToken
            ) as JSONArray
            PendingSubmission(
                row.getString("id"),
                row.getString("caption"),
                if (challengeRows.length() > 0) challengeRows.getJSONObject(0).optString("title") else "Meme War",
                row.optString("created_at")
            )
        }
    }

    fun moderate(submissionId: String, approved: Boolean) {
        val session = requireSession()
        if (!isAdmin()) throw IllegalStateException("This moderation area is for authorised moderators only.")
        discover.request(
            "/rest/v1/arena_meme_submissions?id=eq.$submissionId",
            "PATCH",
            JSONObject().put("status", if (approved) "approved" else "rejected"),
            session.accessToken
        )
    }

    fun activeChallenge(excludeChallengeId: String? = null): Challenge? {
        val rows = discover.request(
            "/rest/v1/arena_meme_challenges?select=id,title,instructions,template_image_url,template_credit,ends_at,starts_at,category&status=eq.published&order=created_at.desc&limit=100",
            "GET", null, null
        ) as JSONArray
        if (rows.length() == 0) return null
        val session = discover.currentSession()
        val submittedIds = if (session == null) emptySet() else {
            val submitted = discover.request(
                "/rest/v1/arena_meme_submissions?select=challenge_id&user_id=eq.${session.userId}&limit=500",
                "GET", null, session.accessToken
            ) as JSONArray
            (0 until submitted.length()).map { submitted.getJSONObject(it).getString("challenge_id") }.toSet()
        }
        val now = java.time.Instant.now().toString()
        val available = (0 until rows.length()).map { rows.getJSONObject(it) }.filter { row ->
            val id = row.getString("id")
            val start = row.optString("starts_at")
            val end = row.optString("ends_at")
            val started = start.isBlank() || start == "null" || start.replace(" ", "T") <= now
            val notEnded = end.isBlank() || end == "null" || end.replace(" ", "T") > now
            started && notEnded && id !in submittedIds && id != excludeChallengeId
        }
        val fallback = (0 until rows.length()).map { rows.getJSONObject(it) }
            .filter { it.getString("id") != excludeChallengeId }
        val row = (if (available.isNotEmpty()) available else fallback).shuffled().firstOrNull() ?: return null
        return Challenge(
            row.getString("id"),
            row.getString("title"),
            row.optString("instructions"),
            row.optString("template_image_url").takeIf { it.isNotBlank() && it != "null" },
            row.optString("template_credit").takeIf { it.isNotBlank() && it != "null" },
            row.optString("ends_at").takeIf { it.isNotBlank() && it != "null" },
            row.optString("category", "Everyday Naija")
        )
    }

    fun mySubmission(challengeId: String): JSONObject? {
        val session = requireSession()
        val rows = discover.request(
            "/rest/v1/arena_meme_submissions?select=id,caption,image_url,status,created_at&challenge_id=eq.$challengeId&user_id=eq.${session.userId}&limit=1",
            "GET", null, session.accessToken
        ) as JSONArray
        return if (rows.length() == 0) null else rows.getJSONObject(0)
    }

    fun submitCaption(challengeId: String, caption: String, imageUrl: String? = null) {
        val session = requireSession()
        val clean = caption.trim()
        if (clean.length !in 1..500) throw IllegalArgumentException("Your caption must be between 1 and 500 characters.")
        discover.request(
            "/rest/v1/arena_meme_submissions",
            "POST",
            JSONObject().put("challenge_id", challengeId)
                .put("user_id", session.userId)
                .put("caption", clean)
                .put("status", "pending")
                .also { if (!imageUrl.isNullOrBlank()) it.put("image_url", imageUrl) },
            session.accessToken
        )
    }

    fun approvedSubmissions(challengeId: String): List<Submission> {
        val session = requireSession()
        val rows = discover.request(
            "/rest/v1/arena_meme_submissions?select=id,caption,image_url,created_at&challenge_id=eq.$challengeId&status=eq.approved&order=created_at.desc&limit=100",
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
            Submission(id, row.getString("caption"), row.optString("image_url").takeIf { it.isNotBlank() && it != "null" },
                row.optString("created_at"), counts[id] ?: 0L, id in myVotes)
        }.sortedByDescending { it.votes }
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
