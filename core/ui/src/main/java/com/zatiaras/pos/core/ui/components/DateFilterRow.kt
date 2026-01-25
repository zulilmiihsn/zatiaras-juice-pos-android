package com.zatiaras.pos.core.ui.components

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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.domain.model.DatePeriod
import com.zatiaras.pos.core.ui.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable date filter component with date range picker and quick period chips.
 * 
 * Used by:
 * - PnlReportScreen (Laporan Laba Rugi)
 * - CashRecordScreen (Buku Kas)
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
    activePeriod: DatePeriod?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onQuickPeriodSelected: (DatePeriod) -> Unit,
    modifier: Modifier = Modifier,
    quickPeriods: List<DatePeriod> = listOf(
        DatePeriod.TODAY,
        DatePeriod.THIS_WEEK,
        DatePeriod.THIS_MONTH,
        DatePeriod.LAST_7_DAYS,
        DatePeriod.LAST_30_DAYS
    )
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Periode",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        // === PRIMARY: Date Range Selector ===
        DateRangeRow(
            startDate = startDate?.let { dateFormat.format(Date(it)) } ?: "Pilih Tanggal",
            endDate = endDate?.let { dateFormat.format(Date(it)) } ?: "Pilih Tanggal",
            onStartDateClick = onStartDateClick,
            onEndDateClick = onEndDateClick
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // === SECONDARY: Quick Period Chips ===
        QuickPeriodChips(
            activePeriod = activePeriod,
            quickPeriods = quickPeriods,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            modifier = Modifier.size(18.dp)
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
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
    activePeriod: DatePeriod?,
    quickPeriods: List<DatePeriod>,
    onPeriodSelected: (DatePeriod) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Pilih Rentang Tanggal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // Start Date
                Text(
                    "Dari:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        startDate?.let { dateFormatter.format(Date(it)) } ?: "Pilih tanggal mulai",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // End Date
                Text(
                    "Sampai:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = startDate != null
                ) {
                    Text(
                        endDate?.let { dateFormatter.format(Date(it)) } ?: "Pilih tanggal selesai",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (startDate == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Pilih tanggal mulai terlebih dahulu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (startDate != null && endDate != null) {
                        onConfirm(startDate!!, endDate!!)
                    }
                },
                enabled = startDate != null && endDate != null
            ) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
    
    // Start Date Picker Dialog
    if (showStartPicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = startDate ?: System.currentTimeMillis()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            startDate = selectedDate
                            if (endDate != null && endDate!! < selectedDate) {
                                endDate = null
                            }
                        }
                        showStartPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showStartPicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
    
    // End Date Picker Dialog
    if (showEndPicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = endDate ?: startDate ?: System.currentTimeMillis()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            if (startDate != null && selectedDate >= startDate!!) {
                                endDate = selectedDate
                            }
                        }
                        showEndPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEndPicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}
