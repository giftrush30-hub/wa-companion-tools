package com.example.wacompanion

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Audio-only LiveKit room connection.
 * The caller must request RECORD_AUDIO permission before connect().
 * A short-lived token must come from GamesVoiceTokenBackend; never embed LiveKit secrets.
 */
class GamesVoiceClient(context: Context, private val listener: Listener) {
    interface Listener {
        fun onConnected()
        fun onMicrophoneChanged(enabled: Boolean)
        fun onDisconnected()
        fun onError(message: String)
    }

    private val room: Room = LiveKit.create(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var connected = false
    @Volatile private var microphoneEnabled = false

    fun connect(serverUrl: String, token: String) {
        if (serverUrl.isBlank() || token.isBlank()) {
            listener.onError("Voice connection details are missing.")
            return
        }
        scope.launch {
            try {
                room.connect(serverUrl, token)
                connected = true
                val enabled = room.localParticipant.setMicrophoneEnabled(true)
                microphoneEnabled = enabled
                if (!enabled) {
                    listener.onError("The microphone could not be started. Check microphone permission.")
                } else {
                    listener.onConnected()
                    listener.onMicrophoneChanged(true)
                }
            } catch (error: Exception) {
                connected = false
                listener.onError(error.message ?: "Could not connect to game voice.")
            }
        }
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        if (!connected) return
        scope.launch {
            try {
                microphoneEnabled = room.localParticipant.setMicrophoneEnabled(enabled)
                listener.onMicrophoneChanged(microphoneEnabled)
            } catch (error: Exception) {
                listener.onError(error.message ?: "Could not change microphone state.")
            }
        }
    }

    /** Mutes local playback only; the player remains connected to the voice room. */
    fun setSpeakerMuted(muted: Boolean) {
        if (connected) room.setSpeakerMute(muted)
    }

    fun activeSpeakerNames(): List<String> =
        room.activeSpeakers.mapNotNull { participant -> participant.name?.takeIf { it.isNotBlank() } }

    fun isMicrophoneEnabled(): Boolean = microphoneEnabled

    fun disconnect() {
        scope.launch {
            try {
                if (connected && microphoneEnabled) {
                    room.localParticipant.setMicrophoneEnabled(false)
                }
                room.disconnect()
                connected = false
                microphoneEnabled = false
                listener.onDisconnected()
            } catch (error: Exception) {
                listener.onError(error.message ?: "Could not leave game voice.")
            } finally {
                room.release()
                scope.cancel()
            }
        }
    }
}
