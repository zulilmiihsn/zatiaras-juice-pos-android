package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
 * Handles:
 * - Fetching products/categories from Supabase
 * - Uploading local changes to Supabase
 * - Delta sync (only changed items)
 */
@Singleton
class InventoryRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    companion object {
        private const val TABLE_PRODUK = "produk"
        private const val TABLE_KATEGORI = "kategori"
    }

    // ============== FETCH FROM REMOTE ==============

    /**
     * Fetch all categories from Supabase.
     */
    suspend fun fetchCategories(): Result<List<CategoryEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = postgrest.from(TABLE_KATEGORI)
                .select()
                .decodeList<KategoriDto>()
            
            val entities = response.map { it.toEntity() }
            Timber.d("Fetched ${entities.size} categories from remote")
            Result.success(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch categories")
            Result.failure(e)
        }
    }

    /**
     * Fetch products updated after given timestamp (delta sync).
     * 
     * @param lastSyncTimestamp Unix timestamp in milliseconds, or 0 for full sync
     */
    suspend fun fetchProducts(lastSyncTimestamp: Long = 0): Result<List<ProductEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = if (lastSyncTimestamp > 0) {
                // Delta sync - only get changed items
                postgrest.from(TABLE_PRODUK)
                    .select()
                    .decodeList<ProdukDto>()
                    .filter { it.updatedAt > lastSyncTimestamp }
            } else {
                // Full sync
                postgrest.from(TABLE_PRODUK)
                    .select()
                    .decodeList<ProdukDto>()
            }
            
            val entities = response.map { it.toEntity() }
            Timber.d("Fetched ${entities.size} products from remote (since $lastSyncTimestamp)")
            Result.success(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch products")
            Result.failure(e)
        }
    }

    // ============== PUSH TO REMOTE ==============

    /**
     * Upsert product to Supabase.
     */
    suspend fun upsertProduct(product: ProductEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dto = product.toDto()
            postgrest.from(TABLE_PRODUK).upsert(dto)
            Timber.d("Upserted product: ${product.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert product: ${product.id}")
            Result.failure(e)
        }
    }

    /**
     * Delete product from Supabase (soft delete by setting is_active = false).
     */
    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from(TABLE_PRODUK).update(
                mapOf("is_active" to false)
            ) {
                filter { eq("id", productId) }
            }
            Timber.d("Soft deleted product on remote: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete product on remote: $productId")
            Result.failure(e)
        }
    }
}

// ============== DTOs ==============

/**
 * DTO for "kategori" table in Supabase.
 */
@Serializable
data class KategoriDto(
    val id: String,
    val nama: String,
    val icon: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
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
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt),
        isSynced = true
    )
}

/**
 * DTO for "produk" table in Supabase.
 */
@Serializable
data class ProdukDto(
    val id: String,
    val nama: String,
    val harga: Long,
    @SerialName("kategori_id")
    val kategoriId: String? = null,
    @SerialName("gambar_url")
    val gambarUrl: String? = null,
    val deskripsi: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: Long = 0
) {
    fun toEntity(): ProductEntity = ProductEntity(
        id = id,
        name = nama,
        price = harga,
        categoryId = kategoriId,
        imageUrl = gambarUrl,
        description = deskripsi,
        isActive = isActive,
        createdAt = parseTimestamp(createdAt),
        updatedAt = updatedAt,
        isSynced = true
    )
}

/**
 * Extension function to convert ProductEntity to DTO for upload.
 */
fun ProductEntity.toDto(): ProdukDto = ProdukDto(
    id = id,
    nama = name,
    harga = price,
    kategoriId = categoryId,
    gambarUrl = imageUrl,
    deskripsi = description,
    isActive = isActive,
    createdAt = null, // Let Supabase handle this
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
