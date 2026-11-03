package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.HorizontalScrollView
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
    private var captionStyle = "classic"
    private var captionPosition = "top"

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

    private fun uploadMemeImage(uri: Uri?, templateUrl: String?, caption: String): String? {
        if (uri == null && templateUrl.isNullOrBlank()) return null
        val bitmap = if (uri != null) {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } else {
            java.net.URL(templateUrl).openStream().use { BitmapFactory.decodeStream(it) }
        } ?: throw IllegalStateException("Could not read the selected image or challenge template.")

        val maxSide = 1440
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
        ) else bitmap
        val composed = resized.copy(Bitmap.Config.ARGB_8888, true)
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        drawCaptionOverlay(composed, caption, captionStyle, captionPosition)

        val output = ByteArrayOutputStream()
        composed.compress(Bitmap.CompressFormat.JPEG, 86, output)
        composed.recycle()
        val bytes = output.toByteArray()
        if (bytes.size > 5 * 1024 * 1024) throw IllegalArgumentException("This image is still too large. Choose another photo.")
        val session = DiscoverBackend(this).currentSession()
            ?: throw IllegalStateException("Sign in first to upload a photo.")
        val path = "${session.userId}/${UUID.randomUUID()}.jpg"
        return DiscoverBackend(this).uploadMemeImage(path, bytes, "image/jpeg")
    }

    private fun drawCaptionOverlay(bitmap: Bitmap, caption: String, style: String, position: String) {
        val canvas = Canvas(bitmap)
        val text = caption.trim().uppercase()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (bitmap.width * 0.052f).coerceIn(28f, 68f)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val maxWidth = bitmap.width * 0.88f
        val lines = mutableListOf<String>()
        var current = ""
        text.split(Regex("\\s+")).forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textPaint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                lines.add(current)
                current = word
            }
        }
        if (current.isNotBlank()) lines.add(current)
        val visibleLines = lines.take(6).toMutableList()
        if (lines.size > 6 && visibleLines.isNotEmpty()) {
            visibleLines[5] = visibleLines[5].take(24) + "…"
        }
        val lineHeight = textPaint.textSize * 1.12f
        val padding = bitmap.width * 0.035f
        val bannerHeight = padding * 2 + lineHeight * visibleLines.size
        val top = if (position == "bottom") bitmap.height - bannerHeight else 0f
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (style == "clean") Color.argb(220, 255, 255, 255) else Color.argb(178, 0, 0, 0)
        }
        canvas.drawRect(0f, top, bitmap.width.toFloat(), top + bannerHeight, bannerPaint)
        visibleLines.forEachIndexed { index, line ->
            val y = top + padding + textPaint.textSize + index * lineHeight
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = textPaint.textSize * 0.13f
            textPaint.color = if (style == "clean") Color.WHITE else Color.BLACK
            canvas.drawText(line, bitmap.width / 2f, y, textPaint)
            textPaint.style = Paint.Style.FILL
            textPaint.color = when (style) {
                "bold" -> Color.rgb(255, 220, 45)
                "clean" -> Color.rgb(24, 40, 34)
                else -> Color.WHITE
            }
            canvas.drawText(line, bitmap.width / 2f, y, textPaint)
        }
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
        selectedImageUri = null
        imagePreview = null
        imageStatus = null
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
            val template = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(190)
                )
            }
            card.addView(template)
            loadRemoteImage(item.templateImageUrl, template)
            if (!item.templateCredit.isNullOrBlank()) {
                card.addView(label(item.templateCredit, 11, muted))
            }
        }
        challengeArea.addView(card)
        challengeArea.addView(Button(this).apply {
            text = "Share this Meme War challenge"
            isAllCaps = false
            setOnClickListener {
                ArenaShareCard.share(
                    this@MemeWarActivity,
                    "MEME WAR CHALLENGE",
                    "${item.title}\\n\\n${item.instructions}\\n\\nWrite the funniest clean caption you can think of. Tag a friend who would win this round!",
                    "MAKE YOUR CAPTION  •  DAILY ARENA",
                    item.templateImageUrl,
                    "Share Meme War challenge image"
                )
            }
        })
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
        challengeArea.removeViews(3, (challengeArea.childCount - 3).coerceAtLeast(0))
        val card = panel()
        card.addView(label("YOUR CAPTION", 13, green, true))
        if (mine != null) {
            card.addView(label(mine.optString("caption"), 16, ink))
            val ownImageUrl = mine.optString("image_url").takeIf { it.isNotBlank() && it != "null" }
            if (ownImageUrl != null) {
                val ownPhoto = ImageView(this).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(180)
                    )
                }
                card.addView(ownPhoto)
                loadRemoteImage(ownImageUrl, ownPhoto)
            }
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
            imagePreview = ImageView(this).apply {
                visibility = View.GONE
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(180)
                )
            }
            card.addView(imagePreview)
            imageStatus = label(if (item.templateImageUrl.isNullOrBlank()) "You can add a photo or submit text only." else "Your caption will be placed on this challenge image. You can also choose your own photo.", 12, muted)
            card.addView(imageStatus)
            card.addView(Button(this).apply {
                text = "Add a photo"
                isAllCaps = false
                setOnClickListener {
                    val picker = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                    startActivityForResult(Intent.createChooser(picker, "Choose a Meme War photo"), 4201)
                }
            })
            val styleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            styleRow.addView(Button(this).apply {
                text = "Text style"
                isAllCaps = false
                setOnClickListener {
                    val labels = arrayOf("Classic white", "Bold yellow", "Clean dark")
                    val values = arrayOf("classic", "bold", "clean")
                    AlertDialog.Builder(this@MemeWarActivity).setTitle("Choose caption style")
                        .setItems(labels) { _, which -> captionStyle = values[which] }.show()
                }
            })
            styleRow.addView(Button(this).apply {
                text = "Text position"
                isAllCaps = false
                setOnClickListener {
                    val labels = arrayOf("Top banner", "Bottom banner")
                    val values = arrayOf("top", "bottom")
                    AlertDialog.Builder(this@MemeWarActivity).setTitle("Choose caption position")
                        .setItems(labels) { _, which -> captionPosition = values[which] }.show()
                }
            })
            card.addView(styleRow)
            card.addView(label("STICKER LIBRARY", 12, green, true))
            val stickerScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
            }
            val stickerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val stickerItems = listOf(
                "😂" to "1f602", "🔥" to "1f525", "😭" to "1f62d", "💀" to "1f480",
                "✨" to "2728", "👀" to "1f440", "💯" to "1f4af", "🤝" to "1f91d",
                "🥳" to "1f973", "🤣" to "1f923", "😎" to "1f60e", "🙌" to "1f64c",
                "😅" to "1f605", "❤️" to "2764-fe0f"
            )
            stickerItems.forEach { (sticker, code) ->
                val stickerImage = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                        marginEnd = dp(7)
                    }
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    setBackgroundColor(Color.rgb(242, 246, 244))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = "Insert $sticker sticker"
                    setOnClickListener {
                        val cursor = input.selectionStart.coerceAtLeast(0)
                        input.text.insert(cursor, sticker)
                    }
                }
                stickerRow.addView(stickerImage)
                loadRemoteImage("https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/$code.png", stickerImage)
            }
            stickerScroll.addView(stickerRow)
            card.addView(stickerScroll)
            card.addView(label("Sticker artwork: Twemoji, CC BY 4.0.", 10, muted))
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
                            val photoUrl = uploadMemeImage(selectedImageUri, item.templateImageUrl, caption)
                            backend.submitCaption(item.id, caption, photoUrl)
                            selectedImageUri = null
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
            if (!entry.imageUrl.isNullOrBlank()) {
                val photo = ImageView(this).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(190)
                    )
                }
                card.addView(photo)
                loadRemoteImage(entry.imageUrl, photo)
            }
            card.addView(label("${entry.votes} vote(s)", 13, muted))
            card.addView(Button(this).apply {
                text = "Share this caption"
                isAllCaps = false
                setOnClickListener {
                    ArenaShareCard.share(
                        this@MemeWarActivity,
                        "MEME WAR CAPTION",
                        entry.caption + "\\n\\n${entry.votes} community vote(s). Which caption is your favourite?",
                        "VOTE FOR YOUR FAVOURITE  •  DAILY ARENA",
                        entry.imageUrl,
                        "Share Meme War caption image"
                    )
                }
            })
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
