package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity for Products.
 * Maps to Supabase table: "produk"
 * 
 * Foreign key to CategoryEntity with SET_NULL on delete.
 * Index on categoryId for faster queries.
 * 
 * New fields:
 * - type: Product type ("minuman", "makanan", "snack")
 * - ekstraIds: JSON array of add-on IDs as string
 */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["type"])]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val price: Long,                    // IDR, no decimals
    val categoryId: String? = null,
    val type: String = "makanan",       // "minuman", "makanan", "snack"
    val ekstraIds: String? = null,      // JSON array as string: ["id1", "id2"]
    val imageUrl: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
