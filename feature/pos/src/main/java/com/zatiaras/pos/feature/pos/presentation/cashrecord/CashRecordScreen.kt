package com.zatiaras.pos.feature.pos.presentation.cashrecord

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.zatiaras.pos.feature.pos.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.core.domain.model.DatePeriod
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.DateFilterRow
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ExpenseRed
import com.zatiaras.pos.core.ui.theme.IncomeGreen
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import com.zatiaras.pos.feature.pos.domain.model.Transaction
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cash Record (Buku Kas) screen.
 * 
 * Displays both POS transactions and manual cash records in one unified view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRecordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReceipt: (Transaction) -> Unit,
    viewModel: CashRecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMsg = stringResource(R.string.cash_record_saved)
    val dimensions = LocalDimensions.current
    
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedManualRecord by remember { mutableStateOf<CashFlowItem.FromCashRecord?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    val priceFormatter = CurrencyFormatter.getCurrencyFormatter()
    val timeFormatter = SimpleDateFormat("HH:mm", LocaleUtils.LOCALE_ID)
    
    // Listen for save success
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { success ->
            if (success) {
                showAddSheet = false
                snackbarHostState.showSnackbar(successMsg)
            }
        }
    }
    
    // Show error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(CashRecordEvent.DismissError)
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.cash_record_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.resetForm()
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cash_record_add))
            }
        },
        snackbarHost = { com.zatiaras.pos.core.ui.components.ZatSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Card
            CashSummaryCard(
                totalIncome = uiState.summary.totalIncome,
                totalExpense = uiState.summary.totalExpense,
                netCash = uiState.summary.netCash,
                posTransactionCount = uiState.posTransactionCount,
                priceFormatter = priceFormatter,
                modifier = Modifier.padding(dimensions.paddingM)
            )
            
            // Shared Date Filter Component
            val selectedPeriod by viewModel.selectedDatePeriod.collectAsState()
            
            // Logic to handle date filter events
            var showDateRangePicker by remember { mutableStateOf(false) }
            
            // Note: DateFilterRow component from core:ui implements its own specific UI.
            // We use it here to ensure consistency with Reports screen.
            DateFilterRow(
                startDate = uiState.customStartDate,
                endDate = uiState.customEndDate,
                activePeriod = selectedPeriod,
                onStartDateClick = { 
                    showDateRangePicker = true 
                },
                onEndDateClick = { showDateRangePicker = true },
                onQuickPeriodSelected = { period ->
                    viewModel.onEvent(CashRecordEvent.SetDateFilter(period))
                },
                modifier = Modifier.padding(horizontal = dimensions.paddingM, vertical = dimensions.spacingXS)
            )

            // Date Picker Dialog Logic
            if (showDateRangePicker) {
               com.zatiaras.pos.core.ui.components.DateRangePickerDialog(
                   onDismiss = { showDateRangePicker = false },
                   onConfirm = { start, end ->
                       viewModel.onEvent(CashRecordEvent.SetDateFilter(DatePeriod.CUSTOM, start, end))
                       showDateRangePicker = false
                   }
               )
            }
            
            // Records List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSizeXXL),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(dimensions.paddingM))
                        Text(
                            text = stringResource(R.string.cash_record_empty),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.cash_record_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(dimensions.paddingM),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingXS)
                ) {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { item ->
                        CashFlowItemRow(
                            item = item,
                            priceFormatter = priceFormatter,
                            timeFormatter = timeFormatter,
                            onDelete = {
                                viewModel.onEvent(CashRecordEvent.DeleteRecord(item.id))
                            },
                            onClick = {
                                if (item is CashFlowItem.FromTransaction) {
                                    onNavigateToReceipt(item.transaction)
                                } else if (item is CashFlowItem.FromCashRecord) {
                                    selectedManualRecord = item
                                }
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(dimensions.iconSizeXXL))
                    }
                }
            }
        }
    }
    
    // Add Sheet
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            AddCashRecordSheet(
                formState = formState,
                onEvent = viewModel::onEvent,
                onCancel = {
                    scope.launch {
                        sheetState.hide()
                        showAddSheet = false
                    }
                }
            )
        }
    }

    // Detail Dialog for Manual Records
    if (selectedManualRecord != null) {
        val record = selectedManualRecord!!
        ZatDialog(
            onDismissRequest = { selectedManualRecord = null }
        ) { dismiss ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = AppShapes.XXL,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = AppShapes.M,
                            color = (if (record.isIncome) IncomeGreen else ExpenseRed).copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = if (record.isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (record.isIncome) IncomeGreen else ExpenseRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (record.isIncome) {
                                stringResource(R.string.cash_record_detail_income_title)
                            } else {
                                stringResource(R.string.cash_record_detail_expense_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.cash_record_detail_description_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (!record.category.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.cash_record_detail_category_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record.category!!,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cash_record_detail_time_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", LocaleUtils.LOCALE_ID).format(Date(record.createdAt))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = (if (record.isIncome) IncomeGreen else ExpenseRed).copy(alpha = 0.1f),
                                shape = AppShapes.M
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                R.string.cash_flow_amount_signed,
                                if (record.isIncome) "+" else "-",
                                priceFormatter.format(record.amount)
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (record.isIncome) IncomeGreen else ExpenseRed
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = dismiss,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.cash_record_close), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
