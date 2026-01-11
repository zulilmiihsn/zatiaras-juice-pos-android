package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.pos.domain.model.Cart
import java.text.NumberFormat
import java.util.Locale

/**
 * Cart sidebar component for POS screen.
 * 
 * Displays cart items with quantity controls and checkout button.
 * Separated from PosScreen.kt to follow KISS principle (files under 200 lines).
 */
@Composable
fun CartSidebar(
    cart: Cart,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember priceFormatter to avoid recomposition on every render
    val priceFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            CartHeader(
                itemCount = cart.itemCount,
                onClearCart = onClearCart
            )
            
            HorizontalDivider()
            
            // Cart Items
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = cart.items,
                    key = { it.product.id }
                ) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onIncrement = { onIncrement(cartItem.product.id) },
                        onDecrement = { onDecrement(cartItem.product.id) },
                        onRemove = { onRemove(cartItem.product.id) }
                    )
                }
            }
            
            HorizontalDivider()
            
            // Footer with Total and Checkout
            CartFooter(
                subtotal = cart.subtotal,
                priceFormatter = priceFormatter,
                onCheckout = onCheckout
            )
        }
    }
}

@Composable
private fun CartHeader(
    itemCount: Int,
    onClearCart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Keranjang ($itemCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = onClearCart,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Kosongkan",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CartFooter(
    subtotal: Long,
    priceFormatter: NumberFormat,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Subtotal",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = priceFormatter.format(subtotal),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Bayar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
