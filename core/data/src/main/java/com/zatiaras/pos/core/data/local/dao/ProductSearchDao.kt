package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductSearchDao {
    /**
     * Requires a Room FTS MATCH-compatible query, for example "kopi*".
     */
    @Query(
        """
        SELECT products.* FROM products
        JOIN products_fts ON products.rowid = products_fts.rowid
        WHERE products_fts MATCH :query AND products.isActive = 1
        ORDER BY products.createdAt DESC
    """,
    )
    fun search(query: String): Flow<List<ProductEntity>>

    /**
     * Accepts raw user input and is used by repository search.
     */
    @Query(
        """
        SELECT * FROM products 
        WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """,
    )
    fun searchSimple(query: String): Flow<List<ProductEntity>>
}
