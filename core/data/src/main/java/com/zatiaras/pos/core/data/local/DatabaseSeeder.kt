package com.zatiaras.pos.core.data.local

import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database Seeder for development and testing.
 * 
 * Populates database with sample data if empty.
 * Should only be used in development builds.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao
) {

    /**
     * Seed database with sample data if tables are empty.
     * Call this from Application.onCreate() or ViewModel init.
     */
    suspend fun seedIfEmpty() {
        try {
            val existingCategories = categoryDao.getAll().first()
            
            if (existingCategories.isEmpty()) {
                Timber.d("Database is empty, seeding with sample data...")
                seedCategories()
                seedProducts()
                Timber.d("Database seeding completed!")
            } else {
                Timber.d("Database already has data, skipping seed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to seed database")
        }
    }

    private suspend fun seedCategories() {
        val categories = listOf(
            CategoryEntity(
                id = "cat-kopi",
                name = "Kopi",
                icon = "coffee",
                sortOrder = 1,
                isSynced = true
            ),
            CategoryEntity(
                id = "cat-teh",
                name = "Teh",
                icon = "tea",
                sortOrder = 2,
                isSynced = true
            ),
            CategoryEntity(
                id = "cat-snack",
                name = "Snack",
                icon = "cookie",
                sortOrder = 3,
                isSynced = true
            ),
            CategoryEntity(
                id = "cat-minuman",
                name = "Minuman Lain",
                icon = "drink",
                sortOrder = 4,
                isSynced = true
            )
        )
        
        categoryDao.insertAll(categories)
        Timber.d("Seeded ${categories.size} categories")
    }

    private suspend fun seedProducts() {
        val products = listOf(
            // Kopi
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Es Kopi Susu",
                price = 18000,
                categoryId = "cat-kopi",
                description = "Kopi espresso dengan susu segar dan gula aren",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Kopi Hitam",
                price = 12000,
                categoryId = "cat-kopi",
                description = "Kopi hitam robusta tanpa gula",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Cappuccino",
                price = 22000,
                categoryId = "cat-kopi",
                description = "Espresso dengan susu foam lembut",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Kopi Gula Aren",
                price = 20000,
                categoryId = "cat-kopi",
                description = "Kopi susu dengan gula aren asli",
                isSynced = true
            ),
            
            // Teh
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Teh Manis",
                price = 8000,
                categoryId = "cat-teh",
                description = "Teh hangat dengan gula",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Es Teh Manis",
                price = 10000,
                categoryId = "cat-teh",
                description = "Teh dingin dengan gula",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Teh Tarik",
                price = 15000,
                categoryId = "cat-teh",
                description = "Teh susu khas Malaysia",
                isSynced = true
            ),
            
            // Snack
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Roti Bakar Coklat",
                price = 12000,
                categoryId = "cat-snack",
                description = "Roti panggang dengan selai coklat",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Kentang Goreng",
                price = 15000,
                categoryId = "cat-snack",
                description = "Kentang goreng crispy dengan saus",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Pisang Goreng",
                price = 10000,
                categoryId = "cat-snack",
                description = "Pisang kepok goreng crispy",
                isSynced = true
            ),
            
            // Minuman Lain
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Jus Jeruk",
                price = 15000,
                categoryId = "cat-minuman",
                description = "Jus jeruk segar tanpa gula",
                isSynced = true
            ),
            ProductEntity(
                id = UUID.randomUUID().toString(),
                name = "Air Mineral",
                price = 5000,
                categoryId = "cat-minuman",
                description = "Air mineral kemasan",
                isSynced = true
            )
        )
        
        productDao.insertAll(products)
        Timber.d("Seeded ${products.size} products")
    }
}
