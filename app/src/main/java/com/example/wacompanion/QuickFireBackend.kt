package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Supabase-backed Quick Fire. Correct answers stay in the private schema. */
class QuickFireBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)
    private val rotationPrefs = context.applicationContext.getSharedPreferences("quickfire_rotation", Context.MODE_PRIVATE)

    data class Quiz(val id: String, val title: String, val description: String, val questions: List<Question>, val difficulty: String)
    data class Option(val text: String, val originalIndex: Int)
    data class Question(val id: String, val text: String, val options: List<Option>, val points: Int, val difficulty: String)
    data class AnswerResult(
        val correct: Boolean,
        val correctIndex: Int,
        val explanation: String,
        val alreadyAnswered: Boolean
    )

    fun activeQuiz(difficulty: String = "mixed"): Quiz? {
        val sets = discover.request(
            "/rest/v1/arena_quickfire_sets?select=id,title,description&status=eq.published&order=created_at.desc&limit=100",
            "GET", null, null
        ) as JSONArray
        if (sets.length() == 0) return null
        val allSets = (0 until sets.length()).map { sets.getJSONObject(it) }
        val lastSetId = rotationPrefs.getString("last_set_id", null)
        val alternatives = allSets.filter { it.getString("id") != lastSetId }
        val chosenSet = (if (alternatives.isNotEmpty()) alternatives else allSets).random()
        val setId = chosenSet.getString("id")
        rotationPrefs.edit().putString("last_set_id", setId).apply()
        val difficultyFilter = if (difficulty == "mixed") "" else "&difficulty=eq.$difficulty"
        val rows = discover.request(
            "/rest/v1/arena_quickfire_questions?select=id,question_text,options,position,points,difficulty&set_id=eq.$setId$difficultyFilter&order=position.asc&limit=100",
            "GET", null, null
        ) as JSONArray
        val questionPool = (0 until rows.length()).map { index ->
            val item = rows.getJSONObject(index)
            val choices = item.getJSONArray("options")
            val shuffledOptions = (0 until choices.length())
                .map { Option(choices.getString(it), it) }
                .shuffled()
            Question(item.getString("id"), item.getString("question_text"),
                shuffledOptions, item.optInt("points", 10), item.optString("difficulty", "standard"))
        }.shuffled()
        val recentIds = rotationPrefs.getStringSet("recent_question_ids", emptySet()) ?: emptySet()
        val freshPool = questionPool.filter { it.id !in recentIds }
        val availablePool = if (freshPool.size >= 3) freshPool else questionPool
        val roundQuestions = availablePool.take(if (difficulty == "mixed") 10 else 5)
        if (roundQuestions.isEmpty()) return null
        val updatedRecent = (recentIds + roundQuestions.map { it.id }).takeLast(40).toSet()
        rotationPrefs.edit().putStringSet("recent_question_ids", updatedRecent).apply()
        return Quiz(setId, chosenSet.getString("title"), chosenSet.optString("description"), roundQuestions, difficulty)
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
