package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zatiaras.pos.feature.reports.domain.model.ProfitLossReport

/**
 * Comprehensive P&L breakdown card.
 */
@Composable
fun PnlBreakdownCard(
    report: ProfitLossReport,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Text(
                text = "Ringkasan Laba Rugi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${report.transactionCount} transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // --- 1. INCOME SECTION ---
            SectionHeader(
                title = "PENDAPATAN",
                color = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PnlLineItem(
                label = "Pendapatan Usaha",
                amount = report.operatingRevenue,
                icon = Icons.Default.ArrowUpward,
                iconColor = Color(0xFF4CAF50)
            )
            
            if (report.otherRevenue > 0) {
                PnlLineItem(
                    label = "Pendapatan Lainnya",
                    amount = report.otherRevenue,
                    icon = Icons.Default.ArrowUpward,
                    iconColor = Color(0xFF8BC34A)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            PnlLineItem(
                label = "Total Pendapatan",
                amount = report.grossRevenue,
                isBold = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- 2. EXPENSE SECTION ---
            SectionHeader(
                title = "BEBAN / PENGELUARAN",
                color = Color(0xFFE53935)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PnlLineItem(
                label = "Beban Usaha",
                amount = report.operatingExpenses,
                icon = Icons.Default.ArrowDownward,
                iconColor = Color(0xFFE53935),
                isNegative = true
            )
            
            if (report.otherExpenses > 0) {
                PnlLineItem(
                    label = "Beban Lainnya",
                    amount = report.otherExpenses,
                    icon = Icons.Default.ArrowDownward,
                    iconColor = Color(0xFFFF5722),
                    isNegative = true
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            PnlLineItem(
                label = "Total Beban",
                amount = -report.totalExpenses, // Display as negative
                isBold = true,
                isNegative = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- 3. PROFIT / TAX SECTION ---
            SectionHeader(
                title = "LABA & PAJAK",
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PnlLineItem(
                label = "Laba Kotor",
                amount = report.grossProfit,
                isBold = true
            )

            PnlLineItem(
                label = "Pajak (0.5% Omzet)",
                amount = report.tax, 
                icon = Icons.Default.Remove,
                iconColor = Color(0xFF2196F3),
                isNegative = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ProfitRow(
                label = "Laba Bersih",
                amount = report.netProfit,
                isProfit = report.netProfit >= 0
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PnlLineItem(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isBold: Boolean = false,
    isNegative: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = if (isNegative && amount != 0L) {
                "(${formatRupiah(kotlin.math.abs(amount))})"
            } else {
                formatRupiah(amount)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isNegative) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GrandTotalRow(
    amount: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Diterima",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProfitRow(
    label: String,
    amount: Long,
    isProfit: Boolean
) {
    val backgroundColor = if (isProfit) {
        Color(0xFF4CAF50).copy(alpha = 0.1f)
    } else {
        Color(0xFFE53935).copy(alpha = 0.1f)
    }
    
    val textColor = if (isProfit) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFC62828)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isProfit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
