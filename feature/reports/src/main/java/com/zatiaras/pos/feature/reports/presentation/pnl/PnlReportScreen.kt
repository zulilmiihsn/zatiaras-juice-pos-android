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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zatiaras.pos.feature.reports.R
import androidx.compose.ui.draw.clip
import com.zatiaras.pos.core.ui.theme.PdfRed
import com.zatiaras.pos.core.ui.theme.AppShapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zatiaras.pos.core.data.access.AccessControlManager
import com.zatiaras.pos.core.data.access.LockableRoute
import com.zatiaras.pos.core.ui.components.AccessControlGate
import com.zatiaras.pos.core.ui.components.DateFilterRow
import com.zatiaras.pos.feature.reports.domain.model.ReportPeriod
import com.zatiaras.pos.feature.reports.presentation.components.PnlBreakdownCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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
        screenName = stringResource(R.string.pnl_title),
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
                    val message = context.getString(R.string.export_saved, event.fileName)
                    Toast.makeText(
                        context, 
                        message, 
                        Toast.LENGTH_LONG
                    ).show()
                }
                is ExportEvent.ShareFile -> {
                    val prefix = context.getString(R.string.export_share_prefix, event.fileName)
                    val chooserIntent = android.content.Intent.createChooser(
                        event.intent,
                        prefix
                    )
                    context.startActivity(chooserIntent)
                }
                is ExportEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Izin diberikan, silakan tekan Export sekali lagi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin ditolak, file tidak otomatis tersimpan. Silakan simpan manual dari menu Share", Toast.LENGTH_LONG).show()
        }
    }

    val checkAndExportPdf = {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && 
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.exportToPdf(context)
        }
    }

    val checkAndExportCsv = {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && 
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.exportToCsv(context)
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
        onRangeSelected = viewModel::setCustomRange,
        onExportPdf = checkAndExportPdf,
        onExportCsv = checkAndExportCsv
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
    onShowDatePicker: () -> Unit,
    onHideDatePicker: () -> Unit,
    onRangeSelected: (Long, Long) -> Unit,
    onExportPdf: () -> Unit = {},
    onExportCsv: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Date range picker dialog
    if (uiState.showDatePicker) {
        com.zatiaras.pos.core.ui.components.DateRangePickerDialog(
            onDismiss = onHideDatePicker,
            onConfirm = { start, end ->
                onRangeSelected(start, end)
            }
        )
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reports_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.reports_back)
                            )
                        }
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
                text = { Text(stringResource(R.string.pnl_ask_ai)) }
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
            val dimensions = LocalDimensions.current
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(dimensions.paddingM),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
            ) {
                // Date Filter Row (always visible date range + quick period chips)
                item {
                    DateFilterRow(
                        startDate = uiState.customStartDate,
                        endDate = uiState.customEndDate,
                        activePeriod = if (uiState.selectedPeriod != ReportPeriod.CUSTOM) uiState.selectedPeriod else null,
                        onStartDateClick = onShowDatePicker,
                        onEndDateClick = onShowDatePicker,
                        onQuickPeriodSelected = onPeriodSelected
                    )
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
                                .clip(AppShapes.M)
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
    var showExportMenu by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.export_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Single Export Button with Dropdown
        Box {
            Button(
                onClick = { showExportMenu = true },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.export_exporting))
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.export_title))
                }
            }
            
            // Export Format Dropdown Menu
            androidx.compose.material3.DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false }
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = PdfRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.export_pdf))
                        }
                    },
                    onClick = {
                        showExportMenu = false
                        onExportPdf()
                    }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.export_csv))
                        }
                    },
                    onClick = {
                        showExportMenu = false
                        onExportCsv()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stringResource(R.string.export_save_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}


