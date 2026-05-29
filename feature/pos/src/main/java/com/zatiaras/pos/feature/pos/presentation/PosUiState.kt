package com.zatiaras.pos.feature.pos.presentation

import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.SugarLevel
import com.zatiaras.pos.feature.pos.domain.model.Cart

/**
 * Render state for the main POS screen.
 *
 * Products are exposed as Flow<PagingData<Product>> outside this state to keep
 * large catalogs paged while cart and customization drafts remain small.
 */
data class PosUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val cart: Cart = Cart(),
    val isLoading: Boolean = true,
    val productCount: Int = 0,
    val error: String? = null,
    val isStoreOpen: Boolean = true, // Optimistic until the store-session flow emits.
    val isGridView: Boolean = true,

    // Product customization draft state.
    val showProductOptionsSheet: Boolean = false,
    val selectedProduct: Product? = null,
    val availableAddOns: List<AddOn> = emptyList(),
    val selectedAddOnIds: Set<String> = emptySet(),
    val selectedSugarLevel: SugarLevel = SugarLevel.NORMAL,
    val selectedIceLevel: IceLevel = IceLevel.NORMAL,
    val productNote: String = "",
    val productQuantity: Int = 1,
)
