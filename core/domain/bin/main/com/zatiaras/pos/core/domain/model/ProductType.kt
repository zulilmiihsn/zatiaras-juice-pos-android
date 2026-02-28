package com.zatiaras.pos.core.domain.model

/**
 * Product type classification.
 * 
 * Determines which customization options are available:
 * - MINUMAN (beverage): Shows sugar & ice level options
 * - MAKANAN (food): No sugar/ice options
 * 
 * Maps to Supabase column: "tipe" with values "minuman", "makanan"
 */
enum class ProductType(val value: String, val displayName: String) {
    MINUMAN("minuman", "Minuman"),
    MAKANAN("makanan", "Makanan");
    
    companion object {
        /**
         * Parse product type from string value.
         * Defaults to MAKANAN if value is null or unrecognized.
         */
        fun fromValue(value: String?): ProductType {
            return entries.find { it.value == value?.lowercase() } ?: MAKANAN
        }
    }
    
    /**
     * Returns true if this product type supports sugar & ice customization.
     * Only beverages (MINUMAN) support this.
     */
    val supportsSugarIce: Boolean
        get() = this == MINUMAN
}
