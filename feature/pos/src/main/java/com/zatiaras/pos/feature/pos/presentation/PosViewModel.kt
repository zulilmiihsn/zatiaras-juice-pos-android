package com.zatiaras.pos.feature.pos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.ProductType
import com.zatiaras.pos.core.domain.model.SugarLevel
import com.zatiaras.pos.core.domain.repository.AddOnRepository
import com.zatiaras.pos.core.domain.repository.ProductRepository
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.CartItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main POS screen.
 * 
 * Manages:
 * - Product catalog display (with pagination)
 * - Category filtering
 * - Search functionality
 * - Shopping cart state
 * - Product options (add-ons, sugar/ice levels)
 * 
 * The cart is stored in-memory only (not persisted).
 * This is intentional POS behavior - carts are session-based.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val storeSessionRepository: StoreSessionRepository,
    private val addOnRepository: AddOnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    // Separate flows for category and search query to trigger pagination refresh
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    /**
     * Paginated products flow.
     * Automatically refreshes when category or search query changes.
     * Using cachedIn(viewModelScope) to cache data across configuration changes.
     */
    val pagedProducts: Flow<PagingData<Product>> = combine(
        _selectedCategoryId,
        _searchQuery
    ) { categoryId, query ->
        Pair(categoryId, query)
    }.flatMapLatest { (categoryId, query) ->
        when {
            query.isNotBlank() -> productRepository.searchProductsPaged(query)
            categoryId != null -> productRepository.getProductsByCategoryPaged(categoryId)
            else -> productRepository.getProductsPaged()
        }
    }.cachedIn(viewModelScope)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load categories (not paginated - typically small list)
            productRepository.getCategories()
                .catch { e ->
                    Timber.e(e, "Error loading categories")
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Gagal memuat kategori"
                    )
                }
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(
                        categories = categories,
                        isLoading = false
                    )
                }
        }

        // Load product count for UI display
        viewModelScope.launch {
            productRepository.getProductCount()
                .catch { e ->
                    Timber.e(e, "Error loading product count")
                }
                .collect { count ->
                    _uiState.value = _uiState.value.copy(productCount = count)
                }
        }
        
        // Observe Store Session
        viewModelScope.launch {
            storeSessionRepository.getActiveSession().collectLatest { session ->
                _uiState.value = _uiState.value.copy(isStoreOpen = session != null)
            }
        }
    }

    fun onEvent(event: PosEvent) {
        when (event) {
            is PosEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query
                _uiState.value = _uiState.value.copy(searchQuery = event.query)
            }
            
            is PosEvent.CategorySelected -> {
                _selectedCategoryId.value = event.categoryId
                _uiState.value = _uiState.value.copy(selectedCategoryId = event.categoryId)
            }
            
            is PosEvent.AddToCart -> {
                // Always show product options sheet (matching web app behavior)
                // The sheet content adapts based on product type:
                // - Sugar/ice options shown only for MINUMAN
                // - Add-ons shown only if available
                // - Quantity and notes always available
                showProductOptionsSheet(event.product)
            }
            
            // Product Options Dialog Events
            is PosEvent.ShowProductOptions -> showProductOptionsSheet(event.product)
            is PosEvent.HideProductOptions -> hideProductOptionsSheet()
            is PosEvent.ToggleAddOn -> toggleAddOn(event.addOnId)
            is PosEvent.SetSugarLevel -> setSugarLevel(event.level)
            is PosEvent.SetIceLevel -> setIceLevel(event.level)
            is PosEvent.SetProductNote -> setProductNote(event.note)
            is PosEvent.SetProductQuantity -> setProductQuantity(event.quantity)
            is PosEvent.ConfirmAddToCart -> confirmAddToCart()
            
            // Cart Operations (using uniqueKey)
            is PosEvent.IncrementItem -> incrementItem(event.uniqueKey)
            is PosEvent.DecrementItem -> decrementItem(event.uniqueKey)
            is PosEvent.RemoveFromCart -> removeFromCart(event.uniqueKey)
            is PosEvent.UpdateItemQuantity -> updateQuantity(event.uniqueKey, event.quantity)
            is PosEvent.ClearCart -> clearCart()
            
            is PosEvent.DismissError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
            
            is PosEvent.ToggleViewMode -> {
                _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
            }
            
            is PosEvent.AddCustomItem -> {
                val customProduct = Product(
                    id = "custom_${java.util.UUID.randomUUID()}",
                    name = event.name,
                    price = event.price,
                    category = null,
                    type = ProductType.MAKANAN,
                    imageUrl = null,
                    description = "Custom Item",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isActive = true
                )
                addToCartSimple(customProduct, event.quantity)
            }

            // Navigation events are handled by the UI layer
            is PosEvent.ProceedToCheckout,
            is PosEvent.BackToCatalog -> {
                // No-op in ViewModel, handled by navigation
            }
        }
    }

    // ==================== Product Options Dialog ====================
    
    private fun showProductOptionsSheet(product: Product) {
        viewModelScope.launch {
            Timber.d("Opening product options for: ${product.name}, type=${product.type}, ekstraIds=${product.ekstraIds}")
            
            // Load available add-ons for this product
            val addOns = if (product.ekstraIds.isNotEmpty()) {
                addOnRepository.getAddOnsByIds(product.ekstraIds)
            } else {
                emptyList()
            }
            
            Timber.d("Loaded ${addOns.size} add-ons for ${product.name}. supportsSugarIce=${product.supportsSugarIce}")
            
            _uiState.value = _uiState.value.copy(
                showProductOptionsSheet = true,
                selectedProduct = product,
                availableAddOns = addOns,
                selectedAddOnIds = emptySet(),
                selectedSugarLevel = SugarLevel.NORMAL,
                selectedIceLevel = IceLevel.NORMAL,
                productNote = "",
                productQuantity = 1
            )
        }
    }
    
    private fun hideProductOptionsSheet() {
        _uiState.value = _uiState.value.copy(
            showProductOptionsSheet = false,
            selectedProduct = null,
            availableAddOns = emptyList(),
            selectedAddOnIds = emptySet(),
            selectedSugarLevel = SugarLevel.NORMAL,
            selectedIceLevel = IceLevel.NORMAL,
            productNote = "",
            productQuantity = 1
        )
    }
    
    private fun toggleAddOn(addOnId: String) {
        val current = _uiState.value.selectedAddOnIds
        val updated = if (current.contains(addOnId)) {
            current - addOnId
        } else {
            current + addOnId
        }
        _uiState.value = _uiState.value.copy(selectedAddOnIds = updated)
    }
    
    private fun setSugarLevel(level: SugarLevel) {
        _uiState.value = _uiState.value.copy(selectedSugarLevel = level)
    }
    
    private fun setIceLevel(level: IceLevel) {
        _uiState.value = _uiState.value.copy(selectedIceLevel = level)
    }
    
    private fun setProductNote(note: String) {
        _uiState.value = _uiState.value.copy(productNote = note)
    }
    
    private fun setProductQuantity(quantity: Int) {
        if (quantity >= 1) {
            _uiState.value = _uiState.value.copy(productQuantity = quantity)
        }
    }
    
    private fun confirmAddToCart() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        
        // Get selected add-ons from available list
        val selectedAddOns = state.availableAddOns.filter { 
            state.selectedAddOnIds.contains(it.id) 
        }
        
        // Create cart item with customizations
        val cartItem = CartItem(
            product = product,
            quantity = state.productQuantity,
            addOns = selectedAddOns,
            sugarLevel = state.selectedSugarLevel,
            iceLevel = state.selectedIceLevel,
            notes = state.productNote
        )
        
        // Add to cart
        val updatedCart = state.cart.addItem(cartItem)
        _uiState.value = state.copy(cart = updatedCart)
        
        Timber.d("Added ${product.name} to cart with ${selectedAddOns.size} add-ons. Total items: ${updatedCart.itemCount}")
        
        // Close the dialog
        hideProductOptionsSheet()
    }

    // ==================== Cart Operations ====================

    private fun addToCartSimple(product: Product, quantity: Int = 1) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.addItem(
            product = product,
            quantity = quantity
        )
        _uiState.value = _uiState.value.copy(cart = updatedCart)
        Timber.d("Added ${product.name} to cart. Total items: ${updatedCart.itemCount}")
    }

    private fun incrementItem(uniqueKey: String) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.incrementItem(uniqueKey)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun decrementItem(uniqueKey: String) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.decrementItem(uniqueKey)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun removeFromCart(uniqueKey: String) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.removeItem(uniqueKey)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun updateQuantity(uniqueKey: String, quantity: Int) {
        val currentCart = _uiState.value.cart
        val updatedCart = currentCart.updateQuantity(uniqueKey, quantity)
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    private fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = Cart())
        Timber.d("Cart cleared")
    }

    /**
     * Get the current cart for checkout process.
     */
    fun getCurrentCart(): Cart = _uiState.value.cart
}
