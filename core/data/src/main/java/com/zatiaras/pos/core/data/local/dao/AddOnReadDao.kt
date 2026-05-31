package com.zatiaras.pos.core.data.local.dao

import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import kotlinx.coroutines.flow.Flow

interface AddOnReadDao {
    @Query("SELECT * FROM add_ons WHERE isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    fun observeActiveAddOns(): Flow<List<AddOnEntity>>

    @Query("SELECT * FROM add_ons WHERE isDeleted = 0 ORDER BY sortOrder, name")
    fun observeAllAddOns(): Flow<List<AddOnEntity>>

    @Query("SELECT * FROM add_ons WHERE isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    suspend fun getActiveAddOns(): List<AddOnEntity>

    @Query("SELECT * FROM add_ons WHERE category = :category AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    fun observeAddOnsByCategory(category: String): Flow<List<AddOnEntity>>

    @Query("SELECT * FROM add_ons WHERE id = :id LIMIT 1")
    suspend fun getAddOnById(id: String): AddOnEntity?

    @Query("SELECT * FROM add_ons WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): AddOnEntity?

    @Query("SELECT * FROM add_ons WHERE id IN (:ids) AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    suspend fun getAddOnsByIds(ids: List<String>): List<AddOnEntity>

    @Query("SELECT * FROM add_ons WHERE id IN (:ids) AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder, name")
    fun observeAddOnsByIds(ids: List<String>): Flow<List<AddOnEntity>>

    @Query("SELECT DISTINCT category FROM add_ons WHERE category IS NOT NULL AND isActive = 1 AND isDeleted = 0")
    suspend fun getCategories(): List<String>

    @Query("SELECT * FROM add_ons WHERE name LIKE '%' || :query || '%' AND isActive = 1 AND isDeleted = 0 ORDER BY name")
    suspend fun searchAddOns(query: String): List<AddOnEntity>
}
