package com.zatiaras.pos.feature.inventory.data.mapper

import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product

/**
 * Mapper functions to convert between Entity (Room) and Domain Model.
 * 
 * Keeps layers clean and independent.
 */

// ==================== CATEGORY ====================

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        sortOrder = sortOrder
    )
}

fun Category.toEntity(isSynced: Boolean = false): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        sortOrder = sortOrder,
        isSynced = isSynced
    )
}

// ==================== PRODUCT ====================

fun ProductEntity.toDomain(category: Category? = null): Product {
    return Product(
        id = id,
        name = name,
        price = price,
        category = category,
        imageUrl = imageUrl,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Product.toEntity(isSynced: Boolean = false): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        price = price,
        categoryId = category?.id,
        imageUrl = imageUrl,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isSynced = isSynced
    )
}

// ==================== LIST EXTENSIONS ====================

fun List<CategoryEntity>.toDomainList(): List<Category> = map { it.toDomain() }

fun List<ProductEntity>.toDomainList(
    categories: Map<String, Category> = emptyMap()
): List<Product> = map { entity ->
    entity.toDomain(
        category = entity.categoryId?.let { categories[it] }
    )
}
