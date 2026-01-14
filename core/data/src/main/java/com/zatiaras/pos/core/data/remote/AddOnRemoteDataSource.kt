package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Add-Ons operations with Supabase.
 * 
 * Handles:
 * - Fetching add-ons from Supabase
 * - Uploading local add-on changes to Supabase
 * - Delta sync (only changed items)
 * 
 * Supabase table: tambahan
 */
@Singleton
class AddOnRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_TAMBAHAN = "tambahan"
    }

    // ==================== FETCH FROM REMOTE ====================

    /**
     * Fetch all active add-ons from Supabase.
     */
    suspend fun fetchActiveAddOns(): Result<List<AddOnEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = postgrest.from(TABLE_TAMBAHAN)
                    .select {
                        filter { eq("is_active", true) }
                    }
                    .decodeList<TambahanDto>()
                
                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} active add-ons from remote")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch add-ons from remote")
                Result.failure(e)
            }
        }

    /**
     * Fetch all add-ons from Supabase (including inactive).
     */
    suspend fun fetchAllAddOns(): Result<List<AddOnEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = postgrest.from(TABLE_TAMBAHAN)
                    .select()
                    .decodeList<TambahanDto>()
                
                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} add-ons from remote")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch add-ons from remote")
                Result.failure(e)
            }
        }

    /**
     * Fetch add-ons updated after given timestamp (delta sync).
     * 
     * @param lastSyncTimestamp Unix timestamp in milliseconds, or 0 for full sync
     */
    suspend fun fetchAddOns(lastSyncTimestamp: Long = 0): Result<List<AddOnEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = if (lastSyncTimestamp > 0) {
                    postgrest.from(TABLE_TAMBAHAN)
                        .select()
                        .decodeList<TambahanDto>()
                        .filter { it.updatedAt > lastSyncTimestamp }
                } else {
                    postgrest.from(TABLE_TAMBAHAN)
                        .select()
                        .decodeList<TambahanDto>()
                }
                
                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} add-ons from remote (since $lastSyncTimestamp)")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch add-ons from remote")
                Result.failure(e)
            }
        }

    // ==================== PUSH TO REMOTE ====================

    /**
     * Upload a single add-on to Supabase.
     * Uses upsert for idempotency.
     */
    suspend fun uploadAddOn(addOn: AddOnEntity): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val dto = addOn.toDto()
                postgrest.from(TABLE_TAMBAHAN).upsert(dto)
                Timber.d("Uploaded add-on to remote: ${addOn.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload add-on to remote: ${addOn.id}")
                Result.failure(e)
            }
        }

    /**
     * Upload multiple add-ons in batch.
     */
    suspend fun uploadAddOns(addOns: List<AddOnEntity>): Result<Int> = 
        withContext(Dispatchers.IO) {
            if (addOns.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            try {
                val dtos = addOns.map { it.toDto() }
                postgrest.from(TABLE_TAMBAHAN).upsert(dtos)
                Timber.d("Uploaded ${dtos.size} add-ons in batch")
                Result.success(dtos.size)
            } catch (e: Exception) {
                Timber.e(e, "Failed to batch upload add-ons")
                // Fallback to individual uploads
                var successCount = 0
                for (addOn in addOns) {
                    uploadAddOn(addOn).fold(
                        onSuccess = { successCount++ },
                        onFailure = { /* continue */ }
                    )
                }
                if (successCount > 0) {
                    Result.success(successCount)
                } else {
                    Result.failure(e)
                }
            }
        }

    /**
     * Soft delete an add-on on remote.
     */
    suspend fun deleteAddOn(addOnId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                postgrest.from(TABLE_TAMBAHAN).update(
                    mapOf(
                        "is_active" to false,
                        "updated_at" to System.currentTimeMillis()
                    )
                ) {
                    filter { eq("id", addOnId) }
                }
                Timber.d("Soft deleted add-on on remote: $addOnId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete add-on on remote: $addOnId")
                Result.failure(e)
            }
        }
}

// ==================== DTOs ====================

/**
 * DTO for "tambahan" table in Supabase.
 */
@Serializable
data class TambahanDto(
    val id: String,
    @SerialName("nama")
    val name: String,
    @SerialName("harga")
    val price: Long,
    @SerialName("kategori")
    val category: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    val icon: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: Long = 0
) {
    fun toEntity(): AddOnEntity = AddOnEntity(
        id = id,
        name = name,
        price = price,
        category = category,
        sortOrder = sortOrder,
        icon = icon,
        isActive = isActive,
        createdAt = parseTimestamp(createdAt),
        updatedAt = updatedAt,
        isSynced = true, // Came from remote
        isDeleted = !isActive
    )
}

/**
 * Extension function to convert AddOnEntity to DTO for upload.
 */
fun AddOnEntity.toDto(): TambahanDto = TambahanDto(
    id = id,
    name = name,
    price = price,
    category = category,
    sortOrder = sortOrder,
    icon = icon,
    isActive = isActive && !isDeleted,
    createdAt = null, // Let Supabase handle
    updatedAt = updatedAt
)

/**
 * Parse ISO timestamp string to Unix milliseconds.
 */
private fun parseTimestamp(isoString: String?): Long {
    if (isoString == null) return System.currentTimeMillis()
    return try {
        java.time.Instant.parse(isoString).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
