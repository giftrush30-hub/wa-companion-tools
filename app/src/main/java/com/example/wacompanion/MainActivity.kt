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

        card("NIGERIAN MEME & REACTION STUDIO", "Choose a Nigerian-style reaction or write your own, turn it into a shareable image, and send it to WhatsApp or your status.") { memeStudio() }
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


    private fun memeStudio() {
        val search = EditText(this).apply { hint = "Search reactions"; singleLine = true }
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
