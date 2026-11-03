package com.example.wacompanion

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

/** Games Arena online lobby with persistent room chat and optional LiveKit voice. */
class GamesMultiplayerActivity : Activity(), GamesVoiceClient.Listener {
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var api: GamesMultiplayerBackend
    private lateinit var body: LinearLayout
    private lateinit var name: EditText
    private lateinit var code: EditText
    private lateinit var status: TextView
    private lateinit var roster: TextView
    private lateinit var chat: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var message: EditText
    private lateinit var readyButton: Button
    private lateinit var voiceButton: Button
    private lateinit var micButton: Button
    private lateinit var speakerButton: Button
    private var game = "ludo"
    private var nickname = ""
    private var userId = ""
    private var room: GamesMultiplayerBackend.Room? = null
    private var roomMembers: List<GamesMultiplayerBackend.Member> = emptyList()
    private var realtime: GamesRealtimeClient? = null
    private var voice: GamesVoiceClient? = null
    private var ready = false
    private var connectingVoice = false
    private var micMuted = false
    private var speakerMuted = false
    private val seen = hashSetOf<String>()
    private var refreshing = false
    private var gameLaunched = false
    private var chatOnly = false
    private val poll = object : Runnable {
        override fun run() {
            val active = room ?: return
            if (refreshing || isFinishing) return
            refreshing = true
            io.execute {
                try {
                    val fresh = api.roomDetails(active.id)
                    val players = api.members(active.id)
                    val history = api.messages(active.id, 50)
                    main.post {
                        refreshing = false
                        if (room?.id != active.id) return@post
                        roomMembers = players
                        showRoster(fresh, players)
                        history.forEach { addChat(it.id, players.firstOrNull { p -> p.userId == it.senderId }?.displayName ?: "Player", it.body) }
                    }
                } catch (e: Exception) {
                    main.post { refreshing = false; if (room?.id == active.id) status.text = e.message ?: "Room update failed." }
                }
            }
            main.postDelayed(this, 3500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = GamesMultiplayerBackend(this)
        userId = DiscoverBackend(this).currentSession()?.userId.orEmpty()
        shell()
        val existingRoom = intent.getStringExtra("open_room_id")
        chatOnly = intent.getBooleanExtra("chat_only", false)
        if (!intent.getStringExtra("game_display_name").isNullOrBlank()) {
            nickname = intent.getStringExtra("game_display_name").orEmpty()
        }
        if (!existingRoom.isNullOrBlank()) {
            io.execute {
                try {
                    val value = api.roomDetails(existingRoom)
                    main.post { enterRoom(value) }
                } catch (e: Exception) { main.post { landing(); status.text = e.message ?: "Could not reopen game chat." } }
            }
        } else landing()
    }

    private fun shell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(20)); setBackgroundColor(Color.rgb(246,249,247)) }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun landing() {
        body.removeAllViews()
        body.addView(label("GAMES ARENA • PLAY TOGETHER", 24, green, true))
        body.addView(label("Create a room for friends, join by code, or find a public game.", 14, muted))
        name = EditText(this).apply { hint = "Your player nickname"; setText(nickname); singleLine = true }
        body.addView(name)
        body.addView(label("CHOOSE GAME", 12, green, true))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("ludo" to "Ludo", "whot" to "WHOT!", "big_quiz" to "Big Quiz").forEach { pair ->
            row.addView(Button(this).apply {
                text = if (game == pair.first) "✓ " + pair.second else pair.second
                isAllCaps = false
                setOnClickListener { game = pair.first; landing() }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        body.addView(row)
        body.addView(Button(this).apply { text = "Create private room"; isAllCaps = false; setOnClickListener { create("private") } })
        body.addView(Button(this).apply { text = "Create public room"; isAllCaps = false; setOnClickListener { create("public") } })
        code = EditText(this).apply { hint = "8-character room code"; singleLine = true }
        body.addView(code)
        body.addView(Button(this).apply { text = "Join room"; isAllCaps = false; setOnClickListener { join(code.text.toString()) } })
        body.addView(Button(this).apply { text = "Find public rooms"; isAllCaps = false; setOnClickListener { findRooms() } })
        body.addView(label("Online play requires sign-in. Offline practice remains available.", 12, muted))
        body.addView(Button(this).apply { text = "Back to Games Arena"; isAllCaps = false; setOnClickListener { finish() } })
        status = label("", 13, green)
        body.addView(status)
    }

    private fun validateName(): Boolean {
        nickname = name.text.toString().trim()
        if (nickname.length !in 2..40) { toast("Enter a nickname between 2 and 40 characters."); return false }
        return true
    }

    private fun create(visibility: String) {
        if (!validateName()) return
        status.text = "Creating room…"
        io.execute {
            try {
                val value = api.createRoom(game, visibility, nickname, if (game == "big_quiz") 8 else 4)
                main.post { enterRoom(value) }
            } catch (e: Exception) { main.post { status.text = e.message ?: "Could not create room." } }
        }
    }

    private fun join(value: String) {
        if (!validateName()) return
        status.text = "Joining room…"
        io.execute {
            try {
                val joined = api.joinRoom(value, nickname)
                main.post { enterRoom(joined) }
            } catch (e: Exception) { main.post { status.text = e.message ?: "Could not join room." } }
        }
    }

    private fun findRooms() {
        if (!validateName()) return
        status.text = "Searching public rooms…"
        io.execute {
            try {
                val rooms = api.publicRooms(game)
                main.post {
                    if (rooms.isEmpty()) { status.text = "No public rooms are waiting. Create one and invite friends."; return@post }
                    AlertDialog.Builder(this).setTitle("Public games")
                        .setItems(rooms.map { it.code + " • " + it.status }.toTypedArray()) { _, i -> join(rooms[i].code) }
                        .setNegativeButton("Close", null).show()
                }
            } catch (e: Exception) { main.post { status.text = e.message ?: "Search failed." } }
        }
    }

    private fun enterRoom(value: GamesMultiplayerBackend.Room) {
        room = value
        ready = false
        seen.clear()
        roomUi()
        io.execute {
            try {
                val players = api.members(value.id)
                val history = api.messages(value.id)
                main.post {
                    roomMembers = players
                    showRoster(value, players)
                    history.forEach { addChat(it.id, players.firstOrNull { p -> p.userId == it.senderId }?.displayName ?: "Player", it.body) }
                }
                val socket = GamesRealtimeClient(this, value.id, nickname, object : GamesRealtimeClient.Listener {
                    override fun onConnected() { main.post { status.text = "LIVE • Room chat connected" } }
                    override fun onDisconnected(reason: String) { main.post { status.text = "Reconnecting chat…" } }
                    override fun onError(message: String) { main.post { status.text = message } }
                    override fun onPresence(payload: JSONObject) { main.post { status.text = "LIVE • Players connected" } }
                    override fun onBroadcast(event: String, payload: JSONObject) {
                        if (event == "chat_message") main.post { addChat(payload.optString("id"), payload.optString("display_name", "Player"), payload.optString("body")) }
                    }
                })
                realtime = socket
                socket.connect()
            } catch (e: Exception) { main.post { status.text = e.message ?: "Could not connect to room." } }
        }
        main.removeCallbacks(poll)
        main.post(poll)
    }

    private fun roomUi() {
        val active = room ?: return
        body.removeAllViews()
        body.addView(label(gameName(active.gameType).uppercase() + " • GAME ROOM", 23, green, true))
        body.addView(label("ROOM CODE", 11, muted, true))
        body.addView(label(active.code, 32, ink, true))
        body.addView(Button(this).apply {
            text = "Invite friends • Share code"
            isAllCaps = false
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Join my WA Companion " + gameName(active.gameType) + " game. Room code: " + active.code)
                startActivity(Intent.createChooser(send, "Invite friends"))
            }
        })
        status = label("Connecting…", 13, green, true)
        body.addView(status)
        body.addView(label("Room chat is shared live. Voice requires LiveKit service credentials. Game moves and scores are not yet synchronized between phones.", 12, muted))
        roster = label("Waiting for players…", 14, ink)
        body.addView(roster)
        readyButton = Button(this).apply { text = "I'm ready"; isAllCaps = false; setOnClickListener { setReady(!ready) } }
        body.addView(readyButton)
        readyButton.visibility = if (active.status == "waiting" && !chatOnly) View.VISIBLE else View.GONE
        if (active.hostId == userId && active.status == "waiting" && !chatOnly) body.addView(Button(this).apply {
            text = "Start room session"; isAllCaps = false; setOnClickListener { startMatch() }
        })
        body.addView(label("LIVE TEXT CHAT", 15, green, true))
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        chatScroll = ScrollView(this).apply { setBackgroundColor(Color.WHITE); addView(chat) }
        body.addView(chatScroll, LinearLayout.LayoutParams(-1, dp(220)))
        val send = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        message = EditText(this).apply { hint = "Message your players"; maxLines = 3 }
        send.addView(message, LinearLayout.LayoutParams(0, -2, 1f))
        send.addView(Button(this).apply { text = "Send"; isAllCaps = false; setOnClickListener { sendMessage() } })
        body.addView(send)
        voiceButton = Button(this).apply { text = "🎙 Join live voice"; isAllCaps = false; setOnClickListener { toggleVoice() } }
        body.addView(voiceButton)
        micButton = Button(this).apply {
            text = "Mute microphone"; isAllCaps = false; isEnabled = false
            setOnClickListener {
                micMuted = !micMuted
                voice?.setMicrophoneEnabled(!micMuted)
                text = if (micMuted) "Unmute microphone" else "Mute microphone"
            }
        }
        body.addView(micButton)
        speakerButton = Button(this).apply {
            text = "Mute speaker"; isAllCaps = false; isEnabled = false
            setOnClickListener {
                speakerMuted = !speakerMuted
                voice?.setSpeakerMuted(speakerMuted)
                text = if (speakerMuted) "Unmute speaker" else "Mute speaker"
            }
        }
        body.addView(speakerButton)
        body.addView(label("Optional voice call. Mute when you need privacy. Headphones help prevent echo.", 12, muted))
        body.addView(Button(this).apply { text = "Leave room"; isAllCaps = false; setOnClickListener { leave() } })
    }

    private fun showRoster(value: GamesMultiplayerBackend.Room, players: List<GamesMultiplayerBackend.Member>) {
        status.text = when (value.status) { "in_progress" -> "MATCH IN PROGRESS"; "completed" -> "MATCH COMPLETE"; "cancelled" -> "ROOM CLOSED"; else -> "WAITING FOR PLAYERS" }
        roster.text = players.joinToString("\n") {
            (if (it.userId == userId) "YOU • " else "") + it.displayName + " • " +
                (if (it.role == "host") "HOST" else "PLAYER") + " • " +
                (if (it.isReady) "READY" else "NOT READY")
        } +
            "\n\n" + players.size + "/" + value.maxPlayers + " players"
        ready = players.firstOrNull { it.userId == userId }?.isReady ?: ready
        readyButton.text = if (ready) "Cancel ready" else "I'm ready"
        if (value.status == "in_progress" && !gameLaunched && !chatOnly) {
            status.text = "MATCH STARTED • Tap Start match to enter"
            if (value.hostId != userId) {
                // A joining player gets a launch button once the host starts.
                if (body.findViewWithTag<View>("join-start") == null) body.addView(Button(this).apply {
                    tag = "join-start"; text = "Enter match"; isAllCaps = false; setOnClickListener { launchGame() }
                })
            }
        }
    }

    private fun setReady(value: Boolean) {
        val active = room ?: return
        io.execute {
            try { api.setReady(active.id, value); main.post { ready = value; readyButton.text = if (value) "Cancel ready" else "I'm ready" } }
            catch (e: Exception) { main.post { status.text = e.message ?: "Could not change ready status." } }
        }
    }

    private fun startMatch() {
        val active = room ?: return
        io.execute {
            try { api.startRoom(active.id); main.post { launchGame() } }
            catch (e: Exception) { main.post { status.text = e.message ?: "Everyone must be ready before starting." } }
        }
    }

    private fun launchGame() {
        val active = room ?: return
        if (gameLaunched) return
        gameLaunched = true
        val target = when (active.gameType) { "ludo" -> LudoActivity::class.java; "whot" -> WhotActivity::class.java; else -> BigQuizActivity::class.java }
        startActivity(Intent(this, target).apply {
            putExtra("game_room_id", active.id); putExtra("game_room_code", active.code)
            putExtra("game_room_type", active.gameType); putExtra("game_display_name", nickname)
            putExtra("game_host_id", active.hostId); putExtra("game_is_host", active.hostId == userId)
            putExtra("game_player_count", roomMembers.size)
            putStringArrayListExtra("game_player_names", ArrayList(roomMembers.map { it.displayName }))
            putStringArrayListExtra("game_player_ids", ArrayList(roomMembers.map { it.userId }))
        })
    }

    private fun sendMessage() {
        val active = room ?: return
        val text = message.text.toString().trim()
        if (text.isBlank()) return
        if (text.length > 300) { toast("Messages can contain up to 300 characters."); return }
        message.setText("")
        io.execute {
            try {
                val saved = api.sendMessage(active.id, text)
                realtime?.broadcast("chat_message", JSONObject().put("id", saved.id).put("display_name", nickname).put("body", saved.body))
                main.post { addChat(saved.id, nickname, saved.body) }
            } catch (e: Exception) { main.post { status.text = e.message ?: "Message not sent." } }
        }
    }

    private fun addChat(id: String, sender: String, text: String) {
        if (id.isNotBlank() && !seen.add(id)) return
        if (!::chat.isInitialized) return
        chat.addView(label(sender + ": " + text, 14, ink))
        if (chat.childCount > 70) chat.removeViewAt(0)
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun toggleVoice() {
        if (voice != null) {
            voice?.setMicrophoneEnabled(false); voice?.disconnect(); voice = null
            voiceButton.text = "🎙 Join live voice"
            micButton.isEnabled = false; speakerButton.isEnabled = false
            micMuted = false; speakerMuted = false
            micButton.text = "Mute microphone"; speakerButton.text = "Mute speaker"
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 77); return
        }
        connectVoice()
    }

    private fun connectVoice() {
        val active = room ?: return
        if (connectingVoice) return
        connectingVoice = true; voiceButton.isEnabled = false; voiceButton.text = "Connecting voice…"
        io.execute {
            try {
                val token = GamesVoiceTokenBackend(this).issue(active.id, nickname)
                val client = GamesVoiceClient(this, this); voice = client
                client.connect(token.serverUrl, token.token)
            } catch (e: Exception) {
                main.post { connectingVoice = false; voiceButton.isEnabled = true; voiceButton.text = "🎙 Join live voice"; status.text = e.message ?: "Voice connection failed." }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) connectVoice()
            else toast("Microphone permission denied. Text chat is still available.")
        }
    }

    override fun onConnected() { main.post {
        connectingVoice = false
        voiceButton.isEnabled = true
        voiceButton.text = "🔇 Leave live voice"
        micButton.isEnabled = true
        speakerButton.isEnabled = true
        micMuted = false
        speakerMuted = false
        micButton.text = "Mute microphone"
        speakerButton.text = "Mute speaker"
        status.text = "LIVE VOICE CONNECTED"
    } }
    override fun onMicrophoneChanged(enabled: Boolean) { main.post {
        micMuted = !enabled
        micButton.text = if (enabled) "Mute microphone" else "Unmute microphone"
    } }
    override fun onDisconnected() { main.post {
        connectingVoice = false
        voiceButton.isEnabled = true
        voiceButton.text = "🎙 Join live voice"
        micButton.isEnabled = false
        speakerButton.isEnabled = false
    } }
    override fun onError(message: String) { main.post {
        connectingVoice = false
        voiceButton.isEnabled = true
        voiceButton.text = "🎙 Join live voice"
        micButton.isEnabled = false
        speakerButton.isEnabled = false
        status.text = message
    } }

    private fun leave() {
        val active = room ?: return
        AlertDialog.Builder(this).setTitle("Leave game room?").setMessage("You will leave the player list and voice.")
            .setNegativeButton("Stay", null).setPositiveButton("Leave") { _, _ ->
                io.execute { try { api.leaveRoom(active.id) } catch (_: Exception) {}; main.post { closeRoom() } }
            }.show()
    }

    private fun closeRoom() {
        main.removeCallbacks(poll); realtime?.close(); realtime = null; voice?.disconnect(); voice = null
        room = null; roomMembers = emptyList(); gameLaunched = false; chatOnly = false; seen.clear(); landing()
    }

    override fun onDestroy() {
        main.removeCallbacks(poll); realtime?.close(); voice?.disconnect(); io.shutdownNow(); super.onDestroy()
    }

    private fun gameName(value: String) = when (value) { "ludo" -> "Ludo"; "whot" -> "WHOT!"; else -> "Big Quiz" }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); if (bold) setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(4), 0, dp(7))
    }
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private val green get() = Color.rgb(7,91,69)
    private val ink get() = Color.rgb(18,42,55)
    private val muted get() = Color.rgb(91,108,119)
}
