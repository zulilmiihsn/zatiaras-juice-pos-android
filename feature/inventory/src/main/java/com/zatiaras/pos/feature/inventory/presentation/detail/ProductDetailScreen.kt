package com.zatiaras.pos.feature.inventory.presentation.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.ui.theme.LocalDimensions

/**
 * Product Detail Screen for Add/Edit product.
 * 
 * Features:
 * - Image picker from gallery
 * - Form fields: name, price, category, description
 * - Validation with error messages
 * - Save button with loading state
 * - Delete button (edit mode only)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(ProductDetailEvent.SetImageUri(it)) }
    }

    // Handle success navigation
    LaunchedEffect(uiState) {
        if (uiState is ProductDetailUiState.Success) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is ProductDetailUiState.Form -> {
                                if ((uiState as ProductDetailUiState.Form).isEditMode) {
                                    "Edit Produk"
                                } else {
                                    "Tambah Produk"
                                }
                            }
                            else -> "Detail Produk"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (uiState is ProductDetailUiState.Form && 
                        (uiState as ProductDetailUiState.Form).isEditMode) {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        if (showDeleteDialog) {
                            DeleteConfirmationDialog(
                                onConfirm = {
                                    viewModel.onEvent(ProductDetailEvent.Delete)
                                    showDeleteDialog = false
                                },
                                onDismiss = { showDeleteDialog = false }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ProductDetailUiState.Loading -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            is ProductDetailUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is ProductDetailUiState.Form -> {
                FormContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is ProductDetailUiState.Success -> {
                // Will navigate away via LaunchedEffect
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormContent(
    state: ProductDetailUiState.Form,
    onEvent: (ProductDetailEvent) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.paddingM),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
    ) {
        // Image Picker
        ImagePickerBox(
            imageUrl = state.imageUrl,
            imageUri = state.imageUri,
            onImageClick = onPickImage
        )

        // Name Field
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(ProductDetailEvent.SetName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama Produk *") },
            placeholder = { Text("Contoh: Es Kopi Susu") },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Price Field
        OutlinedTextField(
            value = state.price,
            onValueChange = { onEvent(ProductDetailEvent.SetPrice(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Harga (Rp) *") },
            placeholder = { Text("15000") },
            isError = state.priceError != null,
            supportingText = state.priceError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            prefix = { Text("Rp ") }
        )

        // Category Dropdown
        CategoryDropdown(
            categories = state.categories,
            selectedCategoryId = state.categoryId,
            onCategorySelected = { onEvent(ProductDetailEvent.SetCategory(it)) }
        )

        // Description Field
        OutlinedTextField(
            value = state.description,
            onValueChange = { onEvent(ProductDetailEvent.SetDescription(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text("Deskripsi (opsional)") },
            placeholder = { Text("Deskripsi singkat produk...") },
            maxLines = 4,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = { onEvent(ProductDetailEvent.Save) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isSubmitting,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (state.isEditMode) "Perbarui Produk" else "Simpan Produk",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ImagePickerBox(
    imageUrl: String?,
    imageUri: android.net.Uri?,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onImageClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUri != null -> {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            imageUrl != null -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Product image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Tap untuk tambah foto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            label = { Text("Kategori") },
            placeholder = { Text("Pilih kategori...") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "None" option
            DropdownMenuItem(
                text = { Text("Tanpa kategori") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Produk?") },
        text = { Text("Produk yang dihapus tidak dapat dikembalikan.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val dimensions = LocalDimensions.current
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensions.paddingXXL)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
