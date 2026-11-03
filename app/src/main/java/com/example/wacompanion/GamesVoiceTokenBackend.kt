package com.example.wacompanion

import android.content.Context
import org.json.JSONObject

/** Fetches short-lived LiveKit credentials from the trusted Supabase Edge Function. */
class GamesVoiceTokenBackend(context: Context) {
    private val discover = DiscoverBackend(context.applicationContext)

    data class VoiceToken(val serverUrl: String, val token: String, val roomName: String, val expiresInSeconds: Int)

    /** Call from a worker thread. The LiveKit API secret is never sent to or stored by this app. */
    fun issue(roomId: String, displayName: String): VoiceToken {
        val session = discover.currentSession()
            ?: throw IllegalStateException("Sign in first to use game voice chat.")
        val result = discover.request(
            "/functions/v1/game-voice-token",
            "POST",
            JSONObject().put("room_id", roomId).put("display_name", displayName.trim().take(40)),
            session.accessToken
        ) as JSONObject
        val url = result.optString("url")
        val token = result.optString("token")
        if (url.isBlank() || token.isBlank()) {
            throw IllegalStateException(result.optString("error").ifBlank { "Voice service did not return a room token." })
        }
        return VoiceToken(url, token, result.optString("room_name"), result.optInt("expires_in", 900))
    }
}
