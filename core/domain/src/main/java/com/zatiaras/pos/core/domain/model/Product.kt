package com.zatiaras.pos.core.domain.model

/**
 * Domain model for Product.
 * 
 * Used across layers: presentation, domain, data.
 * Independent of database/API implementation details.
 * 
 * Design: price is Long (IDR, no decimals) for precision and simplicity.
 */
data class Product(
    val id: String,
    val name: String,
    val price: Long,
    val category: Category? = null,
    val type: ProductType = ProductType.MAKANAN,
    val ekstraIds: List<String> = emptyList(),
    val imageUrl: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {

    
    /**
     * Returns true if this product is a beverage (minuman) and supports sugar/ice customization.
     */
    val supportsSugarIce: Boolean
        get() = type.supportsSugarIce
    
    /**
     * Returns true if this product has available add-ons.
     */
    val hasAddOns: Boolean
        get() = ekstraIds.isNotEmpty()
}
