package com.zatiaras.pos.core.data.local

import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to seed the database with initial data if it's empty.
 * Used for development and testing.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao
) {

    suspend fun seedIfEmpty() {
        val categories = categoryDao.getAll().first()
        if (categories.isEmpty()) {
            seedData()
        }
    }

    private suspend fun seedData() {
        val cat1Id = UUID.randomUUID().toString()
        val cat2Id = UUID.randomUUID().toString()

        val sampleCategories = listOf(
            CategoryEntity(id = cat1Id, name = "Makanan", sortOrder = 1),
            CategoryEntity(id = cat2Id, name = "Minuman", sortOrder = 2)
        )

        val sampleProducts = listOf(
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Nasi Goreng",
                price = 15000,
                categoryId = cat1Id,
                description = "Nasi goreng spesial dengan telur"
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Es Teh Manis",
                price = 5000,
                categoryId = cat2Id,
                description = "Es teh segar"
            )
        )

        categoryDao.insertAll(sampleCategories)
        productDao.insertAll(sampleProducts)
    }
}
