package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 Virtual Table for full-text search on products.
 * Linked to ProductEntity via contentEntity.
 * 
 * Enables fast, typo-tolerant search on name and description.
 */
@Fts4(contentEntity = ProductEntity::class)
@Entity(tableName = "products_fts")
data class ProductFtsEntity(
    val name: String,
    val description: String?
)
