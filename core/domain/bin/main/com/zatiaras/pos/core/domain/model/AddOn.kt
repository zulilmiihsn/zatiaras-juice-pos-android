package com.zatiaras.pos.core.domain.model

/**
 * Domain model for Add-On / Ekstra / Topping.
 * 
 * Add-ons are extra items that can be added to products during POS checkout.
 * Each product can have a configured set of available add-ons (via ekstra_ids).
 * 
 * Maps to Supabase table: "tambahan"
 */
data class AddOn(
    val id: String,
    val name: String,
    val price: Long, // Price in IDR (no decimals)
    val isActive: Boolean = true
) {

}
