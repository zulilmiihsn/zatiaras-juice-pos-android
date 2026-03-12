package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Inventory operations with Supabase.
 *
 * Design principles:
 * - Separate Read DTOs (with timestamps) and Write DTOs (without timestamps)
 * - Write DTOs exclude created_at/updated_at — Supabase handles via trigger/default
 * - All operations use @Serializable DTOs (not Map<String, Any?>) for type safety
 * - Soft delete for both products and categories (is_active = false)
 */
@Singleton
class InventoryRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_PRODUK = "produk"
        private const val TABLE_KATEGORI = "kategori"
    }

    // ============== FETCH (PULL) ==============

    /**
     * Fetch all categories from Supabase.
     */
    suspend fun fetchCategories(): Result<List<CategoryEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = postgrest.from(TABLE_KATEGORI)
                .select()
                .decodeList<KategoriReadDto>()

            val entities = response.map { it.toEntity() }
            Timber.d("Fetched ${entities.size} categories from remote")
            Result.success(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch categories")
            Result.failure(e)
        }
    }

    /**
     * Fetch all products from Supabase (full sync).
     * No delta filter to avoid timestamp type mismatch issues.
     */
    suspend fun fetchProducts(lastSyncTimestamp: Long = 0): Result<List<ProductEntity>> = withContext(Dispatchers.IO) {
        try {
            if (lastSyncTimestamp > 0L) {
                Timber.d("Delta product sync is not implemented yet, running full pull")
            }
            val response = postgrest.from(TABLE_PRODUK)
                .select()
                .decodeList<ProdukReadDto>()

            val entities = response.map { it.toEntity() }
            Timber.d("Fetched ${entities.size} products from remote")
            Result.success(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch products")
            Result.failure(e)
        }
    }

    // ============== PUSH ==============

    /**
     * Upsert a single product to Supabase.
     */
    suspend fun upsertProduct(product: ProductEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dto = product.toWriteDto()
            postgrest.from(TABLE_PRODUK).upsert(dto)
            Timber.d("Upserted product: ${product.id} (${product.name})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert product: ${product.id} — ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upsert multiple products to Supabase (batch).
     */
    suspend fun upsertProducts(products: List<ProductEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dtos = products.map { it.toWriteDto() }
            postgrest.from(TABLE_PRODUK).upsert(dtos)
            Timber.d("Upserted ${products.size} products (batch)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert batch products — ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Soft-delete a product on Supabase (set is_active = false).
     */
    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from(TABLE_PRODUK).update(
                ProdukSoftDeleteDto(isActive = false)
            ) {
                filter { eq("id", productId) }
            }
            Timber.d("Soft deleted product on remote: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to soft-delete product on remote: $productId")
            Result.failure(e)
        }
    }

    /**
     * Upsert a single category to Supabase.
     * Also used for soft-delete (isActive = false).
     */
    suspend fun upsertCategory(category: CategoryEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dto = category.toWriteDto()
            postgrest.from(TABLE_KATEGORI).upsert(dto)
            Timber.d("Upserted category: ${category.id} (${category.name}, active=${category.isActive})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert category: ${category.id} — ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upsert multiple categories to Supabase (batch).
     */
    suspend fun upsertCategories(categories: List<CategoryEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dtos = categories.map { it.toWriteDto() }
            postgrest.from(TABLE_KATEGORI).upsert(dtos)
            Timber.d("Upserted ${categories.size} categories (batch)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert batch categories — ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Hard-delete category from Supabase.
     * Kept as fallback but soft-delete via upsertCategory is preferred.
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from(TABLE_KATEGORI).delete {
                filter { eq("id", categoryId) }
            }
            Timber.d("Hard deleted category on remote: $categoryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to hard-delete category on remote: $categoryId")
            Result.failure(e)
        }
    }
}

// =====================================================================
// READ DTOs — For pulling from Supabase (include ALL columns)
// =====================================================================

/**
 * DTO for reading from "kategori" table.
 * is_active is optional with default true for backward compatibility
 * (in case the column hasn't been added to Supabase yet).
 */
@Serializable
data class KategoriReadDto(
    val id: String,
    val nama: String,
    val icon: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toEntity(): CategoryEntity = CategoryEntity(
        id = id,
        name = nama,
        icon = icon,
        sortOrder = sortOrder,
        isActive = isActive,
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt),
        isSynced = true
    )
}

/**
 * DTO for reading from "produk" table.
 */
@Serializable
data class ProdukReadDto(
    val id: String,
    val nama: String,
    val harga: Long,
    @SerialName("kategori_id")
    val kategoriId: String? = null,
    val tipe: String = "makanan",
    @SerialName("ekstra_ids")
    val ekstraIds: List<String>? = null,
    @SerialName("gambar_url")
    val gambarUrl: String? = null,
    val deskripsi: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toEntity(): ProductEntity = ProductEntity(
        id = id,
        name = nama,
        price = harga,
        categoryId = kategoriId,
        type = tipe,
        ekstraIds = ekstraIds?.let { list ->
            if (list.isEmpty()) null else org.json.JSONArray(list).toString()
        },
        imageUrl = gambarUrl,
        description = deskripsi,
        isActive = isActive,
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt),
        isSynced = true
    )
}

// =====================================================================
// WRITE DTOs — For pushing to Supabase (EXCLUDE timestamps)
// Timestamps are handled by Supabase trigger/default.
// =====================================================================

/**
 * DTO for writing to "kategori" table.
 * Includes is_active for soft-delete support.
 * Excludes timestamps.
 */
@Serializable
data class KategoriWriteDto(
    val id: String,
    val nama: String,
    val icon: String?,
    @SerialName("sort_order")
    val sortOrder: Int,
    @SerialName("is_active")
    val isActive: Boolean
)

/**
 * DTO for writing to "produk" table.
 * Includes is_active for soft-delete support.
 * Excludes timestamps.
 */
@Serializable
data class ProdukWriteDto(
    val id: String,
    val nama: String,
    val harga: Long,
    @SerialName("kategori_id")
    val kategoriId: String?,
    val tipe: String,
    @SerialName("ekstra_ids")
    val ekstraIds: List<String>?,
    @SerialName("gambar_url")
    val gambarUrl: String?,
    val deskripsi: String?,
    @SerialName("is_active")
    val isActive: Boolean
)

/**
 * DTO for soft-deleting a product (update is_active only).
 */
@Serializable
data class ProdukSoftDeleteDto(
    @SerialName("is_active")
    val isActive: Boolean
)

// =====================================================================
// Entity → Write DTO conversions
// =====================================================================

/**
 * Convert CategoryEntity to write DTO for push.
 */
fun CategoryEntity.toWriteDto(): KategoriWriteDto = KategoriWriteDto(
    id = id,
    nama = name,
    icon = icon,
    sortOrder = sortOrder,
    isActive = isActive
)

/**
 * Convert ProductEntity to write DTO for push.
 */
fun ProductEntity.toWriteDto(): ProdukWriteDto {
    val ekstraIdsList = ekstraIds?.let { json ->
        try {
            val jsonArray = org.json.JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            null
        }
    }

    return ProdukWriteDto(
        id = id,
        nama = name,
        harga = price,
        kategoriId = categoryId,
        tipe = type,
        ekstraIds = ekstraIdsList,
        gambarUrl = imageUrl,
        deskripsi = description,
        isActive = isActive
    )
}

/**
 * Parse ISO timestamp string to Unix milliseconds.
 * Handles various PostgreSQL formats safely, including:
 * - ISO 8601 with Z suffix (e.g., "2026-02-23T19:42:46.487606Z")
 * - ISO 8601 with timezone offset (e.g., "2026-02-23T19:42:46.487606+00:00")
 * - ISO 8601 without timezone (assumes UTC)
 * Returns 0L on failure (not System.currentTimeMillis()!) to prevent accidental overwrites.
 */
private fun parseTimestamp(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return 0L
    return try {
        val cleanStr = isoString.replace(" ", "T")
        // Try OffsetDateTime first — handles +00:00 offsets and Z suffix
        java.time.OffsetDateTime.parse(cleanStr).toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            // Fallback: append Z if no timezone info and try Instant.parse
            val cleanStr = isoString.replace(" ", "T")
            val withZ = if (!cleanStr.endsWith("Z")) "${cleanStr}Z" else cleanStr
            java.time.Instant.parse(withZ).toEpochMilli()
        } catch (e2: Exception) {
            Timber.w("parseTimestamp failed for: $isoString")
            0L
        }
    }
}
