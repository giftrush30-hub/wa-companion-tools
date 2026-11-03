package com.example.wacompanion

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Small Supabase Realtime Phoenix-protocol client for one private game room.
 * All callbacks arrive on a background thread; callers must switch to the UI thread.
 * Room topic is "room:<uuid>" (wire topic: "realtime:room:<uuid>").
 */
class GamesRealtimeClient(
    context: android.content.Context,
    private val roomId: String,
    private val displayName: String,
    private val listener: Listener
) {
    interface Listener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onBroadcast(event: String, payload: JSONObject)
        fun onPresence(payload: JSONObject)
        fun onError(message: String)
    }

    private val discover = DiscoverBackend(context.applicationContext)
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val refs = AtomicLong(1L)
    private val topic = "realtime:room:$roomId"

    @Volatile private var socket: WebSocket? = null
    @Volatile private var joined = false
    @Volatile private var closed = false
    @Volatile private var userId = ""
    @Volatile private var accessToken = ""
    @Volatile private var joinRef = ""
    private var reconnectAttempt = 0
    private var heartbeatStarted = false
    private var tokenRefreshStarted = false

    /** Call from a worker thread because session validation performs an HTTP request. */
    fun connect() {
        if (closed) return
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to join online game chat.")
        userId = session.userId
        accessToken = session.accessToken
        // This authenticated request also lets DiscoverBackend refresh an expired session once.
        discover.request("/auth/v1/user", "GET", null, accessToken)
        accessToken = discover.currentSession()?.accessToken
            ?: throw IllegalStateException("Your sign-in session has expired. Please sign in again.")
        openSocket()
    }

    private fun openSocket() {
        if (closed) return
        val key = URLEncoder.encode(discover.publishableKey, "UTF-8")
        val url = discover.baseUrl.replaceFirst("https://", "wss://") +
            "/realtime/v1/websocket?apikey=$key&vsn=1.0.0"
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                joined = false
                joinRef = refs.getAndIncrement().toString()
                val config = JSONObject()
                    .put("broadcast", JSONObject().put("ack", true).put("self", false))
                    .put("presence", JSONObject().put("enabled", true).put("key", userId))
                    .put("private", true)
                val payload = JSONObject().put("config", config).put("access_token", accessToken)
                sendFrame("phx_join", payload, joinRef)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                joined = false
                if (!closed) {
                    listener.onDisconnected(reason.ifBlank { "Connection closed." })
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                joined = false
                if (!closed) {
                    listener.onDisconnected(t.message ?: "Network connection interrupted.")
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleMessage(raw: String) {
        try {
            val message = JSONObject(raw)
            when (message.optString("event")) {
                "phx_reply" -> {
                    val payload = message.optJSONObject("payload") ?: JSONObject()
                    val status = payload.optString("status")
                    if (message.optString("ref") == joinRef && status == "ok") {
                        joined = true
                        reconnectAttempt = 0
                        listener.onConnected()
                        trackPresence()
                        startHeartbeat()
                        startTokenRefresh()
                    } else if (status == "error") {
                        val reason = payload.optJSONObject("response")?.optString("reason")
                            ?: "The game room connection was rejected."
                        listener.onError(reason)
                    }
                }
                "broadcast" -> {
                    val payload = message.optJSONObject("payload") ?: return
                    val eventName = payload.optString("event")
                    val data = payload.optJSONObject("payload") ?: JSONObject()
                    if (eventName.isNotBlank()) listener.onBroadcast(eventName, data)
                }
                "presence_state", "presence_diff" -> {
                    listener.onPresence(message.optJSONObject("payload") ?: JSONObject())
                }
                "phx_error", "phx_close" -> {
                    joined = false
                    if (!closed) scheduleReconnect()
                }
            }
        } catch (error: Exception) {
            listener.onError("Could not read a live room update.")
        }
    }

    fun broadcast(event: String, payload: JSONObject): Boolean {
        if (!joined || closed || event.isBlank()) return false
        val body = JSONObject()
            .put("type", "broadcast")
            .put("event", event)
            .put("payload", payload)
        return sendFrame("broadcast", body, refs.getAndIncrement().toString())
    }

    private fun trackPresence() {
        val state = JSONObject()
            .put("user_id", userId)
            .put("display_name", displayName.take(40))
        val payload = JSONObject()
            .put("type", "presence")
            .put("event", "track")
            .put("payload", state)
        sendFrame("presence", payload, refs.getAndIncrement().toString())
    }

    private fun startHeartbeat() {
        if (heartbeatStarted) return
        heartbeatStarted = true
        scheduler.scheduleAtFixedRate({
            if (joined && !closed) {
                val frame = JSONObject()
                    .put("topic", "phoenix")
                    .put("event", "heartbeat")
                    .put("payload", JSONObject())
                    .put("ref", refs.getAndIncrement().toString())
                socket?.send(frame.toString())
            }
        }, 20, 20, TimeUnit.SECONDS)
    }

    private fun startTokenRefresh() {
        if (tokenRefreshStarted) return
        tokenRefreshStarted = true
        scheduler.scheduleAtFixedRate({
            if (!joined || closed) return@scheduleAtFixedRate
            try {
                val current = discover.currentSession()
                    ?: throw IllegalStateException("Sign in again to keep voice and chat connected.")
                discover.request("/auth/v1/user", "GET", null, current.accessToken)
                val refreshed = discover.currentSession()?.accessToken ?: return@scheduleAtFixedRate
                if (refreshed != accessToken) {
                    accessToken = refreshed
                    sendFrame("access_token", JSONObject().put("access_token", refreshed),
                        refs.getAndIncrement().toString())
                }
            } catch (error: Exception) {
                listener.onError(error.message ?: "Could not refresh the live room session.")
            }
        }, 35, 35, TimeUnit.MINUTES)
    }

    private fun sendFrame(event: String, payload: JSONObject, ref: String): Boolean {
        val frame = JSONObject()
            .put("topic", topic)
            .put("event", event)
            .put("payload", payload)
            .put("ref", ref)
            .put("join_ref", joinRef)
        return socket?.send(frame.toString()) == true
    }

    private fun scheduleReconnect() {
        if (closed) return
        val delays = longArrayOf(1, 2, 5, 10)
        val delay = delays[reconnectAttempt.coerceAtMost(delays.lastIndex)]
        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(delays.lastIndex)
        scheduler.schedule({ if (!closed && !joined) openSocket() }, delay, TimeUnit.SECONDS)
    }

    fun close() {
        closed = true
        joined = false
        try {
            val frame = JSONObject()
                .put("topic", topic)
                .put("event", "phx_leave")
                .put("payload", JSONObject())
                .put("ref", refs.getAndIncrement().toString())
                .put("join_ref", joinRef)
            socket?.send(frame.toString())
            socket?.close(1000, "Player left the game room")
        } catch (_: Exception) {
            socket?.cancel()
        }
        scheduler.shutdownNow()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
