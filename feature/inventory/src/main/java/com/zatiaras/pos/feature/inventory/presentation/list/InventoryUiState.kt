package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.annotation.StringRes
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.feature.inventory.R

enum class InventoryTab(@StringRes val titleResId: Int) {
    MENU(R.string.inventory_tab_menu),
    CATEGORIES(R.string.inventory_tab_categories),
    ADD_ONS(R.string.inventory_tab_addons),
}

/**
 * Complete render state for the inventory list screen.
 *
 * Filtering is derived from the loaded data so ViewModel writes stay simple and
 * the screen can switch tabs/categories without mutating product lists.
 */
sealed interface InventoryUiState {

    /**
     * Initial loading state - fetching data for the first time.
     */
    data object Loading : InventoryUiState

    /**
     * Successfully loaded data.
     * Contains products, categories, add-ons, and UI control state.
     */
    data class Success(
        val products: List<Product>,
        val categories: List<Category>,
        val addOns: List<AddOn>,
        val selectedTab: InventoryTab = InventoryTab.MENU,
        val selectedCategoryId: String? = null,
        val searchQuery: String = "",
        val isGridView: Boolean = true,
        val isRefreshing: Boolean = false,
        val snackbarMessage: String? = null,
    ) : InventoryUiState {

        /**
         * Filtered products based on current search and category selection.
         */
        val filteredProducts: List<Product>
            get() {
                var result = products

                // Category narrows first so search runs over the visible tab
                // context operators selected.
                if (selectedCategoryId != null) {
                    result = result.filter { it.category?.id == selectedCategoryId }
                }

                // Search includes description because product names can be short
                // or abbreviated in POS operations.
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
    data class ChangeTab(val tab: InventoryTab) : InventoryEvent

    // Product commands.
    data class SelectCategory(val categoryId: String?) : InventoryEvent
    data class DeleteProduct(val productId: String) : InventoryEvent

    // Category commands.
    data class AddCategory(val name: String, val icon: String? = null) : InventoryEvent
    data class UpdateCategoryWithProducts(
        val categoryId: String,
        val name: String,
        val productIds: List<String>,
    ) : InventoryEvent
    data class DeleteCategory(val categoryId: String) : InventoryEvent

    // Add-on commands.
    data class AddAddOn(val name: String, val price: Long) : InventoryEvent
    data class UpdateAddOn(val addOnId: String, val name: String, val price: Long) : InventoryEvent
    data class DeleteAddOn(val addOnId: String) : InventoryEvent

    // Pure UI events.
    data object ToggleViewMode : InventoryEvent
    data object SnackbarDismissed : InventoryEvent
}
