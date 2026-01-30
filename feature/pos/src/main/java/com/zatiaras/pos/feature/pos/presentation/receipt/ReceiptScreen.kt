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
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.pos.domain.model.PaymentMethod
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import com.zatiaras.pos.feature.pos.domain.model.TransactionItem
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.zatiaras.pos.core.ui.theme.LocalDimensions

/**
 * Receipt Preview Screen shown after successful transaction.
 */
@Composable
fun ReceiptScreen(
    transaction: Transaction,
    onNewTransaction: () -> Unit,
    onPrintReceipt: () -> Unit,
    isPrinting: Boolean = false,
    isPrinterConnected: Boolean = false,
    printerName: String? = null,
    modifier: Modifier = Modifier
) {
    val priceFormatter = CurrencyFormatter.getCurrencyFormatter()
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    
    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        val dimensions = LocalDimensions.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(dimensions.paddingM),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
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
                        modifier = Modifier.padding(dimensions.paddingM)
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
                // Printer Status Indicator
                if (isPrinterConnected && printerName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Printer: $printerName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS)
                ) {
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPrinterConnected) Icons.Default.Print else Icons.Default.PrintDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isPrinting) "Mencetak..." 
                            else if (isPrinterConnected) "Cetak Struk" 
                            else "Setup Printer"
                        )
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
