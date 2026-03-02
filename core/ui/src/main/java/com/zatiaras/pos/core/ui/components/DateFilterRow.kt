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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.core.ui.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zatiaras.pos.core.domain.model.DatePeriod
import com.zatiaras.pos.core.domain.util.LocaleUtils
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
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocaleUtils.LOCALE_ID)
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.core_periode),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        // === PRIMARY: Date Range Selector ===
        DateRangeRow(
            startDate = startDate?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.core_pilih_tanggal),
            endDate = endDate?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.core_pilih_tanggal),
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
            label = stringResource(R.string.core_dari),
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
            label = stringResource(R.string.core_sampai),
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
    val isPlaceholder = dateText == stringResource(R.string.core_pilih_tanggal)
    
    Box(
        modifier = modifier
            .clip(AppShapes.M)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = AppShapes.M
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
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", LocaleUtils.LOCALE_ID)
    
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.XXL),
                shape = AppShapes.XXL,
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.core_rentang_tanggal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.core_tentukan_periode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Date Selection Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.core_dari),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            OutlinedButton(
                                onClick = { showStartPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.M,
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Text(
                                    startDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(R.string.core_mulai),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.core_sampai),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            OutlinedButton(
                                onClick = { showEndPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.M,
                                enabled = startDate != null,
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Text(
                                    endDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(R.string.core_selesai),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (startDate == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.core_pilih_tgl_mulai_dulu),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = dismiss,
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.M,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(stringResource(R.string.core_batal), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (startDate != null && endDate != null) {
                                    onConfirm(startDate!!, endDate!!)
                                    dismiss()
                                }
                            },
                            enabled = startDate != null && endDate != null,
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.M
                        ) {
                            Text(stringResource(R.string.core_terapkan), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
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
                    Text(stringResource(R.string.core_batal))
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
                    Text(stringResource(R.string.core_batal))
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}
