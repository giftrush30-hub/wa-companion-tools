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

/** Trusted-admin queue for reports submitted across Daily Arena. */
class ArenaModerationActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val backend by lazy { MemeWarBackend(this) }
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
        page.addView(label("DAILY ARENA SAFETY REVIEW", 22, green, true))
        status = label("Checking moderator access…", 14, muted)
        page.addView(status)
        loadReports()
    }

    private fun loadReports() {
        worker.execute {
            try {
                if (!backend.isAdmin()) throw IllegalStateException("This account is not assigned the moderator role.")
                val reports = backend.pendingReports()
                runOnUiThread {
                    page.removeViews(2, (page.childCount - 2).coerceAtLeast(0))
                    status.text = if (reports.isEmpty()) "No open reports. The review queue is clear."
                        else "${reports.size} report(s) need attention."
                    reports.forEach { report ->
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(14), dp(12), dp(14), dp(12))
                            setBackgroundColor(Color.WHITE)
                        }
                        card.addView(label("${report.targetType.replace('_', ' ').uppercase()} • ${report.reason.replace('_', ' ')}", 13, green, true))
                        card.addView(label("Target: ${report.targetId}", 11, muted))
                        if (!report.details.isNullOrBlank()) card.addView(label(report.details, 14, ink))
                        card.addView(label("Submitted: ${report.createdAt} • Status: ${report.status}", 11, muted))
                        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                        actions.addView(Button(this@ArenaModerationActivity).apply {
                            text = "Reviewing"
                            isAllCaps = false
                            setOnClickListener { update(report.id, "reviewing") }
                        })
                        actions.addView(Button(this@ArenaModerationActivity).apply {
                            text = "Resolve"
                            isAllCaps = false
                            setOnClickListener { update(report.id, "resolved") }
                        })
                        actions.addView(Button(this@ArenaModerationActivity).apply {
                            text = "Dismiss"
                            isAllCaps = false
                            setOnClickListener { update(report.id, "dismissed") }
                        })
                        card.addView(actions)
                        page.addView(card)
                        page.addView(gap(9))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not load the report queue." }
            }
        }
    }

    private fun update(id: String, nextStatus: String) {
        status.text = "Saving report decision…"
        worker.execute {
            try {
                backend.updateReportStatus(id, nextStatus)
                runOnUiThread {
                    status.text = "Report updated."
                    loadReports()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not update this report." }
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
