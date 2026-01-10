package com.zatiaras.pos.feature.auth.lock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
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
        val hashedPin = hashPin(pin)
        context.appLockDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hashedPin
            prefs[KEY_PIN_SET] = true
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
        return hashPin(pin) == storedHash
    }

    /**
     * Clear PIN.
     */
    suspend fun clearPin() {
        context.appLockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs[KEY_PIN_SET] = false
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

    // ==================== HELPERS ====================

    /**
     * Hash PIN using SHA-256.
     */
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
