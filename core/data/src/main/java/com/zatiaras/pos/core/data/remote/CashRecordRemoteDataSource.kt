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
 * Design Principles:
 * - Read DTO for Pulling (String timestamps)
 * - Write DTO for Pushing (No timestamps)
 */
@Singleton
class CashRecordRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_BUKU_KAS = "buku_kas"
    }

    // ==================== PUSH ====================

    suspend fun uploadCashRecord(record: CashRecordEntity): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val dto = record.toWriteDto()
                postgrest.from(TABLE_BUKU_KAS).upsert(dto)
                Timber.d("Uploaded cash record: ${record.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload cash record: ${record.id}")
                Result.failure(e)
            }
        }

    suspend fun uploadCashRecords(records: List<CashRecordEntity>): Result<Int> = 
        withContext(Dispatchers.IO) {
            if (records.isEmpty()) return@withContext Result.success(0)
            try {
                val dtos = records.map { it.toWriteDto() }
                postgrest.from(TABLE_BUKU_KAS).upsert(dtos)
                Timber.d("Uploaded ${dtos.size} cash records in batch")
                Result.success(dtos.size)
            } catch (e: Exception) {
                Timber.e(e, "Failed to batch upload cash records")
                Result.failure(e)
            }
        }

    // ==================== PULL ====================

    suspend fun fetchCashRecords(lastSyncTimestamp: Long = 0): Result<List<CashRecordEntity>> =
        withContext(Dispatchers.IO) {
            try {
                if (lastSyncTimestamp > 0L) {
                    Timber.d("Delta cash record sync is not implemented yet, running full pull")
                }
                // Fetch all to avoid complex timestamp filters for now (same as Inventory)
                val response = postgrest.from(TABLE_BUKU_KAS)
                    .select()
                    .decodeList<BukuKasReadDto>()

                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} cash records from remote")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch cash records")
                Result.failure(e)
            }
        }

    suspend fun deleteCashRecord(recordId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                // Use a concrete DTO for update, not Map
                postgrest.from(TABLE_BUKU_KAS).update(
                    BukuKasSoftDeleteDto(isDeleted = true)
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

@Serializable
data class BukuKasReadDto(
    val id: String,
    val type: String,
    val amount: Long,
    val description: String,
    val category: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
) {
    fun toEntity(): CashRecordEntity = CashRecordEntity(
        id = id,
        type = type,
        amount = amount,
        description = description,
        category = category,
        notes = notes,
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt),
        isSynced = true,
        isDeleted = isDeleted
    )
}

@Serializable
data class BukuKasWriteDto(
    val id: String,
    val type: String,
    val amount: Long,
    val description: String,
    val category: String?,
    val notes: String?,
    @SerialName("is_deleted") val isDeleted: Boolean
)

@Serializable
data class BukuKasSoftDeleteDto(
    @SerialName("is_deleted") val isDeleted: Boolean
)

// ==================== MAPPERS ====================

fun CashRecordEntity.toWriteDto(): BukuKasWriteDto = BukuKasWriteDto(
    id = id,
    type = type,
    amount = amount,
    description = description,
    category = category,
    notes = notes,
    isDeleted = isDeleted
)

private fun parseTimestamp(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return 0L
    return try {
        val cleanStr = isoString.replace(" ", "T")
        val withZ = if (!cleanStr.contains("Z") && !cleanStr.contains("+")) "${cleanStr}Z" else cleanStr
        java.time.Instant.parse(withZ).toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}
