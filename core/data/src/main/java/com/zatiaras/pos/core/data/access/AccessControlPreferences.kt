package com.zatiaras.pos.core.data.access

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accessControlDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "access_control_prefs"
)

/**
 * Manages access control preferences with sensitive data stored in EncryptedSharedPreferences.
 * 
 * Features:
 * - Owner PIN: Stored encrypted at rest using AES256.
 * - Locked Routes: List of routes that require owner PIN.
 */
@Singleton
class AccessControlPreferences @Inject constructor(
    @ApplicationContext private val context: Context
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
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to regular prefs (Warning: Unencrypted)")
            context.getSharedPreferences(SECURE_PREFS_FILE + "_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val SECURE_PREFS_FILE = "secure_access_control"
        private const val KEY_OWNER_PIN_HASH = "owner_pin_hash_v2" // Versioned to force re-entry
        private val KEY_LOCKED_ROUTES = stringSetPreferencesKey("locked_routes")
    }

    // ==================== OWNER PIN (SECURE STORAGE) ====================

    /**
     * Check if owner PIN is set.
     */
    fun isOwnerPinSet(): Flow<Boolean> {
        // Since SharedPreferences doesn't have a direct Flow, we wrap it
        // and also allow for simple manual check
        return context.accessControlDataStore.data.map {
            encryptedPrefs.contains(KEY_OWNER_PIN_HASH)
        }
    }

    /**
     * Check if owner PIN is set (suspend).
     */
    suspend fun isOwnerPinSetNow(): Boolean {
        return encryptedPrefs.contains(KEY_OWNER_PIN_HASH)
    }

    /**
     * Set owner PIN (stores hashed value in encrypted storage).
     */
    suspend fun setOwnerPin(pin: String) {
        val hashedPin = hashPin(pin)
        encryptedPrefs.edit().putString(KEY_OWNER_PIN_HASH, hashedPin).apply()
        
        // Trigger a change in DataStore just to notify observers of isOwnerPinSet()
        // This is a common pattern to bridge SharedPreferences items that UI observes via Flow
        context.accessControlDataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("pin_update_trigger")] = setOf(System.currentTimeMillis().toString())
        }
    }

    /**
     * Verify PIN against stored owner PIN hash.
     */
    suspend fun verifyOwnerPin(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_OWNER_PIN_HASH, null)
        if (storedHash == null) return false
        return hashPin(pin) == storedHash
    }

    /**
     * Clear owner PIN.
     */
    suspend fun clearOwnerPin() {
        encryptedPrefs.edit().remove(KEY_OWNER_PIN_HASH).apply()
        context.accessControlDataStore.edit { it.clear() }
    }

    // ==================== LOCKED ROUTES (DATASTORE) ====================

    /**
     * Get set of locked route strings.
     */
    fun getLockedRoutes(): Flow<Set<String>> {
        return context.accessControlDataStore.data.map { prefs ->
            prefs[KEY_LOCKED_ROUTES] ?: emptySet()
        }
    }

    /**
     * Get locked routes as LockableRoute enum list.
     */
    fun getLockedRoutesEnum(): Flow<List<LockableRoute>> {
        return getLockedRoutes().map { routeStrings ->
            routeStrings.mapNotNull { LockableRoute.fromRoute(it) }
        }
    }

    /**
     * Get locked routes now (suspend).
     */
    suspend fun getLockedRoutesNow(): Set<String> {
        return getLockedRoutes().first()
    }

    /**
     * Check if a specific route is locked.
     */
    fun isRouteLocked(route: String): Flow<Boolean> {
        return getLockedRoutes().map { lockedRoutes ->
            lockedRoutes.contains(route)
        }
    }

    /**
     * Check if route is locked (suspend).
     */
    suspend fun isRouteLockedNow(route: String): Boolean {
        return getLockedRoutesNow().contains(route)
    }

    /**
     * Set locked routes.
     */
    suspend fun setLockedRoutes(routes: Set<String>) {
        context.accessControlDataStore.edit { prefs ->
            prefs[KEY_LOCKED_ROUTES] = routes
        }
    }

    /**
     * Lock a specific route.
     */
    suspend fun lockRoute(route: String) {
        val current = getLockedRoutesNow().toMutableSet()
        current.add(route)
        setLockedRoutes(current)
    }

    /**
     * Unlock a specific route.
     */
    suspend fun unlockRoute(route: String) {
        val current = getLockedRoutesNow().toMutableSet()
        current.remove(route)
        setLockedRoutes(current)
    }

    /**
     * Toggle route lock status.
     */
    suspend fun toggleRouteLock(route: String) {
        if (isRouteLockedNow(route)) {
            unlockRoute(route)
        } else {
            lockRoute(route)
        }
    }

    // ==================== RESET ====================

    suspend fun resetAll() {
        encryptedPrefs.edit().clear().apply()
        context.accessControlDataStore.edit { it.clear() }
    }

    // ==================== HELPERS ====================

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

