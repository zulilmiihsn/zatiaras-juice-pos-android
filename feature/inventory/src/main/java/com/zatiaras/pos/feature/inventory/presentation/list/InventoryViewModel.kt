package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.local.DatabaseSeeder
import com.zatiaras.pos.feature.inventory.domain.repository.ProductRepository
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
 * ViewModel for Inventory List Screen.
 * 
 * Responsibilities:
 * - Load and observe products/categories from repository
 * - Handle search and category filter state
 * - Handle refresh/sync operations
 * 
 * Follows Single Responsibility: Only manages InventoryScreen state.
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val databaseSeeder: DatabaseSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)

    init {
        seedAndLoadData()
    }

    private fun seedAndLoadData() {
        viewModelScope.launch {
            // Seed sample data for development/testing
            databaseSeeder.seedIfEmpty()
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    productRepository.getProducts(),
                    productRepository.getCategories(),
                    _searchQuery,
                    _selectedCategoryId
                ) { products, categories, query, categoryId ->
                    InventoryUiState.Success(
                        products = products,
                        categories = categories,
                        selectedCategoryId = categoryId,
                        searchQuery = query
                    )
                }
                .catch { e ->
                    Timber.e(e, "Error loading inventory")
                    _uiState.value = InventoryUiState.Error(
                        e.message ?: "Gagal memuat data produk"
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
            is InventoryEvent.SelectCategory -> selectCategory(event.categoryId)
            is InventoryEvent.DeleteProduct -> deleteProduct(event.productId)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is InventoryUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            productRepository.syncFromRemote()
                .onSuccess {
                    Timber.d("Sync completed successfully")
                }
                .onFailure { e ->
                    Timber.e(e, "Sync failed")
                }

            // Reset refreshing state
            val newState = _uiState.value
            if (newState is InventoryUiState.Success) {
                _uiState.value = newState.copy(isRefreshing = false)
            }
        }
    }

    private fun search(query: String) {
        _searchQuery.value = query
    }

    private fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    private fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
                .onSuccess {
                    Timber.d("Product deleted: $productId")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete product: $productId")
                    // TODO: Show error snackbar
                }
        }
    }
}
