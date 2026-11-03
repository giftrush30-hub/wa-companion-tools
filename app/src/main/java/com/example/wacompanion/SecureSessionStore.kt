package com.example.wacompanion

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the small Supabase session record with an AES-GCM key held by Android Keystore.
 * The key is non-exportable; ciphertext alone in SharedPreferences is not a usable session.
 */
internal class SecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("wa_discover_session_secure", Context.MODE_PRIVATE)
    private val alias = "wa_companion_discover_session_v1"

    @Synchronized
    fun read(): JSONObject? {
        val packed = prefs.getString("session_blob", null) ?: return null
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            require(bytes.size > 12) { "Invalid encrypted session" }
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        } catch (_: Exception) {
            clear()
            null
        }
    }

    @Synchronized
    fun write(session: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(session.toString().toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        check(prefs.edit().putString("session_blob", Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "Could not save the encrypted sign-in session"
        }
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().commit()
        try {
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                if (containsAlias(alias)) deleteEntry(alias)
            }
        } catch (_: Exception) {
            // A later sign-in can recreate the key if the platform keystore is temporarily unavailable.
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
