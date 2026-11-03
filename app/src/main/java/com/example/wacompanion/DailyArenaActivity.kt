package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/** Daily Arena home with the first live feature: Hot Seat polls. */
class DailyArenaActivity : Activity() {
    private val ink = Color.rgb(18, 42, 55)
    private val muted = Color.rgb(91, 108, 119)
    private val green = Color.rgb(7, 91, 69)
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var pollArea: LinearLayout
    private val arena by lazy { ArenaBackend(this) }
    private var activePoll: ArenaBackend.Poll? = null
    private var voted = false
    private var pollReportButtonAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 247))
        }
        setContentView(ScrollView(this).apply { addView(content) })
        content.addView(text("DAILY ARENA", 27, green, true))
        content.addView(text("A fresh reason to have fun every day.", 15, muted))
        content.addView(gap(12))

        val intro = panel()
        intro.addView(text("PLAY • SHARE IDEAS • CHALLENGE YOURSELF", 12, green, true))
        intro.addView(text("Your daily entertainment space.", 20, ink, true))
        intro.addView(text("Points reward genuine participation—not advertising, views or business visibility.", 14, muted))
        content.addView(intro)
        content.addView(gap(12))

        val hotSeat = panel()
        hotSeat.addView(text("HOT SEAT", 19, green, true))
        hotSeat.addView(text("One question. Your opinion. A community conversation.", 14, muted))
        status = text("Loading today's question…", 14, muted)
        hotSeat.addView(status)
        pollArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        hotSeat.addView(pollArea)
        content.addView(hotSeat)
        content.addView(gap(12))

        addFeature(content, "QUICK FIRE", "Short quizzes covering general knowledge, Nigerian culture, entertainment, technology and more.")
        content.addView(Button(this).apply {
            text = "Play Quick Fire"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@DailyArenaActivity, QuickFireActivity::class.java)) }
        })
        content.addView(gap(9))
        addFeature(content, "MEME WAR", "Write original captions, vote for approved entries and report inappropriate content.")
        content.addView(Button(this).apply {
            text = "Enter Meme War"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@DailyArenaActivity, MemeWarActivity::class.java)) }
        })
        content.addView(gap(9))
        addFeature(content, "GAMES ARENA", "Big Quiz, multiplayer Ludo and WHOT! with fair-play protections. Coming next.")
        addFeature(content, "POINTS & REWARDS", "Cosmetics, avatars, profile decorations and game customisation only.")
        addFeature(content, "LEADERBOARDS", "Separate Quiz, Ludo, WHOT! and All-Round rankings. Coming next.")
        if (MemeWarBackend(this).isAdmin()) {
            content.addView(Button(this).apply {
                text = "Open Daily Arena safety review"
                isAllCaps = false
                setOnClickListener { startActivity(Intent(this@DailyArenaActivity, ArenaModerationActivity::class.java)) }
            })
            content.addView(gap(9))
        }

        val note = panel()
        note.addView(text("TWO SEPARATE ECONOMIES", 12, green, true))
        note.addView(text("Games Points can never buy business ads, sponsored placement, views, followers or visibility. Business promotion remains money-based.", 14, ink))
        content.addView(note)
        loadPoll()
    }

    private fun loadPoll(excludePollId: String? = null) {
        status.text = "Finding a fresh Hot Seat question…"
        pollArea.removeAllViews()
        pollReportButtonAdded = false
        worker.execute {
            try {
                val poll = arena.activePoll(excludePollId)
                runOnUiThread {
                    activePoll = poll
                    if (poll == null) {
                        status.text = "No live question yet. Check back soon."
                    } else {
                        voted = poll.myVoteOptionId != null
                        if (voted) {
                            status.text = "You have already voted. Here are the community results:"
                            worker.execute {
                                try {
                                    val results = arena.results(poll.id)
                                    runOnUiThread { showResults(poll, results) }
                                } catch (e: Exception) {
                                    runOnUiThread { status.text = "You have already voted. Results could not be loaded just now." }
                                }
                            }
                        } else {
                            renderPoll(poll)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not load the question. Check your connection and try again." }
            }
        }
    }

    private fun renderPoll(poll: ArenaBackend.Poll) {
        status.text = "HOT SEAT • ${poll.topic.uppercase()} • ${poll.pollType.replace('_', ' ').uppercase()}"
        pollArea.removeAllViews()
        pollArea.addView(text(poll.question, 18, ink, true))
        poll.options.forEach { option ->
            val choice = Button(this).apply {
                text = option.text
                isAllCaps = false
                setTextColor(ink)
                setBackgroundColor(Color.rgb(232, 242, 237))
                isEnabled = !voted
                setOnClickListener { submitVote(poll, option) }
            }
            pollArea.addView(choice)
        }
        if (poll.options.isEmpty()) status.text = "This question has no answer choices yet."
        pollArea.addView(Button(this).apply {
            text = "Skip to another question"
            isAllCaps = false
            setOnClickListener { loadPoll(poll.id) }
        })
        addPollReportButton(poll.id)
    }

    private fun submitVote(poll: ArenaBackend.Poll, option: ArenaBackend.Option) {
        status.text = "Submitting your vote…"
        worker.execute {
            try {
                arena.vote(poll.id, option.id)
                val results = arena.results(poll.id)
                runOnUiThread {
                    voted = true
                    status.text = "Thanks for taking part! Here are the community results:"
                    showResults(poll, results)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = (e.message ?: "Your vote could not be submitted.") +
                        if (e.message.orEmpty().contains("duplicate", true) || e.message.orEmpty().contains("unique", true))
                            "\nIt looks like you have already voted in this poll." else ""
                }
            }
        }
    }

    private fun showResults(poll: ArenaBackend.Poll, results: List<ArenaBackend.Result>) {
        pollArea.removeAllViews()
        pollReportButtonAdded = false
        val counts = results.associate { it.optionId to it.count }
        val totalVotes = results.sumOf { it.count }
        pollArea.addView(text(poll.question, 18, ink, true))
        pollArea.addView(text("$totalVotes community vote(s)", 13, muted))
        poll.options.forEach { option ->
            val count = counts[option.id] ?: 0L
            val percent = if (totalVotes == 0L) 0 else ((count * 100) / totalVotes).toInt()
            pollArea.addView(text("${option.text}  •  $count votes  ($percent%)", 14, ink, true))
            val track = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = percent
            }
            pollArea.addView(track)
        }
        pollArea.addView(Button(this).apply {
            text = "Next Hot Seat question"
            isAllCaps = false
            setOnClickListener { loadPoll(poll.id) }
        })
        addPollReportButton(poll.id)
    }

    private fun addPollReportButton(pollId: String) {
        if (pollReportButtonAdded) return
        pollReportButtonAdded = true
        pollArea.addView(Button(this).apply {
            text = "Report this question"
            isAllCaps = false
            setOnClickListener { showPollReportDialog(pollId) }
        })
    }

    private fun showPollReportDialog(pollId: String) {
        val labels = arrayOf("Harassment", "Hate speech", "Sexual content", "Violence", "Spam", "Privacy violation", "Other")
        val values = arrayOf("harassment", "hate", "sexual_content", "violence", "spam", "privacy", "other")
        AlertDialog.Builder(this)
            .setTitle("Report this Hot Seat question")
            .setItems(labels) { _, which ->
                worker.execute {
                    try {
                        arena.reportPoll(pollId, values[which])
                        runOnUiThread { status.text = "Report received. Thank you for helping keep Daily Arena safe." }
                    } catch (e: Exception) {
                        runOnUiThread { status.text = e.message ?: "Could not submit your report." }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addFeature(parent: LinearLayout, title: String, description: String) {
        val box = panel()
        box.addView(text(title, 16, ink, true))
        box.addView(text(description, 14, muted))
        parent.addView(box)
        parent.addView(gap(9))
    }

    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        setBackgroundColor(Color.WHITE)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(5))
        }

    private fun gap(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
