package com.zatiaras.pos.core.data.local.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zatiaras.pos.core.data.local.entity.ProductEntity

interface ProductWriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query(
        """
        UPDATE products 
        SET isActive = 0, updatedAt = :timestamp, isSynced = 0 
        WHERE id = :id
    """,
    )
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE products 
        SET categoryId = :categoryId, updatedAt = :timestamp, isSynced = 0 
        WHERE id IN (:productIds)
    """,
    )
    suspend fun setCategoryForProducts(
        categoryId: String,
        productIds: List<String>,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE products 
        SET categoryId = NULL, updatedAt = :timestamp, isSynced = 0 
        WHERE categoryId = :categoryId AND id NOT IN (:keepProductIds)
    """,
    )
    suspend fun clearCategoryExcept(
        categoryId: String,
        keepProductIds: List<String>,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE products 
        SET categoryId = NULL, updatedAt = :timestamp, isSynced = 0 
        WHERE categoryId = :categoryId
    """,
    )
    suspend fun clearCategoryFromProducts(
        categoryId: String,
        timestamp: Long = System.currentTimeMillis(),
    )
}
