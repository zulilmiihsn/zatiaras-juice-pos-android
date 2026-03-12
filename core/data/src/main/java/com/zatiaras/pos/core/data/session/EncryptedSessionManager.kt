package com.zatiaras.pos.core.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure Session Manager using Android's EncryptedSharedPreferences.
 * 
 * This implementation stores Supabase tokens encrypted at rest using AES256-GCM.
 * Token data is protected even if the device is rooted or the app data is extracted.
 * 
 * Security: MasterKey uses Android Keystore for key management.
 */
@Singleton
class EncryptedSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : SessionManager {

    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create MasterKey")
            throw e
        }
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences for secure session storage")
            throw IllegalStateException("EncryptedSharedPreferences initialization failed", e)
        }
    }

    override suspend fun saveSession(session: UserSession) {
        try {
            val sessionJson = json.encodeToString(UserSession.serializer(), session)
            encryptedPrefs.edit()
                .putString(KEY_USER_SESSION, sessionJson)
                .commit() // Use commit() for synchronous save
            Timber.d("Session saved securely")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save session")
        }
    }

    override suspend fun loadSession(): UserSession? {
        return try {
            val sessionJson = encryptedPrefs.getString(KEY_USER_SESSION, null)
            if (sessionJson.isNullOrEmpty()) {
                Timber.d("No session found")
                return null
            }
            val session = json.decodeFromString(UserSession.serializer(), sessionJson)
            Timber.d("Session loaded successfully")
            session
        } catch (e: Exception) {
            Timber.e(e, "Failed to load session, clearing corrupted data")
            // Clear corrupted session data
            try {
                deleteSession()
            } catch (deleteError: Exception) {
                Timber.e(deleteError, "Failed to delete corrupted session")
            }
            null
        }
    }

    override suspend fun deleteSession() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_USER_SESSION)
                .commit()
            Timber.d("Session deleted")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete session")
        }
    }

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "zatiaras_secure_session"
        private const val KEY_USER_SESSION = "user_session"
    }
}
