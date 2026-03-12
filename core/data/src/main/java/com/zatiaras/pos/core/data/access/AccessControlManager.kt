package com.zatiaras.pos.core.data.access

import com.zatiaras.pos.core.data.local.dao.AppSettingsDao
import com.zatiaras.pos.core.data.repository.AppSettingsRepository
import com.zatiaras.pos.core.data.session.SessionPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of access check for a route.
 */
sealed class AccessCheckResult {
    /** User has full access (owner or route not locked) */
    data object Granted : AccessCheckResult()
    
    /** User needs to enter owner PIN to access */
    data object RequiresOwnerPin : AccessCheckResult()
    
    /** Owner PIN is not set, so access is granted (fallback) */
    data object GrantedNoPinSet : AccessCheckResult()
}

/**
 * Manages access control logic by combining user role and locked routes.
 * 
 * Access Rules:
 * 1. PEMILIK (Owner) always has full access
 * 2. KASIR (Cashier) needs owner PIN for locked routes
 * 3. If owner PIN is not set, all routes are accessible
 * 
 * This class uses AppSettingsRepository for synced settings when available,
 * and falls back to AccessControlPreferences for local-only operation.
 */
@Singleton
class AccessControlManager @Inject constructor(
    private val sessionPreferences: SessionPreferences,
    private val accessControlPreferences: AccessControlPreferences,
    private val appSettingsRepository: AppSettingsRepository,
    private val appSettingsDao: AppSettingsDao
) {
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Initialize access control settings.
     * Call this on app startup.
     */
    suspend fun initialize() {
        try {
            appSettingsRepository.initializeIfNeeded()
            Timber.d("AccessControlManager initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AccessControlManager, using local fallback")
        }
    }
    
    // ==================== ROLE ====================
    
    /**
     * Get current user role from session.
     */
    fun getCurrentRole(): Flow<UserRole> {
        return flowOf(UserRole.fromString(sessionPreferences.getRole()))
    }

    /**
     * Get current user role now (suspend).
     */
    suspend fun getCurrentRoleNow(): UserRole {
        return getCurrentRole().first()
    }

    /**
     * Check if current user is owner.
     */
    fun isOwner(): Flow<Boolean> {
        return getCurrentRole().map { it.isOwner() }
    }

    /**
     * Check if current user is owner (suspend).
     */
    suspend fun isOwnerNow(): Boolean {
        return getCurrentRoleNow().isOwner()
    }

    // ==================== ACCESS CHECK ====================

    /**
     * Check if a route requires PIN for current user.
     * Uses synced settings from AppSettings with fallback to local preferences.
     * 
     * @param route The route to check (use LockableRoute.route values)
     * @return Flow of AccessCheckResult
     */
    fun checkAccess(route: String): Flow<AccessCheckResult> {
        return combine(
            getCurrentRole(),
            observeIsRouteLocked(route),
            observeIsOwnerPinSet()
        ) { role, isLocked, isPinSet ->
            when {
                // Owner always has access
                role.isOwner() -> AccessCheckResult.Granted
                
                // Route not locked, kasir can access
                !isLocked -> AccessCheckResult.Granted
                
                // Route is locked but no PIN set (fallback to allow)
                !isPinSet -> AccessCheckResult.GrantedNoPinSet
                
                // Kasir + locked route + PIN set = requires PIN
                else -> AccessCheckResult.RequiresOwnerPin
            }
        }.catch { e ->
            Timber.e(e, "Error checking access, granting by default")
            emit(AccessCheckResult.Granted)
        }
    }

    /**
     * Check access now (suspend).
     */
    suspend fun checkAccessNow(route: String): AccessCheckResult {
        return try {
            checkAccess(route).first()
        } catch (e: Exception) {
            Timber.e(e, "Error checking access now, granting by default")
            AccessCheckResult.Granted
        }
    }

    /**
     * Check if route requires PIN (simplified boolean check).
     */
    suspend fun requiresPin(route: String): Boolean {
        return checkAccessNow(route) == AccessCheckResult.RequiresOwnerPin
    }

    // ==================== OWNER PIN (SYNCED with fallback) ====================

    /**
     * Verify owner PIN.
     */
    suspend fun verifyOwnerPin(pin: String): Boolean {
        val lockoutRemaining = accessControlPreferences.getOwnerPinLockoutRemainingMillis()
        if (lockoutRemaining > 0L) {
            Timber.w("Owner PIN is locked out for ${lockoutRemaining}ms")
            return false
        }

        val isValid = try {
            // Try synced settings first
            appSettingsRepository.verifyOwnerPin(pin)
        } catch (e: Exception) {
            Timber.w(e, "Using local preferences for PIN verification")
            accessControlPreferences.verifyOwnerPin(pin)
        }

        if (isValid) {
            accessControlPreferences.clearOwnerPinLockout()
        } else {
            accessControlPreferences.recordFailedOwnerPinAttempt()
        }

        return isValid
    }

    /**
     * Set owner PIN.
     * This syncs to Supabase.
     */
    suspend fun setOwnerPin(pin: String): Result<Unit> {
        return try {
            Timber.d("Setting owner PIN (synced)")
            val result = appSettingsRepository.setOwnerPin(pin)
            // Also set in local preferences as backup
            accessControlPreferences.setOwnerPin(pin)
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to set synced PIN, using local only")
            accessControlPreferences.setOwnerPin(pin)
            Result.failure(e)
        }
    }

    /**
     * Check if owner PIN is set.
     */
    suspend fun isOwnerPinSetNow(): Boolean {
        return try {
            appSettingsRepository.isOwnerPinSet()
        } catch (e: Exception) {
            Timber.w(e, "Using local preferences for PIN check")
            accessControlPreferences.isOwnerPinSetNow()
        }
    }

    /**
     * Check if owner PIN is set (flow).
     */
    fun isOwnerPinSet(): Flow<Boolean> {
        return try {
            appSettingsDao.observeSettings()
                .map { settings -> settings?.ownerPinHash != null }
                .catch { e ->
                    Timber.w(e, "Using local preferences for PIN set flow")
                    accessControlPreferences.isOwnerPinSet().collect { emit(it) }
                }
        } catch (e: Exception) {
            Timber.w(e, "Falling back to local preferences for PIN set")
            accessControlPreferences.isOwnerPinSet()
        }
    }

    /**
     * Observe if owner PIN is set.
     */
    private fun observeIsOwnerPinSet(): Flow<Boolean> {
        return isOwnerPinSet()
    }

    // ==================== LOCKED ROUTES (SYNCED with fallback) ====================

    /**
     * Get list of all lockable routes with their current lock status.
     * Only relevant for owner to configure.
     */
    fun getLockableRoutesWithStatus(): Flow<List<Pair<LockableRoute, Boolean>>> {
        return try {
            appSettingsDao.observeSettings()
                .map { settings ->
                    val lockedRoutes = settings?.lockedRoutes ?: emptyList()
                    LockableRoute.all().map { route ->
                        route to lockedRoutes.contains(route.route)
                    }
                }
                .catch { e ->
                    Timber.w(e, "Using local preferences for lockable routes")
                    accessControlPreferences.getLockedRoutes().map { lockedRoutes ->
                        LockableRoute.all().map { route ->
                            route to lockedRoutes.contains(route.route)
                        }
                    }.collect { emit(it) }
                }
        } catch (e: Exception) {
            Timber.w(e, "Falling back to local preferences for lockable routes")
            accessControlPreferences.getLockedRoutes().map { lockedRoutes ->
                LockableRoute.all().map { route ->
                    route to lockedRoutes.contains(route.route)
                }
            }
        }
    }

    /**
     * Observe if a specific route is locked.
     */
    private fun observeIsRouteLocked(route: String): Flow<Boolean> {
        return try {
            appSettingsDao.observeSettings()
                .map { settings -> settings?.lockedRoutes?.contains(route) ?: false }
                .catch { e ->
                    Timber.w(e, "Using local preferences for route lock check")
                    accessControlPreferences.isRouteLocked(route).collect { emit(it) }
                }
        } catch (e: Exception) {
            Timber.w(e, "Falling back to local preferences for route lock")
            accessControlPreferences.isRouteLocked(route)
        }
    }

    /**
     * Toggle lock status for a route.
     * This syncs to Supabase.
     */
    suspend fun toggleRouteLock(route: LockableRoute): Result<Unit> {
        return try {
            Timber.d("Toggling route lock (synced): ${route.route}")
            val result = appSettingsRepository.toggleRouteLock(route.route)
            // Also update local preferences as backup
            accessControlPreferences.toggleRouteLock(route.route)
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle synced route lock, using local only")
            accessControlPreferences.toggleRouteLock(route.route)
            Result.failure(e)
        }
    }

    /**
     * Get locked routes now.
     */
    suspend fun getLockedRoutesNow(): List<String> {
        return try {
            appSettingsRepository.getLockedRoutes()
        } catch (e: Exception) {
            Timber.w(e, "Using local preferences for locked routes")
            accessControlPreferences.getLockedRoutesNow().toList()
        }
    }

    // ==================== SYNC ====================

    /**
     * Sync access control settings from remote.
     * Call this when online to pull latest settings.
     */
    suspend fun syncFromRemote(): Result<Unit> {
        return try {
            Timber.d("Syncing access control from remote")
            appSettingsRepository.syncFromRemote()
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync from remote")
            Result.failure(e)
        }
    }

    /**
     * Force sync settings to remote.
     */
    suspend fun forceSyncToRemote(): Result<Unit> {
        return try {
            appSettingsRepository.forceSyncToRemote()
        } catch (e: Exception) {
            Timber.e(e, "Failed to force sync to remote")
            Result.failure(e)
        }
    }

    // ==================== MIGRATION ====================

    /**
     * Migrate from local AccessControlPreferences to synced AppSettings.
     * Call this once during app upgrade.
     */
    suspend fun migrateFromLocalPreferences() {
        try {
            // Check if already migrated (AppSettings has data)
            if (appSettingsRepository.isOwnerPinSet()) {
                Timber.d("Already migrated to synced settings")
                return
            }
            
            // Migrate owner PIN
            if (accessControlPreferences.isOwnerPinSetNow()) {
                // Can't migrate hash directly, user will need to reset PIN
                Timber.w("Owner PIN needs to be reset after migration (cannot migrate hash)")
            }
            
            // Migrate locked routes
            val localLockedRoutes = accessControlPreferences.getLockedRoutesNow()
            if (localLockedRoutes.isNotEmpty()) {
                appSettingsRepository.setLockedRoutes(localLockedRoutes.toList())
                Timber.d("Migrated ${localLockedRoutes.size} locked routes to synced settings")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate access control preferences")
        }
    }
}
