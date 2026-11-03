package com.example.wacompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Supabase-backed room and in-room chat data access for Games Arena.
 * Call every method from a worker thread; DiscoverBackend uses blocking HTTP.
 *
 * This layer deliberately does not pretend to synchronize game turns yet.
 * The authoritative Ludo, WHOT! and Big Quiz state engines are separate work.
 */
class GamesMultiplayerBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)

    data class Room(
        val id: String,
        val code: String,
        val gameType: String,
        val visibility: String,
        val status: String,
        val hostId: String,
        val maxPlayers: Int,
        val createdAt: String = "",
        val expiresAt: String = ""
    )

    data class Member(
        val userId: String,
        val displayName: String,
        val role: String,
        val isReady: Boolean
    )

    data class ChatMessage(
        val id: String,
        val roomId: String,
        val senderId: String,
        val body: String,
        val createdAt: String
    )

    private fun session(): DiscoverBackend.SellerSession =
        discover.currentSession()
            ?: throw IllegalStateException("Sign in first to play online. Open Discover and sign in, then return to Games Arena.")

    fun createRoom(gameType: String, visibility: String, displayName: String, maxPlayers: Int): Room {
        val active = session()
        val payload = JSONObject()
            .put("p_game_type", gameType)
            .put("p_visibility", visibility)
            .put("p_display_name", cleanName(displayName))
            .put("p_max_players", maxPlayers)
        val result = discover.request("/rest/v1/rpc/create_game_room", "POST", payload, active.accessToken) as JSONObject
        return parseRoom(result)
    }

    fun joinRoom(roomCode: String, displayName: String): Room {
        val active = session()
        val code = roomCode.trim().uppercase()
        if (!code.matches(Regex("[A-F0-9]{8}"))) {
            throw IllegalArgumentException("Enter the 8-character room code.")
        }
        val result = discover.request(
            "/rest/v1/rpc/join_game_room",
            "POST",
            JSONObject().put("p_room_code", code).put("p_display_name", cleanName(displayName)),
            active.accessToken
        ) as JSONObject
        return parseRoom(result)
    }

    fun roomDetails(roomId: String): Room {
        val active = session()
        val rows = discover.request(
            "/rest/v1/game_rooms?select=id,room_code,game_type,visibility,status,host_id,max_players,created_at,expires_at&id=eq.$roomId&limit=1",
            "GET", null, active.accessToken
        ) as JSONArray
        if (rows.length() == 0) throw IllegalStateException("This game room no longer exists.")
        return parseRoom(rows.getJSONObject(0))
    }

    fun publicRooms(gameType: String): List<Room> {
        val active = session()
        require(gameType in setOf("big_quiz", "ludo", "whot")) { "Choose a supported game." }
        val rows = discover.request(
            "/rest/v1/game_rooms?select=id,room_code,game_type,visibility,status,host_id,max_players,created_at,expires_at" +
                "&game_type=eq.$gameType&visibility=eq.public&status=eq.waiting&order=created_at.desc&limit=30",
            "GET", null, active.accessToken
        ) as JSONArray
        return (0 until rows.length()).map { parseRoom(rows.getJSONObject(it)) }
    }

    fun members(roomId: String): List<Member> {
        val active = session()
        val rows = discover.request(
            "/rest/v1/game_room_members?select=user_id,display_name,role,is_ready" +
                "&room_id=eq.$roomId&left_at=is.null&order=joined_at.asc",
            "GET", null, active.accessToken
        ) as JSONArray
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            Member(
                row.getString("user_id"),
                row.getString("display_name"),
                row.optString("role", "player"),
                row.optBoolean("is_ready", false)
            )
        }
    }

    fun messages(roomId: String, limit: Int = 60): List<ChatMessage> {
        val active = session()
        val safeLimit = limit.coerceIn(1, 100)
        val rows = discover.request(
            "/rest/v1/game_chat_messages?select=id,room_id,sender_id,body,created_at" +
                "&room_id=eq.$roomId&order=created_at.desc&limit=$safeLimit",
            "GET", null, active.accessToken
        ) as JSONArray
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            ChatMessage(
                row.getString("id"),
                row.getString("room_id"),
                row.getString("sender_id"),
                row.getString("body"),
                row.getString("created_at")
            )
        }.reversed()
    }

    fun sendMessage(roomId: String, body: String): ChatMessage {
        val active = session()
        val clean = body.trim()
        if (clean.isEmpty() || clean.length > 300) {
            throw IllegalArgumentException("Message must be between 1 and 300 characters.")
        }
        val inserted = discover.request(
            "/rest/v1/game_chat_messages",
            "POST",
            JSONObject().put("room_id", roomId).put("sender_id", active.userId).put("body", clean),
            active.accessToken,
            returnRepresentation = true
        ) as JSONArray
        if (inserted.length() == 0) throw IllegalStateException("The message was not saved.")
        val row = inserted.getJSONObject(0)
        return ChatMessage(row.getString("id"), row.getString("room_id"), row.getString("sender_id"),
            row.getString("body"), row.getString("created_at"))
    }

    fun setReady(roomId: String, ready: Boolean) {
        val active = session()
        discover.request(
            "/rest/v1/rpc/set_game_room_ready",
            "POST",
            JSONObject().put("p_room_id", roomId).put("p_ready", ready),
            active.accessToken
        )
    }

    fun startRoom(roomId: String) {
        val active = session()
        discover.request(
            "/rest/v1/rpc/start_game_room",
            "POST",
            JSONObject().put("p_room_id", roomId),
            active.accessToken
        )
    }

    fun leaveRoom(roomId: String) {
        val active = session()
        discover.request(
            "/rest/v1/rpc/leave_game_room",
            "POST",
            JSONObject().put("p_room_id", roomId),
            active.accessToken
        )
    }

    private fun cleanName(value: String): String {
        val clean = value.trim()
        if (clean.length !in 2..40) {
            throw IllegalArgumentException("Player name must be between 2 and 40 characters.")
        }
        return clean
    }

    private fun parseRoom(row: JSONObject): Room = Room(
        id = row.optString("room_id").ifBlank { row.optString("id") },
        code = row.optString("room_code"),
        gameType = row.optString("game_type"),
        visibility = row.optString("visibility"),
        status = row.optString("status"),
        hostId = row.optString("host_id"),
        maxPlayers = row.optInt("max_players", 4),
        createdAt = row.optString("created_at"),
        expiresAt = row.optString("expires_at")
    )
}
