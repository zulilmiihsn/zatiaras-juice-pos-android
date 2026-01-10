package com.zatiaras.pos.feature.pos.presentation.receipt

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.TransactionItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receipt Preview Screen shown after successful transaction.
 */
@Composable
fun ReceiptScreen(
    transaction: Transaction,
    onNewTransaction: () -> Unit,
    onPrintReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priceFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    
    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Transaksi Berhasil!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = transaction.transactionNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Receipt Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Store Header (placeholder)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ZATIARAS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateFormatter.format(Date(transaction.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Items List
                        transaction.items.forEach { item ->
                            ReceiptItemRow(
                                item = item,
                                priceFormatter = priceFormatter
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Totals
                        ReceiptTotalRow(
                            label = "Subtotal",
                            value = priceFormatter.format(transaction.subtotal)
                        )
                        
                        if (transaction.discountAmount > 0) {
                            ReceiptTotalRow(
                                label = "Diskon (${transaction.discountPercent.toInt()}%)",
                                value = "-${priceFormatter.format(transaction.discountAmount)}",
                                isDiscount = true
                            )
                        }
                        
                        if (transaction.taxAmount > 0) {
                            ReceiptTotalRow(
                                label = "PPN (${transaction.taxPercent.toInt()}%)",
                                value = priceFormatter.format(transaction.taxAmount)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ReceiptTotalRow(
                            label = "TOTAL",
                            value = priceFormatter.format(transaction.grandTotal),
                            isBold = true,
                            isPrimary = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Payment Info
                        ReceiptTotalRow(
                            label = "Metode Bayar",
                            value = transaction.paymentMethod.displayName
                        )
                        
                        ReceiptTotalRow(
                            label = "Dibayar",
                            value = priceFormatter.format(transaction.amountPaid)
                        )
                        
                        if (transaction.paymentMethod == PaymentMethod.CASH && transaction.changeAmount > 0) {
                            ReceiptTotalRow(
                                label = "Kembalian",
                                value = priceFormatter.format(transaction.changeAmount),
                                isPrimary = true
                            )
                        }
                        
                        // Notes
                        if (!transaction.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DashedDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Catatan:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = transaction.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Footer
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Terima kasih atas kunjungan Anda!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Struk")
                    }
                    
                    Button(
                        onClick = onNewTransaction,
                        modifier = Modifier.weight(1f),
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
                        Text("Transaksi Baru")
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReceiptItemRow(
    item: TransactionItem,
    priceFormatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${item.quantity}x ${priceFormatter.format(item.productPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = priceFormatter.format(item.subtotal),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReceiptTotalRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isPrimary: Boolean = false,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isDiscount -> MaterialTheme.colorScheme.error
                isPrimary -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun DashedDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
