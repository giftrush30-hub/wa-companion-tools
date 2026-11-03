package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ReferralHubActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("wa_referral_hub", MODE_PRIVATE) }
    private val green = Color.rgb(6, 63, 50)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(Color.rgb(247, 249, 247))
        }
        val scroll = ScrollView(this)
        scroll.addView(page)
        setContentView(scroll)
        page.addView(text("REFERRAL HUB", 25, green, true))
        page.addView(text("Find and prepare to share useful online opportunities.", 14, Color.DKGRAY))
        page.addView(space(14))
        page.addView(text("Share a referral opportunity", 18, green, true))
        page.addView(text("Add a clear description and the link. Your draft stays on this phone for now; public publishing and paid promotion are not active yet.", 13, Color.DKGRAY))
        val title = EditText(this).apply { hint = "Offer or business name" }
        val description = EditText(this).apply { hint = "What is it, and who may find it useful?"; minLines = 3 }
        val link = EditText(this).apply { hint = "Referral link (https://…)" }
        page.addView(title); page.addView(description); page.addView(link)
        page.addView(Button(this).apply {
            text = "Save referral draft"
            isAllCaps = false
            setOnClickListener {
                val t = title.text.toString().trim()
                val d = description.text.toString().trim()
                val u = link.text.toString().trim()
                if (t.isBlank() || d.isBlank() || !u.startsWith("https://")) {
                    AlertDialog.Builder(this@ReferralHubActivity).setMessage("Enter an offer name, a short description and a secure https:// link.")
                        .setPositiveButton("OK", null).show()
                } else {
                    val all = prefs.getString("drafts", "") ?: ""
                    val row = "$t\n$d\n$u"
                    prefs.edit().putString("drafts", if (all.isBlank()) row else "$all\n\n---\n\n$row").apply()
                    title.setText(""); description.setText(""); link.setText("")
                    AlertDialog.Builder(this@ReferralHubActivity).setMessage("Referral draft saved on this phone. It has not been published.")
                        .setPositiveButton("OK", null).show()
                }
            }
        })
        page.addView(space(14))
        page.addView(Button(this).apply { text = "View saved drafts"; isAllCaps = false; setOnClickListener {
            val drafts = prefs.getString("drafts", "") ?: ""
            AlertDialog.Builder(this@ReferralHubActivity).setTitle("Your referral drafts")
                .setMessage(if (drafts.isBlank()) "No drafts saved yet." else drafts)
                .setPositiveButton("OK", null).show()
        } })
        page.addView(Button(this).apply { text = "Back"; isAllCaps = false; setOnClickListener { finish() } })
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(color); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD) }
    private fun space(height: Int) = android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(height)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}