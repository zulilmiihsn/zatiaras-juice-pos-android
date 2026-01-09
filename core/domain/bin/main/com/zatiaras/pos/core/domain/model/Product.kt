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
    val imageUrl: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Formatted price for display (e.g., "Rp15.000")
     */
    val formattedPrice: String
        get() = "Rp${String.format("%,d", price).replace(',', '.')}"
}
