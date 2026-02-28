package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.repository.AddOnRepository
import com.zatiaras.pos.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Internal data class for UI control state.
 * Separates UI controls from data for cleaner combine logic.
 */
private data class UiControlState(
    val selectedTab: InventoryTab = InventoryTab.MENU,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val isRefreshing: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * ViewModel for Inventory List Screen.
 * 
 * Responsibilities:
 * - Load and observe products/categories/add-ons from repository
 * - Handle search and category filter state
 * - Handle refresh/sync operations
 * - Manage CRUD operations with user feedback
 * 
 * Follows Single Responsibility: Only manages InventoryScreen state.
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val addOnRepository: AddOnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _uiControlState = MutableStateFlow(UiControlState())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Combine data sources with UI control state
                combine(
                    productRepository.getProducts(),
                    productRepository.getCategories(),
                    addOnRepository.observeActiveAddOns(),
                    _uiControlState
                ) { products, categories, addOns, controlState ->
                    InventoryUiState.Success(
                        products = products,
                        categories = categories,
                        addOns = addOns,
                        selectedTab = controlState.selectedTab,
                        selectedCategoryId = controlState.selectedCategoryId,
                        searchQuery = controlState.searchQuery,
                        isGridView = controlState.isGridView,
                        isRefreshing = controlState.isRefreshing,
                        snackbarMessage = controlState.snackbarMessage
                    )
                }
                .catch { e ->
                    Timber.e(e, "Error loading inventory")
                    _uiState.value = InventoryUiState.Error(
                        e.message ?: "Gagal memuat data"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in loadData")
                _uiState.value = InventoryUiState.Error(
                    e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }

    fun onEvent(event: InventoryEvent) {
        when (event) {
            is InventoryEvent.Refresh -> refresh()
            is InventoryEvent.Search -> search(event.query)
            is InventoryEvent.ChangeTab -> changeTab(event.tab)
            
            // Product
            is InventoryEvent.SelectCategory -> selectCategory(event.categoryId)
            is InventoryEvent.DeleteProduct -> deleteProduct(event.productId)
            
            // Category
            is InventoryEvent.AddCategory -> addCategory(event.name, event.icon)
            is InventoryEvent.UpdateCategoryWithProducts -> updateCategoryWithProducts(event.categoryId, event.name, event.productIds)
            is InventoryEvent.DeleteCategory -> deleteCategory(event.categoryId)
            
            // Add-On
            is InventoryEvent.AddAddOn -> addAddOn(event.name, event.price)
            is InventoryEvent.UpdateAddOn -> updateAddOn(event.addOnId, event.name, event.price)
            is InventoryEvent.DeleteAddOn -> deleteAddOn(event.addOnId)
            
            // UI
            is InventoryEvent.ToggleViewMode -> toggleViewMode()
            is InventoryEvent.SnackbarDismissed -> clearSnackbar()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            updateControlState { it.copy(isRefreshing = true) }

            // Sync all data
            productRepository.syncFromRemote()
            addOnRepository.syncFromRemote()

            updateControlState { it.copy(isRefreshing = false) }
        }
    }

    private fun search(query: String) {
        updateControlState { it.copy(searchQuery = query) }
    }
    
    private fun changeTab(tab: InventoryTab) {
        updateControlState { it.copy(selectedTab = tab) }
    }

    private fun selectCategory(categoryId: String?) {
        updateControlState { it.copy(selectedCategoryId = categoryId) }
    }

    private fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
                .onSuccess { showSnackbar("Produk berhasil dihapus") }
                .onFailure { 
                    Timber.e(it, "Failed to delete product: $productId")
                    showSnackbar("Gagal menghapus produk")
                }
        }
    }
    
    private fun addCategory(name: String, icon: String?) {
        viewModelScope.launch {
            productRepository.createCategory(name, icon)
                .onSuccess { showSnackbar("Kategori \"$name\" berhasil ditambahkan") }
                .onFailure { error ->
                    val message = if (error.message?.startsWith("DUPLICATE_ACTIVE:") == true) {
                        "Kategori \"$name\" sudah ada"
                    } else {
                        Timber.e(error, "Failed to create category")
                        "Gagal menambahkan kategori"
                    }
                    showSnackbar(message)
                }
        }
    }
    
    private fun updateCategoryWithProducts(categoryId: String, name: String, productIds: List<String>) {
        viewModelScope.launch {
            // 1. Update category name
            productRepository.updateCategory(categoryId, name)
                .onFailure { 
                    Timber.e(it, "Failed to update category: $categoryId")
                    showSnackbar("Gagal memperbarui kategori")
                    return@launch
                }
            
            // 2. Assign products to this category
            productRepository.assignProductsToCategory(categoryId, productIds)
                .onSuccess { 
                    showSnackbar("Kategori dan produk berhasil diperbarui") 
                }
                .onFailure { 
                    Timber.e(it, "Failed to assign products to category: $categoryId")
                    showSnackbar("Kategori diperbarui tapi gagal mengatur produk")
                }
        }
    }
    
    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            productRepository.deleteCategory(categoryId)
                .onSuccess { showSnackbar("Kategori berhasil dihapus") }
                .onFailure { 
                    Timber.e(it, "Failed to delete category: $categoryId")
                    showSnackbar("Gagal menghapus kategori")
                }
        }
    }
    
    private fun addAddOn(name: String, price: Long) {
        viewModelScope.launch {
            addOnRepository.createAddOn(name, price)
                .onSuccess { showSnackbar("Tambahan \"$name\" berhasil ditambahkan") }
                .onFailure { error ->
                    val message = if (error.message?.startsWith("DUPLICATE_ACTIVE:") == true) {
                        "Tambahan \"$name\" sudah ada"
                    } else {
                        Timber.e(error, "Failed to create add-on")
                        "Gagal menambahkan tambahan"
                    }
                    showSnackbar(message)
                }
        }
    }
    
    private fun updateAddOn(addOnId: String, name: String, price: Long) {
        viewModelScope.launch {
            addOnRepository.updateAddOn(addOnId, name, price)
                .onSuccess { showSnackbar("Add-on berhasil diperbarui") }
                .onFailure { 
                    Timber.e(it, "Failed to update add-on: $addOnId")
                    showSnackbar("Gagal memperbarui add-on")
                }
        }
    }
    
    private fun deleteAddOn(addOnId: String) {
        viewModelScope.launch {
            addOnRepository.deleteAddOn(addOnId)
                .onSuccess { showSnackbar("Add-on berhasil dihapus") }
                .onFailure { 
                    Timber.e(it, "Failed to delete add-on: $addOnId")
                    showSnackbar("Gagal menghapus add-on")
                }
        }
    }
    
    private fun toggleViewMode() {
        updateControlState { it.copy(isGridView = !it.isGridView) }
    }
    
    private fun showSnackbar(message: String) {
        updateControlState { it.copy(snackbarMessage = message) }
    }
    
    private fun clearSnackbar() {
        updateControlState { it.copy(snackbarMessage = null) }
    }
    
    private inline fun updateControlState(update: (UiControlState) -> UiControlState) {
        _uiControlState.value = update(_uiControlState.value)
    }
}
