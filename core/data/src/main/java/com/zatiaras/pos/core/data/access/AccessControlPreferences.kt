package com.zatiaras.pos.core.data.access

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accessControlDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "access_control_prefs"
)

/**
 * Manages access control preferences for multi-role system.
 * 
 * Features:
 * - Owner PIN: A PIN set by the owner that kasir must enter to access locked screens
 * - Locked Routes: List of routes that require owner PIN for kasir access
 * 
 * This is SEPARATE from AppLockPreferences which handles app-level biometric/PIN lock.
 * This specifically handles role-based access control.
 */
@Singleton
class AccessControlPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_OWNER_PIN_HASH = stringPreferencesKey("owner_pin_hash")
        private val KEY_LOCKED_ROUTES = stringSetPreferencesKey("locked_routes")
    }

    // ==================== OWNER PIN ====================

    /**
     * Check if owner PIN is set.
     */
    fun isOwnerPinSet(): Flow<Boolean> {
        return context.accessControlDataStore.data.map { prefs ->
            prefs[KEY_OWNER_PIN_HASH] != null
        }
    }

    /**
     * Check if owner PIN is set (suspend).
     */
    suspend fun isOwnerPinSetNow(): Boolean {
        return isOwnerPinSet().first()
    }

    /**
     * Set owner PIN (stores hashed value).
     * Only owner can set this PIN.
     */
    suspend fun setOwnerPin(pin: String) {
        val hashedPin = hashPin(pin)
        context.accessControlDataStore.edit { prefs ->
            prefs[KEY_OWNER_PIN_HASH] = hashedPin
        }
    }

    /**
     * Verify PIN against stored owner PIN hash.
     * Used when kasir tries to access locked screens.
     */
    suspend fun verifyOwnerPin(pin: String): Boolean {
        val storedHash = context.accessControlDataStore.data.map { prefs ->
            prefs[KEY_OWNER_PIN_HASH]
        }.first()

        if (storedHash == null) return false
        return hashPin(pin) == storedHash
    }

    /**
     * Clear owner PIN.
     */
    suspend fun clearOwnerPin() {
        context.accessControlDataStore.edit { prefs ->
            prefs.remove(KEY_OWNER_PIN_HASH)
        }
    }

    // ==================== LOCKED ROUTES ====================

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
     * Set locked routes (replaces existing).
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

    /**
     * Reset all access control settings.
     */
    suspend fun resetAll() {
        context.accessControlDataStore.edit { prefs ->
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
