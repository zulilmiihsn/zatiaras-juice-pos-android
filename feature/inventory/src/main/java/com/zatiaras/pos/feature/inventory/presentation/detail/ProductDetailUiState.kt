package com.zatiaras.pos.feature.inventory.presentation.detail

import android.net.Uri
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.ProductType

/**
 * UI State for Product Detail (Add/Edit) Screen.
 * 
 * Uses data class Form for mutable form state.
 */
sealed interface ProductDetailUiState {
    
    /**
     * Loading existing product data (edit mode).
     */
    data object Loading : ProductDetailUiState
    
    /**
     * Form state for both create and edit modes.
     */
    data class Form(
        val id: String? = null,           // null = create mode
        val name: String = "",
        val price: String = "",
        val categoryId: String? = null,
        val type: ProductType = ProductType.MAKANAN,
        val ekstraIds: Set<String> = emptySet(),
        val imageUri: Uri? = null,
        val imageUrl: String? = null,     // existing image URL (edit mode)
        val description: String = "",
        val categories: List<Category> = emptyList(),
        val availableAddOns: List<AddOn> = emptyList(),
        val isSubmitting: Boolean = false,
        val nameError: String? = null,
        val priceError: String? = null
    ) : ProductDetailUiState {
        
        val isEditMode: Boolean
            get() = id != null
        
        val isValid: Boolean
            get() = nameError == null && priceError == null && 
                    name.isNotBlank() && price.isNotBlank()
        
        val selectedCategory: Category?
            get() = categories.find { it.id == categoryId }
    }
    
    /**
     * Successfully saved product.
     */
    data class Success(val message: String) : ProductDetailUiState
    
    /**
     * Error loading or saving product.
     */
    data class Error(val message: String) : ProductDetailUiState
}

/**
 * Events from ProductDetailScreen to ViewModel.
 */
sealed interface ProductDetailEvent {
    data class SetName(val name: String) : ProductDetailEvent
    data class SetPrice(val price: String) : ProductDetailEvent
    data class SetCategory(val categoryId: String?) : ProductDetailEvent
    data class SetType(val type: ProductType) : ProductDetailEvent
    data class ToggleAddOn(val addOnId: String) : ProductDetailEvent
    data class SetDescription(val description: String) : ProductDetailEvent
    data class SetImageUri(val uri: Uri?) : ProductDetailEvent
    data object Save : ProductDetailEvent
    data object Delete : ProductDetailEvent
    data class AddNewAddOn(val name: String, val price: Long) : ProductDetailEvent
}
