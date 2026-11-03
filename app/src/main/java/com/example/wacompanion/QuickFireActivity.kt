package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.os.CountDownTimer
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/** A real, server-checked Quick Fire quiz. No answer key is shipped in the APK. */
class QuickFireActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val backend by lazy { QuickFireBackend(this) }
    private val green = Color.rgb(7, 91, 69)
    private val ink = Color.rgb(18, 42, 55)
    private val muted = Color.rgb(91, 108, 119)
    private lateinit var page: LinearLayout
    private lateinit var status: TextView
    private lateinit var headerArea: LinearLayout
    private lateinit var questionArea: LinearLayout
    private var quiz: QuickFireBackend.Quiz? = null
    private var questionIndex = 0
    private var score = 0
    private var locked = false
    private var streak = 0
    private var bestStreak = 0
    private var selectedDifficulty = "mixed"
    private var questionTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 247))
        }
        setContentView(ScrollView(this).apply { addView(page) })
        page.addView(label("QUICK FIRE", 27, green, true))
        page.addView(label("Think fast. Learn something new.", 15, muted))
        status = label("Loading quiz…", 15, muted)
        page.addView(status)
        headerArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(headerArea)
        questionArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(questionArea)
        chooseDifficulty()
    }

    private fun chooseDifficulty() {
        val labels = arrayOf("Mixed challenge • 10 questions", "Easy • 5 questions", "Standard • 5 questions", "Expert • 5 questions")
        val values = arrayOf("mixed", "easy", "standard", "expert")
        AlertDialog.Builder(this)
            .setTitle("Choose your Quick Fire level")
            .setItems(labels) { _, which ->
                selectedDifficulty = values[which]
                loadQuiz()
            }
            .setCancelable(false)
            .show()
    }

    private fun loadQuiz() {
        questionTimer?.cancel()
        worker.execute {
            try {
                val loaded = backend.activeQuiz(selectedDifficulty)
                runOnUiThread {
                    headerArea.removeAllViews()
                    questionArea.removeAllViews()
                    questionIndex = 0
                    score = 0
                    streak = 0
                    bestStreak = 0
                    quiz = loaded
                    if (loaded == null || loaded.questions.isEmpty()) {
                        status.text = "No published quiz is available yet. Please check back soon."
                    } else {
                        headerArea.addView(label(loaded.title, 20, ink, true))
                        headerArea.addView(label("MODE: " + loaded.difficulty.uppercase() + " • TIMER + STREAK CHALLENGE", 12, green, true))
                        if (loaded.description.isNotBlank()) headerArea.addView(label(loaded.description, 14, muted))
                        renderQuestion()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not load Quick Fire. Check your connection." }
            }
        }
    }

    private fun renderQuestion() {
        val current = quiz ?: return
        if (questionIndex >= current.questions.size) {
            showSummary()
            return
        }
        locked = false
        questionTimer?.cancel()
        val question = current.questions[questionIndex]
        questionArea.removeAllViews()
        status.text = "Question ${questionIndex + 1} of ${current.questions.size} • Streak: $streak"
        questionArea.addView(label(question.text, 18, ink, true))
        questionArea.addView(Button(this).apply {
            text = "Challenge a friend with this question"
            isAllCaps = false
            setOnClickListener {
                val options = question.options.joinToString("\n") { "• " + it.text }
                ArenaShareCard.share(
                    this@QuickFireActivity,
                    "QUICK FIRE CHALLENGE",
                    "${question.text}\n\n${options}\n\nThink you know it? Play and compare answers!",
                    "CHALLENGE A FRIEND  •  DAILY ARENA"
                )
            }
        })
        val choices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        question.options.forEach { option ->
            val button = Button(this).apply {
                text = option.text
                isAllCaps = false
                setTextColor(ink)
                setBackgroundColor(Color.rgb(232, 242, 237))
                setOnClickListener {
                    if (!locked) submitAnswer(question, option.originalIndex, choices)
                }
            }
            choices.addView(button)
        }
        questionArea.addView(choices)
        questionArea.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(14)) })
        startQuestionTimer()
    }

    private fun submitAnswer(question: QuickFireBackend.Question, selected: Int, choices: LinearLayout) {
        locked = true
        questionTimer?.cancel()
        for (i in 0 until choices.childCount) choices.getChildAt(i).isEnabled = false
        status.text = "Checking your answer securely…"
        worker.execute {
            try {
                val result = backend.submit(question.id, selected)
                runOnUiThread {
                    if (result.correct && !result.alreadyAnswered) {
                        score += question.points
                        streak++
                        bestStreak = maxOf(bestStreak, streak)
                    } else {
                        streak = 0
                    }
                    val feedback = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                        setBackgroundColor(Color.WHITE)
                    }
                    feedback.addView(label(
                        if (result.alreadyAnswered) "You already answered this question." else if (result.correct) "Correct!" else "Not quite — keep going!",
                        16, if (result.correct) green else ink, true
                    ))
                    val correctText = question.options.firstOrNull { it.originalIndex == result.correctIndex }?.text ?: "Unavailable"
                    feedback.addView(label("Correct answer: $correctText", 14, ink, true))
                    if (result.explanation.isNotBlank()) feedback.addView(label(result.explanation, 14, muted))
                    questionArea.addView(feedback)
                    questionArea.addView(Button(this).apply {
                        text = if (questionIndex + 1 == (quiz?.questions?.size ?: 0)) "See my result" else "Next question"
                        isAllCaps = false
                        setOnClickListener {
                            questionIndex++
                            renderQuestion()
                        }
                    })
                    status.text = "Question ${questionIndex + 1} of ${quiz?.questions?.size ?: 0}"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    locked = false
                    for (i in 0 until choices.childCount) choices.getChildAt(i).isEnabled = true
                    status.text = e.message ?: "Could not submit your answer. Please try again."
                }
            }
        }
    }


    private fun startQuestionTimer() {
        val seconds = when (selectedDifficulty) {
            "easy" -> 25
            "standard" -> 18
            "expert" -> 12
            else -> 18
        }
        questionTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished / 1000L).toInt() + 1
                status.text = "Question ${questionIndex + 1} of ${quiz?.questions?.size ?: 0} • ${remaining}s • Streak: $streak"
            }
            override fun onFinish() {
                if (locked) return
                locked = true
                streak = 0
                status.text = "Time is up! Your streak has reset."
                questionArea.addView(label("TIME'S UP", 18, Color.rgb(166, 72, 38), true))
                questionArea.addView(Button(this@QuickFireActivity).apply {
                    text = if (questionIndex + 1 == (quiz?.questions?.size ?: 0)) "See my result" else "Next question"
                    isAllCaps = false
                    setOnClickListener {
                        questionIndex++
                        renderQuestion()
                    }
                })
            }
        }.start()
    }

    private fun showSummary() {
        questionTimer?.cancel()
        questionArea.removeAllViews()
        status.text = "Quiz complete"
        val total = quiz?.questions?.sumOf { it.points } ?: 0
        questionArea.addView(label("Quiz score: $score / $total", 21, green, true))
        questionArea.addView(label("Best correct-answer streak: $bestStreak", 16, ink, true))
        questionArea.addView(label("Your answers are checked and recorded by the server. This is a quiz score, not spendable Games Points. Rewards and leaderboards will be connected in a later milestone.", 14, muted))
        questionArea.addView(Button(this).apply {
            text = "Share my Quick Fire result"
            isAllCaps = false
            setOnClickListener {
                ArenaShareCard.share(
                    this@QuickFireActivity,
                    "QUICK FIRE RESULT",
                    "Score: ${score} / ${total}\nBest correct-answer streak: ${bestStreak}\n\nCan you beat this score?",
                    "YOUR TURN  •  DAILY ARENA"
                )
            }
        })
        questionArea.addView(Button(this).apply {
            text = "Play another random round"
            isAllCaps = false
            setOnClickListener { chooseDifficulty() }
        })
        questionArea.addView(Button(this).apply {
            text = "Back to Daily Arena"
            isAllCaps = false
            setOnClickListener { finish() }
        })
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(7))
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        questionTimer?.cancel()
        worker.shutdownNow()
        super.onDestroy()
    }
}
