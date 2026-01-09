package com.zatiaras.pos.feature.inventory.presentation.list

import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product

/**
 * UI State for Inventory List Screen.
 * 
 * Represents all possible states the screen can be in.
 * ViewModel exposes this as StateFlow, UI observes it reactively.
 */
sealed interface InventoryUiState {
    
    /**
     * Initial loading state - fetching data for the first time.
     */
    data object Loading : InventoryUiState
    
    /**
     * Successfully loaded data.
     * Contains products, categories, and filter state.
     */
    data class Success(
        val products: List<Product>,
        val categories: List<Category>,
        val selectedCategoryId: String? = null,
        val searchQuery: String = "",
        val isRefreshing: Boolean = false
    ) : InventoryUiState {
        
        /**
         * Filtered products based on current search and category selection.
         */
        val filteredProducts: List<Product>
            get() {
                var result = products
                
                // Filter by category
                if (selectedCategoryId != null) {
                    result = result.filter { it.category?.id == selectedCategoryId }
                }
                
                // Filter by search query
                if (searchQuery.isNotBlank()) {
                    val query = searchQuery.lowercase()
                    result = result.filter { product ->
                        product.name.lowercase().contains(query) ||
                        product.description?.lowercase()?.contains(query) == true
                    }
                }
                
                return result
            }
        
        val isEmpty: Boolean
            get() = filteredProducts.isEmpty()
    }
    
    /**
     * Error state - failed to load data.
     */
    data class Error(val message: String) : InventoryUiState
}

/**
 * Events from InventoryScreen to ViewModel.
 * Using sealed class for type-safe event handling.
 */
sealed interface InventoryEvent {
    data object Refresh : InventoryEvent
    data class Search(val query: String) : InventoryEvent
    data class SelectCategory(val categoryId: String?) : InventoryEvent
    data class DeleteProduct(val productId: String) : InventoryEvent
}
