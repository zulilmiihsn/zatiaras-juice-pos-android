package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Close
import compose.icons.evaicons.outline.Plus
import compose.icons.evaicons.outline.Search
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Slate100
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.Slate500
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.Cart

@Composable
fun PagedProductCatalog(
    products: LazyPagingItems<Product>,
    categories: List<Category>,
    selectedCategory: Category?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryResult: (Category?) -> Unit,
    cart: Cart,
    isGridView: Boolean,
    onToggleViewMode: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddCustomItem: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomItemDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Search and Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onToggleViewMode,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridView) "Tampilan daftar" else "Tampilan grid",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CategoryList(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategoryResult
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Product Grid/List
        Box(modifier = Modifier.weight(1f)) {
            PagedProducts(
                products = products,
                cart = cart,
                isGridView = isGridView,
                onProductClick = onProductClick,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Action Button for Custom Item
            // Stylized as a premium button at bottom center or corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    onClick = { showCustomItemDialog = true },
                    shape = AppShapes.Full,
                    color = Brand500,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = EvaIcons.Outline.Plus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pos_custom_item),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCustomItemDialog) {
        CustomItemDialog(
            onDismiss = { showCustomItemDialog = false },
            onConfirm = { name, price ->
                onAddCustomItem(name, price)
                showCustomItemDialog = false
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = modifier
            .height(56.dp),
        shape = AppShapes.L,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = EvaIcons.Outline.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Cari produk...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = EvaIcons.Outline.Close,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CategoryChip(
                category = Category("-1", "All"),
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories) { category ->
            CategoryChip(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Surface(
        onClick = onClick,
        shape = AppShapes.M,
        color = if (isSelected) Brand500 else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = dimensions.paddingM),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}


@Composable
private fun PagedProducts(
    products: LazyPagingItems<Product>,
    cart: Cart,
    isGridView: Boolean,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    Box(modifier = modifier) {
        if (products.loadState.refresh is LoadState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Brand500)
            }
        } else if (products.loadState.refresh is LoadState.Error) {
             val error = (products.loadState.refresh as LoadState.Error).error
             ErrorState(
                 message = error.localizedMessage ?: stringResource(R.string.pos_error_load_products),
                 onRetry = { products.retry() }
             )
        } else {
             if (products.itemCount == 0) {
                 EmptyState()
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(
                            start = dimensions.paddingM,
                            end = dimensions.paddingM,
                            top = dimensions.paddingXS,
                            bottom = 80.dp // Space for FAB
                        ),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
                    ) {
                        items(
                            count = products.itemCount,
                            key = { index -> products[index]?.id ?: index }
                        ) { index ->
                            val product = products[index]
                            if (product != null) {
                                PosProductCard(
                                    product = product,
                                    quantityInCart = cart.getQuantity(product.id),
                                    onAddToCart = { onProductClick(product) }
                                )
                            }
                        }

                        if (products.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier.padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = Brand500, modifier = Modifier.size(24.dp)) }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = dimensions.paddingM,
                            end = dimensions.paddingM,
                            top = dimensions.paddingXS,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingS)
                    ) {
                        items(
                            count = products.itemCount,
                            key = { index -> products[index]?.id ?: index }
                        ) { index ->
                            val product = products[index]
                            if (product != null) {
                                PosProductListItem(
                                    product = product,
                                    quantityInCart = cart.getQuantity(product.id),
                                    onAddToCart = { onProductClick(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Brand500)
        ) {
            Text("Coba lagi")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = EvaIcons.Outline.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Slate200
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Produk tidak ditemukan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PosProductListItem(
    product: Product,
    quantityInCart: Int,
    onAddToCart: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Surface(
        onClick = onAddToCart,
        shape = AppShapes.M,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().height(84.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimensions.paddingS)
        ) {
            // Placeholder or Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(AppShapes.S)
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                 // Simple text placeholder if no image
                 Text(
                     text = product.name.take(1).uppercase(),
                     style = MaterialTheme.typography.titleMedium,
                     color = Slate500
                 )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand500,
                    fontWeight = FontWeight.Bold
                )
            }

            if (quantityInCart > 0) {
                 Surface(
                    shape = CircleShape,
                    color = Brand500
                ) {
                    Text(
                        text = "$quantityInCart",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableLongStateOf(0L) }

    val isValid = name.isNotBlank() && price > 0L

    ZatDialog(onDismissRequest = onDismiss) { dismiss ->
        Box(
            modifier = Modifier.fillMaxWidth(0.95f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = AppShapes.XXL,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.pos_custom_item),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Brand500
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama menu") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.M,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500,
                            focusedLabelColor = Brand500
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CurrencyTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Harga") },
                        showPrefix = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.M,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500,
                            focusedLabelColor = Brand500
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = dismiss,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = AppShapes.M,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                        ) {
                            Text("Batal", color = Slate500)
                        }

                        Button(
                            onClick = {
                                onConfirm(name.trim(), price)
                            },
                            enabled = isValid,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = AppShapes.M,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Brand500,
                                disabledContainerColor = Slate200
                            )
                        ) {
                            Text("Tambah", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

