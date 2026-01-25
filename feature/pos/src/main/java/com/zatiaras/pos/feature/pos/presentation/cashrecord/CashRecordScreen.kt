package com.zatiaras.pos.feature.pos.presentation.cashrecord

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.core.ui.theme.ExpenseRed
import com.zatiaras.pos.core.ui.theme.IncomeGreen
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.zatiaras.pos.core.domain.model.DatePeriod
import com.zatiaras.pos.core.ui.components.DateFilterRow

/**
 * Cash Record (Buku Kas) screen.
 * 
 * Displays both POS transactions and manual cash records in one unified view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRecordScreen(
    onNavigateBack: () -> Unit,
    viewModel: CashRecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    val priceFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    val timeFormatter = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    
    // Listen for save success
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { success ->
            if (success) {
                showAddSheet = false
                snackbarHostState.showSnackbar("Data berhasil disimpan")
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
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Buku Kas",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                modifier = Modifier.padding(16.dp)
            )
            
            // Shared Date Filter Component
            val selectedPeriod by viewModel.selectedDatePeriod.collectAsState()
            
            // Logic to handle date filter events
            var showDateRangePicker by remember { mutableStateOf(false) }
            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            
            // Since the new DateFilterRow component requires explicit date range values and handlers,
            // we use the component but adapt it to use the ViewModel's state and events
            
            // Note: DateFilterRow component from core:ui implements its own specific UI.
            // We use it here to ensure consistency with Reports screen.
            DateFilterRow(
                startDate = uiState.customStartDate,
                endDate = uiState.customEndDate,
                activePeriod = selectedPeriod,
                onStartDateClick = { 
                    // To simplify, we trigger the custom date period selection which will show picker in the reusable component if needed.
                    // However, the reusable component DateFilterRow handles start/end date clicks by calling the callback.
                    // The actual date picking logic is not embedded in the component itself (it's stateless regarding picker dialog).
                    showDateRangePicker = true 
                },
                onEndDateClick = { showDateRangePicker = true },
                onQuickPeriodSelected = { period ->
                    viewModel.onEvent(CashRecordEvent.SetDateFilter(period))
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Date Picker Dialog Logic (since we removed local implementation, we need a way to pick dates if needed)
            if (showDateRangePicker) {
               // We need a DatePickerDialog here. 
               // For now, let's use the DateRangePickerDialog from core:ui logic if we want to be fully reused,
               // but DateFilterRow is just a UI component.
               // We should implement a picker dialog or use a shared one.
               // To keep it simple and consistent, we can reuse 'androidx.compose.material3.DatePickerDialog' directly here.
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
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum ada catatan hari ini",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Transaksi POS otomatis tercatat di sini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val dimensions = LocalDimensions.current
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
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
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
                priceFormatter = priceFormatter,
                onCancel = {
                    scope.launch {
                        sheetState.hide()
                        showAddSheet = false
                    }
                }
            )
        }
    }
}

/**
 * Compact summary card with colored backgrounds.
 * Maintains visual clarity with reduced spacing for efficiency.
 */
