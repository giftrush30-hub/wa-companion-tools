package com.example.wacompanion

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("companion", Context.MODE_PRIVATE) }
    private var selectedTime = Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }
    private lateinit var root: LinearLayout
    private var reminderText: String = ""

    private val reactions = listOf(
        "Omo! Wetin be this?" to "Pidgin", "I no fit shout!" to "Pidgin",
        "Abeg, make we calm down." to "Pidgin", "Na wa o!" to "Pidgin",
        "Who send you? 😂" to "Funny", "E choke!" to "Funny",
        "I don tire abeg." to "Funny", "No be small thing!" to "Funny",
        "You made my day ❤️" to "Love", "I miss you, no lie." to "Love",
        "God is still working. 🙏" to "Faith", "Grace will speak for you." to "Faith",
        "It is well. Keep believing." to "Faith", "Congratulations! More wins!" to "Celebration",
        "Happy birthday! More life!" to "Celebration", "Big things are coming!" to "Motivation"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        reminderText = intent.getStringExtra("reminder_message") ?: ""
        showHome()
        if (reminderText.isNotBlank()) showReminder(reminderText)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        }
    }

    private fun showHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(246, 249, 247))
            setPadding(dp(18), dp(22), dp(18), dp(20))
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("WA COMPANION TOOLS", 23, Color.rgb(6, 63, 50), true))
        root.addView(label("Extra tools that work alongside your official WhatsApp.", 14, Color.DKGRAY))
        root.addView(space(16))
        root.addView(label("Your WhatsApp stays installed and unchanged.", 13, Color.rgb(6, 100, 70)))
        root.addView(space(18))

        card("NAIJA WHATSAPP STICKERS", "Preview original Nigerian reactions and add the starter sticker pack to WhatsApp with your confirmation.") { stickerStudio() }
        card("DAILY MESSAGE & CAPTION GENERATOR", "Get fresh Nigerian-friendly ideas for greetings, faith, love, motivation and business. Copy or share the one you like.") { dailyMessageTool() }
        card("NIGERIAN MEME & REACTION STUDIO", "Choose a Nigerian-style reaction or write your own, turn it into a shareable image, and send it to WhatsApp or your status.") { memeStudio() }
        card("DAILY STATUS CHALLENGE", "Get a fresh daily prompt, create your response and share it as a status.") { dailyChallenge() }
        card("REPLY-TO-STATUS ASSISTANT", "Bring in a status or describe it, then get thoughtful reply ideas in different tones.") { replyAssistant() }
        card("STATUS & FLYER STUDIO", "Create shareable cards for birthdays, business adverts and announcements.") { statusStudio() }
        card("NAIJA MESSAGE LAB", "Polish messages in clear English, friendly Pidgin or a respectful business tone.") { messageLab() }
        card("SMALL BUSINESS FOLLOW-UP DESK", "Keep customer names, order notes and follow-ups on this phone.") { businessDesk() }
        card("PRICE, PROFIT & QUOTATION CALCULATOR", "Work out customer prices, estimated profit and a ready-to-share quotation in naira.") { quotationTool() }
        card("QUICK REPLIES", "Save messages you use often, then copy or share them into WhatsApp.") {
            quickReplySection()
        }
        card("MESSAGE REMINDERS", "Set a time to be reminded. At the right time, open the reminder and send it yourself in WhatsApp.") {
            reminderSection()
        }
        card("PRIVACY SHORTCUTS", "Open the official WhatsApp privacy and security settings. These shortcuts do not change hidden WhatsApp controls.") {
            button("Open WhatsApp") {
                val launch = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (launch != null) startActivity(launch) else toast("WhatsApp is not installed.")
            }
            button("Open Android app settings") {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:com.whatsapp")))
            }
        }
        card("STATUS ORGANISER", "Keep your own notes about statuses you have permission to save. This tool does not scrape or secretly download anyone's status.") {
            val note = EditText(this).apply { hint = "Add a note or paste a status caption"; minLines = 2 }
            root.addView(note)
            button("Save note") {
                val old = prefs.getString("status_notes", "") ?: ""
                val addition = note.text.toString().trim()
                if (addition.isEmpty()) {
                    toast("Write a note first.")
                } else {
                    val updated = (old + if (old.isBlank()) "" else "\n\n" + addition).trim()
                    prefs.edit().putString("status_notes", updated).apply()
                    note.setText("")
                    toast("Note saved on this phone.")
                    showNotes()
                }
            }
            showNotes()
        }
        root.addView(space(10))
        root.addView(label("Note: This is a companion utility, not a modified WhatsApp client. It cannot read private chats, hide typing indicators, or send messages automatically.", 12, Color.GRAY))
    }


    private fun stickerStudio() {
        root.addView(label("NAIJA REACTIONS • STARTER PACK", 16, Color.rgb(6, 63, 50), true))
        root.addView(label("Six original Nigerian reaction stickers. Add the pack once, then use the stickers from WhatsApp's sticker tray.", 13, Color.DKGRAY))
        val samples = listOf("Omo! Wetin be this? 😳", "I no fit shout! 😂", "Abeg o! 🙏", "Na wa o! 😮", "God abeg! 🙏", "E choke! 🔥")
        samples.forEach { phrase ->
            val preview = TextView(this).apply {
                text = phrase
                textSize = 20f
                setTextColor(Color.rgb(6, 63, 50))
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundColor(Color.WHITE)
            }
            root.addView(preview)
            root.addView(space(5))
        }
        button("Add Naija Reactions to WhatsApp") {
            try {
                val intent = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").apply {
                    putExtra("sticker_pack_id", NaijaStickerProvider.PACK_ID)
                    putExtra("sticker_pack_authority", NaijaStickerProvider.AUTHORITY)
                    putExtra("sticker_pack_name", "Naija Reactions")
                }
                startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                toast("WhatsApp was not found. Install or update WhatsApp and try again.")
            } catch (_: Exception) {
                toast("Could not open WhatsApp sticker confirmation.")
            }
        }
        button("Add to WhatsApp Business") {
            try {
                val intent = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").apply {
                    setPackage("com.whatsapp.w4b")
                    putExtra("sticker_pack_id", NaijaStickerProvider.PACK_ID)
                    putExtra("sticker_pack_authority", NaijaStickerProvider.AUTHORITY)
                    putExtra("sticker_pack_name", "Naija Reactions")
                }
                startActivity(intent)
            } catch (_: Exception) { toast("WhatsApp Business was not found or could not open the sticker confirmation.") }
        }
        root.addView(label("WhatsApp must show its own confirmation before the pack is added. Stickers are generated on this phone and do not require downloading a large pack.", 11, Color.GRAY))
    }

    private fun dailyMessageTool() {
        val collections = linkedMapOf(
            "Naija vibes" to listOf(
                "Omo, small small progress still be progress. No give up.",
                "No let today's wahala make you forget tomorrow's possibilities.",
                "Abeg, protect your peace and keep your focus.",
                "One step at a time. We go reach there."
            ),
            "Faith" to listOf(
                "God's grace is enough for today. Keep moving in faith.",
                "May strength meet you where you feel tired, and hope rise again.",
                "Your season can change. Keep praying, preparing and doing your part.",
                "May the Lord guide your steps and bless the work of your hands."
            ),
            "Love & friendship" to listOf(
                "You make ordinary moments feel special. I appreciate you.",
                "Real love shows up with patience, honesty and care.",
                "Just checking on you. I hope today is treating you kindly.",
                "Some people make life lighter simply by being in it. Thank you."
            ),
            "Motivation" to listOf(
                "Keep building quietly. Let your progress speak in time.",
                "A slow beginning is still a beginning. Start where you are.",
                "Consistency can take you places excitement alone cannot.",
                "Rest if you need to, but do not give up on what matters."
            ),
            "Small business" to listOf(
                "Thank you for choosing our business. Your support keeps us growing.",
                "Good products, honest prices and customer care. Send a message to order.",
                "We appreciate every referral, enquiry and purchase. Thank you!",
                "Need something reliable? We are ready to help. Send us a message."
            )
        )
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, collections.keys.toList())
        }
        root.addView(label("CHOOSE A COLLECTION", 13, Color.rgb(6, 63, 50), true))
        root.addView(category)
        val output = TextView(this).apply {
            text = "Tap “Give me an idea” to create a message."
            textSize = 18f
            setTextColor(Color.rgb(20, 55, 48))
            setPadding(dp(12), dp(18), dp(12), dp(18))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(output)
        fun generate() {
            val options = collections[category.selectedItem.toString()].orEmpty()
            val previous = output.tag as? String
            val choices = options.filter { it != previous }
            val chosen = (choices.ifEmpty { options }).random()
            output.text = chosen
            output.tag = chosen
        }
        button("Give me an idea") { generate() }
        button("Copy message") {
            val value = output.tag as? String
            if (value.isNullOrBlank()) toast("Generate a message first.") else {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Daily message", value))
                toast("Message copied.")
            }
        }
        button("Share to WhatsApp") {
            val value = output.tag as? String
            if (value.isNullOrBlank()) toast("Generate a message first.") else shareToWhatsApp(value)
        }
        root.addView(label("Messages are built into the app and work offline. Review and personalise before sharing.", 11, Color.GRAY))
    }

    private fun memeStudio() {
        val search = EditText(this).apply { hint = "Search reactions"; isSingleLine = true }
        root.addView(search)
        val categories = listOf("All", "Pidgin", "Funny", "Love", "Faith", "Celebration", "Motivation")
        val categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(categoryRow); root.addView(results)
        var selected = "All"
        fun render() {
            categoryRow.removeAllViews(); results.removeAllViews()
            categories.forEach { category ->
                val b = Button(this).apply { text = category; isAllCaps = false; textSize = 10f; setPadding(dp(3), 0, dp(3), 0) }
                b.setOnClickListener { selected = category; render() }
                categoryRow.addView(b)
            }
            val q = search.text.toString().lowercase(Locale.getDefault())
            val found = reactions.filter { (phrase, cat) ->
                (selected == "All" || cat == selected) &&
                    (q.isBlank() || phrase.lowercase(Locale.getDefault()).contains(q) || cat.lowercase(Locale.getDefault()).contains(q))
            }
            if (found.isEmpty()) results.addView(label("No reactions found.", 13, Color.GRAY))
            found.forEach { (phrase, cat) ->
                val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(7), dp(6), dp(7)) }
                item.addView(label(phrase, 16, Color.rgb(20, 55, 48), true))
                item.addView(label(cat, 11, Color.GRAY))
                val actions = LinearLayout(this)
                val make = Button(this).apply { text = "Create image"; isAllCaps = false }
                make.setOnClickListener { shareMemeImage(phrase) }
                val copy = Button(this).apply { text = "Copy"; isAllCaps = false }
                copy.setOnClickListener {
                    (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Reaction", phrase))
                    toast("Reaction copied.")
                }
                actions.addView(make); actions.addView(copy); item.addView(actions); results.addView(item)
            }
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        render()
        root.addView(space(8))
        root.addView(label("CREATE YOUR OWN", 14, Color.rgb(6, 63, 50), true))
        val custom = EditText(this).apply { hint = "Write your own reaction or caption"; minLines = 2; maxLines = 4 }
        root.addView(custom)
        button("Create and share my image") {
            val text = custom.text.toString().trim()
            if (text.isBlank()) toast("Write a message first.") else shareMemeImage(text)
        }
        root.addView(label("The starter reactions work offline. More original packs can be added in future updates.", 11, Color.GRAY))
    }

    private fun shareMemeImage(message: String) {
        try {
            val size = 1080
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val palette = intArrayOf(Color.rgb(6,63,50), Color.rgb(30,45,80), Color.rgb(105,45,65), Color.rgb(115,75,20))
            canvas.drawColor(palette[message.length % palette.size])
            paint.color = Color.argb(35,255,255,255)
            canvas.drawCircle(950f, 100f, 260f, paint); canvas.drawCircle(80f, 1040f, 300f, paint)
            paint.color = Color.rgb(230,195,120); paint.textSize = 34f
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            canvas.drawText("WA COMPANION • NAIJA REACTIONS", 65f, 105f, paint)
            paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            val words = message.split(Regex("\\s+"))
            var font = 82f
            var lines = listOf<String>()
            while (font >= 44f) {
                paint.textSize = font
                val temp = mutableListOf<String>(); var line = ""
                words.forEach { word ->
                    val next = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(next) > 900f && line.isNotEmpty()) { temp.add(line); line = word } else line = next
                }
                if (line.isNotEmpty()) temp.add(line)
                lines = temp
                if (lines.size <= 5) break
                font -= 6f
            }
            paint.textSize = font
            val lineHeight = font * 1.25f
            var y = (size - lines.size * lineHeight) / 2f + font
            lines.forEach { canvas.drawText(it, size / 2f, y, paint); y += lineHeight }
            paint.textSize = 28f; paint.color = Color.rgb(230,195,120)
            canvas.drawText("Made to share • Keep it kind", size / 2f, 970f, paint)
            val dir = File(cacheDir, "shared_memes").apply { mkdirs() }
            val file = File(dir, "reaction_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, message)
                clipData = ClipData.newUri(contentResolver, "Reaction image", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Share your reaction"))
        } catch (_: Exception) { toast("Could not create image. Please try again.") }
    }


    private fun dailyChallenge() {
        val challenges = listOf(
            "Share one thing you are grateful for today.",
            "Post a photo that reminds you of a beautiful memory.",
            "Encourage someone who may be having a difficult week.",
            "Share one lesson life has taught you recently.",
            "Make somebody smile with a clean Nigerian joke.",
            "Show one small achievement you are proud of.",
            "Share a hope or prayer for the new week.",
            "Tell people about a skill or service you can offer.",
            "Complete this sentence: I am becoming someone who...",
            "Share one piece of advice you wish you knew earlier.",
            "Celebrate someone who has helped you.",
            "Post something beautiful you noticed today."
        )
        val today = java.util.Calendar.getInstance()
        val index = Math.floorMod(today.get(java.util.Calendar.DAY_OF_YEAR) + today.get(java.util.Calendar.YEAR) * 3, challenges.size)
        val prompt = challenges[index]
        val dateLabel = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(today.time)
        root.addView(label("TODAY'S CREATIVE CHALLENGE", 16, Color.rgb(6, 63, 50), true))
        root.addView(label(dateLabel, 12, Color.GRAY))
        root.addView(label(prompt, 21, Color.rgb(20, 55, 48), true))
        val response = EditText(this).apply {
            hint = "Write your response or status here"
            minLines = 3
        }
        root.addView(response)
        button("Use a suggested opening") {
            response.setText("Today's challenge: $prompt\n\nMy answer: ")
            response.setSelection(response.text.length)
        }
        button("Design and share challenge") {
            val value = response.text.toString().trim()
            if (value.isBlank()) toast("Write your response first.") else shareMemeImage(value)
        }
        button("Copy challenge prompt") {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("Daily challenge", prompt))
            toast("Challenge copied.")
        }
        root.addView(label("Your challenge changes by date and works offline. Your progress stays private on this phone.", 11, Color.GRAY))
    }

    private fun replyAssistant() {
        root.addView(label("TURN A STATUS INTO A GOOD CONVERSATION", 16, Color.rgb(6, 63, 50), true))
        root.addView(label("Describe the status or paste its text. Only use content you choose to share with this app.", 12, Color.GRAY))
        val contextInput = EditText(this).apply {
            hint = "Example: My friend posted that they got a new job..."
            minLines = 3
        }
        root.addView(contextInput)
        val tones = listOf("Warm and friendly", "Funny and playful", "Encouraging", "Respectful", "Professional", "Short and simple")
        val tone = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, tones)
        }
        root.addView(label("CHOOSE YOUR TONE", 12, Color.rgb(6, 63, 50), true))
        root.addView(tone)
        val result = EditText(this).apply { hint = "Your reply suggestions will appear here"; minLines = 3 }
        root.addView(result)
        button("Suggest replies") {
            val contextText = contextInput.text.toString().trim()
            if (contextText.isBlank()) {
                toast("Describe or paste the status first.")
            } else {
                val suggestions = when (tone.selectedItemPosition) {
                    0 -> listOf("This made me smile. Wishing you even more beautiful moments!", "So happy for you. You deserve good things!")
                    1 -> listOf("Omo! This update is giving premium enjoyment 😂", "See person wey dey make moves! More wins!")
                    2 -> listOf("Keep going. This is a beautiful step forward.", "May this be the beginning of even greater things.")
                    3 -> listOf("Thank you for sharing this. I genuinely appreciate it.", "Wishing you peace and the very best.")
                    4 -> listOf("Congratulations on this milestone. Wishing you continued success.", "That is wonderful news. Best wishes for what comes next.")
                    else -> listOf("That's lovely! ❤️", "Really happy for you. More wins!")
                }
                result.setText(suggestions.joinToString("\n\n"))
            }
        }
        button("Share selected reply") {
            val value = result.text.toString().trim()
            if (value.isBlank()) toast("Generate a reply first.") else shareToWhatsApp(value)
        }
        root.addView(label("These are offline writing suggestions, not AI and not automatic WhatsApp replies. Choose and edit your words before sending.", 11, Color.GRAY))
    }

    private fun statusStudio() {
        root.addView(label("MAKE A STATUS PEOPLE WANT TO READ", 18, Color.rgb(6, 63, 50), true))
        root.addView(label("Choose a mood, get a ready-made idea, edit it and share it to WhatsApp Status.", 13, Color.DKGRAY))

        val moodNames = listOf("Grateful", "Funny / Naija vibes", "Faith and hope", "Love and friendship",
            "Focused / motivation", "Business promotion", "Birthday / celebration", "Quiet confidence")
        val mood = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, moodNames)
        }
        root.addView(label("WHAT IS YOUR MOOD TODAY?", 12, Color.rgb(6, 63, 50), true))
        root.addView(mood)

        val ideas = listOf(
            listOf("Today, I choose gratitude. Small blessings still count. 🙏", "My heart is full. Thankful for the little things and the big things."),
            listOf("Omo, life no get duplicate. Enjoy your day and make you smile! 😂", "I came, I saw, I laughed. Some things no get explanation."),
            listOf("God is still writing my story. I will not give up halfway. 🙏", "One day at a time, one prayer at a time. Grace will carry me."),
            listOf("Good people make ordinary days feel special. ❤️", "May we never lack genuine love, loyal friends and peaceful hearts."),
            listOf("Quiet work. Clear goals. Consistent progress. No noise needed.", "I may be moving slowly, but I am not standing still."),
            listOf("Your support keeps this dream growing. Send a message to place your order today.", "Good value, honest service and care in every order. Thank you for choosing us."),
            listOf("Another year, another reason to celebrate life. More grace and beautiful memories! 🎉", "May this new chapter bring peace, good health and new opportunities."),
            listOf("Not every move needs an announcement. Let the results speak.", "Protect your peace. Keep your standards. Continue growing.")
        )
        val output = EditText(this).apply {
            hint = "Your status will appear here. You can edit it."
            minLines = 4
            setText(ideas[0][0])
        }
        root.addView(space(8))
        button("Give me a fresh idea") {
            val options = ideas[mood.selectedItemPosition]
            val current = output.text.toString().trim()
            output.setText(options.firstOrNull { it != current } ?: options.last())
        }
        button("Create a 3-part status story") {
            val topic = output.text.toString().trim().ifBlank { "A new chapter is beginning" }
            output.setText("1/3 • A little reminder for today...\n\n2/3 • $topic\n\n3/3 • Keep going. Your next chapter is still being written. ✨")
        }
        root.addView(output)
        button("Design and share to WhatsApp") {
            val value = output.text.toString().trim()
            if (value.isBlank()) toast("Write your status first.") else shareMemeImage(value)
        }
        button("Save this as a draft") {
            val value = output.text.toString().trim()
            if (value.isBlank()) toast("Write something first.") else {
                val old = prefs.getString("status_drafts", "") ?: ""
                prefs.edit().putString("status_drafts", old + (if (old.isBlank()) "" else "\n\n") + value).apply()
                toast("Draft saved on this phone.")
            }
        }
        val drafts = prefs.getString("status_drafts", "") ?: ""
        if (drafts.isNotBlank()) {
            root.addView(space(10))
            root.addView(label("YOUR SAVED STATUS DRAFTS", 13, Color.rgb(6, 63, 50), true))
            root.addView(label(drafts, 13, Color.DKGRAY))
        }
        root.addView(label("Works offline. Review your wording before sharing. WhatsApp asks you to confirm the Status post.", 11, Color.GRAY))
    }

    private fun messageLab() {
        val input = EditText(this).apply { hint = "Type a message to improve"; minLines = 3 }
        root.addView(input)
        val style = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Clear and polite", "Nigerian Pidgin", "Professional business", "Warm and friendly", "Firm but respectful"))
        }
        root.addView(style)
        val output = TextView(this).apply { textSize = 15f; setTextColor(Color.rgb(6, 63, 50)); setPadding(0, dp(10), 0, dp(10)) }
        root.addView(output)
        button("Improve my message") {
            val original = input.text.toString().trim()
            if (original.isBlank()) toast("Type a message first.") else {
                val base = original.replaceFirstChar { it.lowercase() }
                val improved = when (style.selectedItemPosition) {
                    1 -> "Abeg, " + base + " Make we understand each other. Thank you."
                    2 -> "Good day. " + base + " Kindly let me know if you need further information. Thank you."
                    3 -> "Hello! " + base + " I appreciate you."
                    4 -> "Please, " + base + " I would appreciate your response. Thank you."
                    else -> original.trimEnd('.', ' ') + ". Thank you for your understanding."
                }
                output.text = improved; output.tag = improved
            }
        }
        button("Copy improved message") {
            val value = output.tag as? String
            if (value.isNullOrBlank()) toast("Improve a message first.") else {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Improved message", value))
                toast("Copied.")
            }
        }
        button("Share improved message") {
            val value = output.tag as? String
            if (value.isNullOrBlank()) toast("Improve a message first.") else shareToWhatsApp(value)
        }
        root.addView(label("Offline phrase styling, not live AI translation. Review the meaning before sending.", 11, Color.GRAY))
    }

    private fun businessDesk() {
        val customer = EditText(this).apply { hint = "Customer name or nickname"; isSingleLine = true }
        val contact = EditText(this).apply { hint = "Phone number (optional)"; isSingleLine = true; inputType = 3 }
        val task = EditText(this).apply { hint = "Order, promise or follow-up note"; minLines = 2 }
        val amount = EditText(this).apply { hint = "Amount in naira (optional)"; isSingleLine = true; inputType = 2 }
        val addReminder = CheckBox(this).apply { text = "Remind me to follow up with this customer"; isChecked = false }
        val dueButton = Button(this).apply { text = "Choose follow-up reminder time"; isAllCaps = false }
        root.addView(customer); root.addView(contact); root.addView(task); root.addView(amount)
        root.addView(addReminder); root.addView(dueButton)
        dueButton.setOnClickListener { chooseDateTime(dueButton) }
        val list = TextView(this).apply { textSize = 13f; setTextColor(Color.DKGRAY); setPadding(0, dp(10), 0, dp(6)) }
        fun refresh() {
            val entries = prefs.getString("business_followups", "") ?: ""
            list.text = if (entries.isBlank()) "No customer follow-ups saved yet." else "SAVED FOLLOW-UPS\n\n" + entries
        }
        button("Save customer follow-up") {
            val name = customer.text.toString().trim(); val note = task.text.toString().trim()
            if (name.isBlank() || note.isBlank()) toast("Enter a customer name and follow-up note.") else {
                val money = amount.text.toString().trim(); val phone = contact.text.toString().trim()
                val record = "• " + name + (if (phone.isBlank()) "" else " (" + phone + ")") +
                    (if (money.isBlank()) "" else " — ₦" + money) + "\n  " + note
                val old = prefs.getString("business_followups", "") ?: ""
                prefs.edit().putString("business_followups", old + (if (old.isBlank()) "" else "\n\n") + record).apply()
                if (addReminder.isChecked) {
                    if (selectedTime.timeInMillis <= System.currentTimeMillis()) {
                        toast("Customer saved, but choose a future time to set the reminder.")
                    } else {
                        scheduleReminder("Follow up with $name: $note", selectedTime.timeInMillis)
                        toast("Customer saved and reminder scheduled.")
                    }
                } else {
                    toast("Saved on this phone.")
                }
                customer.setText(""); contact.setText(""); task.setText(""); amount.setText("")
                addReminder.isChecked = false
                dueButton.text = "Choose follow-up reminder time"
                refresh()
            }
        }
        root.addView(list); refresh()
        root.addView(label("Records stay on this phone. Clearing app data or uninstalling may erase them.", 11, Color.GRAY))
    }

    private fun quotationTool() {
        val product = EditText(this).apply { hint = "Product or service name"; isSingleLine = true }
        val cost = EditText(this).apply { hint = "Your cost per item (₦)"; inputType = 2 }
        val price = EditText(this).apply { hint = "Selling price per item (₦)"; inputType = 2 }
        val quantity = EditText(this).apply { hint = "Quantity"; inputType = 2 }
        val customer = EditText(this).apply { hint = "Customer name (optional)"; isSingleLine = true }
        root.addView(product); root.addView(cost); root.addView(price); root.addView(quantity); root.addView(customer)
        val result = TextView(this).apply { textSize = 15f; setTextColor(Color.rgb(6, 63, 50)); setPadding(0, dp(12), 0, dp(12)) }
        root.addView(result)
        var quote = ""
        button("Calculate total and profit") {
            val name = product.text.toString().trim()
            val c = cost.text.toString().toDoubleOrNull()
            val p = price.text.toString().toDoubleOrNull()
            val q = quantity.text.toString().toIntOrNull()
            if (name.isBlank() || c == null || p == null || q == null || c < 0 || p < 0 || q <= 0) {
                toast("Enter a product, valid amounts and a quantity above zero.")
            } else {
                val totalCost = c * q; val total = p * q; val profit = (p - c) * q
                val currency = java.text.NumberFormat.getNumberInstance(Locale("en", "NG"))
                quote = "QUOTATION\n$name\nQuantity: $q\nUnit price: ₦${currency.format(p)}\nTotal: ₦${currency.format(total)}\nEstimated profit: ₦${currency.format(profit)}"
                val buyer = customer.text.toString().trim()
                if (buyer.isNotBlank()) quote = "Hello $buyer,\n\n$quote"
                result.text = quote + "\n\nTotal cost to you: ₦${currency.format(totalCost)}"
            }
        }
        button("Share quotation") { if (quote.isBlank()) toast("Calculate a quotation first.") else shareToWhatsApp(quote) }
        root.addView(label("Profit is an estimate based on the cost and selling price entered. Include transport, fees, tax and other expenses in your cost if needed.", 11, Color.GRAY))
    }
    private fun quickReplySection() {
        val input = EditText(this).apply { hint = "Write a reusable reply"; minLines = 2 }
        root.addView(input)
        button("Save quick reply") {
            val value = input.text.toString().trim()
            if (value.isEmpty()) toast("Write a reply first.")
            else {
                val list = getReplies().toMutableList()
                list.add(value)
                prefs.edit().putString("replies", list.joinToString("\u001F")).apply()
                input.setText("")
                toast("Quick reply saved.")
                showReplies()
            }
        }
        showReplies()
    }

    private fun getReplies(): List<String> =
        (prefs.getString("replies", "") ?: "").split("\u001F").filter { it.isNotBlank() }

    private fun showReplies() {
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(holder)
        getReplies().forEachIndexed { index, reply ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(label(reply, 14, Color.DKGRAY))
            val actions = LinearLayout(this)
            val copy = Button(this).apply { text = "Copy"; isAllCaps = false }
            copy.setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Quick reply", reply))
                toast("Copied. Open WhatsApp and paste.")
            }
            val share = Button(this).apply { text = "Share to WhatsApp"; isAllCaps = false }
            share.setOnClickListener { shareToWhatsApp(reply) }
            val delete = Button(this).apply { text = "Delete"; isAllCaps = false }
            delete.setOnClickListener {
                val updated = getReplies().toMutableList().also { it.removeAt(index) }
                prefs.edit().putString("replies", updated.joinToString("\u001F")).apply()
                showHome()
            }
            actions.addView(copy)
            actions.addView(share)
            actions.addView(delete)
            row.addView(actions)
            holder.addView(row)
        }
    }

    private fun reminderSection() {
        val message = EditText(this).apply { hint = "What do you want to remember to send?"; minLines = 2 }
        root.addView(message)
        val timeButton = Button(this).apply { text = "Choose reminder time"; isAllCaps = false }
        root.addView(timeButton)
        timeButton.setOnClickListener { chooseDateTime(timeButton) }
        button("Set reminder") {
            val value = message.text.toString().trim()
            if (value.isEmpty()) toast("Enter the message first.")
            else if (selectedTime.timeInMillis <= System.currentTimeMillis()) toast("Choose a future time.")
            else {
                scheduleReminder(value, selectedTime.timeInMillis)
                message.setText("")
                toast("Reminder set.")
            }
        }
    }

    private fun chooseDateTime(button: Button) {
        DatePickerDialog(this, { _, y, m, d ->
            selectedTime.set(Calendar.YEAR, y)
            selectedTime.set(Calendar.MONTH, m)
            selectedTime.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(this, { _, h, min ->
                selectedTime.set(Calendar.HOUR_OF_DAY, h)
                selectedTime.set(Calendar.MINUTE, min)
                selectedTime.set(Calendar.SECOND, 0)
                button.text = SimpleDateFormat("EEE, d MMM • h:mm a", Locale.getDefault()).format(selectedTime.time)
            }, selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE), false).show()
        }, selectedTime.get(Calendar.YEAR), selectedTime.get(Calendar.MONTH), selectedTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun scheduleReminder(message: String, at: Long) {
        val intent = Intent(this, ReminderReceiver::class.java).putExtra("message", message)
        val pi = PendingIntent.getBroadcast(this, (at / 60000).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            toast("Allow alarms, then set the reminder again.")
            return
        }
        if (Build.VERSION.SDK_INT >= 23) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun showReminder(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Your message reminder")
            .setMessage(message)
            .setPositiveButton("Share to WhatsApp") { _, _ -> shareToWhatsApp(message) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun shareToWhatsApp(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            startActivity(send)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(fallback, "Share message"))
        }
    }

    private fun showNotes() {
        val notes = prefs.getString("status_notes", "") ?: ""
        if (notes.isNotBlank()) root.addView(label("Saved notes:\n$notes", 13, Color.DKGRAY))
    }

    private fun card(title: String, description: String, content: () -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.WHITE)
        }
        val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        root.addView(box, params)
        box.addView(label(title, 16, Color.rgb(6, 63, 50), true))
        box.addView(label(description, 13, Color.DKGRAY))
        box.addView(space(8))
        val oldRoot = root
        root = box
        content()
        root = oldRoot
    }

    private fun button(text: String, action: () -> Unit) {
        val b = Button(this).apply { this.text = text; isAllCaps = false }
        b.setOnClickListener { action() }
        root.addView(b)
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
