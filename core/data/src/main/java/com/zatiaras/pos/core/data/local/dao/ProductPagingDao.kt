package com.zatiaras.pos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductPagingDao {
    @Query(
        """
        SELECT * FROM products 
        WHERE isActive = 1 
        ORDER BY createdAt DESC
    """,
    )
    fun getAllActivePaged(): PagingSource<Int, ProductEntity>

    @Query(
        """
        SELECT * FROM products 
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY createdAt DESC
    """,
    )
    fun getByCategoryPaged(categoryId: String): PagingSource<Int, ProductEntity>

    @Query(
        """
        SELECT * FROM products 
        WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """,
    )
    fun searchPaged(query: String): PagingSource<Int, ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    fun getActiveProductCount(): Flow<Int>
}
