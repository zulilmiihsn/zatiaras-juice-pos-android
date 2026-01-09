package com.zatiaras.pos.core.domain.model

/**
 * Domain model for Category.
 * 
 * Used across layers: presentation, domain, data.
 * Independent of database/API implementation details.
 */
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val sortOrder: Int = 0
)
