package com.zatiaras.pos.feature.pos.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.ui.components.OwnerPinDialog
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.IncomeGreen
import com.zatiaras.pos.core.ui.theme.Slate100
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.Slate300
import com.zatiaras.pos.core.ui.theme.Slate400
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Slate600
import com.zatiaras.pos.core.ui.theme.Slate700
import com.zatiaras.pos.core.ui.theme.Slate800
import com.zatiaras.pos.core.ui.theme.Slate900
import com.zatiaras.pos.core.ui.theme.WarningAmberBg
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Connects the history ViewModel to a stateless screen.
 *
 * Keep navigation side effects in this route so the screen remains previewable
 * and easier for junior engineers or small AI agents to reason about.
 */
@Composable
fun TransactionHistoryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToReceipt: (Transaction) -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransactionHistoryScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onPaymentFilterChanged = viewModel::onPaymentFilterChanged,
        onTransactionClick = viewModel::showDetail,
        onDeleteClick = viewModel::showDeleteConfirm,
        onHideDetail = viewModel::hideDetail,
        onHideDeleteConfirm = viewModel::hideDeleteConfirm,
        onDeleteConfirm = viewModel::deleteTransaction,
        onUpdatePaymentMethod = viewModel::updatePaymentMethod,
        onPrintReceiptClick = {
            viewModel.hideDetail()
            onNavigateToReceipt(it)
        },
        verifyPin = viewModel::verifyOwnerPin,
        onErrorDismiss = viewModel::clearError,
    )
}

/**
 * Displays today's transactions and owns only screen-local UI state.
 *
 * Deletion is intentionally two-step: the selected transaction comes from the
 * ViewModel, then this screen asks for an owner PIN only when the current user
 * is not already an owner. Keep that authorization flow close to the dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    uiState: TransactionHistoryUiState,
    onNavigateBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPaymentFilterChanged: (PaymentFilter) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    onHideDetail: () -> Unit,
    onHideDeleteConfirm: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onUpdatePaymentMethod: (PaymentMethod) -> Unit,
    onPrintReceiptClick: (Transaction) -> Unit,
    verifyPin: suspend (String) -> Boolean,
    onErrorDismiss: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showPinDialogForDelete by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismiss()
        }
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pos_history_today_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pos_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Slate800,
                    navigationIconContentColor = Slate800,
                ),
                modifier = Modifier,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Filters are kept above the list so loading, empty, and populated
            // states all share the same query/payment context.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.pos_history_search_placeholder),
                            color = Slate400,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Brand500,
                        )
                    },
                    shape = AppShapes.M,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand500,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = Slate50,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    PaymentFilter.values().forEach { filter ->
                        FilterChip(
                            selected = uiState.paymentFilter == filter,
                            onClick = { onPaymentFilterChanged(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (uiState.paymentFilter == filter) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Brand500,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = Slate100,
                                labelColor = Slate600,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.paymentFilter == filter,
                                borderColor = if (uiState.paymentFilter == filter) Brand500 else Color.Transparent,
                            ),
                            shape = AppShapes.XL,
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate200, thickness = 1.dp)

            // Prefer one explicit branch per list state; it prevents loading,
            // empty, and populated UI from accidentally rendering together.
            if (uiState.isLoading && uiState.allTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Brand500)
                }
            } else if (uiState.displayedTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Slate300,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.pos_history_empty_today),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Slate600,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.displayedTransactions, key = { it.id }) { trx ->
                        TransactionHistoryCard(
                            transaction = trx,
                            onClick = { onTransactionClick(trx) },
                            onDeleteClick = { onDeleteClick(trx) },
                        )
                    }
                }
            }
        }

        // Detail and delete dialogs both read the ViewModel-selected
        // transaction. Avoid copying the transaction into local state here.
        if (uiState.showDetailDialog && uiState.selectedTransaction != null) {
            val selectedTransaction = uiState.selectedTransaction
            ZatDialog(onDismissRequest = onHideDetail) { dismiss ->
                TransactionDetailDialogContent(
                    transaction = selectedTransaction,
                    onDismiss = {
                        dismiss()
                        onHideDetail()
                    },
                    onUpdatePaymentMethod = onUpdatePaymentMethod,
                    onPrintReceiptClick = {
                        dismiss()
                        onPrintReceiptClick(selectedTransaction)
                    },
                )
            }
        }

        // Non-owner deletion is deferred to OwnerPinDialog; owners can confirm
        // directly from this warning dialog.
        if (uiState.showDeleteConfirmDialog && uiState.selectedTransaction != null) {
            ZatDialog(onDismissRequest = onHideDeleteConfirm) { dismiss ->
                DeleteConfirmationContent(
                    onDismiss = {
                        dismiss()
                        onHideDeleteConfirm()
                    },
                    onConfirm = {
                        if (!uiState.isOwner) {
                            showPinDialogForDelete = true
                        } else {
                            dismiss()
                            onDeleteConfirm()
                        }
                    },
                )
            }
        }

        if (showPinDialogForDelete) {
            OwnerPinDialog(
                onDismiss = { showPinDialogForDelete = false },
                onPinVerified = {
                    showPinDialogForDelete = false
                    onDeleteConfirm()
                },
                verifyPin = verifyPin,
            )
        }
    }
}

/**
 * Compact list row for one transaction.
 *
 * This row is intentionally presentation-only. Mutations such as delete or
 * payment updates should stay in the screen/ViewModel boundary above.
 */
