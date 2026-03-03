package com.zatiaras.pos.feature.pos.presentation.cashrecord

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ExpenseRed
import com.zatiaras.pos.core.ui.theme.IncomeGreen
import com.zatiaras.pos.feature.pos.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashFlowItemRow(
    item: CashFlowItem,
    priceFormatter: NumberFormat,
    timeFormatter: SimpleDateFormat,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val canDelete = item is CashFlowItem.FromCashRecord
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && canDelete) {
                showDeleteDialog = true
            }
            false
        }
    )
    
    if (canDelete) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color by animateColorAsState(
                    targetValue = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> ExpenseRed
                        else -> Color.Transparent
                    },
                    label = "swipe_color"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, AppShapes.M)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cash_record_delete),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            enableDismissFromStartToEnd = false
        ) {
            CashFlowItemCard(item, priceFormatter, timeFormatter, onClick)
        }
    } else {
        CashFlowItemCard(item, priceFormatter, timeFormatter, onClick)
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        ZatDialog(
            onDismissRequest = { showDeleteDialog = false }
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
                        text = stringResource(R.string.cash_record_delete_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.cash_record_delete_message),
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
                            Text(stringResource(R.string.dialog_cancel))
                        }
                        
                        Button(
                            onClick = {
                                onDelete()
                                dismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = AppShapes.M
                        ) {
                            Text(stringResource(R.string.cash_record_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowItemCard(
    item: CashFlowItem,
    priceFormatter: NumberFormat,
    timeFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val isTransactionItem = item is CashFlowItem.FromTransaction
    val iconColor = if (item.isIncome) IncomeGreen else ExpenseRed
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Surface(
                shape = AppShapes.S,
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = when {
                        isTransactionItem -> Icons.Default.ShoppingCart
                        item.isIncome -> Icons.AutoMirrored.Filled.TrendingUp
                        else -> Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                if (item is CashFlowItem.FromTransaction) {
                    val trxItem = item.transaction
                    Text(
                        text = stringResource(R.string.cash_flow_sales),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trxItem.transactionNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show item details
                    val detailsText = trxItem.items.take(2).joinToString(", ") { "${it.productName} x${it.quantity}" } + if (trxItem.items.size > 2) ", dll..." else ""
                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    // Show transaction notes 
                    if (!trxItem.notes.isNullOrBlank()) {
                        Text(
                            text = trxItem.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = timeFormatter.format(Date(item.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show item count for transactions
                    if (item is CashFlowItem.FromTransaction) {
                        Text(
                            text = stringResource(R.string.cash_flow_item_count, item.itemCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show category for manual records
                    if (item is CashFlowItem.FromCashRecord && !item.category.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.cash_flow_category, item.category.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Amount
            Text(
                text = stringResource(
                    R.string.cash_flow_amount_signed,
                    if (item.isIncome) "+" else "-",
                    priceFormatter.format(item.amount)
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}
