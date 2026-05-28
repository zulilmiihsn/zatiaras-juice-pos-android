package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.Cart
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Search

@Composable
internal fun PagedProducts(
    products: LazyPagingItems<Product>,
    cart: Cart,
    isGridView: Boolean,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (val refreshState = products.loadState.refresh) {
            is LoadState.Loading -> LoadingProducts()
            is LoadState.Error -> ProductErrorState(
                message = refreshState.error.localizedMessage ?: stringResource(R.string.pos_error_load_products),
                onRetry = { products.retry() },
            )
            is LoadState.NotLoading -> {
                if (products.itemCount == 0) {
                    EmptyProductState()
                } else if (isGridView) {
                    ProductGrid(
                        products = products,
                        cart = cart,
                        onProductClick = onProductClick,
                    )
                } else {
                    ProductList(
                        products = products,
                        cart = cart,
                        onProductClick = onProductClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(
    products: LazyPagingItems<Product>,
    cart: Cart,
    onProductClick: (Product) -> Unit,
) {
    val dimensions = LocalDimensions.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(
            start = dimensions.paddingM,
            end = dimensions.paddingM,
            top = dimensions.paddingXS,
            bottom = 80.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingM),
    ) {
        items(
            count = products.itemCount,
            key = { index -> products[index]?.id ?: index },
        ) { index ->
            products[index]?.let { product ->
                PosProductCard(
                    product = product,
                    quantityInCart = cart.getQuantity(product.id),
                    onAddToCart = { onProductClick(product) },
                )
            }
        }

        if (products.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Brand500,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductList(
    products: LazyPagingItems<Product>,
    cart: Cart,
    onProductClick: (Product) -> Unit,
) {
    val dimensions = LocalDimensions.current
    LazyColumn(
        contentPadding = PaddingValues(
            start = dimensions.paddingM,
            end = dimensions.paddingM,
            top = dimensions.paddingXS,
            bottom = 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingS),
    ) {
        items(
            count = products.itemCount,
            key = { index -> products[index]?.id ?: index },
        ) { index ->
            products[index]?.let { product ->
                PosProductListItem(
                    product = product,
                    quantityInCart = cart.getQuantity(product.id),
                    onAddToCart = { onProductClick(product) },
                )
            }
        }
    }
}

@Composable
private fun LoadingProducts() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Brand500)
    }
}

@Composable
private fun ProductErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Brand500),
        ) {
            Text("Coba lagi")
        }
    }
}

@Composable
private fun EmptyProductState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = EvaIcons.Outline.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Slate200,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Produk tidak ditemukan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
