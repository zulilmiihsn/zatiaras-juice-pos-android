package com.zatiaras.pos.feature.auth.lock

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zatiaras.pos.core.data.util.PasswordHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock_prefs")

/**
 * Manages app lock preferences including:
 * - Biometric enabled state
 * - PIN code (hashed)
 * - Lock enabled state
 *
 * Uses DataStore for non-sensitive flags and encrypted storage for PIN material.
 */
@Singleton
class AppLockPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SET = booleanPreferencesKey("pin_set")
        private val KEY_PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        private val KEY_PIN_LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
        private const val SOFT_LOCKOUT_ATTEMPTS = 5
        private const val HARD_LOCKOUT_ATTEMPTS = 10
        private const val SOFT_LOCKOUT_MS = 30_000L
        private const val HARD_LOCKOUT_MS = 5 * 60_000L
        private const val SECURE_PREFS_FILE = "secure_app_lock"
        private const val SECURE_PIN_HASH = "pin_hash"
        private const val SECURE_PIN_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val SECURE_PIN_LOCKOUT_UNTIL = "pin_lockout_until"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // ==================== LOCK STATE ====================

    /**
     * Check if app lock is enabled.
     */
    fun isLockEnabled(): Flow<Boolean> = context.appLockDataStore.data.map { prefs ->
        prefs[KEY_LOCK_ENABLED] ?: false
    }

    /**
     * Get current lock enabled state (suspend).
     */
    suspend fun isLockEnabledNow(): Boolean = isLockEnabled().first()

    /**
     * Enable or disable app lock.
     */
    suspend fun setLockEnabled(enabled: Boolean) {
        context.appLockDataStore.edit { prefs ->
            prefs[KEY_LOCK_ENABLED] = enabled
        }
    }

    // ==================== BIOMETRIC ====================

    /**
     * Check if biometric is enabled.
     */
    fun isBiometricEnabled(): Flow<Boolean> = context.appLockDataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Get current biometric enabled state (suspend).
     */
    suspend fun isBiometricEnabledNow(): Boolean = isBiometricEnabled().first()

    /**
     * Enable or disable biometric authentication.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.appLockDataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    // ==================== PIN ====================

    /**
     * Check if PIN is set.
     */
    fun isPinSet(): Flow<Boolean> = context.appLockDataStore.data.map { prefs ->
        prefs[KEY_PIN_SET] ?: false
    }

    /**
     * Get current PIN set state (suspend).
     */
    suspend fun isPinSetNow(): Boolean {
        migratePinStorageIfNeeded()
        return isPinSet().first()
    }

    /**
     * Set PIN (stores hashed value).
     */
    suspend fun setPin(pin: String) {
        val hashedPin = PasswordHasher.hashPin(pin)
        encryptedPrefs.edit()
            .putString(SECURE_PIN_HASH, hashedPin)
            .putInt(SECURE_PIN_FAILED_ATTEMPTS, 0)
            .putLong(SECURE_PIN_LOCKOUT_UNTIL, 0L)
            .apply()
        context.appLockDataStore.edit { prefs ->
            prefs[KEY_PIN_SET] = true
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_PIN_FAILED_ATTEMPTS)
            prefs.remove(KEY_PIN_LOCKOUT_UNTIL)
        }
    }

    /**
     * Verify PIN against stored hash.
     */
    suspend fun verifyPin(pin: String): Boolean {
        migratePinStorageIfNeeded()
        val storedHash = encryptedPrefs.getString(SECURE_PIN_HASH, null)
        if (storedHash == null) return false
        val isValid = PasswordHasher.verifyPin(pin, storedHash)
        if (isValid && PasswordHasher.needsRehash(storedHash)) {
            encryptedPrefs.edit()
                .putString(SECURE_PIN_HASH, PasswordHasher.hashPin(pin))
                .apply()
        }
        return isValid
    }

    suspend fun isPinLockedOutNow(now: Long = System.currentTimeMillis()): Boolean {
        migratePinStorageIfNeeded()
        return getPinLockoutRemainingMillis(now) > 0
    }

    suspend fun getPinLockoutRemainingMillis(now: Long = System.currentTimeMillis()): Long {
        migratePinStorageIfNeeded()
        val lockoutUntil = encryptedPrefs.getLong(SECURE_PIN_LOCKOUT_UNTIL, 0L)
        return (lockoutUntil - now).coerceAtLeast(0L)
    }

    suspend fun recordFailedPinAttempt(now: Long = System.currentTimeMillis()): Long {
        migratePinStorageIfNeeded()
        var lockoutDuration = 0L
        val attempts = encryptedPrefs.getInt(SECURE_PIN_FAILED_ATTEMPTS, 0) + 1
        lockoutDuration = when {
            attempts >= HARD_LOCKOUT_ATTEMPTS -> HARD_LOCKOUT_MS
            attempts >= SOFT_LOCKOUT_ATTEMPTS -> SOFT_LOCKOUT_MS
            else -> 0L
        }
        encryptedPrefs.edit()
            .putInt(SECURE_PIN_FAILED_ATTEMPTS, attempts)
            .apply()
        if (lockoutDuration > 0L) {
            encryptedPrefs.edit()
                .putLong(SECURE_PIN_LOCKOUT_UNTIL, now + lockoutDuration)
                .apply()
        }
        return lockoutDuration
    }

    suspend fun clearPinLockout() {
        encryptedPrefs.edit()
            .putInt(SECURE_PIN_FAILED_ATTEMPTS, 0)
            .putLong(SECURE_PIN_LOCKOUT_UNTIL, 0L)
            .apply()
        context.appLockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_FAILED_ATTEMPTS)
            prefs.remove(KEY_PIN_LOCKOUT_UNTIL)
        }
    }

    /**
     * Clear PIN.
     */
    suspend fun clearPin() {
        encryptedPrefs.edit()
            .remove(SECURE_PIN_HASH)
            .putInt(SECURE_PIN_FAILED_ATTEMPTS, 0)
            .putLong(SECURE_PIN_LOCKOUT_UNTIL, 0L)
            .apply()
        context.appLockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs[KEY_PIN_SET] = false
            prefs.remove(KEY_PIN_FAILED_ATTEMPTS)
            prefs.remove(KEY_PIN_LOCKOUT_UNTIL)
        }
    }

    /**
     * Reset all lock settings.
     */
    suspend fun resetAllSettings() {
        encryptedPrefs.edit().clear().apply()
        context.appLockDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private suspend fun migratePinStorageIfNeeded() {
        val prefs = context.appLockDataStore.data.first()
        val legacyHash = prefs[KEY_PIN_HASH] ?: return

        if (!encryptedPrefs.contains(SECURE_PIN_HASH)) {
            encryptedPrefs.edit()
                .putString(SECURE_PIN_HASH, legacyHash)
                .putInt(SECURE_PIN_FAILED_ATTEMPTS, prefs[KEY_PIN_FAILED_ATTEMPTS] ?: 0)
                .putLong(SECURE_PIN_LOCKOUT_UNTIL, prefs[KEY_PIN_LOCKOUT_UNTIL] ?: 0L)
                .apply()
        }

        context.appLockDataStore.edit { mutablePrefs ->
            mutablePrefs.remove(KEY_PIN_HASH)
            mutablePrefs.remove(KEY_PIN_FAILED_ATTEMPTS)
            mutablePrefs.remove(KEY_PIN_LOCKOUT_UNTIL)
            mutablePrefs[KEY_PIN_SET] = true
        }
    }
}
