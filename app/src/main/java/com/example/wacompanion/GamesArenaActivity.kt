package com.example.wacompanion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Games Arena hub. Big Quiz practice is the first playable milestone; online modes stay locked until authoritative multiplayer is ready. */
class GamesArenaActivity : Activity() {
    private val ink = Color.rgb(18, 42, 55)
    private val green = Color.rgb(7, 91, 69)
    private val muted = Color.rgb(91, 108, 119)
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 247))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(label("GAMES ARENA", 28, green, true))
        root.addView(label("Play for fun. Build skill. Challenge your friends.", 15, muted))
        root.addView(space(12))
        val intro = panel()
        intro.addView(label("YOUR NEXT MATCH STARTS HERE", 12, green, true))
        intro.addView(label("Choose a game and jump in.", 21, ink, true))
        intro.addView(label("Practice is available first. Competitive rankings and Games Points will only arrive after fair-play checks are ready.", 14, muted))
        root.addView(intro)
        root.addView(space(14))

        val quiz = panel()
        quiz.addView(label("🏆  BIG QUIZ", 21, green, true))
        quiz.addView(label("A proper quiz-show challenge with rising difficulty, a prize ladder and a one-time 50:50 lifeline. Play offline in practice mode.", 14, ink))
        quiz.addView(Button(this).apply {
            text = "Start Big Quiz practice"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@GamesArenaActivity, BigQuizActivity::class.java)) }
        })
        root.addView(quiz)
        root.addView(space(10))

        val ludo = panel()
        ludo.addView(label("🎲  LUDO", 21, green, true))
        ludo.addView(label("Classic 2–4 player board game. Private rooms, online matchmaking, reconnects and rematches are planned. We will not label local-only play as online multiplayer.", 14, ink))
        ludo.addView(label("OFFLINE PASS & PLAY — AVAILABLE", 11, muted, true))
        ludo.addView(Button(this).apply {
            text = "Play Ludo on this phone"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@GamesArenaActivity, LudoActivity::class.java)) }
        })
        root.addView(ludo)
        root.addView(space(10))

        val whot = panel()
        whot.addView(label("🃏  WHOT!", 21, green, true))
        whot.addView(label("A Nigerian card-game experience with a clearly stated rule set, private rooms and fair matchmaking. Rule-set decisions and authoritative gameplay come before release.", 14, ink))
        whot.addView(label("OFFLINE PASS & PLAY — AVAILABLE", 11, muted, true))
        whot.addView(Button(this).apply {
            text = "Play WHOT! on this phone"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@GamesArenaActivity, WhotActivity::class.java)) }
        })
        root.addView(whot)
        root.addView(space(12))

        val economy = panel()
        economy.addView(label("GAMES POINTS: ENTERTAINMENT ONLY", 12, green, true))
        economy.addView(label("Future points may unlock cosmetic items and collectibles. They will never buy business promotion, ads, views, followers or visibility. No cash-out, transfers or betting.", 14, ink))
        root.addView(economy)
        root.addView(space(12))
        root.addView(Button(this).apply {
            text = "Back to home"
            isAllCaps = false
            setOnClickListener { finish() }
        })
    }

    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        setBackgroundColor(Color.WHITE)
    }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(7))
    }
    private fun space(value: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(value)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
