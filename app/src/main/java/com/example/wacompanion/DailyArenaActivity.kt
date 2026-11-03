package com.example.wacompanion

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Phase 1 navigation foundation for Daily Arena.
 * Community voting, moderation, rewards and multiplayer are intentionally
 * not simulated locally; they require trusted backend services.
 */
class DailyArenaActivity : Activity() {
    private val ink = Color.rgb(18, 42, 55)
    private val muted = Color.rgb(91, 108, 119)
    private val green = Color.rgb(7, 91, 69)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 247))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)

        content.addView(text("DAILY ARENA", 27, green, true))
        content.addView(text("A fresh reason to have fun every day.", 15, muted))
        content.addView(gap(16))
        val intro = panel()
        intro.addView(text("PLAY • SHARE IDEAS • CHALLENGE YOURSELF", 12, green, true))
        intro.addView(text("Your daily entertainment space is taking shape.", 20, ink, true))
        intro.addView(text("Join quick activities, friendly competitions and creative challenges. Points will reward genuine participation—not advertising or visibility.", 14, muted))
        content.addView(intro)
        content.addView(gap(12))

        addFeature(content, "HOT SEAT", "Daily questions and polls about life, opinions, culture and everyday Nigerian experiences.")
        addFeature(content, "QUICK FIRE", "Short timed quizzes covering general knowledge, culture, entertainment, technology and more.")
        addFeature(content, "MEME WAR", "Original caption challenges where the community can vote, with reporting and moderation.")
        addFeature(content, "GAMES ARENA", "Big Quiz, multiplayer Ludo and WHOT!, with private rooms, matchmaking and fair-play protections.")
        addFeature(content, "POINTS & REWARDS", "A separate entertainment-points wallet for achievements, avatars, profile decorations and game cosmetics.")
        addFeature(content, "LEADERBOARDS", "Quiz, Ludo, WHOT! and All-Round rankings, with global and friends views plus monthly seasons.")

        content.addView(gap(10))
        val note = panel()
        note.addView(text("IMPORTANT", 12, green, true))
        note.addView(text("Games Points will never buy business ads, sponsored placement, views, followers or visibility. Business promotion remains a separate money-based service.", 14, ink))
        content.addView(note)
        content.addView(gap(12))
        content.addView(text("FOUNDATION PREVIEW", 12, muted, true))
        content.addView(text("This screen establishes the navigation and product structure. Live polls, public submissions, points, leaderboards and online games will be enabled only after secure backend rules, moderation and testing are ready.", 13, muted))
    }

    private fun addFeature(parent: LinearLayout, title: String, description: String) {
        val box = panel()
        box.addView(text(title, 17, ink, true))
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
}
