package com.zatiaras.pos.core.data.access

import com.zatiaras.pos.core.data.session.SessionPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
 */
@Singleton
class AccessControlManager @Inject constructor(
    private val sessionPreferences: SessionPreferences,
    private val accessControlPreferences: AccessControlPreferences
) {
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

    /**
     * Check if a route requires PIN for current user.
     * 
     * @param route The route to check (use LockableRoute.route values)
     * @return Flow of AccessCheckResult
     */
    fun checkAccess(route: String): Flow<AccessCheckResult> {
        return combine(
            getCurrentRole(),
            accessControlPreferences.isRouteLocked(route),
            accessControlPreferences.isOwnerPinSet()
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
        }
    }

    /**
     * Check access now (suspend).
     */
    suspend fun checkAccessNow(route: String): AccessCheckResult {
        return checkAccess(route).first()
    }

    /**
     * Check if route requires PIN (simplified boolean check).
     */
    suspend fun requiresPin(route: String): Boolean {
        return checkAccessNow(route) == AccessCheckResult.RequiresOwnerPin
    }

    /**
     * Verify owner PIN.
     */
    suspend fun verifyOwnerPin(pin: String): Boolean {
        return accessControlPreferences.verifyOwnerPin(pin)
    }

    /**
     * Get list of all lockable routes with their current lock status.
     * Only relevant for owner to configure.
     */
    fun getLockableRoutesWithStatus(): Flow<List<Pair<LockableRoute, Boolean>>> {
        return accessControlPreferences.getLockedRoutes().map { lockedRoutes ->
            LockableRoute.all().map { route ->
                route to lockedRoutes.contains(route.route)
            }
        }
    }

    /**
     * Toggle lock status for a route.
     */
    suspend fun toggleRouteLock(route: LockableRoute) {
        accessControlPreferences.toggleRouteLock(route.route)
    }

    /**
     * Set owner PIN.
     */
    suspend fun setOwnerPin(pin: String) {
        accessControlPreferences.setOwnerPin(pin)
    }

    /**
     * Check if owner PIN is set.
     */
    suspend fun isOwnerPinSetNow(): Boolean {
        return accessControlPreferences.isOwnerPinSetNow()
    }

    /**
     * Check if owner PIN is set (flow).
     */
    fun isOwnerPinSet(): Flow<Boolean> {
        return accessControlPreferences.isOwnerPinSet()
    }
}
