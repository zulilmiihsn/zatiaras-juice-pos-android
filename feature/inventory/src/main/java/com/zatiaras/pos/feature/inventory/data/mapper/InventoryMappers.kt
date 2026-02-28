package com.zatiaras.pos.feature.inventory.data.mapper

import com.zatiaras.pos.core.data.local.entity.AddOnEntity
import com.zatiaras.pos.core.data.local.entity.CategoryEntity
import com.zatiaras.pos.core.data.local.entity.ProductEntity
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.ProductType
import org.json.JSONArray
import org.json.JSONException

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

/**
 * Parse ekstraIds JSON string to List<String>
 */
private fun parseEkstraIds(jsonString: String?): List<String> {
    if (jsonString.isNullOrBlank()) return emptyList()
    
    return try {
        val jsonArray = JSONArray(jsonString)
        (0 until jsonArray.length()).map { jsonArray.getString(it) }
    } catch (e: JSONException) {
        emptyList()
    }
}

/**
 * Convert List<String> to JSON string for storage
 */
private fun ekstraIdsToJson(ekstraIds: List<String>): String? {
    if (ekstraIds.isEmpty()) return null
    
    val jsonArray = JSONArray()
    ekstraIds.forEach { jsonArray.put(it) }
    return jsonArray.toString()
}

fun ProductEntity.toDomain(category: Category? = null): Product {
    return Product(
        id = id,
        name = name,
        price = price,
        category = category,
        type = ProductType.fromValue(type),
        ekstraIds = parseEkstraIds(ekstraIds),
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
        type = type.value,
        ekstraIds = ekstraIdsToJson(ekstraIds),
        imageUrl = imageUrl,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        isSynced = isSynced
    )
}

// ==================== ADD-ON ====================

fun AddOnEntity.toDomain(): AddOn {
    return AddOn(
        id = id,
        name = name,
        price = price,
        isActive = isActive
    )
}

fun AddOn.toEntity(isSynced: Boolean = false): AddOnEntity {
    return AddOnEntity(
        id = id,
        name = name,
        price = price,
        isActive = isActive,
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

fun List<AddOnEntity>.toAddOnDomainList(): List<AddOn> = map { it.toDomain() }
