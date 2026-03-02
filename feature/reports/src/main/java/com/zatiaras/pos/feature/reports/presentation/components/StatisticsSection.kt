package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ErrorRed
import com.zatiaras.pos.core.ui.theme.SuccessGreen
import com.zatiaras.pos.core.ui.util.CurrencyFormatter

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
    val formatCurrency = CurrencyFormatter.getCurrencyFormatter()
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.paddingL)
        ) {
            Text(
                text = stringResource(R.string.reports_stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(dimensions.spacingM))
            
            // Row 1: Rata-rata transaksi & Jam paling ramai
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)
            ) {
                StatItem(
                    value = if (averageTransactions > 0) averageTransactions.toString() else null,
                    label = stringResource(R.string.reports_avg_items),
                    sublabel = stringResource(R.string.reports_per_day),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                StatItem(
                    value = if (peakHours != "-") peakHours else null,
                    label = stringResource(R.string.reports_peak_hour),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingM))
            
            // Row 2: AOV & Items per transaksi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)
            ) {
                StatItem(
                    value = if (averageOrderValue > 0) formatCurrency.format(averageOrderValue) else null,
                    label = stringResource(R.string.reports_avg_value_label),
                    sublabel = stringResource(R.string.reports_per_transaction),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                StatItem(
                    value = if (averageItemsPerTransaction > 0) String.format("%.1f", averageItemsPerTransaction) else null,
                    label = stringResource(R.string.reports_avg_items_label),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingM))
            
            // Row 3: Growth & Busiest day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingM)
            ) {
                StatItem(
                    value = growthPercent?.let { 
                        val sign = if (it >= 0) "+" else ""
                        "$sign${String.format("%.1f", it)}%"
                    },
                    label = stringResource(R.string.reports_growth_label),
                    sublabel = stringResource(R.string.reports_vs_yesterday),
                    valueColor = growthPercent?.let { 
                        if (it >= 0) SuccessGreen else ErrorRed 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                StatItem(
                    value = if (busiestDay != "-") busiestDay else null,
                    label = stringResource(R.string.reports_peak_day),
                    sublabel = stringResource(R.string.reports_this_week),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
    val dimensions = LocalDimensions.current
    Box(
        modifier = modifier
            .clip(AppShapes.L)
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxHeight()
            .padding(horizontal = dimensions.paddingM, vertical = dimensions.paddingL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (value != null) {
                // Has data - show value prominently
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor ?: MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                // No data - show placeholder
                Text(
                    text = stringResource(R.string.reports_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(dimensions.spacingXS))
            
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





