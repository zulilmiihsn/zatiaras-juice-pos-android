package com.zatiaras.pos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity for Add-Ons/Toppings.
 * Maps to Supabase table: "tambahan"
 * 
 * Add-ons are extra items that can be added to products during POS checkout.
 * Examples: Extra cheese, ice, sugar level adjustments, toppings, etc.
 */
@Entity(
    tableName = "add_ons",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isActive"]),
        Index(value = ["isSynced"])
    ]
)
data class AddOnEntity(
    @PrimaryKey
    val id: String,
    
    // Basic info
    val name: String,
    val price: Long, // Price in IDR (no decimals)
    
    // Categorization (optional)
    val category: String? = null, // e.g., "Topping", "Size", "Sugar Level"
    
    // Display
    val sortOrder: Int = 0,
    val icon: String? = null, // Emoji or icon identifier
    
    // Status
    val isActive: Boolean = true,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Sync tracking
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

