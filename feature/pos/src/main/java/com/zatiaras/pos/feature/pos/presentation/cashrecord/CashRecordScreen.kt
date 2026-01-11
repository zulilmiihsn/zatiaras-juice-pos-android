package com.zatiaras.pos.feature.pos.presentation.cashrecord

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
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
                        Text(
                            text = "📒",
                            style = MaterialTheme.typography.displayMedium
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
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (posTransactionCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$posTransactionCount transaksi POS",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Masuk",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = priceFormatter.format(totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                // Expense
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Keluar",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = priceFormatter.format(totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                
                // Net
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Saldo",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = priceFormatter.format(netCash),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (netCash >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
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
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                        else -> Color.Transparent
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
                        tint = Color.White
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
    val iconColor = if (item.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    
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
                        item.isIncome -> Icons.Default.TrendingUp
                        else -> Icons.Default.TrendingDown
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
                            text = " • ${item.itemCount} item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show category for manual records
                    if (item is CashFlowItem.FromCashRecord && !item.category.isNullOrBlank()) {
                        Text(
                            text = " • ${item.category}",
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
        Text(
            text = "Tambah Catatan Manual",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Untuk pemasukan/pengeluaran di luar POS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Type Selection
        Text(
            text = "Jenis",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = formState.type == CashRecordType.INCOME,
                onClick = { onEvent(CashRecordEvent.SetType(CashRecordType.INCOME)) },
                label = { Text("Pemasukan") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.weight(1f)
            )
            
            FilterChip(
                selected = formState.type == CashRecordType.EXPENSE,
                onClick = { onEvent(CashRecordEvent.SetType(CashRecordType.EXPENSE)) },
                label = { Text("Pengeluaran") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF44336).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFFF44336)
                ),
                modifier = Modifier.weight(1f)
            )
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
        
        // Category (optional)
        OutlinedTextField(
            value = formState.category,
            onValueChange = { onEvent(CashRecordEvent.SetCategory(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kategori (opsional)") },
            placeholder = { Text("Contoh: Bahan Baku, Listrik") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
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
            androidx.compose.material3.OutlinedButton(
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
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFF44336)
                    }
                )
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Simpan")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
