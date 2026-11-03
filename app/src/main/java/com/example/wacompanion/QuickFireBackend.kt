package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Supabase-backed Quick Fire. Correct answers stay in the private schema. */
class QuickFireBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)

    data class Quiz(val id: String, val title: String, val description: String, val questions: List<Question>)
    data class Question(val id: String, val text: String, val options: List<String>, val points: Int)
    data class AnswerResult(
        val correct: Boolean,
        val correctIndex: Int,
        val explanation: String,
        val alreadyAnswered: Boolean
    )

    fun activeQuiz(): Quiz? {
        val sets = discover.request(
            "/rest/v1/arena_quickfire_sets?select=id,title,description&status=eq.published&order=created_at.desc&limit=1",
            "GET", null, null
        ) as JSONArray
        if (sets.length() == 0) return null
        val row = sets.getJSONObject(0)
        val setId = row.getString("id")
        val rows = discover.request(
            "/rest/v1/arena_quickfire_questions?select=id,question_text,options,position,points&set_id=eq.$setId&order=position.asc",
            "GET", null, null
        ) as JSONArray
        val questions = (0 until rows.length()).map { index ->
            val item = rows.getJSONObject(index)
            val choices = item.getJSONArray("options")
            Question(item.getString("id"), item.getString("question_text"),
                (0 until choices.length()).map { choices.getString(it) },
                item.optInt("points", 10))
        }
        return Quiz(setId, row.getString("title"), row.optString("description"), questions)
    }

    fun submit(questionId: String, selectedIndex: Int): AnswerResult {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to play Quick Fire. Open Discover, sign in, then return to Daily Arena.")
        val response = discover.request(
            "/rest/v1/rpc/arena_quickfire_submit_answer",
            "POST",
            JSONObject().put("p_question_id", questionId)
                .put("p_selected_option_index", selectedIndex),
            session.accessToken
        ) as JSONArray
        if (response.length() == 0) throw IllegalStateException("No answer result was returned.")
        val row = response.getJSONObject(0)
        return AnswerResult(
            row.optBoolean("is_correct"),
            row.optInt("correct_option_index"),
            row.optString("explanation"),
            row.optBoolean("already_answered")
        )
    }
}
