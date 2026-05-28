package com.zatiaras.pos.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zatiaras.pos.core.data.util.PasswordHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores local offline-login verifiers outside Room and encrypted at rest.
 *
 * The app still supports offline login, but no longer bulk-syncs every user's
 * password hash into the local database.
 */
@Singleton
class LocalCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create encrypted local credential store")
            throw IllegalStateException("Local credential store initialization failed", e)
        }
    }

    fun hasCredential(userId: String): Boolean = encryptedPrefs.contains(keyFor(userId))

    fun verifyPassword(userId: String, password: String): Boolean {
        val storedHash = encryptedPrefs.getString(keyFor(userId), null) ?: return false
        return PasswordHasher.verify(password, storedHash)
    }

    fun savePasswordHash(userId: String, passwordHash: String) {
        encryptedPrefs.edit()
            .putString(keyFor(userId), passwordHash)
            .apply()
    }

    fun savePassword(userId: String, password: String) {
        savePasswordHash(userId, PasswordHasher.hash(password))
    }

    fun clearCredential(userId: String) {
        encryptedPrefs.edit()
            .remove(keyFor(userId))
            .apply()
    }

    private fun keyFor(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
        return "user_password_hash_$digest"
    }

    private companion object {
        const val SECURE_PREFS_FILE = "secure_local_credentials"
    }
}
