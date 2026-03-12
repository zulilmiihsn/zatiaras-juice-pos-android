package com.zatiaras.pos.feature.auth.lock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
 * Uses DataStore for secure preference storage.
 */
@Singleton
class AppLockPreferences @Inject constructor(
    @ApplicationContext private val context: Context
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
    }

    // ==================== LOCK STATE ====================

    /**
     * Check if app lock is enabled.
     */
    fun isLockEnabled(): Flow<Boolean> {
        return context.appLockDataStore.data.map { prefs ->
            prefs[KEY_LOCK_ENABLED] ?: false
        }
    }

    /**
     * Get current lock enabled state (suspend).
     */
    suspend fun isLockEnabledNow(): Boolean {
        return isLockEnabled().first()
    }

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
    fun isBiometricEnabled(): Flow<Boolean> {
        return context.appLockDataStore.data.map { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] ?: false
        }
    }

    /**
     * Get current biometric enabled state (suspend).
     */
    suspend fun isBiometricEnabledNow(): Boolean {
        return isBiometricEnabled().first()
    }

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
    fun isPinSet(): Flow<Boolean> {
        return context.appLockDataStore.data.map { prefs ->
            prefs[KEY_PIN_SET] ?: false
        }
    }

    /**
     * Get current PIN set state (suspend).
     */
    suspend fun isPinSetNow(): Boolean {
        return isPinSet().first()
    }

    /**
     * Set PIN (stores hashed value).
     */
    suspend fun setPin(pin: String) {
        val hashedPin = PasswordHasher.hashPin(pin)
        context.appLockDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hashedPin
            prefs[KEY_PIN_SET] = true
            prefs[KEY_PIN_FAILED_ATTEMPTS] = 0
            prefs[KEY_PIN_LOCKOUT_UNTIL] = 0L
        }
    }

    /**
     * Verify PIN against stored hash.
     */
    suspend fun verifyPin(pin: String): Boolean {
        val storedHash = context.appLockDataStore.data.map { prefs ->
            prefs[KEY_PIN_HASH]
        }.first()
        
        if (storedHash == null) return false
        val isValid = PasswordHasher.verifyPin(pin, storedHash)
        if (isValid && PasswordHasher.needsRehash(storedHash)) {
            context.appLockDataStore.edit { prefs ->
                prefs[KEY_PIN_HASH] = PasswordHasher.hashPin(pin)
            }
        }
        return isValid
    }

    suspend fun isPinLockedOutNow(now: Long = System.currentTimeMillis()): Boolean {
        return getPinLockoutRemainingMillis(now) > 0
    }

    suspend fun getPinLockoutRemainingMillis(now: Long = System.currentTimeMillis()): Long {
        val lockoutUntil = context.appLockDataStore.data.map { prefs ->
            prefs[KEY_PIN_LOCKOUT_UNTIL] ?: 0L
        }.first()
        return (lockoutUntil - now).coerceAtLeast(0L)
    }

    suspend fun recordFailedPinAttempt(now: Long = System.currentTimeMillis()): Long {
        var lockoutDuration = 0L
        context.appLockDataStore.edit { prefs ->
            val attempts = (prefs[KEY_PIN_FAILED_ATTEMPTS] ?: 0) + 1
            prefs[KEY_PIN_FAILED_ATTEMPTS] = attempts
            lockoutDuration = when {
                attempts >= HARD_LOCKOUT_ATTEMPTS -> HARD_LOCKOUT_MS
                attempts >= SOFT_LOCKOUT_ATTEMPTS -> SOFT_LOCKOUT_MS
                else -> 0L
            }
            if (lockoutDuration > 0L) {
                prefs[KEY_PIN_LOCKOUT_UNTIL] = now + lockoutDuration
            }
        }
        return lockoutDuration
    }

    suspend fun clearPinLockout() {
        context.appLockDataStore.edit { prefs ->
            prefs[KEY_PIN_FAILED_ATTEMPTS] = 0
            prefs[KEY_PIN_LOCKOUT_UNTIL] = 0L
        }
    }

    /**
     * Clear PIN.
     */
    suspend fun clearPin() {
        context.appLockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs[KEY_PIN_SET] = false
            prefs[KEY_PIN_FAILED_ATTEMPTS] = 0
            prefs[KEY_PIN_LOCKOUT_UNTIL] = 0L
        }
    }

    /**
     * Reset all lock settings.
     */
    suspend fun resetAllSettings() {
        context.appLockDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
