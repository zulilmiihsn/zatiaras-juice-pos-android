package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

/**
 * Statistics section displaying 6 metrics in a grid layout.
 * Follows Material Design guidelines with proper spacing and typography.
 */
@Composable
fun StatisticsSection(
    averageTransactions: Int,
    peakHours: String,
    averageOrderValue: Long = 0,
    averageItemsPerTransaction: Double = 0.0,
    growthPercent: Double? = null,
    busiestDay: String = "-",
    modifier: Modifier = Modifier
) {
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Statistik",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Row 1: Rata-rata transaksi & Jam paling ramai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    value = if (averageTransactions > 0) averageTransactions.toString() else null,
                    label = "Rata-rata transaksi",
                    sublabel = "per hari",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = if (peakHours != "-") peakHours else null,
                    label = "Jam paling ramai",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Row 2: AOV & Items per transaksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    value = if (averageOrderValue > 0) formatCurrency.format(averageOrderValue) else null,
                    label = "Rata-rata nilai",
                    sublabel = "per transaksi",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = if (averageItemsPerTransaction > 0) String.format("%.1f", averageItemsPerTransaction) else null,
                    label = "Item per transaksi",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Row 3: Growth & Busiest day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(
                    value = growthPercent?.let { 
                        val sign = if (it >= 0) "+" else ""
                        "$sign${String.format("%.1f", it)}%"
                    },
                    label = "Pertumbuhan",
                    sublabel = "vs kemarin",
                    valueColor = growthPercent?.let { 
                        if (it >= 0) Color(0xFF10B981) else Color(0xFFEF4444) 
                    },
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = if (busiestDay != "-") busiestDay else null,
                    label = "Hari paling ramai",
                    sublabel = "minggu ini",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String?,
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    valueColor: Color? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (value != null) {
                // Has data - show value prominently
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = valueColor ?: MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            } else {
                // No data - show placeholder
                Text(
                    text = "Belum ada",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}





