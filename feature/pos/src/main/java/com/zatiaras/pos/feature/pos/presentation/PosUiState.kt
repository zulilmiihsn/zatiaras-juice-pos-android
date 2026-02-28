package com.zatiaras.pos.feature.pos.presentation

import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.SugarLevel
import com.zatiaras.pos.feature.pos.domain.model.Cart

/**
 * UI State for the main POS screen (Catalog + Cart).
 * 
 * Note: Products are exposed separately as Flow<PagingData<Product>>
 * for efficient memory management with large catalogs.
 */
data class PosUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val cart: Cart = Cart(),
    val isLoading: Boolean = true,
    val productCount: Int = 0,
    val error: String? = null,
    val isStoreOpen: Boolean = true, // Default to true until session loaded
    val isGridView: Boolean = true,
    
    // Product Options Dialog State
    val showProductOptionsSheet: Boolean = false,
    val selectedProduct: Product? = null,
    val availableAddOns: List<AddOn> = emptyList(),
    val selectedAddOnIds: Set<String> = emptySet(),
    val selectedSugarLevel: SugarLevel = SugarLevel.NORMAL,
    val selectedIceLevel: IceLevel = IceLevel.NORMAL,
    val productNote: String = "",
    val productQuantity: Int = 1
)
