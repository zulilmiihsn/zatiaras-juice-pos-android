package com.zatiaras.pos.feature.pos.presentation.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.Slate500
import com.zatiaras.pos.core.ui.theme.Slate700
import com.zatiaras.pos.core.ui.theme.Slate900
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.domain.model.Cart
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.presentation.CheckoutEvent

@Composable
fun CheckoutRoute(
    cart: Cart,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (Transaction) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(cart) {
        viewModel.initializeWithCart(cart)
    }

    LaunchedEffect(uiState) {
        if (uiState is CheckoutUiState.Success) {
            onPaymentSuccess((uiState as CheckoutUiState.Success).transaction)
        }
    }

    CheckoutScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    uiState: CheckoutUiState,
    onNavigateBack: () -> Unit,
    onEvent: (CheckoutEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.checkout_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Brand500
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pos_back),
                            tint = Brand500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        when (uiState) {
            is CheckoutUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Brand500)
                }
            }
            is CheckoutUiState.Ready -> {
                CheckoutContent(
                    state = uiState,
                    modifier = Modifier.padding(paddingValues),
                    onEvent = onEvent
                )
            }
            is CheckoutUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Brand500)
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
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CheckoutContent(
    state: CheckoutUiState.Ready,
    modifier: Modifier = Modifier,
    onEvent: (CheckoutEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grand Total Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Brand500,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = AppShapes.L,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.checkout_total_payment_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = CurrencyFormatter.formatCurrency(state.grandTotal),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Customer Info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = AppShapes.L,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.checkout_customer_info),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Brand500
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = state.customerName,
                    onValueChange = { onEvent(CheckoutEvent.SetCustomerName(it)) },
                    label = { Text(stringResource(R.string.checkout_customer_name_hint)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.M,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand500,
                        focusedLabelColor = Brand500,
                        cursorColor = Brand500,
                        unfocusedBorderColor = Slate200
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onEvent(CheckoutEvent.SetNotes(it)) },
                    label = { Text(stringResource(R.string.checkout_order_notes_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.M,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand500,
                        focusedLabelColor = Brand500,
                        cursorColor = Brand500,
                        unfocusedBorderColor = Slate200
                    ),
                    maxLines = 3
                )
            }
        }

        // Payment Method Selection
        Text(
            text = stringResource(R.string.checkout_payment_method),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Slate900
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentMethodCard(
                title = stringResource(R.string.checkout_payment_cash),
                icon = Icons.Default.Money,
                selected = state.selectedPaymentMethod == PaymentMethod.CASH,
                onClick = { onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.CASH)) },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodCard(
                title = stringResource(R.string.checkout_payment_qris),
                icon = Icons.Default.QrCode,
                selected = state.selectedPaymentMethod == PaymentMethod.QRIS,
                onClick = { onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.QRIS)) },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodCard(
                title = stringResource(R.string.checkout_payment_transfer),
                icon = Icons.Outlined.CreditCard,
                selected = state.selectedPaymentMethod == PaymentMethod.TRANSFER,
                onClick = { onEvent(CheckoutEvent.SetPaymentMethod(PaymentMethod.TRANSFER)) },
                modifier = Modifier.weight(1f)
            )
        }

        // Payment Details (Cash only)
        AnimatedVisibility(
            visible = state.selectedPaymentMethod == PaymentMethod.CASH,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = AppShapes.L,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.checkout_payment_title, stringResource(R.string.checkout_payment_cash)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Brand500
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = state.amountPaid,
                        onValueChange = { onEvent(CheckoutEvent.SetAmountPaid(it)) },
                        label = { Text(stringResource(R.string.checkout_customer_payment)) },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.M,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand500,
                            focusedLabelColor = Brand500,
                            cursorColor = Brand500,
                            unfocusedBorderColor = Slate200
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick amounts
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val amounts = listOf(
                            state.grandTotal, 
                            50000L, 
                            100000L
                        ).filter { it >= state.grandTotal || it == state.grandTotal }.distinct().sorted()

                        val suggestions = if (state.grandTotal > 100000L) {
                             listOf(state.grandTotal)
                        } else {
                             amounts
                        }

                        suggestions.forEach { amount ->
                            SuggestionChip(
                                onClick = { onEvent(CheckoutEvent.SetAmountPaid(amount.toString())) },
                                label = { Text(CurrencyFormatter.formatCurrency(amount)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (state.amountPaid == amount.toString()) Slate50 else Color.Transparent,
                                    labelColor = if (state.amountPaid == amount.toString()) Brand500 else Slate900
                                )
                            )
                        }
                    }
                    
                    if (state.amountPaid.toLongOrNull() ?: 0 >= state.grandTotal) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.checkout_change),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                CurrencyFormatter.formatCurrency(state.changeAmount),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Brand500
                                )
                            )
                        }
                    }
                }
            }
        }

        // Order Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = AppShapes.L,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.checkout_order_summary),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Brand500
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.checkout_subtotal),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
                    )
                    Text(
                        CurrencyFormatter.formatCurrency(state.subtotal),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Discount Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = stringResource(R.string.checkout_discount_percent_label),
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.checkout_discount_percent_label),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
                        )
                    }
                    
                    OutlinedTextField(
                        value = if (state.discountPercent > 0.0) state.discountPercent.toString() else "",
                        onValueChange = { onEvent(CheckoutEvent.SetDiscountPercent(it)) },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(100.dp)
                            .height(50.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedBorderColor = Brand500,
                            unfocusedBorderColor = Slate200
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                    )
                }
                
                if (state.discountAmount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.checkout_discount_amount),
                            style = MaterialTheme.typography.bodyMedium.copy(color = ErrorRed)
                        )
                        Text(
                            "- ${CurrencyFormatter.formatCurrency(state.discountAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        )
                    }
                }

                if (state.taxAmount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.checkout_tax, 11),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
                        )
                        Text(
                            CurrencyFormatter.formatCurrency(state.taxAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.pos_total),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        CurrencyFormatter.formatCurrency(state.grandTotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Brand500
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Button
        Button(
            onClick = { onEvent(CheckoutEvent.ConfirmPayment) },
            enabled = state.canComplete && !state.isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = AppShapes.L,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand500,
                disabledContainerColor = Slate500.copy(alpha = 0.5f)
            )
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.checkout_confirm_payment),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Slate50 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (selected) BorderStroke(2.dp, Brand500) else BorderStroke(1.dp, Slate200.copy(alpha=0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Brand500 else Slate500,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Brand500 else Slate700
                )
            )
        }
    }
}

