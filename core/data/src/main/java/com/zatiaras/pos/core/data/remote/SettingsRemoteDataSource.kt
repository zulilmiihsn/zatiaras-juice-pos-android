package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.AppSettingsEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Settings operations with Supabase.
 * 
 * Handles:
 * - Fetching settings from Supabase
 * - Uploading local settings changes to Supabase
 * - Two-way sync for settings
 * 
 * Supabase table: pengaturan
 */
@Singleton
class SettingsRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_PENGATURAN = "pengaturan"
    }

    // ==================== FETCH FROM REMOTE ====================

    /**
     * Fetch settings from Supabase.
     * Returns null if no settings exist.
     */
    suspend fun fetchSettings(settingsId: String = "default"): Result<AppSettingsEntity?> = 
        withContext(Dispatchers.IO) {
            try {
                val response = postgrest.from(TABLE_PENGATURAN)
                    .select {
                        filter { eq("id", settingsId) }
                    }
                    .decodeList<PengaturanDto>()
                
                val settings = response.firstOrNull()?.toEntity()
                Timber.d("Fetched settings from remote: ${settings?.id}")
                Result.success(settings)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch settings from remote")
                Result.failure(e)
            }
        }

    // ==================== PUSH TO REMOTE ====================

    /**
     * Upload settings to Supabase.
     * Uses upsert for idempotency.
     */
    suspend fun uploadSettings(settings: AppSettingsEntity): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val dto = settings.toDto()
                postgrest.from(TABLE_PENGATURAN).upsert(dto)
                Timber.d("Uploaded settings to remote: ${settings.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload settings to remote")
                Result.failure(e)
            }
        }

    /**
     * Update only owner PIN on remote.
     */
    suspend fun updateOwnerPin(settingsId: String, pinHash: String?): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                postgrest.from(TABLE_PENGATURAN).update(
                    mapOf(
                        "owner_pin" to pinHash,
                        "updated_at" to System.currentTimeMillis()
                    )
                ) {
                    filter { eq("id", settingsId) }
                }
                Timber.d("Updated owner PIN on remote")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update owner PIN on remote")
                Result.failure(e)
            }
        }

    /**
     * Update locked routes on remote.
     */
    suspend fun updateLockedRoutes(settingsId: String, routes: List<String>): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                postgrest.from(TABLE_PENGATURAN).update(
                    mapOf(
                        "locked_routes" to routes,
                        "updated_at" to System.currentTimeMillis()
                    )
                ) {
                    filter { eq("id", settingsId) }
                }
                Timber.d("Updated locked routes on remote: $routes")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update locked routes on remote")
                Result.failure(e)
            }
        }

    /**
     * Update store info on remote.
     */
    suspend fun updateStoreInfo(
        settingsId: String,
        storeName: String,
        storeAddress: String?,
        storePhone: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from(TABLE_PENGATURAN).update(
                mapOf(
                    "store_name" to storeName,
                    "store_address" to storeAddress,
                    "store_phone" to storePhone,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter { eq("id", settingsId) }
            }
            Timber.d("Updated store info on remote: $storeName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update store info on remote")
            Result.failure(e)
        }
    }

    /**
     * Initialize settings on remote if not exists.
     */
    suspend fun initializeSettingsIfNotExists(settingsId: String = "default"): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                // Check if exists
                val existing = fetchSettings(settingsId).getOrNull()
                if (existing == null) {
                    // Create default settings
                    val defaultSettings = AppSettingsEntity(id = settingsId)
                    uploadSettings(defaultSettings)
                    Timber.d("Initialized default settings on remote")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize settings on remote")
                Result.failure(e)
            }
        }
}

// ==================== DTOs ====================

/**
 * DTO for "pengaturan" table in Supabase.
 */
@Serializable
data class PengaturanDto(
    val id: String,
    
    @SerialName("owner_pin")
    val ownerPin: String? = null,
    
    @SerialName("locked_routes")
    val lockedRoutes: List<String>? = null,
    
    @SerialName("store_name")
    val storeName: String = "Zatiaras Juice",
    
    @SerialName("store_address")
    val storeAddress: String? = null,
    
    @SerialName("store_phone")
    val storePhone: String? = null,
    
    @SerialName("default_paper_width")
    val defaultPaperWidth: Int = 58,
    
    @SerialName("receipt_footer")
    val receiptFooter: String? = null,
    
    @SerialName("show_logo_on_receipt")
    val showLogoOnReceipt: Boolean = true,
    
    @SerialName("default_tax_percentage")
    val defaultTaxPercentage: Double = 0.5,
    
    @SerialName("updated_at")
    val updatedAt: Long = 0
) {
    fun toEntity(): AppSettingsEntity = AppSettingsEntity(
        id = id,
        ownerPinHash = ownerPin,
        lockedRoutes = lockedRoutes ?: emptyList(),
        storeName = storeName,
        storeAddress = storeAddress,
        storePhone = storePhone,
        defaultPaperWidth = defaultPaperWidth,
        receiptFooter = receiptFooter,
        showLogoOnReceipt = showLogoOnReceipt,
        defaultTaxPercentage = defaultTaxPercentage,
        updatedAt = updatedAt,
        isSynced = true // Came from remote
    )
}

/**
 * Extension function to convert AppSettingsEntity to DTO for upload.
 */
fun AppSettingsEntity.toDto(): PengaturanDto = PengaturanDto(
    id = id,
    ownerPin = ownerPinHash,
    lockedRoutes = lockedRoutes,
    storeName = storeName,
    storeAddress = storeAddress,
    storePhone = storePhone,
    defaultPaperWidth = defaultPaperWidth,
    receiptFooter = receiptFooter,
    showLogoOnReceipt = showLogoOnReceipt,
    defaultTaxPercentage = defaultTaxPercentage,
    updatedAt = updatedAt
)
