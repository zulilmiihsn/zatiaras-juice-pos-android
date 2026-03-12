package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.AppSettingsDao
import com.zatiaras.pos.core.data.local.entity.AppSettingsEntity
import com.zatiaras.pos.core.data.remote.SettingsRemoteDataSource
import com.zatiaras.pos.core.data.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Application Settings.
 * 
 * Implements offline-first pattern:
 * 1. Read from local Room database
 * 2. Sync changes to Supabase in background
 * 3. Pull remote changes on app start
 * 
 * Handles:
 * - Owner PIN management
 * - Locked routes for kasir access control
 * - Store info for receipt printing
 * - Default printer paper width
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    private val settingsDao: AppSettingsDao,
    private val remoteDataSource: SettingsRemoteDataSource
) {
    companion object {
        private const val DEFAULT_SETTINGS_ID = "default"
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize settings if not exists.
     * Call this on app startup.
     */
    suspend fun initializeIfNeeded() {
        settingsDao.initializeDefaultSettings()
    }

    // ==================== READ ====================

    /**
     * Observe settings as Flow.
     */
    fun observeSettings(): Flow<AppSettingsEntity?> {
        return settingsDao.observeSettings()
    }

    /**
     * Get current settings.
     */
    suspend fun getSettings(): AppSettingsEntity? {
        return settingsDao.getSettings()
    }

    /**
     * Get store name for receipt.
     */
    suspend fun getStoreName(): String {
        return settingsDao.getStoreName() ?: "Zatiaras Juice"
    }

    /**
     * Get store address for receipt.
     */
    suspend fun getStoreAddress(): String? {
        return settingsDao.getStoreAddress()
    }

    /**
     * Get default paper width.
     */
    suspend fun getDefaultPaperWidth(): Int {
        return getSettings()?.defaultPaperWidth ?: 58
    }

    // ==================== OWNER PIN ====================

    /**
     * Check if owner PIN is set.
     */
    suspend fun isOwnerPinSet(): Boolean {
        return settingsDao.getOwnerPinHash() != null
    }

    /**
     * Set owner PIN.
     * Hashes the PIN and stores it locally, then syncs to remote.
     */
    suspend fun setOwnerPin(pin: String): Result<Unit> {
        return try {
            val hashedPin = PasswordHasher.hashPin(pin)
            settingsDao.updateOwnerPin(hashedPin)
            
            // Sync to remote
            syncSettingsToRemote()
            
            Timber.d("Owner PIN set successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set owner PIN")
            Result.failure(e)
        }
    }

    /**
     * Verify owner PIN.
     */
    suspend fun verifyOwnerPin(pin: String): Boolean {
        val storedHash = settingsDao.getOwnerPinHash() ?: return false
        val isValid = PasswordHasher.verifyPin(pin, storedHash)
        if (isValid && PasswordHasher.needsRehash(storedHash)) {
            val upgradedHash = PasswordHasher.hashPin(pin)
            settingsDao.updateOwnerPin(upgradedHash)
            syncSettingsToRemote()
            Timber.d("Owner PIN hash upgraded from legacy SHA-256 to PBKDF2")
        }
        return isValid
    }

    /**
     * Clear owner PIN.
     */
    suspend fun clearOwnerPin(): Result<Unit> {
        return try {
            settingsDao.updateOwnerPin(null)
            syncSettingsToRemote()
            Timber.d("Owner PIN cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear owner PIN")
            Result.failure(e)
        }
    }

    // ==================== LOCKED ROUTES ====================

    /**
     * Get locked routes.
     */
    suspend fun getLockedRoutes(): List<String> {
        val settings = settingsDao.getSettings() ?: return emptyList()
        return settings.lockedRoutes
    }

    /**
     * Check if a specific route is locked.
     */
    suspend fun isRouteLocked(route: String): Boolean {
        return getLockedRoutes().contains(route)
    }

    /**
     * Set locked routes (replaces existing).
     */
    suspend fun setLockedRoutes(routes: List<String>): Result<Unit> {
        return try {
            val routesString = routes.joinToString(",")
            settingsDao.updateLockedRoutesRaw(routesString)
            syncSettingsToRemote()
            Timber.d("Locked routes updated: $routes")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update locked routes")
            Result.failure(e)
        }
    }

    /**
     * Lock a specific route.
     */
    suspend fun lockRoute(route: String): Result<Unit> {
        val currentRoutes = getLockedRoutes().toMutableList()
        if (!currentRoutes.contains(route)) {
            currentRoutes.add(route)
        }
        return setLockedRoutes(currentRoutes)
    }

    /**
     * Unlock a specific route.
     */
    suspend fun unlockRoute(route: String): Result<Unit> {
        val currentRoutes = getLockedRoutes().toMutableList()
        currentRoutes.remove(route)
        return setLockedRoutes(currentRoutes)
    }

    /**
     * Toggle route lock status.
     */
    suspend fun toggleRouteLock(route: String): Result<Unit> {
        return if (isRouteLocked(route)) {
            unlockRoute(route)
        } else {
            lockRoute(route)
        }
    }

    // ==================== STORE INFO ====================

    /**
     * Update store info.
     */
    suspend fun updateStoreInfo(
        name: String,
        address: String?,
        phone: String?
    ): Result<Unit> {
        return try {
            settingsDao.updateStoreInfo(name, address, phone)
            syncSettingsToRemote()
            Timber.d("Store info updated: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update store info")
            Result.failure(e)
        }
    }

    /**
     * Update default paper width.
     */
    suspend fun updateDefaultPaperWidth(width: Int): Result<Unit> {
        return try {
            settingsDao.updateDefaultPaperWidth(width)
            syncSettingsToRemote()
            Timber.d("Paper width updated: $width mm")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update paper width")
            Result.failure(e)
        }
    }

    /**
     * Update receipt settings.
     */
    suspend fun updateReceiptSettings(footer: String?, showLogo: Boolean): Result<Unit> {
        return try {
            settingsDao.updateReceiptSettings(footer, showLogo)
            syncSettingsToRemote()
            Timber.d("Receipt settings updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update receipt settings")
            Result.failure(e)
        }
    }

    // ==================== SYNC ====================

    /**
     * Get default tax percentage.
     */
    suspend fun getDefaultTaxPercentage(): Double {
        return getSettings()?.defaultTaxPercentage ?: 0.0
    }

    /**
     * Update default tax percentage.
     */
    suspend fun updateDefaultTaxPercentage(percentage: Double): Result<Unit> {
        return try {
            settingsDao.updateDefaultTaxPercentage(percentage)
            syncSettingsToRemote()
            Timber.d("Tax percentage updated: $percentage%")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update tax percentage")
            Result.failure(e)
        }
    }

    /**
     * Update low performance mode.
     */
    suspend fun updateLowPerformanceMode(enabled: Boolean): Result<Unit> {
        return try {
            settingsDao.updateLowPerformanceMode(enabled)
            syncSettingsToRemote()
            Timber.d("Low performance mode updated: $enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update low performance mode")
            Result.failure(e)
        }
    }

    /**
     * Sync settings from remote to local.
     * Call this on app startup when online.
     */
    suspend fun syncFromRemote(): Result<Unit> {
        return try {
            val remoteSettings = remoteDataSource.fetchSettings(DEFAULT_SETTINGS_ID).getOrNull()
            
            if (remoteSettings != null) {
                val localSettings = settingsDao.getSettings()
                
                // Use more recent version (last-write-wins)
                if (localSettings == null || remoteSettings.updatedAt > localSettings.updatedAt) {
                    settingsDao.upsertSettings(remoteSettings.copy(isSynced = true))
                    Timber.d("Settings synced from remote (remote is newer)")
                } else if (localSettings.updatedAt > remoteSettings.updatedAt && !localSettings.isSynced) {
                    // Local is newer and needs to be pushed
                    syncSettingsToRemote()
                    Timber.d("Settings pushed to remote (local is newer)")
                }
            } else {
                // No remote settings, push local if exists
                val localSettings = settingsDao.getSettings()
                if (localSettings != null && !localSettings.isSynced) {
                    syncSettingsToRemote()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync settings from remote")
            Result.failure(e)
        }
    }

    /**
     * Push local settings to remote.
     */
    private suspend fun syncSettingsToRemote() {
        try {
            val settings = settingsDao.getSettings() ?: return
            remoteDataSource.uploadSettings(settings).onSuccess {
                settingsDao.markAsSynced()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync settings to remote")
        }
    }

    /**
     * Force full sync to remote.
     */
    suspend fun forceSyncToRemote(): Result<Unit> {
        return try {
            val settings = settingsDao.getSettings()
            if (settings != null) {
                remoteDataSource.uploadSettings(settings).onSuccess {
                    settingsDao.markAsSynced()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to force sync settings")
            Result.failure(e)
        }
    }

}
