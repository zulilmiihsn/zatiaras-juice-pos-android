package com.zatiaras.pos.feature.inventory.presentation.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.data.remote.ImageUploader
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.repository.AddOnRepository
import com.zatiaras.pos.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Product Detail (Add/Edit) Screen.
 * 
 * Handles form state, validation, save/delete operations, and image upload.
 * Receives productId from navigation arguments (null = create mode).
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val addOnRepository: AddOnRepository,
    private val imageUploader: ImageUploader,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String? = savedStateHandle.get<String>("productId")
    
    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load categories and add-ons first
                val categories = productRepository.getCategories().first()
                val addOns = addOnRepository.observeActiveAddOns().first()
                
                if (productId != null) {
                    // Edit mode - load existing product
                    val product = productRepository.getProductById(productId)
                    if (product != null) {
                        _uiState.value = ProductDetailUiState.Form(
                            id = product.id,
                            name = product.name,
                            price = product.price.toString(),
                            categoryId = product.category?.id,
                            type = product.type,
                            ekstraIds = product.ekstraIds.toSet(),
                            imageUrl = product.imageUrl,
                            description = product.description ?: "",
                            categories = categories,
                            availableAddOns = addOns
                        )
                    } else {
                        _uiState.value = ProductDetailUiState.Error("Produk tidak ditemukan")
                    }
                } else {
                    // Create mode - empty form
                    _uiState.value = ProductDetailUiState.Form(
                        categories = categories,
                        availableAddOns = addOns
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product detail")
                _uiState.value = ProductDetailUiState.Error(
                    e.message ?: "Gagal memuat data"
                )
            }
        }
    }

    fun onEvent(event: ProductDetailEvent) {
        val currentState = _uiState.value
        if (currentState !is ProductDetailUiState.Form) return

        when (event) {
            is ProductDetailEvent.SetName -> {
                _uiState.value = currentState.copy(
                    name = event.name,
                    nameError = validateName(event.name)
                )
            }
            is ProductDetailEvent.SetPrice -> {
                _uiState.value = currentState.copy(
                    price = event.price.filter { it.isDigit() },
                    priceError = validatePrice(event.price)
                )
            }
            is ProductDetailEvent.SetCategory -> {
                _uiState.value = currentState.copy(categoryId = event.categoryId)
            }
            is ProductDetailEvent.SetType -> {
                _uiState.value = currentState.copy(type = event.type)
            }
            is ProductDetailEvent.ToggleAddOn -> {
                val currentIds = currentState.ekstraIds
                val newIds = if (currentIds.contains(event.addOnId)) {
                    currentIds - event.addOnId
                } else {
                    currentIds + event.addOnId
                }
                _uiState.value = currentState.copy(ekstraIds = newIds)
            }
            is ProductDetailEvent.SetDescription -> {
                _uiState.value = currentState.copy(description = event.description)
            }
            is ProductDetailEvent.SetImageUri -> {
                _uiState.value = currentState.copy(imageUri = event.uri)
            }
            is ProductDetailEvent.Save -> saveProduct(currentState)
            is ProductDetailEvent.Delete -> deleteProduct(currentState)
            is ProductDetailEvent.AddNewAddOn -> createAddOn(event.name, event.price)
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Nama produk wajib diisi"
            name.length < 2 -> "Nama produk minimal 2 karakter"
            else -> null
        }
    }

    private fun validatePrice(price: String): String? {
        val numericPrice = price.filter { it.isDigit() }.toLongOrNull() ?: 0
        return when {
            price.isBlank() -> "Harga wajib diisi"
            numericPrice <= 0 -> "Harga harus lebih dari 0"
            else -> null
        }
    }
    
    private fun createAddOn(name: String, price: Long) {
        viewModelScope.launch {
            try {
                val result = addOnRepository.createAddOn(name, price)
                
                if (result.isSuccess) {
                    val newAddOn = result.getOrThrow()
                    // Reload add-ons to update list - call suspend function here
                    val addOns = addOnRepository.observeActiveAddOns().first()
                    
                    val currentState = _uiState.value
                    if (currentState is ProductDetailUiState.Form) {
                        // Auto-select the newly created add-on
                        val newIds = currentState.ekstraIds + newAddOn.id
                        _uiState.value = currentState.copy(
                            availableAddOns = addOns,
                            ekstraIds = newIds
                        )
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Timber.e(exception, "Failed to create add-on")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating add-on")
            }
        }
    }

    private fun saveProduct(formState: ProductDetailUiState.Form) {
        // Validate all fields
        val nameError = validateName(formState.name)
        val priceError = validatePrice(formState.price)

        if (nameError != null || priceError != null) {
            _uiState.value = formState.copy(
                nameError = nameError,
                priceError = priceError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = formState.copy(isSubmitting = true)

            try {
                val productId = formState.id ?: UUID.randomUUID().toString()
                
                // Upload image if new image selected
                val imageUrl: String? = if (formState.imageUri != null) {
                    Timber.d("Uploading new product image for product: $productId")
                    
                    val uploadResult = imageUploader.uploadProductImage(
                        context, 
                        formState.imageUri, 
                        productId
                    )
                    
                    uploadResult.fold(
                        onSuccess = { url -> url },
                        onFailure = { 
                            formState.imageUrl // Keep existing if failed
                        }
                    )
                } else {
                    formState.imageUrl
                }
                
                val product = Product(
                    id = productId,
                    name = formState.name.trim(),
                    price = formState.price.toLongOrNull() ?: 0,
                    category = formState.selectedCategory,
                    type = formState.type,
                    ekstraIds = formState.ekstraIds.toList(),
                    imageUrl = imageUrl,
                    description = formState.description.trim().ifBlank { null },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val result = if (formState.isEditMode) {
                    productRepository.updateProduct(product)
                } else {
                    productRepository.createProduct(product)
                }

                result
                    .onSuccess {
                        val message = if (formState.isEditMode) {
                            "Produk berhasil diperbarui"
                        } else {
                            "Produk berhasil ditambahkan"
                        }
                        _uiState.value = ProductDetailUiState.Success(message)
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to save product")
                        _uiState.value = formState.copy(
                            isSubmitting = false
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error saving product")
                _uiState.value = formState.copy(isSubmitting = false)
            }
        }
    }

    private fun deleteProduct(formState: ProductDetailUiState.Form) {
        val id = formState.id ?: return

        viewModelScope.launch {
            _uiState.value = formState.copy(isSubmitting = true)

            // Delete image from storage if exists
            formState.imageUrl?.let { url ->
                imageUploader.deleteProductImage(url)
                    .onFailure { Timber.w(it, "Failed to delete product image") }
            }

            productRepository.deleteProduct(id)
                .onSuccess {
                    _uiState.value = ProductDetailUiState.Success("Produk berhasil dihapus")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete product")
                    _uiState.value = formState.copy(isSubmitting = false)
                }
        }
    }
}

