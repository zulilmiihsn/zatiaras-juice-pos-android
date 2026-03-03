package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.zatiaras.pos.feature.inventory.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import androidx.compose.foundation.border

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCategoryDialog(
    category: Category,
    allProducts: List<com.zatiaras.pos.core.domain.model.Product>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, name: String, productIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Get products currently in this category
    val initialProductIds = remember { 
        allProducts.filter { it.category?.id == category.id }.map { it.id }.toSet()
    }
    var selectedProductIds by remember { mutableStateOf(initialProductIds) }
    
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        Card(
                modifier = Modifier.fillMaxWidth(0.95f)
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
                    modifier = Modifier
                        .padding(24.dp)
                        .heightIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Text(
                        text = stringResource(R.string.inventory_edit_category),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            error = null
                        },
                        label = { Text(stringResource(R.string.inventory_category_name)) },
                        isError = error != null,
                        supportingText = error?.let { { Text(it) } },
                        singleLine = true,
                        shape = AppShapes.M,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Products section header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.inventory_category_products_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = AppShapes.S,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.inventory_product_selected, selectedProductIds.size),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    // Search products
                    var productSearchQuery by remember { mutableStateOf("") }
                    val filteredProducts = remember(allProducts, productSearchQuery) {
                        if (productSearchQuery.isBlank()) {
                            allProducts
                        } else {
                            allProducts.filter { 
                                it.name.contains(productSearchQuery, ignoreCase = true)
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        label = { Text(stringResource(R.string.inventory_search_placeholder)) },
                        singleLine = true,
                        shape = AppShapes.M,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Products list with pills grouped by category
                    if (allProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = stringResource(R.string.inventory_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.inventory_product_not_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Group products by category
                        val productsInThisCategory = remember(filteredProducts, category.id) {
                            filteredProducts.filter { it.category?.id == category.id }
                        }
                        val productsWithoutCategory = remember(filteredProducts) {
                            filteredProducts.filter { it.category == null }
                        }
                        val otherCategoryName = stringResource(R.string.inventory_other)
                        val productsInOtherCategories = remember(filteredProducts, category.id) {
                            filteredProducts.filter { it.category != null && it.category?.id != category.id }
                                .groupBy { it.category?.name ?: otherCategoryName }
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current category products
                            if (productsInThisCategory.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.inventory_categorized_products, productsInThisCategory.size),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                item {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        productsInThisCategory.forEach { product ->
                                            val isSelected = selectedProductIds.contains(product.id)
                                            FilterChip(
                                                modifier = Modifier.height(38.dp),
                                                selected = isSelected,
                                                onClick = {
                                                    selectedProductIds = if (isSelected) {
                                                        selectedProductIds - product.id
                                                    } else {
                                                        selectedProductIds + product.id
                                                    }
                                                },
                                                label = { 
                                                    Text(
                                                        text = product.name,
                                                        style = MaterialTheme.typography.labelLarge
                                                    ) 
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Uncategorized products
                            if (productsWithoutCategory.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.inventory_uncategorized_products, productsWithoutCategory.size),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        productsWithoutCategory.forEach { product ->
                                            val isSelected = selectedProductIds.contains(product.id)
                                            FilterChip(
                                                modifier = Modifier.height(38.dp),
                                                selected = isSelected,
                                                onClick = {
                                                    selectedProductIds = if (isSelected) {
                                                        selectedProductIds - product.id
                                                    } else {
                                                        selectedProductIds + product.id
                                                    }
                                                },
                                                label = { 
                                                    Text(
                                                        text = product.name,
                                                        style = MaterialTheme.typography.labelLarge
                                                    ) 
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Products in other categories
                            productsInOtherCategories.forEach { (categoryName, products) ->
                                item {
                                    Text(
                                        text = stringResource(
                                            R.string.inventory_category_with_count,
                                            categoryName,
                                            products.size
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        products.forEach { product ->
                                            val isSelected = selectedProductIds.contains(product.id)
                                            FilterChip(
                                                modifier = Modifier.height(38.dp),
                                                selected = isSelected,
                                                onClick = {
                                                    selectedProductIds = if (isSelected) {
                                                        selectedProductIds - product.id
                                                    } else {
                                                        selectedProductIds + product.id
                                                    }
                                                },
                                                label = { 
                                                    Text(
                                                        text = product.name,
                                                        style = MaterialTheme.typography.labelLarge
                                                    ) 
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Action buttons
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
                        val nameEmptyStr = stringResource(R.string.validation_name_empty)
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    error = nameEmptyStr
                                } else {
                                    onConfirm(category.id, name, selectedProductIds.toList())
                                    dismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.M
                        ) {
                            Text(stringResource(R.string.inventory_action_save))
                        }
                    }
                }
            }
        }
    }
