package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonalSpaceActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("wa_personal_space", MODE_PRIVATE) }
    private val green = Color.rgb(6, 63, 50)
    private val muted = Color.rgb(95, 105, 100)
    private lateinit var list: LinearLayout
    private var diary = false
    private val pickFileRequest = 4107

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
        page.addView(text(if (diary) "Your private daily memories." else "Keep notes and choose documents or photos from your phone.", 14, muted))
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
            text = if (diary) "Save diary entry" else "Save note to My Vault"
            isAllCaps = false
            setOnClickListener {
                val value = editor.text.toString().trim()
                if (value.isBlank()) {
                    editor.error = "Write something first"
                    return@setOnClickListener
                }
                val key = if (diary) "diary_entries" else "vault_items"
                val items = readItems(key)
                items.put(JSONObject().put("type", "note").put("text", value).put("created_at", System.currentTimeMillis()))
                prefs.edit().putString(key, items.toString()).apply()
                editor.setText("")
                refreshList()
            }
        })
        if (!diary) {
            page.addView(space(7))
            page.addView(Button(this).apply {
                text = "Add a document or photo"
                isAllCaps = false
                setOnClickListener { chooseFile() }
            })
            page.addView(text("Files are copied into this app's private storage on this phone. They are not uploaded or backed up online.", 12, muted))
        }
        page.addView(space(18))
        page.addView(text(if (diary) "YOUR ENTRIES" else "SAVED ITEMS", 13, green, true))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(list)
        refreshList()
        page.addView(space(12))
        page.addView(Button(this).apply { text = "Back"; isAllCaps = false; setOnClickListener { finish() } })
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf", "text/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        startActivityForResult(intent, pickFileRequest)
    }

    @Deprecated("Handled for the Android document picker result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickFileRequest || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val displayName = queryDisplayName(uri) ?: "Saved file"
            val folder = File(filesDir, "vault_files").apply { mkdirs() }
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "saved_file" }
            var destination = File(folder, safeName)
            var count = 1
            while (destination.exists()) {
                val dot = safeName.lastIndexOf('.')
                val candidate = if (dot > 0) safeName.substring(0, dot) + "_$count" + safeName.substring(dot) else "${safeName}_$count"
                destination = File(folder, candidate)
                count++
            }
            contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Could not read the selected file")
            val mime = contentResolver.getType(uri) ?: "*/*"
            val items = readItems("vault_items")
            items.put(JSONObject()
                .put("type", "file")
                .put("name", destination.name)
                .put("path", destination.name)
                .put("mime", mime)
                .put("created_at", System.currentTimeMillis()))
            prefs.edit().putString("vault_items", items.toString()).apply()
            refreshList()
        } catch (error: Exception) {
            AlertDialog.Builder(this)
                .setMessage("This file could not be saved. Please try another file.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun openSavedFile(name: String, mime: String) {
        try {
            val file = File(File(filesDir, "vault_files"), name)
            if (!file.exists()) throw IllegalStateException("File not found")
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val view = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(view, "Open file"))
        } catch (_: Exception) {
            AlertDialog.Builder(this)
                .setMessage("No suitable app could open this file, or the file is missing.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun refreshList() {
        list.removeAllViews()
        val key = if (diary) "diary_entries" else "vault_items"
        val items = readItems(key)
        if (items.length() == 0) {
            list.addView(text(if (diary) "Your diary is empty. Add your first memory above." else "Nothing saved yet. Add your first note or file above.", 14, muted))
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
            if (item.optString("type") == "file") {
                card.addView(text("📎 " + item.optString("name"), 15, Color.DKGRAY, true))
                card.addView(Button(this).apply {
                    text = "Open file"
                    isAllCaps = false
                    setOnClickListener { openSavedFile(item.optString("path"), item.optString("mime", "*/*")) }
                })
            } else {
                card.addView(text(item.optString("text"), 15, Color.DKGRAY))
            }
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
                            for (j in 0 until current.length()) {
                                val candidate = current.optJSONObject(j) ?: continue
                                if (j != i) next.put(candidate)
                                else if (candidate.optString("type") == "file") {
                                    File(File(filesDir, "vault_files"), candidate.optString("path")).delete()
                                }
                            }
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
