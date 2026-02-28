package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zatiaras.pos.core.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for AppSettings operations.
 * 
 * Settings is a singleton table - only one row exists at "default" ID.
 * All operations target this single row.
 */
@Dao
interface AppSettingsDao {

    // ==================== READ ====================

    /**
     * Get current settings as Flow (reactive).
     */
    @Query("SELECT * FROM app_settings WHERE id = 'default' LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    /**
     * Get current settings (suspend).
     */
    @Query("SELECT * FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    /**
     * Get owner PIN hash.
     */
    @Query("SELECT ownerPinHash FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun getOwnerPinHash(): String?

    /**
     * Get store name.
     */
    @Query("SELECT storeName FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun getStoreName(): String?

    /**
     * Get store address.
     */
    @Query("SELECT storeAddress FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun getStoreAddress(): String?

    // ==================== WRITE ====================

    /**
     * Insert or replace settings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    /**
     * Insert settings (ignore if exists).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(settings: AppSettingsEntity)

    /**
     * Update owner PIN hash.
     */
    @Query("UPDATE app_settings SET ownerPinHash = :pinHash, updatedAt = :updatedAt, isSynced = 0 WHERE id = 'default'")
    suspend fun updateOwnerPin(pinHash: String?, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update locked routes (as comma-separated string).
     */
    @Query("UPDATE app_settings SET lockedRoutes = :routes, updatedAt = :updatedAt, isSynced = 0 WHERE id = 'default'")
    suspend fun updateLockedRoutesRaw(routes: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update store info.
     */
    @Query("""
        UPDATE app_settings 
        SET storeName = :name, 
            storeAddress = :address, 
            storePhone = :phone,
            updatedAt = :updatedAt, 
            isSynced = 0 
        WHERE id = 'default'
    """)
    suspend fun updateStoreInfo(
        name: String, 
        address: String?, 
        phone: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Update default paper width.
     */
    @Query("UPDATE app_settings SET defaultPaperWidth = :width, updatedAt = :updatedAt, isSynced = 0 WHERE id = 'default'")
    suspend fun updateDefaultPaperWidth(width: Int, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update receipt settings.
     */
    @Query("""
        UPDATE app_settings 
        SET receiptFooter = :footer, 
            showLogoOnReceipt = :showLogo,
            updatedAt = :updatedAt, 
            isSynced = 0 
        WHERE id = 'default'
    """)
    suspend fun updateReceiptSettings(
        footer: String?, 
        showLogo: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Update default tax percentage.
     */
    @Query("UPDATE app_settings SET defaultTaxPercentage = :percentage, updatedAt = :updatedAt, isSynced = 0 WHERE id = 'default'")
    suspend fun updateDefaultTaxPercentage(percentage: Double, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update low performance mode.
     */
    @Query("UPDATE app_settings SET lowPerformanceMode = :enabled, updatedAt = :updatedAt, isSynced = 0 WHERE id = 'default'")
    suspend fun updateLowPerformanceMode(enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    // ==================== SYNC ====================

    /**
     * Mark settings as synced.
     */
    @Query("UPDATE app_settings SET isSynced = 1 WHERE id = 'default'")
    suspend fun markAsSynced()

    /**
     * Check if settings need sync.
     */
    @Query("SELECT CASE WHEN isSynced = 0 THEN 1 ELSE 0 END FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun needsSync(): Boolean?

    /**
     * Get settings for sync (unsynced).
     */
    @Query("SELECT * FROM app_settings WHERE isSynced = 0 LIMIT 1")
    suspend fun getUnsyncedSettings(): AppSettingsEntity?

    // ==================== INITIALIZATION ====================

    /**
     * Initialize default settings if not exists.
     * Call this on first app launch.
     */
    @Transaction
    suspend fun initializeDefaultSettings(timestamp: Long = System.currentTimeMillis()) {
        val existing = getSettings()
        if (existing == null) {
            insertIfNotExists(
                AppSettingsEntity(
                    id = "default",
                    storeName = "Zatiaras Juice",
                    defaultPaperWidth = 58,
                    updatedAt = timestamp,
                    isSynced = false
                )
            )
        }
    }
}

