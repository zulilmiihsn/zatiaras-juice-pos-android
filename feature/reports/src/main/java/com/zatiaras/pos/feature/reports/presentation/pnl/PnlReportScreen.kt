package com.zatiaras.pos.feature.reports.presentation.pnl

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.data.access.LockableRoute
import com.zatiaras.pos.core.ui.components.AccessControlGate
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import com.zatiaras.pos.feature.reports.presentation.components.PeriodSelector
import com.zatiaras.pos.feature.reports.presentation.components.PnlBreakdownCard
import com.zatiaras.pos.feature.reports.presentation.components.toDisplayName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PnlReportRoute(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToChat: () -> Unit = {},
    accessControlManager: AccessControlManager,
    viewModel: PnlReportViewModel = hiltViewModel()
) {
    // Wrap with access control gate
    AccessControlGate(
        accessControlManager = accessControlManager,
        route = LockableRoute.PNL_REPORT.route,
        screenName = "Laporan Laba Rugi",
        onAccessDenied = { onNavigateBack?.invoke() }
    ) {
        PnlReportContent(
            onNavigateBack = onNavigateBack,
            onNavigateToChat = onNavigateToChat,
            viewModel = viewModel
        )
    }
}

/**
 * Overload for backward compatibility when access control is not needed.
 */
@Composable
fun PnlReportRoute(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToChat: () -> Unit = {},
    viewModel: PnlReportViewModel = hiltViewModel()
) {
    PnlReportContent(
        onNavigateBack = onNavigateBack,
        onNavigateToChat = onNavigateToChat,
        viewModel = viewModel
    )
}

@Composable
private fun PnlReportContent(
    onNavigateBack: (() -> Unit)?,
    onNavigateToChat: () -> Unit,
    viewModel: PnlReportViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle export events
    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { event ->
            when (event) {
                is ExportEvent.SavedToDownloads -> {
                    Toast.makeText(
                        context, 
                        "File tersimpan: ${event.fileName}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                is ExportEvent.ShareFile -> {
                    val chooserIntent = android.content.Intent.createChooser(
                        event.intent,
                        "Bagikan ${event.fileName}"
                    )
                    context.startActivity(chooserIntent)
                }
                is ExportEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    PnlReportScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToChat = onNavigateToChat,
        onPeriodSelected = viewModel::selectPeriod,
        onRefresh = viewModel::loadReport,
        onShowDatePicker = viewModel::showDatePicker,
        onHideDatePicker = viewModel::hideDatePicker,
        onDateSelected = viewModel::setCustomDate,
        onExportPdf = { viewModel.exportToPdf(context) },
        onExportCsv = { viewModel.exportToCsv(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnlReportScreen(
    uiState: PnlReportUiState,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToChat: () -> Unit = {},
    onPeriodSelected: (ReportPeriod) -> Unit,
    onRefresh: () -> Unit,
    onShowDatePicker: (Boolean) -> Unit,
    onHideDatePicker: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onExportPdf: () -> Unit = {},
    onExportCsv: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Date picker dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (uiState.isSelectingStartDate) {
                uiState.customStartDate ?: System.currentTimeMillis()
            } else {
                uiState.customEndDate ?: System.currentTimeMillis()
            }
        )
        
        DatePickerDialog(
            onDismissRequest = onHideDatePicker,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onHideDatePicker) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = if (uiState.isSelectingStartDate) "Pilih Tanggal Mulai" else "Pilih Tanggal Akhir",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Laporan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Tanya AI") }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period Selector
                item {
                    Column {
                        Text(
                            text = "Periode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        PeriodSelector(
                            selectedPeriod = uiState.selectedPeriod,
                            onPeriodSelected = onPeriodSelected
                        )
                    }
                }
                
                // Custom Date Range (if CUSTOM period selected)
                if (uiState.selectedPeriod == ReportPeriod.CUSTOM) {
                    item {
                        CustomDateRangeSelector(
                            startDate = uiState.customStartDate,
                            endDate = uiState.customEndDate,
                            onSelectStartDate = { onShowDatePicker(true) },
                            onSelectEndDate = { onShowDatePicker(false) }
                        )
                    }
                }
                
                // Period Info
                item {
                    PeriodInfoCard(uiState)
                }
                
                // Loading State
                if (uiState.isLoading && uiState.report == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                // P&L Breakdown
                uiState.report?.let { report ->
                    item {
                        AnimatedVisibility(
                            visible = !uiState.isLoading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            PnlBreakdownCard(report = report)
                        }
                    }
                    
                    // Export Buttons
                    item {
                        ExportSection(
                            isExporting = uiState.isExporting,
                            onExportPdf = onExportPdf,
                            onExportCsv = onExportCsv
                        )
                    }
                }
                
                // Error State
                uiState.error?.let { error ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
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
}

@Composable
private fun CustomDateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DateButton(
            label = "Dari",
            date = startDate?.let { dateFormat.format(Date(it)) } ?: "Pilih Tanggal",
            onClick = onSelectStartDate,
            modifier = Modifier.weight(1f)
        )
        
        DateButton(
            label = "Sampai",
            date = endDate?.let { dateFormat.format(Date(it)) } ?: "Pilih Tanggal",
            onClick = onSelectEndDate,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateButton(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PeriodInfoCard(uiState: PnlReportUiState) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    
    val periodText = when (uiState.selectedPeriod) {
        ReportPeriod.CUSTOM -> {
            val start = uiState.customStartDate?.let { dateFormat.format(Date(it)) } ?: "-"
            val end = uiState.customEndDate?.let { dateFormat.format(Date(it)) } ?: "-"
            "$start - $end"
        }
        else -> uiState.selectedPeriod.toDisplayName()
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(12.dp)
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
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Periode Laporan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExportSection(
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Export Laporan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PDF Export Button
            Button(
                onClick = onExportPdf,
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("PDF")
            }
            
            // CSV/Excel Export Button
            OutlinedButton(
                onClick = onExportCsv,
                enabled = !isExporting,
                modifier = Modifier.weight(1f)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Excel/CSV")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "File akan tersimpan di folder aplikasi",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

