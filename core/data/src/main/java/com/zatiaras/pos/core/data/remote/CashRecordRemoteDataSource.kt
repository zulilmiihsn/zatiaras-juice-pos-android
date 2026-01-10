package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.CashRecordEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Cash Record (Buku Kas) operations with Supabase.
 * 
 * Handles:
 * - Uploading cash records to Supabase
 * - Fetching cash records for sync
 * - Delta sync (only changed items)
 * 
 * Supabase table: buku_kas
 */
@Singleton
class CashRecordRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_BUKU_KAS = "buku_kas"
    }

    // ==================== UPLOAD TO REMOTE ====================

    /**
     * Upload a cash record to Supabase.
     * Uses upsert for idempotency (safe to retry on failure).
     */
    suspend fun uploadCashRecord(record: CashRecordEntity): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val dto = record.toDto()
                postgrest.from(TABLE_BUKU_KAS).upsert(dto)
                Timber.d("Uploaded cash record: ${record.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload cash record: ${record.id}")
                Result.failure(e)
            }
        }

    /**
     * Upload multiple cash records in batch.
     */
    suspend fun uploadCashRecords(records: List<CashRecordEntity>): Result<Int> = 
        withContext(Dispatchers.IO) {
            if (records.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            try {
                val dtos = records.map { it.toDto() }
                postgrest.from(TABLE_BUKU_KAS).upsert(dtos)
                Timber.d("Uploaded ${dtos.size} cash records in batch")
                Result.success(dtos.size)
            } catch (e: Exception) {
                Timber.e(e, "Failed to batch upload cash records")
                // Fallback to individual uploads
                var successCount = 0
                for (record in records) {
                    uploadCashRecord(record).fold(
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

    // ==================== FETCH FROM REMOTE ====================

    /**
     * Fetch cash records updated after given timestamp.
     * Used for pulling remote changes to local.
     */
    suspend fun fetchCashRecords(lastSyncTimestamp: Long = 0): Result<List<CashRecordEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val response = if (lastSyncTimestamp > 0) {
                    postgrest.from(TABLE_BUKU_KAS)
                        .select()
                        .decodeList<BukuKasDto>()
                        .filter { it.updatedAt > lastSyncTimestamp }
                } else {
                    postgrest.from(TABLE_BUKU_KAS)
                        .select()
                        .decodeList<BukuKasDto>()
                }

                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} cash records from remote (since $lastSyncTimestamp)")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch cash records")
                Result.failure(e)
            }
        }

    /**
     * Delete a cash record from Supabase (soft delete).
     */
    suspend fun deleteCashRecord(recordId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                postgrest.from(TABLE_BUKU_KAS).update(
                    mapOf("is_deleted" to true)
                ) {
                    filter { eq("id", recordId) }
                }
                Timber.d("Soft deleted cash record on remote: $recordId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete cash record on remote: $recordId")
                Result.failure(e)
            }
        }
}

// ==================== DTOs ====================

/**
 * DTO for "buku_kas" table in Supabase.
 */
@Serializable
data class BukuKasDto(
    val id: String,
    val type: String,                     // INCOME or EXPENSE
    val amount: Long,
    val description: String,
    val category: String? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("updated_at")
    val updatedAt: Long = 0,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
) {
    fun toEntity(): CashRecordEntity = CashRecordEntity(
        id = id,
        type = type,
        amount = amount,
        description = description,
        category = category,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSynced = true, // Came from remote, so it's synced
        isDeleted = isDeleted
    )
}

// ==================== Extension Functions ====================

/**
 * Convert CashRecordEntity to DTO for upload.
 */
fun CashRecordEntity.toDto(): BukuKasDto = BukuKasDto(
    id = id,
    type = type,
    amount = amount,
    description = description,
    category = category,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)
