package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductReadDao {
    @Query(
        """
        SELECT * FROM products 
        WHERE isActive = 1 
        ORDER BY createdAt DESC
    """,
    )
    fun getAllActive(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products 
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY createdAt DESC
    """,
    )
    fun getByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE updatedAt > :timestamp")
    suspend fun getUpdatedAfter(timestamp: Long): List<ProductEntity>
}
