package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Playable Meme War: moderated caption entries, voting, and community reporting. */
class MemeWarActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val backend by lazy { MemeWarBackend(this) }
    private val green = Color.rgb(7, 91, 69)
    private val ink = Color.rgb(18, 42, 55)
    private val muted = Color.rgb(91, 108, 119)
    private lateinit var page: LinearLayout
    private lateinit var status: TextView
    private lateinit var challengeArea: LinearLayout
    private lateinit var entriesArea: LinearLayout
    private var challenge: MemeWarBackend.Challenge? = null
    private var mySubmission: org.json.JSONObject? = null
    private var selectedImageUri: Uri? = null
    private var imagePreview: ImageView? = null
    private var imageStatus: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 247))
        }
        setContentView(ScrollView(this).apply { addView(page) })
        page.addView(label("MEME WAR", 27, green, true))
        page.addView(label("Bring the funniest caption. Keep it original and kind.", 15, muted))
        status = label("Loading today's challenge…", 14, muted)
        page.addView(status)
        challengeArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(challengeArea)
        entriesArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(entriesArea)
        loadChallenge()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4201 && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            val uri = selectedImageUri
            if (uri != null) {
                imagePreview?.setImageURI(uri)
                imagePreview?.visibility = View.VISIBLE
                imageStatus?.text = "Photo selected. It will be uploaded with your caption."
            }
        }
    }

    private fun uploadSelectedImage(uri: Uri): String {
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        if (mime !in setOf("image/jpeg", "image/png", "image/webp")) {
            throw IllegalArgumentException("Choose a JPG, PNG or WEBP image.")
        }
        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalStateException("Could not read the selected image.")
        val maxSide = 1440
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
        ) else bitmap
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 84, output)
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        val bytes = output.toByteArray()
        if (bytes.size > 5 * 1024 * 1024) throw IllegalArgumentException("This photo is still too large. Choose another image.")
        val session = DiscoverBackend(this).currentSession()
            ?: throw IllegalStateException("Sign in first to upload a photo.")
        val path = "${session.userId}/${UUID.randomUUID()}.jpg"
        return DiscoverBackend(this).uploadMemeImage(path, bytes, "image/jpeg")
    }

    private fun loadRemoteImage(url: String, target: ImageView) {
        worker.execute {
            try {
                val bitmap = java.net.URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) runOnUiThread { target.setImageBitmap(bitmap) }
            } catch (_: Exception) { }
        }
    }

    private fun loadChallenge(excludeChallengeId: String? = null) {
        worker.execute {
            try {
                val loaded = backend.activeChallenge(excludeChallengeId)
                runOnUiThread {
                    challenge = loaded
                    if (loaded == null) {
                        status.text = "No live Meme War challenge right now. Check back soon."
                        return@runOnUiThread
                    }
                    renderChallenge(loaded)
                    if (DiscoverBackend(this).currentSession() == null) {
                        status.text = "Sign in to submit a caption, vote and report entries."
                        challengeArea.addView(Button(this).apply {
                            text = "Open sign in"
                            isAllCaps = false
                            setOnClickListener { startActivity(Intent(this@MemeWarActivity, DiscoverActivity::class.java)) }
                        })
                    } else {
                        loadPlayerData(loaded)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not load Meme War. Check your connection." }
            }
        }
    }

    private fun renderChallenge(item: MemeWarBackend.Challenge) {
        challengeArea.removeAllViews()
        val card = panel()
        card.addView(label(item.category.uppercase(), 12, green, true))
        card.addView(label(item.title, 20, ink, true))
        card.addView(label(item.instructions, 15, muted))
        if (!item.templateImageUrl.isNullOrBlank()) {
            card.addView(label("A challenge image is attached to this round.", 13, green))
        }
        challengeArea.addView(card)
        challengeArea.addView(Button(this).apply {
            text = "Try another challenge"
            isAllCaps = false
            setOnClickListener { loadChallenge(item.id) }
        })
        challengeArea.addView(gap(10))
    }

    private fun loadPlayerData(item: MemeWarBackend.Challenge) {
        status.text = "Loading approved captions…"
        worker.execute {
            try {
                val mine = backend.mySubmission(item.id)
                val entries = backend.approvedSubmissions(item.id)
                runOnUiThread {
                    mySubmission = mine
                    status.text = if (mine == null) "Your turn: submit one caption for this challenge." else
                        "Your caption status: ${mine.optString("status").uppercase()}. Each player gets one entry per challenge."
                    renderComposer(item, mine)
                    if (backend.isAdmin()) {
                        challengeArea.addView(Button(this@MemeWarActivity).apply {
                            text = "Open moderation queue"
                            isAllCaps = false
                            setOnClickListener {
                                startActivity(Intent(this@MemeWarActivity, MemeModerationActivity::class.java))
                            }
                        })
                    }
                    renderEntries(entries)
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Could not load Meme War entries." }
            }
        }
    }

    private fun renderComposer(item: MemeWarBackend.Challenge, mine: org.json.JSONObject?) {
        challengeArea.removeViews(1, (challengeArea.childCount - 1).coerceAtLeast(0))
        val card = panel()
        card.addView(label("YOUR CAPTION", 13, green, true))
        if (mine != null) {
            card.addView(label(mine.optString("caption"), 16, ink))
            card.addView(label("This challenge accepts one caption per player. You can join the next round.", 13, muted))
        } else {
            val input = EditText(this).apply {
                hint = "Write your funniest original caption…"
                minLines = 3
                maxLines = 5
                gravity = android.view.Gravity.TOP
                filters = arrayOf(InputFilter.LengthFilter(500))
                setTextColor(ink)
                setHintTextColor(muted)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setBackgroundColor(Color.rgb(242, 246, 244))
            }
            card.addView(input)
            card.addView(label("QUICK STICKERS", 12, green, true))
            val stickerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            listOf("😂", "🔥", "😭", "💀", "✨", "👀", "💯", "🤝").forEach { sticker ->
                stickerRow.addView(Button(this).apply {
                    text = sticker
                    textSize = 18f
                    isAllCaps = false
                    minWidth = dp(38)
                    setPadding(dp(2), 0, dp(2), 0)
                    setOnClickListener {
                        val cursor = input.selectionStart.coerceAtLeast(0)
                        input.text.insert(cursor, sticker)
                    }
                })
            }
            card.addView(stickerRow)
            card.addView(label("Up to 500 characters. Entries are reviewed before appearing publicly.", 12, muted))
            card.addView(Button(this).apply {
                text = "Submit caption"
                isAllCaps = false
                setOnClickListener {
                    val caption = input.text.toString().trim()
                    if (caption.isBlank()) {
                        input.error = "Write a caption first."
                        return@setOnClickListener
                    }
                    isEnabled = false
                    status.text = "Submitting your caption for review…"
                    worker.execute {
                        try {
                            backend.submitCaption(item.id, caption)
                            val refreshed = backend.mySubmission(item.id)
                            runOnUiThread {
                                mySubmission = refreshed
                                status.text = "Caption submitted. It will appear in voting after review."
                                renderComposer(item, refreshed)
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                isEnabled = true
                                status.text = e.message ?: "Your caption could not be submitted."
                            }
                        }
                    }
                }
            })
        }
        challengeArea.addView(card)
    }

    private fun renderEntries(entries: List<MemeWarBackend.Submission>) {
        entriesArea.removeAllViews()
        entriesArea.addView(label("COMMUNITY CAPTIONS", 18, green, true))
        if (entries.isEmpty()) {
            entriesArea.addView(label("No captions have been approved for voting yet. Be the first to submit one!", 14, muted))
            return
        }
        entries.forEach { entry ->
            val card = panel()
            card.addView(label(entry.caption, 17, ink, true))
            card.addView(label("${entry.votes} vote(s)", 13, muted))
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(5), 0, 0)
            }
            actions.addView(Button(this).apply {
                text = if (entry.votedByMe) "Voted" else "Vote"
                isAllCaps = false
                isEnabled = !entry.votedByMe
                setOnClickListener {
                    isEnabled = false
                    worker.execute {
                        try {
                            backend.vote(entry.id)
                            val current = challenge
                            val refreshed = if (current == null) emptyList() else backend.approvedSubmissions(current.id)
                            runOnUiThread {
                                status.text = "Your vote has been counted. Thanks for joining in!"
                                renderEntries(refreshed)
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                isEnabled = true
                                status.text = e.message ?: "Your vote could not be recorded."
                            }
                        }
                    }
                }
            })
            actions.addView(Button(this).apply {
                text = "Report"
                isAllCaps = false
                setOnClickListener { showReportDialog(entry.id) }
            })
            card.addView(actions)
            entriesArea.addView(card)
            entriesArea.addView(gap(8))
        }
    }

    private fun showReportDialog(submissionId: String) {
        val reasons = arrayOf("Harassment", "Hate speech", "Sexual content", "Violence", "Spam", "Privacy violation", "Other")
        val values = arrayOf("harassment", "hate", "sexual_content", "violence", "spam", "privacy", "other")
        AlertDialog.Builder(this)
            .setTitle("Report this caption")
            .setItems(reasons) { _, which ->
                worker.execute {
                    try {
                        backend.report(submissionId, values[which], null)
                        runOnUiThread { status.text = "Report received. Our moderation process can review it." }
                    } catch (e: Exception) {
                        runOnUiThread { status.text = e.message ?: "Could not submit your report." }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        setBackgroundColor(Color.WHITE)
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
