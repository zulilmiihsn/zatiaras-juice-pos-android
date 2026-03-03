package com.zatiaras.pos.feature.inventory.presentation.detail

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.zatiaras.pos.feature.inventory.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.theme.AppShapes
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

    // Permission launcher — request before opening gallery
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        }
    }
    
    // Function to pick image with permission check
    val pickImage: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Handle success navigation
    LaunchedEffect(uiState) {
        if (uiState is ProductDetailUiState.Success) {
            onSaveSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is ProductDetailUiState.Form -> {
                                if ((uiState as ProductDetailUiState.Form).isEditMode) {
                                    stringResource(R.string.product_detail_edit_title)
                                } else {
                                    stringResource(R.string.product_detail_add_title)
                                }
                            }
                            else -> stringResource(R.string.product_detail_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
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
                                contentDescription = stringResource(R.string.inventory_action_delete),
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
                    onPickImage = pickImage,
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
            label = { Text(stringResource(R.string.product_name_label)) },
            placeholder = { Text(stringResource(R.string.product_name_placeholder)) },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            singleLine = true,
            shape = AppShapes.M
        )

        // Price Field with currency formatting
        CurrencyTextField(
            value = state.price,
            onValueChange = { onEvent(ProductDetailEvent.SetPrice(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.product_price_label)) },
            isError = state.priceError != null,
            showPrefix = true,
            singleLine = true,
            shape = AppShapes.M
        )
        
        // Show price error if any
        state.priceError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // Category Dropdown
        CategoryDropdown(
            categories = state.categories,
            selectedCategoryId = state.categoryId,
            onCategorySelected = { onEvent(ProductDetailEvent.SetCategory(it)) }
        )
        
        // Product Type Selector
        ProductTypeSelector(
            selectedType = state.type,
            onTypeSelected = { onEvent(ProductDetailEvent.SetType(it)) }
        )
        
        // Add-Ons Selector
        var showAddOnDialog by remember { mutableStateOf(false) }
        
        AddOnSelector(
            availableAddOns = state.availableAddOns,
            selectedAddOnIds = state.ekstraIds,
            onAddOnToggle = { onEvent(ProductDetailEvent.ToggleAddOn(it)) },
            onAddNewAddOn = { showAddOnDialog = true }
        )
        
        if (showAddOnDialog) {
            AddNewAddOnDialog(
                onDismiss = { showAddOnDialog = false },
                onConfirm = { name, price ->
                    onEvent(ProductDetailEvent.AddNewAddOn(name, price))
                    showAddOnDialog = false
                }
            )
        }

        // Description Field
        OutlinedTextField(
            value = state.description,
            onValueChange = { onEvent(ProductDetailEvent.SetDescription(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text(stringResource(R.string.product_desc_label)) },
            placeholder = { Text(stringResource(R.string.product_desc_placeholder)) },
            maxLines = 4,
            shape = AppShapes.M
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = { onEvent(ProductDetailEvent.Save) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isSubmitting,
            shape = AppShapes.M
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (state.isEditMode) stringResource(R.string.product_action_update) else stringResource(R.string.product_action_save),
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
    imageUri: Uri?,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(AppShapes.L)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = AppShapes.L
            )
            .clickable(onClick = onImageClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUri != null -> {
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.product_image_selected),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            imageUrl != null -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.product_image_existing),
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
                        text = stringResource(R.string.product_image_placeholder),
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            label = { Text(stringResource(R.string.product_category_label)) },
            placeholder = { Text(stringResource(R.string.product_category_placeholder)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = AppShapes.M
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "None" option
            DropdownMenuItem(
                text = { Text(stringResource(R.string.product_category_none)) },
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
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        Card(
            modifier = Modifier.fillMaxWidth(0.9f)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    AppShapes.XXL
                ),
            shape = AppShapes.XXL,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.product_delete_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.product_delete_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = dismiss,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_cancel))
                    }
                    
                    Button(
                        onClick = {
                            onConfirm()
                            dismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_delete))
                    }
                }
            }
        }
    }
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
