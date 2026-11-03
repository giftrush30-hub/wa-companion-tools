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
import android.graphics.BitmapFactory
import android.graphics.RectF
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
        card("STATUS CONVERSATION GAMES", "Create fun polls, choose-one questions and conversation starters for your Status.") { statusConversationGames() }
        card("7-DAY STATUS CALENDAR", "Plan, save and share a week of status ideas without needing internet.") { statusCalendar() }
        card("PERSONAL BRAND BUILDER", "Save your name, business identity and contact call-to-action for consistent status cards.") { personalBrandBuilder() }
        card("PRODUCT CATALOGUE MAKER", "Create product cards with photos, prices and descriptions, save your items and share them with customers.") { productCatalogueMaker() }
        card("CUSTOMER ORDER TRACKER", "Record customer orders, payments and balances, track progress and share updates.") { orderTracker() }
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

    private fun statusConversationGames() {
        val games = linkedMapOf(
            "Choose one" to listOf(
                "Peace of mind or plenty of money?",
                "A quiet life or a famous life?",
                "Love that is calm or love that is exciting?",
                "Travel the world or build your dream home?",
                "Forgive quickly or take time to heal?"
            ),
            "Complete the sentence" to listOf(
                "One thing I have learned the hard way is...",
                "If I could advise my younger self, I would say...",
                "A small thing that makes me happy is...",
                "I know I am growing because...",
                "The kind of friend I value most is..."
            ),
            "Friendly debate" to listOf(
                "Can friendship survive without regular communication?",
                "Is honesty always better, even when it hurts?",
                "Should couples share passwords?",
                "Is consistency more important than talent?",
                "Does money change people or reveal them?"
            ),
            "Get to know me" to listOf(
                "What is one skill you wish you could learn?",
                "What song can instantly improve your mood?",
                "What is your favourite way to spend a free day?",
                "What is one place you hope to visit?",
                "What quality do you admire most in people?"
            ),
            "Naija fun" to listOf(
                "Which is harder: December spending or January bills? 😂",
                "Would you rather have free food or free transport for a year?",
                "Who is more likely to say 'I am on my way' while still at home?",
                "What's one Nigerian food you can eat any day?",
                "If NEPA gives you 24 hours light, what is your first move?"
            )
        )
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, games.keys.toList())
        }
        root.addView(label("PICK A STATUS GAME", 14, Color.rgb(6, 63, 50), true))
        root.addView(category)
        val prompt = TextView(this).apply {
            text = "Choose a game category and tap for a prompt."
            textSize = 19f
            setTextColor(Color.rgb(20, 55, 48))
            setPadding(dp(12), dp(18), dp(12), dp(18))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(prompt)
        var selectedPrompt = ""
        button("Give me a question") {
            val options = games[category.selectedItem.toString()].orEmpty()
            val previous = selectedPrompt
            selectedPrompt = (options.filter { it != previous }.ifEmpty { options }).random()
            prompt.text = selectedPrompt
        }
        button("Create shareable question card") {
            if (selectedPrompt.isBlank()) toast("Choose a question first.") else shareMemeImage(selectedPrompt + "\\n\\nWhat do you think? Reply and let's talk. 💬")
        }
        button("Copy question") {
            if (selectedPrompt.isBlank()) toast("Choose a question first.") else {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Status question", selectedPrompt))
                toast("Question copied.")
            }
        }
        root.addView(label("These prompts are designed for genuine conversation. Users share them manually and read replies in WhatsApp.", 11, Color.GRAY))
    }

    private fun statusCalendar() {
        val categories = linkedMapOf(
            "Personal / lifestyle" to listOf("A little moment I am grateful for today...", "One lesson I am carrying into this week...", "Something simple that makes me happy is..."),
            "Business / promotion" to listOf("Here is what we can help you with this week...", "A happy customer is the best reward. Thank you for your support.", "Need quality service? Send us a message and let's talk."),
            "Faith / encouragement" to listOf("May this week bring renewed strength and fresh hope.", "Keep doing what is right, even when progress feels slow.", "One prayer I am holding close today is..."),
            "Fun / Naija vibes" to listOf("Omo, we thank God for another day. Who else is grateful? 😂", "If enjoyment was a subject, what grade would you get?", "Small wins deserve celebration too. No dulling!"),
            "Connection / friendship" to listOf("Tag or message someone whose kindness you appreciate.", "What is one quality you value in a true friend?", "Checking on you: how is your heart today?")
        )
        val date = Calendar.getInstance()
        val days = (0..6).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        }
        val dayNames = days.map { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(it.time) }
        val dayPicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, dayNames)
        }
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, categories.keys.toList())
        }
        val draft = EditText(this).apply { hint = "Your planned status for this day"; minLines = 3 }
        val saved = TextView(this).apply { textSize = 13f; setTextColor(Color.DKGRAY) }
        root.addView(label("YOUR NEXT SEVEN DAYS", 16, Color.rgb(6, 63, 50), true))
        root.addView(label("Choose a day and a theme. Your plans are saved locally on this phone.", 12, Color.DKGRAY))
        root.addView(dayPicker); root.addView(category); root.addView(draft)
        fun key(): String = "status_calendar_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(days[dayPicker.selectedItemPosition].time)
        fun refreshSaved() {
            val value = prefs.getString(key(), "") ?: ""
            saved.text = if (value.isBlank()) "No plan saved for this day yet." else "SAVED PLAN\n$value"
            draft.setText(value)
        }
        dayPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { refreshSaved() }
        }
        button("Suggest a status for this day") {
            val options = categories[category.selectedItem.toString()].orEmpty()
            draft.setText(options.random())
        }
        button("Save this day's plan") {
            val value = draft.text.toString().trim()
            if (value.isBlank()) toast("Write or generate a status first.") else {
                prefs.edit().putString(key(), value).apply()
                saved.text = "SAVED PLAN\n$value"
                toast("Status plan saved.")
            }
        }
        button("Share this planned status") {
            val value = draft.text.toString().trim()
            if (value.isBlank()) toast("Choose or write a status first.") else shareMemeImage(value)
        }
        root.addView(saved)
        refreshSaved()
        root.addView(label("This is a planning calendar, not automatic posting. You choose when to share each status.", 11, Color.GRAY))
    }

    private fun orderTracker() {
        root.addView(label("CUSTOMER ORDERS", 16, Color.rgb(6, 63, 50), true))
        root.addView(label("Keep a simple record of each order and what the customer has paid.", 12, Color.DKGRAY))
        val customer = EditText(this).apply { hint = "Customer name"; isSingleLine = true }
        val item = EditText(this).apply { hint = "Item or service ordered"; isSingleLine = true }
        val total = EditText(this).apply { hint = "Total order amount (₦)"; inputType = 2 }
        val paid = EditText(this).apply { hint = "Amount paid so far (₦)"; inputType = 2 }
        val status = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Order received", "In progress", "Ready for collection", "Delivered"))
        }
        root.addView(customer); root.addView(item); root.addView(total); root.addView(paid)
        root.addView(label("ORDER PROGRESS", 12, Color.rgb(6, 63, 50), true))
        root.addView(status)
        val summary = TextView(this).apply { textSize = 13f; setTextColor(Color.DKGRAY); setPadding(0, dp(10), 0, dp(8)) }
        root.addView(summary)
        fun money(value: Double): String = java.text.NumberFormat.getNumberInstance(Locale("en", "NG")).format(value)
        fun updateSummary() {
            val t = total.text.toString().toDoubleOrNull()
            val p = paid.text.toString().toDoubleOrNull()
            summary.text = if (t == null || p == null || t < 0 || p < 0 || p > t) {
                "Enter valid amounts. Amount paid cannot be greater than the order total."
            } else {
                "Outstanding balance: ₦\${money(t - p)}"
            }
        }
        total.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSummary() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        paid.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSummary() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        button("Save customer order") {
            val who = customer.text.toString().trim()
            val what = item.text.toString().trim()
            val t = total.text.toString().toDoubleOrNull()
            val p = paid.text.toString().toDoubleOrNull()
            if (who.isBlank() || what.isBlank() || t == null || p == null || t <= 0 || p < 0 || p > t) {
                toast("Enter the customer, item and valid payment amounts.")
            } else {
                val fields = listOf(who, what, t.toString(), p.toString(), status.selectedItem.toString())
                val record = fields.joinToString("\u001e")
                val old = prefs.getString("customer_orders", "") ?: ""
                prefs.edit().putString("customer_orders", old + (if (old.isBlank()) "" else "\u001f") + record).apply()
                customer.setText(""); item.setText(""); total.setText(""); paid.setText("")
                status.setSelection(0)
                updateSummary()
                toast("Order saved on this phone.")
                renderCustomerOrders()
            }
        }
        button("Share payment reminder") {
            val who = customer.text.toString().trim()
            val what = item.text.toString().trim()
            val t = total.text.toString().toDoubleOrNull()
            val p = paid.text.toString().toDoubleOrNull()
            if (who.isBlank() || what.isBlank() || t == null || p == null || t <= 0 || p < 0 || p > t) {
                toast("Enter valid order details first.")
            } else {
                val balance = t - p
                val note = "Hello $who, this is a friendly update about your $what order. Total: ₦\${money(t)}. Paid: ₦\${money(p)}. Balance: ₦\${money(balance)}. Thank you."
                shareToWhatsApp(note)
            }
        }
        root.addView(space(10))
        root.addView(label("SAVED ORDERS", 13, Color.rgb(6, 63, 50), true))
        renderCustomerOrders()
        root.addView(label("This is a manual record book. It does not collect payments or send messages automatically. Keep customer information private.", 11, Color.GRAY))
    }

    private fun renderCustomerOrders() {
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val raw = prefs.getString("customer_orders", "") ?: ""
        if (raw.isBlank()) {
            holder.addView(label("No customer orders saved yet.", 12, Color.GRAY))
        } else {
            raw.split("\u001f").filter { it.isNotBlank() }.forEach { record ->
                val fields = record.split("\u001e")
                val who = fields.getOrNull(0).orEmpty()
                val what = fields.getOrNull(1).orEmpty()
                val total = fields.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                val paid = fields.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                val status = fields.getOrNull(4).orEmpty()
                val balance = (total - paid).coerceAtLeast(0.0)
                val format = java.text.NumberFormat.getNumberInstance(Locale("en", "NG"))
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    setBackgroundColor(Color.WHITE)
                }
                card.addView(label(who + " • " + what, 14, Color.rgb(6, 63, 50), true))
                card.addView(label("Total: ₦\${format.format(total)}  |  Paid: ₦\${format.format(paid)}  |  Balance: ₦\${format.format(balance)}", 12, Color.DKGRAY))
                card.addView(label("Status: $status", 12, Color.GRAY))
                val share = Button(this).apply { text = "Share order update"; isAllCaps = false }
                share.setOnClickListener {
                    shareToWhatsApp("Hello $who, an update on your $what order: $status. Total: ₦\${format.format(total)}. Paid: ₦\${format.format(paid)}. Balance: ₦\${format.format(balance)}. Thank you.")
                }
                card.addView(share)
                holder.addView(card)
                holder.addView(space(6))
            }
        }
        root.addView(holder)
    }

    private var selectedProductImage: Uri? = null
    private var productImageStatus: TextView? = null

    private fun productCatalogueMaker() {
        root.addView(label("YOUR MINI PRODUCT CATALOGUE", 16, Color.rgb(6, 63, 50), true))
        root.addView(label("Add an item, choose an optional photo, then create a shareable product card.", 12, Color.DKGRAY))
        val name = EditText(this).apply { hint = "Product or service name"; isSingleLine = true }
        val price = EditText(this).apply { hint = "Price in naira (e.g. 12500)"; isSingleLine = true; inputType = 2 }
        val description = EditText(this).apply { hint = "Short description or key benefits"; minLines = 2; maxLines = 4 }
        val contact = EditText(this).apply {
            hint = "How customers can order (optional)"
            isSingleLine = true
            setText(prefs.getString("brand_contact", ""))
        }
        productImageStatus = label("No product photo selected. A designed text card can still be created.", 11, Color.GRAY)
        root.addView(name); root.addView(price); root.addView(description); root.addView(contact)
        val photoButton = Button(this).apply { text = "Choose product photo (optional)"; isAllCaps = false }
        root.addView(photoButton); root.addView(productImageStatus)
        photoButton.setOnClickListener {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
            try { startActivityForResult(Intent.createChooser(pick, "Choose product photo"), 73) }
            catch (_: Exception) { toast("Could not open the photo picker.") }
        }
        fun productText(): String {
            val title = name.text.toString().trim()
            val amount = price.text.toString().trim()
            val details = description.text.toString().trim()
            val order = contact.text.toString().trim()
            return listOf(title, amount, details, order).joinToString("\u001e")
        }
        button("Save product to my catalogue") {
            val title = name.text.toString().trim()
            val amount = price.text.toString().trim()
            if (title.isBlank() || amount.isBlank()) {
                toast("Enter the product name and price.")
            } else {
                val old = prefs.getString("product_catalogue", "") ?: ""
                val updated = old + (if (old.isBlank()) "" else "\u001f") + productText()
                prefs.edit().putString("product_catalogue", updated).apply()
                toast("Product saved on this phone.")
                renderProductCatalogue()
            }
        }
        button("Create and share product card") {
            val title = name.text.toString().trim()
            val amount = price.text.toString().trim()
            if (title.isBlank() || amount.isBlank()) toast("Enter the product name and price first.")
            else shareProductCard(title, amount, description.text.toString().trim(),
                contact.text.toString().trim(), selectedProductImage)
        }
        root.addView(space(8))
        root.addView(label("SAVED PRODUCTS", 13, Color.rgb(6, 63, 50), true))
        renderProductCatalogue()
        root.addView(label("Photos are selected by you from your device. Products stay in this app; this feature does not upload them to an online shop.", 11, Color.GRAY))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 73 && resultCode == RESULT_OK) {
            selectedProductImage = data?.data
            productImageStatus?.text = if (selectedProductImage == null) "No photo selected." else "Product photo selected."
        }
    }

    private fun renderProductCatalogue() {
        val raw = prefs.getString("product_catalogue", "") ?: ""
        if (raw.isBlank()) {
            root.addView(label("No products saved yet.", 12, Color.GRAY))
            return
        }
        raw.split("\u001f").filter { it.isNotBlank() }.forEach { record ->
            val fields = record.split("\u001e")
            val title = fields.getOrNull(0).orEmpty()
            val amount = fields.getOrNull(1).orEmpty()
            val details = fields.getOrNull(2).orEmpty()
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(9), dp(10), dp(9))
                setBackgroundColor(Color.WHITE)
            }
            row.addView(label(title, 15, Color.rgb(6, 63, 50), true))
            row.addView(label("₦$amount", 14, Color.DKGRAY))
            if (details.isNotBlank()) row.addView(label(details, 12, Color.GRAY))
            val share = Button(this).apply { text = "Share this product"; isAllCaps = false }
            share.setOnClickListener {
                shareProductCard(title, amount, details, fields.getOrNull(3).orEmpty(), null)
            }
            row.addView(share)
            root.addView(row)
            root.addView(space(5))
        }
    }

    private fun shareProductCard(name: String, price: String, description: String, contact: String, photoUri: Uri?) {
        try {
            val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(Color.rgb(6, 63, 50))
            var photo: Bitmap? = null
            if (photoUri != null) {
                photo = contentResolver.openInputStream(photoUri)?.use { BitmapFactory.decodeStream(it) }
            }
            if (photo != null) {
                val scale = maxOf(1080f / photo.width, 610f / photo.height)
                val width = (photo.width * scale).toInt()
                val height = (photo.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(photo, width, height, true)
                val srcLeft = ((width - 1080) / 2).coerceAtLeast(0)
                val srcTop = ((height - 610) / 2).coerceAtLeast(0)
                canvas.drawBitmap(scaled, android.graphics.Rect(srcLeft, srcTop, (srcLeft + 1080).coerceAtMost(width), (srcTop + 610).coerceAtMost(height)),
                    RectF(0f, 0f, 1080f, 610f), paint)
                if (scaled !== photo) scaled.recycle()
                photo.recycle()
            } else {
                paint.color = Color.argb(35, 255, 255, 255)
                canvas.drawCircle(930f, 120f, 260f, paint)
                canvas.drawCircle(80f, 620f, 210f, paint)
            }
            paint.color = Color.rgb(230, 195, 120)
            paint.textSize = 32f
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            canvas.drawText("AVAILABLE NOW", 65f, 675f, paint)
            paint.color = Color.WHITE
            paint.textSize = 66f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            val title = if (name.length > 25) name.take(24) + "…" else name
            canvas.drawText(title, 65f, 770f, paint)
            paint.color = Color.rgb(245, 211, 145)
            paint.textSize = 54f
            canvas.drawText("₦$price", 65f, 850f, paint)
            paint.color = Color.WHITE
            paint.textSize = 29f
            val detail = if (description.length > 75) description.take(72) + "…" else description
            if (detail.isNotBlank()) canvas.drawText(detail, 65f, 910f, paint)
            paint.color = Color.rgb(245, 211, 145)
            paint.textSize = 26f
            val footer = if (contact.isBlank()) "Message us to order" else contact
            canvas.drawText(if (footer.length > 48) footer.take(45) + "…" else footer, 65f, 1000f, paint)
            val dir = File(cacheDir, "product_cards").apply { mkdirs() }
            val file = File(dir, "product_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "$name • ₦$price" + if (description.isBlank()) "" else "\n$description")
                clipData = ClipData.newUri(contentResolver, "Product card", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Share product card"))
        } catch (_: Exception) {
            toast("Could not create the product card. Try another photo or remove the photo.")
        }
    }

    private fun personalBrandBuilder() {
        root.addView(label("BUILD A CONSISTENT IDENTITY", 16, Color.rgb(6, 63, 50), true))
        root.addView(label("Save a few details once, then use them when creating promotional status cards.", 12, Color.DKGRAY))
        val brandName = EditText(this).apply { hint = "Your name or business name"; isSingleLine = true; setText(prefs.getString("brand_name", "")) }
        val tagline = EditText(this).apply { hint = "Short tagline (e.g. Quality you can trust)"; isSingleLine = true; setText(prefs.getString("brand_tagline", "")) }
        val contact = EditText(this).apply { hint = "Contact or call-to-action (optional)"; isSingleLine = true; setText(prefs.getString("brand_contact", "")) }
        val sample = EditText(this).apply { hint = "What are you announcing or promoting?"; minLines = 3 }
        root.addView(brandName); root.addView(tagline); root.addView(contact); root.addView(sample)
        button("Save my brand details") {
            val name = brandName.text.toString().trim()
            if (name.isBlank()) toast("Enter your name or business name first.") else {
                prefs.edit().putString("brand_name", name)
                    .putString("brand_tagline", tagline.text.toString().trim())
                    .putString("brand_contact", contact.text.toString().trim()).apply()
                toast("Brand details saved on this phone.")
            }
        }
        button("Create branded status card") {
            val name = brandName.text.toString().trim()
            val message = sample.text.toString().trim()
            if (name.isBlank() || message.isBlank()) {
                toast("Enter your name or business name and a message.")
            } else {
                val line = tagline.text.toString().trim()
                val call = contact.text.toString().trim()
                val branded = message + "\n\n" + name +
                    (if (line.isBlank()) "" else "\n" + line) +
                    (if (call.isBlank()) "" else "\n" + call)
                shareMemeImage(branded)
            }
        }
        root.addView(label("Your details stay in this app on this phone. Preview your wording before sharing; the tool does not post automatically.", 11, Color.GRAY))
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
