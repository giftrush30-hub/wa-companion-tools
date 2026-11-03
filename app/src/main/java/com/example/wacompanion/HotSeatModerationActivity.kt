package com.example.wacompanion

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/** Trusted-admin review queue for community Hot Seat questions. */
class HotSeatModerationActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val backend by lazy { ArenaBackend(this) }
    private val green = Color.rgb(7, 91, 69)
    private val ink = Color.rgb(18, 42, 55)
    private val muted = Color.rgb(91, 108, 119)
    private lateinit var page: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 247))
        }
        setContentView(ScrollView(this).apply { addView(page) })
        page.addView(label("HOT SEAT QUESTION REVIEW", 22, green, true))
        status = label("Loading pending community questions…", 14, muted)
        page.addView(status)
        load()
    }

    private fun load() {
        worker.execute {
            try {
                val items = backend.pendingSuggestions()
                runOnUiThread {
                    page.removeViews(2, (page.childCount - 2).coerceAtLeast(0))
                    status.text = if (items.isEmpty()) "No community questions are waiting for review."
                        else items.size.toString() + " question(s) need review."
                    items.forEach { item ->
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(14), dp(12), dp(14), dp(12))
                            setBackgroundColor(Color.WHITE)
                        }
                        card.addView(label(item.topic.uppercase(), 12, green, true))
                        card.addView(label(item.question, 17, ink, true))
                        card.addView(label(item.options.joinToString(" • "), 13, muted))
                        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                        actions.addView(Button(this@HotSeatModerationActivity).apply {
                            text = "Approve"
                            isAllCaps = false
                            setOnClickListener { decide(item.id, true) }
                        })
                        actions.addView(Button(this@HotSeatModerationActivity).apply {
                            text = "Reject"
                            isAllCaps = false
                            setOnClickListener { decide(item.id, false) }
                        })
                        card.addView(actions)
                        page.addView(card)
                        page.addView(gap(9))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not load the review queue." }
            }
        }
    }

    private fun decide(id: String, approve: Boolean) {
        status.text = "Saving moderation decision…"
        worker.execute {
            try {
                backend.moderateSuggestion(id, approve)
                runOnUiThread {
                    status.text = if (approve) "Question approved." else "Question rejected."
                    load()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not save the decision." }
            }
        }
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(7))
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
