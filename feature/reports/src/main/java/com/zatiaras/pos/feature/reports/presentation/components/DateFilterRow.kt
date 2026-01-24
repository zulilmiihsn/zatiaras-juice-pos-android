package com.zatiaras.pos.feature.reports.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A user-friendly date filter component that prioritizes direct date selection.
 * 
 * Features:
 * - Date range is always visible and clickable (primary interaction)
 * - Quick period chips below as shortcuts
 * - Active chip is determined by matching date range
 */
@Composable
fun DateFilterRow(
    startDate: Long?,
    endDate: Long?,
    activePeriod: ReportPeriod?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onQuickPeriodSelected: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Periode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // === PRIMARY: Date Range Selector ===
        DateRangeRow(
            startDate = startDate?.let { dateFormat.format(Date(it)) } ?: "Pilih Tanggal",
            endDate = endDate?.let { dateFormat.format(Date(it)) } ?: "Pilih Tanggal",
            onStartDateClick = onStartDateClick,
            onEndDateClick = onEndDateClick
        )
        
        Spacer(modifier = Modifier.height(dimensions.spacingM))
        
        // === SECONDARY: Quick Period Chips ===
        
        QuickPeriodChips(
            activePeriod = activePeriod,
            onPeriodSelected = onQuickPeriodSelected
        )
    }
}

@Composable
private fun DateRangeRow(
    startDate: String,
    endDate: String,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start Date Button
        DatePickerButton(
            label = "Dari",
            dateText = startDate,
            onClick = onStartDateClick,
            modifier = Modifier.weight(1f)
        )
        
        // Arrow/Dash Separator
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        // End Date Button
        DatePickerButton(
            label = "Sampai",
            dateText = endDate,
            onClick = onEndDateClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DatePickerButton(
    label: String,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaceholder = dateText == "Pilih Tanggal"
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isPlaceholder) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickPeriodChips(
    activePeriod: ReportPeriod?,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    val dimensions = LocalDimensions.current
    
    // Exclude CUSTOM since it's now the default behavior
    val quickPeriods = listOf(
        ReportPeriod.TODAY,
        ReportPeriod.THIS_WEEK,
        ReportPeriod.THIS_MONTH,
        ReportPeriod.LAST_7_DAYS,
        ReportPeriod.LAST_30_DAYS
    )
    
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingXS)
    ) {
        quickPeriods.forEach { period ->
            QuickPeriodChip(
                label = period.toDisplayName(),
                isActive = period == activePeriod,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

@Composable
private fun QuickPeriodChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(200),
        label = "chipBackground"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "chipText"
    )
    
    val borderColor = if (isActive) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    
    Box(
        modifier = Modifier
            .clip(AppShapes.XL)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = AppShapes.XL
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}
