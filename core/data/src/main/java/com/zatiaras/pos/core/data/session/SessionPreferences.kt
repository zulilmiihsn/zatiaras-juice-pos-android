package com.zatiaras.pos.core.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user login session persistence.
 * 
 * Uses EncryptedSharedPreferences to securely store session data.
 * Session persists across app restarts until user logs out or session expires.
 * 
 * Session timeout: 8 hours (configurable via companion object)
 */
@Singleton
class SessionPreferences @Inject constructor(
    private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "zatiaras_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save user session after successful login.
     */
    fun saveSession(userId: String, username: String, displayName: String, role: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_ROLE, role)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Timber.d("Session saved for user: $username")
    }

    /**
     * Check if user has an active session.
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Check if session has expired based on SESSION_TIMEOUT_MS.
     * 
     * @return true if session is expired and user needs to re-login
     */
    fun isSessionExpired(): Boolean {
        if (!isLoggedIn()) return true
        
        val loginTime = getLoginTime()
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - loginTime
        
        val isExpired = elapsed > SESSION_TIMEOUT_MS
        if (isExpired) {
            Timber.d("Session expired. Elapsed: ${elapsed / 1000 / 60} minutes")
        }
        return isExpired
    }

    /**
     * Check if session is valid (logged in and not expired).
     */
    fun isSessionValid(): Boolean {
        return isLoggedIn() && !isSessionExpired()
    }

    /**
     * Get saved user ID from session.
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get saved username from session.
     */
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    /**
     * Get saved display name from session.
     */
    fun getDisplayName(): String? {
        return prefs.getString(KEY_DISPLAY_NAME, null)
    }

    /**
     * Get saved role from session.
     */
    fun getRole(): String? {
        return prefs.getString(KEY_ROLE, null)
    }

    /**
     * Get login timestamp.
     */
    fun getLoginTime(): Long {
        return prefs.getLong(KEY_LOGIN_TIME, 0)
    }

    /**
     * Clear session on logout.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
        Timber.d("Session cleared")
    }

    /**
     * Refresh session timestamp (extend session on activity).
     * Call this periodically to keep session alive for active users.
     */
    fun refreshSession() {
        if (isLoggedIn()) {
            prefs.edit().putLong(KEY_LOGIN_TIME, System.currentTimeMillis()).apply()
            Timber.d("Session refreshed")
        }
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE = "role"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        
        /**
         * Session timeout duration.
         * Default: 8 hours (reasonable for a full work shift)
         */
        const val SESSION_TIMEOUT_MS = 8 * 60 * 60 * 1000L // 8 hours
    }
}