@Composable
fun TransactionHistoryCard(
    transaction: Transaction,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.L,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Use a deterministic text avatar instead of loading customer
            // images in a dense transaction list.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.M)
                    .background(Slate50),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = transaction.customerName?.firstOrNull()?.toString()?.uppercase() ?: "#",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Brand500,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.customerName.takeUnless { it.isNullOrBlank() }
                        ?: transaction.transactionNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(transaction.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                    )
                    Text(
                        text = transaction.paymentMethod.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand500,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatCurrency(transaction.grandTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen,
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(com.zatiaras.pos.core.ui.R.string.btn_delete),
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Shows editable transaction metadata before receipt navigation.
 *
 * Payment method correction is allowed from history because cashiers often fix
 * tender mistakes after checkout; totals and line items remain immutable here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailDialogContent(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onUpdatePaymentMethod: (PaymentMethod) -> Unit,
    onPrintReceiptClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = AppShapes.XXL,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.pos_history_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Slate100, CircleShape),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(com.zatiaras.pos.core.ui.R.string.btn_close), modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Slate100)

            DetailRow(label = stringResource(R.string.pos_history_customer), value = transaction.customerName ?: "-")
            DetailRow(label = stringResource(R.string.pos_history_transaction_id), value = transaction.transactionNumber)
            DetailRow(label = stringResource(R.string.pos_history_date), value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(transaction.createdAt)))

            Spacer(modifier = Modifier.height(16.dp))

            // Payment method is the only editable field in this dialog.
            Text(
                text = stringResource(R.string.pos_history_payment_type),
                style = MaterialTheme.typography.labelMedium,
                color = Slate600,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = transaction.paymentMethod.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = AppShapes.M,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand500,
                        unfocusedBorderColor = Slate200,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    PaymentMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name.replace("_", " ")) },
                            onClick = {
                                expanded = false
                                if (method != transaction.paymentMethod) {
                                    onUpdatePaymentMethod(method)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50, AppShapes.M)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.checkout_total_payment_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = Brand500,
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(transaction.grandTotal),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Brand500,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPrintReceiptClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = AppShapes.M,
                colors = ButtonDefaults.buttonColors(containerColor = Brand500),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pos_history_view_receipt), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Simple label/value pair used inside dialogs where table layout would be too
 * dense on small screens.
 */
@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Slate600,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Slate900,
        )
    }
}

/**
 * Confirmation body only. Owner authorization is handled by the parent screen
 * so this component can stay reusable for owner and non-owner flows.
 */
@Composable
fun DeleteConfirmationContent(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        shape = AppShapes.XXL,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(WarningAmberBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.pos_history_delete_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pos_history_delete_message),
                textAlign = TextAlign.Center,
                color = Slate600,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = AppShapes.M,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                ) {
                    Text(stringResource(com.zatiaras.pos.core.ui.R.string.btn_cancel))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = AppShapes.M,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                ) {
                    Text(stringResource(com.zatiaras.pos.core.ui.R.string.btn_delete))
                }
            }
        }
    }
}
