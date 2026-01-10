package com.zatiaras.pos.feature.pos.presentation

import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.pos.domain.model.Cart

/**
 * UI State for the main POS screen (Catalog + Cart).
 */
data class PosUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val cart: Cart = Cart(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /**
     * Filtered products based on search query and selected category.
     */
    val filteredProducts: List<Product>
        get() {
            var result = products
            
            // Filter by category if selected
            if (selectedCategoryId != null) {
                result = result.filter { it.category?.id == selectedCategoryId }
            }
            
            // Filter by search query
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase()
                result = result.filter { 
                    it.name.lowercase().contains(query) 
                }
            }
            
            return result
        }
    
    /**
     * Show empty state when no products match filters.
     */
    val showEmptyState: Boolean
        get() = !isLoading && filteredProducts.isEmpty()
}
