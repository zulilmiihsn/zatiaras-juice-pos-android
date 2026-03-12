package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Add-Ons (Tambahan) operations with Supabase.
 * 
 * Design Principles:
 * - Read DTO with String timestamps
 * - Write DTO without timestamps
 */
@Singleton
class AddOnRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_TAMBAHAN = "tambahan"
    }

    // ==================== PULL ====================

    suspend fun fetchAddOns(lastSyncTimestamp: Long = 0): Result<List<AddOnEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                if (lastSyncTimestamp > 0L) {
                    Timber.d("Delta add-on sync is not implemented yet, running full pull")
                }
                val response = postgrest.from(TABLE_TAMBAHAN)
                    .select()
                    .decodeList<TambahanReadDto>()
                
                val entities = response.map { it.toEntity() }
                Timber.d("Fetched ${entities.size} add-ons from remote")
                Result.success(entities)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch add-ons from remote")
                Result.failure(e)
            }
        }

    // ==================== PUSH ====================

    suspend fun uploadAddOn(addOn: AddOnEntity): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val dto = addOn.toWriteDto()
                postgrest.from(TABLE_TAMBAHAN).upsert(dto)
                Timber.d("Uploaded add-on to remote: ${addOn.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload add-on to remote: ${addOn.id}")
                Result.failure(e)
            }
        }

    suspend fun uploadAddOns(addOns: List<AddOnEntity>): Result<Int> = 
        withContext(Dispatchers.IO) {
            if (addOns.isEmpty()) return@withContext Result.success(0)
            try {
                val dtos = addOns.map { it.toWriteDto() }
                postgrest.from(TABLE_TAMBAHAN).upsert(dtos)
                Timber.d("Uploaded ${dtos.size} add-ons in batch")
                Result.success(dtos.size)
            } catch (e: Exception) {
                Timber.e(e, "Failed to batch upload add-ons")
                Result.failure(e)
            }
        }

    suspend fun deleteAddOn(addOnId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                postgrest.from(TABLE_TAMBAHAN).update(
                    TambahanSoftDeleteDto(isActive = false)
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

@Serializable
data class TambahanReadDto(
    val id: String,
    @SerialName("nama") val name: String,
    @SerialName("harga") val price: Long,
    @SerialName("kategori") val category: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val icon: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: JsonElement? = null,
    @SerialName("updated_at") val updatedAt: JsonElement? = null
) {
    fun toEntity(): AddOnEntity = AddOnEntity(
        id = id,
        name = name,
        price = price,
        category = category,
        sortOrder = sortOrder,
        icon = icon,
        isActive = isActive,
        createdAt = parseJsonTimestamp(createdAt),
        updatedAt = parseJsonTimestamp(updatedAt),
        isSynced = true,
        isDeleted = !isActive
    )
}

@Serializable
data class TambahanWriteDto(
    val id: String,
    @SerialName("nama") val name: String,
    @SerialName("harga") val price: Long,
    @SerialName("kategori") val category: String?,
    @SerialName("sort_order") val sortOrder: Int,
    val icon: String?,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class TambahanSoftDeleteDto(
    @SerialName("is_active") val isActive: Boolean
)

// ==================== MAPPERS ====================

fun AddOnEntity.toWriteDto(): TambahanWriteDto = TambahanWriteDto(
    id = id,
    name = name,
    price = price,
    category = category,
    sortOrder = sortOrder,
    icon = icon,
    isActive = isActive && !isDeleted
)

/**
 * Parse a JsonElement timestamp that may be either:
 * - A string (ISO 8601 format from Supabase)
 * - A number (epoch milliseconds from local writes)
 * Returns 0L on failure.
 */
private fun parseJsonTimestamp(element: JsonElement?): Long {
    if (element == null || element is JsonPrimitive && element.jsonPrimitive.content == "null") return 0L
    return try {
        val primitive = element.jsonPrimitive
        // Try numeric first (epoch millis)
        primitive.longOrNull?.let { return it }
        // Otherwise parse as ISO string
        val isoString = primitive.content
        if (isoString.isBlank()) return 0L
        parseTimestampString(isoString)
    } catch (e: Exception) {
        Timber.w("parseJsonTimestamp failed for: $element")
        0L
    }
}

private fun parseTimestampString(isoString: String): Long {
    if (isoString.isBlank()) return 0L
    return try {
        val cleanStr = isoString.replace(" ", "T")
        // Handle timezone offset like +00:00 using OffsetDateTime
        if (cleanStr.contains("+") || cleanStr.indexOf('-', startIndex = 10) >= 0) {
            java.time.OffsetDateTime.parse(cleanStr).toInstant().toEpochMilli()
        } else {
            val withZ = if (!cleanStr.endsWith("Z")) "${cleanStr}Z" else cleanStr
            java.time.Instant.parse(withZ).toEpochMilli()
        }
    } catch (e: Exception) {
        Timber.w("parseTimestampString failed for: $isoString")
        0L
    }
}