@Composable
private fun CashSummaryCard(
    totalIncome: Long,
    totalExpense: Long,
    netCash: Long,
    posTransactionCount: Int,
    priceFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Compact Header
            Text(
                text = "Ringkasan Hari Ini ($posTransactionCount Transaksi)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Income and Expense in compact colored cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MASUK Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = IncomeGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MASUK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = priceFormatter.format(totalIncome),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
                
                // KELUAR Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "KELUAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = priceFormatter.format(totalExpense),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtle divider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // SALDO - Compact and clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saldo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = priceFormatter.format(netCash),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (netCash >= 0) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashFlowItemRow(
    item: CashFlowItem,
    priceFormatter: NumberFormat,
    timeFormatter: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val canDelete = item is CashFlowItem.FromCashRecord
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && canDelete) {
                showDeleteDialog = true
            }
            false
        }
    )
    
    if (canDelete) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color by animateColorAsState(
                    targetValue = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> ExpenseRed
                        else -> androidx.compose.ui.graphics.Color.Transparent
                    },
                    label = "swipe_color"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            },
            enableDismissFromStartToEnd = false
        ) {
            CashFlowItemCard(item, priceFormatter, timeFormatter)
        }
    } else {
        CashFlowItemCard(item, priceFormatter, timeFormatter)
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Catatan") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun CashFlowItemCard(
    item: CashFlowItem,
    priceFormatter: NumberFormat,
    timeFormatter: SimpleDateFormat
) {
    val isTransactionItem = item is CashFlowItem.FromTransaction
    val iconColor = if (item.isIncome) IncomeGreen else ExpenseRed
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = when {
                        isTransactionItem -> Icons.Default.ShoppingCart
                        item.isIncome -> Icons.AutoMirrored.Filled.TrendingUp
                        else -> Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        text = timeFormatter.format(Date(item.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show item count for transactions
                    if (item is CashFlowItem.FromTransaction) {
                        Text(
                            text = " \u2022 ${item.itemCount} item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show category for manual records
                    if (item is CashFlowItem.FromCashRecord && !item.category.isNullOrBlank()) {
                        Text(
                            text = " \u2022 ${item.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Amount
            Text(
                text = "${if (item.isIncome) "+" else "-"}${priceFormatter.format(item.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCashRecordSheet(
    formState: CashRecordFormState,
    onEvent: (CashRecordEvent) -> Unit,
    priceFormatter: NumberFormat,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Centered Title
        Text(
            text = "Tambah Catatan Manual",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Untuk pemasukan/pengeluaran di luar POS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Type Selection - LARGE Segmented Button Style
        Text(
            text = "Jenis Transaksi",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Large segmented buttons for easy touch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Standard touch target
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PEMASUKAN Button
            Surface(
                onClick = { onEvent(CashRecordEvent.SetType(CashRecordType.INCOME)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = if (formState.type == CashRecordType.INCOME) 
                    IncomeGreen.copy(alpha = 0.15f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                border = if (formState.type == CashRecordType.INCOME)
                    androidx.compose.foundation.BorderStroke(2.dp, IncomeGreen)
                else
                    null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (formState.type == CashRecordType.INCOME) 
                            IncomeGreen 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PEMASUKAN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (formState.type == CashRecordType.INCOME) 
                            IncomeGreen 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // PENGELUARAN Button
            Surface(
                onClick = { onEvent(CashRecordEvent.SetType(CashRecordType.EXPENSE)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = if (formState.type == CashRecordType.EXPENSE) 
                    ExpenseRed.copy(alpha = 0.15f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                border = if (formState.type == CashRecordType.EXPENSE)
                    androidx.compose.foundation.BorderStroke(2.dp, ExpenseRed)
                else
                    null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (formState.type == CashRecordType.EXPENSE) 
                            ExpenseRed 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PENGELUARAN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (formState.type == CashRecordType.EXPENSE) 
                            ExpenseRed 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date Selection
        val date = formState.date ?: System.currentTimeMillis()
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        var showDatePicker by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dateFormatter.format(Date(date)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Tanggal") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Pilih Tanggal")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Invisible clickable layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = date
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onEvent(CashRecordEvent.SetDate(it))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Batal")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Amount
        OutlinedTextField(
            value = formState.amount,
            onValueChange = { onEvent(CashRecordEvent.SetAmount(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Jumlah *") },
            prefix = { Text("Rp ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = formState.amountError != null,
            supportingText = formState.amountError?.let { { Text(it) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description
        OutlinedTextField(
            value = formState.description,
            onValueChange = { onEvent(CashRecordEvent.SetDescription(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Keterangan *") },
            placeholder = { Text("Contoh: Beli stok es batu") },
            isError = formState.descriptionError != null,
            supportingText = formState.descriptionError?.let { { Text(it) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Category (Dropdown)
        val categories = if (formState.type == CashRecordType.INCOME) {
            com.zatiaras.pos.core.domain.model.CashCategories.INCOME_CATEGORIES
        } else {
            com.zatiaras.pos.core.domain.model.CashCategories.EXPENSE_CATEGORIES
        }
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori (opsional)") },
                placeholder = { Text("Pilih kategori") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onEvent(CashRecordEvent.SetCategory(category))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Notes (optional)
        OutlinedTextField(
            value = formState.notes,
            onValueChange = { onEvent(CashRecordEvent.SetNotes(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Catatan (opsional)") },
            minLines = 2,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Batal")
            }
            
            Button(
                onClick = { onEvent(CashRecordEvent.SaveRecord) },
                modifier = Modifier.weight(1f),
                enabled = formState.isValid && !formState.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (formState.type == CashRecordType.INCOME) {
                        IncomeGreen
                    } else {
                        ExpenseRed
                    }
                )
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                } else {
                    Text("Simpan")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

