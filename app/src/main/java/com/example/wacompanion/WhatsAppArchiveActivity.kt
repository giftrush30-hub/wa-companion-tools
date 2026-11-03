package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WhatsAppArchiveActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("wa_chat_archive", MODE_PRIVATE) }
    private val folder by lazy { File(filesDir, "chat_archives").apply { mkdirs() } }
    private val green = Color.rgb(6, 63, 50)
    private val muted = Color.rgb(95, 105, 100)
    private lateinit var root: LinearLayout
    private lateinit var entries: LinearLayout
    private val pickRequest = 731

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showArchiveList()
    }

    private fun showArchiveList() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(Color.rgb(247, 249, 247))
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        root.addView(label("WHATSAPP CHAT ARCHIVE", 23, green, true))
        root.addView(label("Keep an independent copy of chats you choose to export from WhatsApp.", 14, muted))
        root.addView(space(10))
        val import = Button(this).apply {
            text = "Import an exported WhatsApp chat"
            isAllCaps = false
            setOnClickListener { chooseExport() }
        }
        root.addView(import)
        root.addView(space(8))
        root.addView(label("Your archives stay on this phone. Importing does not restore messages into WhatsApp or access chats automatically.", 12, muted))
        root.addView(space(14))
        root.addView(label("SAVED CHAT EXPORTS", 13, green, true))
        val search = EditText(this).apply {
            hint = "Search saved chat names or messages"
            singleLine = true
        }
        root.addView(search)
        entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(entries)
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderEntries(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        renderEntries("")
        root.addView(space(12))
        root.addView(Button(this).apply { text = "Back"; isAllCaps = false; setOnClickListener { finish() } })
    }

    private fun chooseExport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        startActivityForResult(intent, pickRequest)
    }

    @Deprecated("Handled for the Android document picker result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickRequest || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > 10 * 1024 * 1024) throw IllegalArgumentException("Chat export is larger than 10 MB")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: throw IllegalStateException("Could not read selected export")
            val chat = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF").trim()
            if (chat.isBlank()) throw IllegalArgumentException("The selected file is empty")
            val name = queryName(uri) ?: "WhatsApp chat"
            val safeBase = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60).ifBlank { "WhatsApp_chat" }
            val file = File(folder, "${System.currentTimeMillis()}_${safeBase}.txt")
            file.writeText(chat, Charsets.UTF_8)
            val count = chat.lineSequence().count { looksLikeMessage(it) }
            val records = readRecords()
            records.put(JSONObject()
                .put("name", name.removeSuffix(".txt"))
                .put("file", file.name)
                .put("messages", count)
                .put("imported_at", System.currentTimeMillis()))
            prefs.edit().putString("archives", records.toString()).apply()
            showArchiveList()
            AlertDialog.Builder(this)
                .setMessage("Chat export saved on this phone. About $count message lines were recognised.")
                .setPositiveButton("OK", null)
                .show()
        } catch (error: Exception) {
            AlertDialog.Builder(this)
                .setMessage(error.message ?: "This chat export could not be imported. Choose a WhatsApp .txt export.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun queryName(uri: android.net.Uri): String? {
        val cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun looksLikeMessage(line: String): Boolean {
        val clean = line.trimStart()
        return Regex("^(\\[)?\\d{1,4}[./-]\\d{1,2}[./-]\\d{2,4},?\\s+\\d{1,2}:\\d{2}").containsMatchIn(clean) ||
            Regex("^\\d{1,2}:\\d{2}\\s?(AM|PM)?\\s+-\\s+").containsMatchIn(clean)
    }

    private fun renderEntries(query: String) {
        entries.removeAllViews()
        val records = readRecords()
        val q = query.trim().lowercase(Locale.getDefault())
        var shown = 0
        for (i in records.length() - 1 downTo 0) {
            val item = records.optJSONObject(i) ?: continue
            val name = item.optString("name", "WhatsApp chat")
            val file = File(folder, item.optString("file"))
            val content = if (file.exists()) runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("") else ""
            if (q.isNotBlank() && !name.lowercase(Locale.getDefault()).contains(q) &&
                !content.lowercase(Locale.getDefault()).contains(q)) continue
            shown++
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(Color.WHITE)
            }
            val date = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(item.optLong("imported_at")))
            card.addView(label(name, 16, green, true))
            card.addView(label("${item.optInt("messages")} recognised message lines • Imported $date", 11, muted))
            card.addView(Button(this).apply {
                text = "Open chat archive"
                isAllCaps = false
                setOnClickListener { if (file.exists()) showConversation(name, content) else toast("The saved archive file is missing.") }
            })
            card.addView(Button(this).apply {
                text = "Delete archive"
                isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@WhatsAppArchiveActivity)
                        .setMessage("Permanently delete this saved chat copy?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            file.delete()
                            val next = JSONArray()
                            for (j in 0 until records.length()) if (j != i) next.put(records.getJSONObject(j))
                            prefs.edit().putString("archives", next.toString()).apply()
                            renderEntries(query)
                        }.show()
                }
            })
            entries.addView(card)
            entries.addView(space(8))
        }
        if (shown == 0) entries.addView(label(if (q.isBlank()) "No chat exports saved yet. Export a chat from WhatsApp, then import its .txt file here." else "No matching saved chats or messages.", 13, muted))
    }

    private fun showConversation(name: String, content: String) {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(20))
            setBackgroundColor(Color.rgb(247, 249, 247))
        }
        val scroll = ScrollView(this)
        scroll.addView(page)
        setContentView(scroll)
        page.addView(label(name, 20, green, true))
        page.addView(label("Read-only copy • Stored on this phone", 12, muted))
        page.addView(space(8))
        page.addView(TextView(this).apply {
            text = content
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setTextIsSelectable(true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        })
        page.addView(space(10))
        page.addView(Button(this).apply { text = "Back to archives"; isAllCaps = false; setOnClickListener { showArchiveList() } })
    }

    private fun readRecords(): JSONArray = try {
        JSONArray(prefs.getString("archives", "[]") ?: "[]")
    } catch (_: Exception) { JSONArray() }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(-1, dp(height))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}
