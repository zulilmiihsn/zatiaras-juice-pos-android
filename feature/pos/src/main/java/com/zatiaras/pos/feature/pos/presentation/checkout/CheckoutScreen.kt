package com.zatiaras.pos.feature.pos.presentation.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.presentation.CheckoutEvent
import java.text.NumberFormat

/**
 * Checkout Screen for completing payment.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(
    cart: Cart,
    onNavigateBack: () -> Unit,
    onTransactionComplete: (Transaction) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val priceFormatter = CurrencyFormatter.getCurrencyFormatter()
    
    // Initialize cart on first composition
    LaunchedEffect(cart) {
        viewModel.initializeWithCart(cart)
    }
    
    // Handle success state
    LaunchedEffect(uiState) {
        if (uiState is CheckoutUiState.Success) {
            val transaction = (uiState as CheckoutUiState.Success).transaction
            onTransactionComplete(transaction)
        }
    }
    
    // Show error snackbar
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is CheckoutUiState.Ready && state.paymentError != null) {
            snackbarHostState.showSnackbar(state.paymentError)
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Pembayaran",
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is CheckoutUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is CheckoutUiState.Ready -> {
                CheckoutContent(
                    state = state,
                    priceFormatter = priceFormatter,
                    onEvent = viewModel::onEvent,
                    onQuickAmount = viewModel::setQuickAmount,
                    onExactAmount = viewModel::setExactAmount,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is CheckoutUiState.Success -> {
                // Will navigate away via LaunchedEffect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Transaksi Berhasil!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            is CheckoutUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckoutContent(
    state: CheckoutUiState.Ready,
    priceFormatter: NumberFormat,
    onEvent: (CheckoutEvent) -> Unit,
    onQuickAmount: (Long) -> Unit,
    onExactAmount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensions.paddingM),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
    ) {
        // Order Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ringkasan Pesanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider()
                    
                    // Item count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jumlah Item")
                        Text("${state.cart.itemCount} item")
                    }
                    
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal")
                        Text(priceFormatter.format(state.subtotal))
                    }
                    
                    // Discount (if any)
                    if (state.discountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Diskon (${state.discountPercent.toInt()}%)",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "-${priceFormatter.format(state.discountAmount)}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Tax
                    if (state.taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PPN (${state.taxPercent.toInt()}%)")
                            Text(priceFormatter.format(state.taxAmount))
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            priceFormatter.format(state.grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Payment Method Selection
        item {
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodChip(
                    method = PaymentMethod.CASH,
                    icon = Icons.Default.Money,
                    isSelected = state.selectedPaymentMethod == PaymentMethod.CASH,
                    onClick = { onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.CASH)) },
                    modifier = Modifier.weight(1f)
                )
                
                PaymentMethodChip(
                    method = PaymentMethod.QRIS,
                    icon = Icons.Default.QrCode2,
                    isSelected = state.selectedPaymentMethod == PaymentMethod.QRIS,
                    onClick = { onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.QRIS)) },
                    modifier = Modifier.weight(1f)
                )
                
                PaymentMethodChip(
                    method = PaymentMethod.TRANSFER,
                    icon = Icons.Default.AccountBalance,
                    isSelected = state.selectedPaymentMethod == PaymentMethod.TRANSFER,
                    onClick = { onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.TRANSFER)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Cash Payment Section
        if (state.selectedPaymentMethod == PaymentMethod.CASH) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Jumlah Bayar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        CurrencyTextField(
                            value = state.amountPaid,
                            onValueChange = { onEvent(CheckoutEvent.SetAmountPaid(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Jumlah uang diterima") },
                            showPrefix = true,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        // Quick amount buttons
                        Text(
                            text = "Nominal Cepat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = onExactAmount) {
                                Text("Uang Pas")
                            }
                            
                            listOf(10_000L, 20_000L, 50_000L, 100_000L).forEach { amount ->
                                OutlinedButton(onClick = { onQuickAmount(amount) }) {
                                    Text(CurrencyFormatter.formatCurrency(amount, includeSymbol = false))
                                }
                            }
                        }
                        
                        // Change display
                        AnimatedVisibility(visible = state.changeAmount > 0) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Kembalian",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(state.changeAmount),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // QRIS/Transfer info
        if (state.selectedPaymentMethod != PaymentMethod.CASH) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
                                Icons.Default.QrCode2
                            } else {
                                Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (state.selectedPaymentMethod == PaymentMethod.QRIS) {
                                "Tampilkan QR Code ke pelanggan"
                            } else {
                                "Konfirmasi transfer dari pelanggan"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = CurrencyFormatter.formatCurrency(state.grandTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Customer Name field
        item {
            OutlinedTextField(
                value = state.customerName,
                onValueChange = { onEvent(CheckoutEvent.SetCustomerName(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Pelanggan (opsional)") },
                placeholder = { Text("Masukkan nama pelanggan...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        // Notes field
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onEvent(CheckoutEvent.SetNotes(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("Tambahkan catatan transaksi...") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        // Complete Payment Button
        item {
            Button(
                onClick = { onEvent(CheckoutEvent.ConfirmPayment) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.canComplete && !state.isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selesaikan Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PaymentMethodChip(
    method: PaymentMethod,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = method.displayName,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        modifier = modifier.height(72.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
