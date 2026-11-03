package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonalSpaceActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("wa_personal_space", MODE_PRIVATE) }
    private val green = Color.rgb(6, 63, 50)
    private val muted = Color.rgb(95, 105, 100)
    private lateinit var list: LinearLayout
    private var diary = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        diary = intent.getStringExtra("space") == "diary"
        render()
    }

    private fun render() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(Color.rgb(247, 249, 247))
        }
        val scroll = ScrollView(this)
        scroll.addView(page)
        setContentView(scroll)

        page.addView(text(if (diary) "MY DIARY" else "MY VAULT", 25, green, true))
        page.addView(text(if (diary) "Your private daily memories." else "Keep important writing and notes in one place.", 14, muted))
        page.addView(space(14))

        val editor = EditText(this).apply {
            hint = if (diary) "What happened today? Write a memory…" else "Write or paste something you want to save…"
            minLines = 4
            gravity = android.view.Gravity.TOP
            textSize = 16f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        page.addView(editor)
        page.addView(space(8))
        page.addView(Button(this).apply {
            text = if (diary) "Save diary entry" else "Save to My Vault"
            isAllCaps = false
            setOnClickListener {
                val value = editor.text.toString().trim()
                if (value.isBlank()) {
                    editor.error = "Write something first"
                    return@setOnClickListener
                }
                val key = if (diary) "diary_entries" else "vault_items"
                val items = readItems(key)
                items.put(JSONObject().put("text", value).put("created_at", System.currentTimeMillis()))
                prefs.edit().putString(key, items.toString()).apply()
                editor.setText("")
                refreshList()
            }
        })
        page.addView(space(18))
        page.addView(text(if (diary) "YOUR ENTRIES" else "SAVED ITEMS", 13, green, true))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(list)
        refreshList()
        page.addView(space(12))
        page.addView(Button(this).apply { text = "Back"; isAllCaps = false; setOnClickListener { finish() } })
    }

    private fun refreshList() {
        list.removeAllViews()
        val key = if (diary) "diary_entries" else "vault_items"
        val items = readItems(key)
        if (items.length() == 0) {
            list.addView(text(if (diary) "Your diary is empty. Add your first memory above." else "Nothing saved yet. Add your first note above.", 14, muted))
            return
        }
        for (i in items.length() - 1 downTo 0) {
            val item = items.optJSONObject(i) ?: continue
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(12), dp(13), dp(12))
                setBackgroundColor(Color.WHITE)
            }
            val time = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(item.optLong("created_at")))
            card.addView(text(time, 11, muted))
            card.addView(space(5))
            card.addView(text(item.optString("text"), 15, Color.DKGRAY))
            card.addView(Button(this).apply {
                text = "Delete"
                isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@PersonalSpaceActivity)
                        .setMessage("Delete this saved item?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            val current = readItems(key)
                            val next = JSONArray()
                            for (j in 0 until current.length()) if (j != i) next.put(current.getJSONObject(j))
                            prefs.edit().putString(key, next.toString()).apply()
                            refreshList()
                        }.show()
                }
            })
            list.addView(card)
            list.addView(space(8))
        }
    }

    private fun readItems(key: String): JSONArray = try {
        JSONArray(prefs.getString(key, "[]") ?: "[]")
    } catch (_: Exception) { JSONArray() }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(height)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}